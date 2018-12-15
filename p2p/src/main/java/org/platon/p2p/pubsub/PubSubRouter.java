package org.platon.p2p.pubsub;

import org.apache.http.util.Asserts;
import org.platon.common.cache.DelayCache;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.pubsub.*;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yangzhou
 * @create 2018-07-23 17:06
 */
public class PubSubRouter {

    private static Logger logger = LoggerFactory.getLogger(PubSubRouter.class);

    private static final int SubHeartbeatInterval = 5000;
    private static final int SubCount = 6;
    private static final int MsgCacheTime = 1000000;
    private static final int FantOutTTL = 60 * 1000;
    private Map<String, Set<Bytes>> mesh = new ConcurrentHashMap<>();
    private Map<String, Set<Bytes>> fanout = new ConcurrentHashMap<>();
    private Map<String, Long> lastpub = new ConcurrentHashMap<>();

    private Map<Bytes, Map<String, IHaveMessage>> unSendMessage = new ConcurrentHashMap<>();
    private MessageIdCache messageIdCache = new MessageIdCache();
    private DelayCache<String, EntryMessage> msgCache = new DelayCache<>();
    private PubSub pubSub;

    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    private RoutingTable routingTable;

    public PubSubRouter() {
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    public void attach(PubSub pubSub) {
        this.pubSub = pubSub;
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable(){

            @Override
            public void run() {
                heartbeat();

            }
        }, SubHeartbeatInterval, SubHeartbeatInterval, TimeUnit.MILLISECONDS);

    }







    
    public void removePeer(Bytes peer) {

        for (Set<Bytes> peers : mesh.values()) {
            peers.remove(peer);
        }

        for (Set<Bytes> peers : fanout.values()) {
            peers.remove(peer);
        }
    }


    
    public void publish(Bytes from, EntryMessage msg) {
        logger.trace("publish from:{}", NodeUtils.getNodeIdString(from.getKey()));

        String msgId = PubSub.msgId(msg);
        messageIdCache.add(msg.getTopic(), msgId);
        msgCache.put(msgId, msg, MsgCacheTime, TimeUnit.MILLISECONDS);

        String topic = msg.getTopic();
        Set<Bytes> nodeIds = mesh.get(topic);
        if (nodeIds == null || nodeIds.isEmpty()) {

            nodeIds = fanout.get(topic);
            if (nodeIds == null || nodeIds.isEmpty()) {

                nodeIds = getPeers(topic, SubCount);
                if (nodeIds != null && !nodeIds.isEmpty()) {
                    fanout.put(topic, nodeIds);
                }
            }
            lastpub.put(topic, System.currentTimeMillis());
        }

        if (nodeIds != null && !nodeIds.isEmpty()) {
            for (Bytes peer : nodeIds) {
                TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();

                topicMessageBuilder.addPublishedEntry(msg);

                sendMessage(peer, topicMessageBuilder);
            }
        }
    }


    
    private void sendMessage(Bytes peer, TopicMessage.Builder topicMessageBuilder) {

        logger.trace("send peer:{}", NodeUtils.getNodeIdString(peer.getKey()));
        CompletableFuture.runAsync(()-> {
            try {

                Map<String, IHaveMessage> iHaveMessageMap = unSendMessage.get(peer);

                if (iHaveMessageMap != null) {

                    for (Map.Entry<String, IHaveMessage> entry : iHaveMessageMap.entrySet()) {

                        topicMessageBuilder.getControlBuilder().addIhave(entry.getValue());
                        unSendMessage.remove(peer);
                    }
                }

                logger.trace("send message local:{} -> remote:{}", NodeUtils.getNodeIdString(routingTable.getLocalNode().getId()), NodeUtils.getNodeIdString(peer.getKey()));

                messageRouter.sendRequest(topicMessageBuilder.build(),
                        Collections.singletonList(NodeUtils.toRoutableID(peer, RoutableID.DestinationType.NODEIDTYPE)),
                        MessageRouter.ForwardingOptionType.DIRECT_CONNECTION, false);
            }catch (Exception e) {
                logger.error("error:", e);
            }
        });
    }



    
    private Set<Bytes> getPeers(String topic, int count) {
        Map<String, Set<Bytes>> topics = pubSub.getTopic();
        Set<Bytes> peers = topics.get(topic);
        if (peers == null || peers.isEmpty()) {
            return null;
        }

        return shufflePeers(peers, count);
    }



    
    private Set<Bytes> shufflePeers(Set<Bytes> peers, int count) {
        Set<Bytes> nodeIDSet = ConcurrentHashMap.newKeySet();

        Asserts.notNull(peers, "peers is null");
        if (peers.size() < count) {
            nodeIDSet.addAll(peers);
            return nodeIDSet;
        }

        List<Bytes> list = new ArrayList(peers);
        for (int i = 0; i < peers.size(); i++) {
            Random random = new Random();
            int j = random.nextInt() % (i+1);
            Collections.swap(list, i, j);
        }

        nodeIDSet.addAll(list);
        return  nodeIDSet;
    }


    
    public void handleControl(Bytes from, ControlMessage msg){
        if (msg == null) {
            logger.trace("receive control message is null");
            return;
        }

        List<IWantMessage> iwant = handleIHave(msg.getIhaveList());
        List<EntryMessage> ihave = handleIWant(msg.getIwantList());
        PruneMessage prune = handleGraft(msg.getGraft());
        handlePrune(msg.getPrune());

        logger.trace("iwant is {}, ihave is {} prune is {}",
                iwant == null ? "none":"have",
                ihave == null ? "none":"have",
                prune == null ? "none":"have");

        if (iwant == null && ihave == null && prune == null) {

            return;
        }




        TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();

        if (ihave != null) {
            topicMessageBuilder.addAllPublishedEntry(ihave);
        }

        ControlMessage.Builder controlMessageBuilder = ControlMessage.newBuilder();
        if (iwant != null) {
            controlMessageBuilder.addAllIwant(iwant);
        }

        if (prune != null) {
            controlMessageBuilder.setPrune(prune);
        }

        topicMessageBuilder.setControl(controlMessageBuilder);

        sendMessage(from, topicMessageBuilder);

    }

    
    private List<IWantMessage> handleIHave(List<IHaveMessage> msg) {
        if (msg == null) {
            logger.trace("IHaveMessage is null");
            return null;
        }

        List<IWantMessage> iWantMessageList = new LinkedList<>();
        for (IHaveMessage ihave : msg) {
            logger.trace("ihave:getTopic:{}, nodeId size:{}  mesh size:{}", ihave.getTopic(), ihave.getMessageIdList().size(), mesh.size());
            if (!mesh.containsKey(ihave.getTopic())) {
                continue;
            }

            IWantMessage.Builder iWantMessageBuild = IWantMessage.newBuilder();

            iWantMessageBuild.setTopic(ihave.getTopic());

            for (String messageID : ihave.getMessageIdList()) {
                EntryMessage entryMessage = msgCache.get(messageID);
                if (entryMessage == null) {
                    iWantMessageBuild.addMessageId(messageID);
                }
            }

            iWantMessageList.add(iWantMessageBuild.build());
        }

        if (iWantMessageList.isEmpty()) {
            return null;
        }
        return iWantMessageList;
    }


    
    private List<EntryMessage> handleIWant(List<IWantMessage> msg) {
        if (msg == null) {
            logger.trace("IWantMessage is null");
            return null;
        }
        List<EntryMessage> entryMessageList = new LinkedList<>();
        for (IWantMessage iwant : msg) {
            if(!mesh.containsKey(iwant.getTopic())){
                continue;
            }
            for(String messageID : iwant.getMessageIdList()) {
                EntryMessage entryMessage = msgCache.get(messageID);

                if (entryMessage != null) {
                    entryMessageList.add(entryMessage);
                }
            }
        }
        if (entryMessageList.isEmpty()) {
            return null;
        }
        return entryMessageList;
    }

    
    private PruneMessage handleGraft(GraftMessage msg) {
        logger.trace("handle graft");

        if (msg == null) {
            logger.trace("graft is null");
            return null;
        }



        List<String> pruneTopics = new LinkedList<>();

        for (String topic : msg.getTopicList()) {
            Set<Bytes> peers = mesh.get(topic);
            if (peers == null) {
                pruneTopics.add(topic);
            } else {
                peers.add(Bytes.valueOf(msg.getNodeId()));
            }
        }

        if (pruneTopics.isEmpty()) {
            return null;
        }

        PruneMessage.Builder pruneMessageBuilder = PruneMessage.newBuilder();
        pruneMessageBuilder.addAllTopic(pruneTopics);
        pruneMessageBuilder.setNodeId(routingTable.getLocalNode().getId());

        return pruneMessageBuilder.build();
    }



    
    private void handlePrune(PruneMessage msg) {
        if (msg == null) {
            logger.trace("prune message is null");
            return;
        }
        for (String topic : msg.getTopicList()) {
            logger.trace("handle pruned nodeid:{} topic:{}", NodeUtils.getNodeIdString(msg.getNodeId()), topic);
            Set<Bytes> peers = mesh.get(topic);
            if (peers != null) {
                logger.trace("handle pruned removeSessionFuture:{}", NodeUtils.getNodeIdString(msg.getNodeId()));
                peers.remove(Bytes.valueOf(msg.getNodeId()));
            }
        }
    }


    
    private void sendGraft(Bytes peer, String topic) {
        logger.trace("send graft:{} topic:{}", NodeUtils.getNodeIdString(peer.getKey()), topic);



        TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();

        ControlMessage.Builder controlMessageBuilder = ControlMessage.newBuilder();
        GraftMessage.Builder graftMessageBuilder = GraftMessage.newBuilder();

        graftMessageBuilder.setNodeId(routingTable.getLocalNode().getId());
        graftMessageBuilder.addTopic(topic);

        controlMessageBuilder.setGraft(graftMessageBuilder);
        topicMessageBuilder.setControl(controlMessageBuilder);

        sendMessage(peer, topicMessageBuilder);
    }


    
    private void sendPrune(Bytes peer, String topic) {
        logger.trace("send Prune topic:{}", topic);


        TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();

        ControlMessage.Builder controlMessageBuilder = ControlMessage.newBuilder();
        PruneMessage.Builder pruneMessageBuilder = PruneMessage.newBuilder();

        pruneMessageBuilder.setNodeId(routingTable.getLocalNode().getId());
        pruneMessageBuilder.addTopic(topic);

        controlMessageBuilder.setPrune(pruneMessageBuilder);
        topicMessageBuilder.setControl(controlMessageBuilder);

        sendMessage(peer, topicMessageBuilder);
    }


    
    public void join(String topic) {
        logger.trace("join topic:{}", topic);
        if (mesh.containsKey(topic)) {
            return;
        }

        Set<Bytes> nodeIds = fanout.get(topic);
        if (nodeIds != null && !nodeIds.isEmpty()) {
            mesh.put(topic, nodeIds);
            fanout.remove(topic);
        } else {
            nodeIds = getPeers(topic, SubCount);
            if (nodeIds == null){
                nodeIds = ConcurrentHashMap.newKeySet();
            }
            mesh.put(topic, nodeIds);
        }

        logger.trace("mesh size:{} nodeIds size:{}", mesh.size(), nodeIds.size());
        for (Bytes peer : nodeIds) {
            logger.trace("send graft to :{}", peer);
            sendGraft(peer, topic);
        }
    }


    
    public void leave(String topic) {
        logger.trace("leave topic:{}" , topic);

        Set<Bytes> peers = mesh.remove(topic);
        if (peers != null) {
            logger.trace("send prunce:{}", topic);
            for (Bytes peer : peers) {

                sendPrune(peer, topic);
            }
        }
    }



    
    public void heartbeat() {
        logger.trace("heartbeat..........");

        Map<Bytes, List<String>> toGraft = new HashMap<>();
        Map<Bytes, List<String>> toPrune = new HashMap<>();

        logger.trace("msgCache:{}, pubsub:{} mesh size:{} hashcode:{} fanout size:{}", messageIdCache.dump(), System.identityHashCode(this), mesh.size(), System.identityHashCode(mesh), fanout.size());

        for (Map.Entry<String, Set<Bytes>> topicPeers : mesh.entrySet()) {
            String topic = topicPeers.getKey();
            Set<Bytes> peers = topicPeers.getValue();

            if (peers.size() > SubCount) {
                logger.trace("topic:{} mesh peers size > {}", topic, SubCount);

                int count = peers.size() - SubCount;
                Set<Bytes> plst = shufflePeers(peers, count);

                for (Bytes peer : plst) {
                    List<String> topics = toPrune.getOrDefault(peer, new LinkedList<>());
                    topics.add(topic);
                    toPrune.putIfAbsent(peer, topics);
                }
            } else if (peers.size() < SubCount){
                logger.trace("topic:{} mesh peers size < {}", topic, SubCount);
                if (mesh.size() == 1) {
                    logger.trace("mesh size:{} fanout size:{}", mesh.size(), fanout.size());
                }
                try {


                    int count = SubCount - peers.size();
                    Set<Bytes> plst = getPeers(topic, count);

                    if (plst != null) {
                        peers.addAll(plst);

                        for (Bytes peer : plst) {
                            List<String> topics = toGraft.getOrDefault(peer, new LinkedList<>());
                            topics.add(topic);
                            toGraft.putIfAbsent(peer, topics);
                        }
                    }
                }catch (Exception e) {
                    logger.error("error:", e);
                }
            }
            logger.trace("mesh size:{} fanout size:{}", mesh.size(), fanout.size());
            emitMessage(topic, peers);
            logger.trace("mesh size:{} fanout size:{}", mesh.size(), fanout.size());
        }

        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Long>> it = lastpub.entrySet().iterator(); it.hasNext();){
            Map.Entry<String, Long> item = it.next();
            if (item.getValue() - now > FantOutTTL) {
                fanout.remove(item.getKey());
                it.remove();
            }
        }




        for (Map.Entry<String, Set<Bytes>> topicPeers : fanout.entrySet()) {
            String topic = topicPeers.getKey();
            Set<Bytes> peers = topicPeers.getValue();

            if (peers.size() < SubCount){

                Set<Bytes> plst = getPeers(topic, SubCount);
                if (plst != null) {
                    peers.addAll(plst);
                    for (Bytes peer : plst) {
                        emitMessage(topic, peers);
                    }
                }
            }
        }
        logger.trace("mesh size:{} fanout size:{}", mesh.size(), fanout.size());
        for (Map.Entry<Bytes, List<String>> graft : toGraft.entrySet()) {
            logger.trace("graft:{}", NodeUtils.getNodeIdString(graft.getKey().getKey()));
        }
        sendGraftAndPrune(toGraft, toPrune);
        messageIdCache.sweep();
        logger.trace("mesh size:{} fanout size:{}", mesh.size(), fanout.size());
    }

    
    private void sendGraftAndPrune(Map<Bytes, List<String>> toGraft, Map<Bytes, List<String>> toPrune) {
        logger.trace("toGraft size:{}, toPrune size:{}", toGraft.size(), toPrune.size());


        CompletableFuture.runAsync(()->{
           for (Map.Entry<Bytes, List<String>> graft : toGraft.entrySet()) {
               try {
                   logger.trace("local:{} graft:{}", NodeUtils.getNodeIdString(routingTable.getLocalNode().getId().toByteArray()), NodeUtils.getNodeIdString(graft.getKey().getKey()));
                   TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();
                   ControlMessage.Builder controlMessageBuilder = ControlMessage.newBuilder();

                   GraftMessage.Builder graftMessageBuilder = GraftMessage.newBuilder();
                   graftMessageBuilder.addAllTopic(graft.getValue());
                   graftMessageBuilder.setNodeId(routingTable.getLocalNode().getId());

                   List<String> pruneTopic = toPrune.get(graft.getKey());

                   if (pruneTopic != null) {
                       PruneMessage.Builder pruneMessageBuilder = PruneMessage.newBuilder();
                       pruneMessageBuilder.setNodeId(routingTable.getLocalNode().getId());
                       pruneMessageBuilder.addAllTopic(pruneTopic);
                       toPrune.remove(graft.getKey());
                       controlMessageBuilder.setPrune(pruneMessageBuilder);
                   }

                   controlMessageBuilder.setGraft(graftMessageBuilder);

                   topicMessageBuilder.setControl(controlMessageBuilder);
                   sendMessage(graft.getKey(), topicMessageBuilder);
               } catch (Exception e) {
                   logger.error("send graft error:", e);
               }
           }

           for (Map.Entry<Bytes, List<String>> prune : toPrune.entrySet()) {
                TopicMessage.Builder topicMessageBuilder = newTopicMessageBuilder();
               ControlMessage.Builder controlMessageBuilder = ControlMessage.newBuilder();

               PruneMessage.Builder pruneMessageBuilder = PruneMessage.newBuilder();

               pruneMessageBuilder.setNodeId(routingTable.getLocalNode().getId());
               pruneMessageBuilder.addAllTopic(prune.getValue());



               controlMessageBuilder.setPrune(pruneMessageBuilder);
               topicMessageBuilder.setControl(controlMessageBuilder);
               sendMessage(prune.getKey(), topicMessageBuilder);
            }

        });
    }

    
    private void emitMessage(String topic, Set<Bytes> peers) {
        logger.trace("emit peers:{}", peers.size());
        for (Bytes peer : peers) {
            Set<String> msgIds = messageIdCache.get(topic);

            logger.trace("emit message nodeid:{} msg nodeId size:{}", NodeUtils.getNodeIdString(peer.getKey()), msgIds.size());

            Map<String, IHaveMessage> unSendList = unSendMessage.getOrDefault(peer, new HashMap<>());
            IHaveMessage iHaveMessage = unSendList.getOrDefault(topic, IHaveMessage.newBuilder().build());
            iHaveMessage = iHaveMessage.toBuilder().setTopic(topic).addAllMessageId(msgIds).build();

            unSendList.putIfAbsent(topic, iHaveMessage);

            unSendMessage.putIfAbsent(peer, unSendList);

        }
    }


    private TopicMessage.Builder newTopicMessageBuilder() {

        TopicMessage.Builder topicMessageBuilder = TopicMessage.newBuilder();
        topicMessageBuilder.setFromNodeId(routingTable.getLocalNode().getId());
        return topicMessageBuilder;
    }



}
