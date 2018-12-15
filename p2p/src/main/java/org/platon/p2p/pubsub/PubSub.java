package org.platon.p2p.pubsub;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.netty.util.internal.ConcurrentSet;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.pubsub.EntryMessage;
import org.platon.p2p.proto.pubsub.SubMessage;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-23 16:33
 */
@Component("pubSub")
public class PubSub {
    static Logger logger = LoggerFactory.getLogger(PubSub.class);

    private final Map<String, TopicValidator> topicValidators = new ConcurrentHashMap<>();
    private final Map<String, Set<Bytes>> topics = new ConcurrentHashMap<>();
    private final Map<String, Map<String, SubscribeCallback>> mytopics = new HashMap<>();
    private final TimeCache seenMessage = new TimeCache(10);


    private final Set<Bytes> peers = new HashSet<>();

    public PubSubRouter pubSubRouter = new PubSubRouter();


    @Autowired
    private RoutingTable routingTable;



    @Autowired
    private MessageRouter messageRouter;











    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }


    void registerTopicValidator(String topic, TopicValidator validator) {
        topicValidators.put(topic, validator);
    }

    void unregisterTopicValidator(String topic){
        topicValidators.remove(topic);
    }

    public void addPeer(ByteString peer) {
        synchronized (peers) {
            peers.add(Bytes.valueOf(peer));
        }
    }

    public void removePeer(ByteString peer) {
        synchronized (peers) {
            peers.remove(Bytes.valueOf(peer));
        }
        pubSubRouter.removePeer(Bytes.valueOf(peer));
    }

    public Map<String, Set<Bytes>> getTopic() {
        return topics;
    }


    
    public void subscribe(String topic, String serviceName, SubscribeCallback callback) {
        logger.trace("subscribe:{}, serviceName:{} local:{}", topic, serviceName, NodeUtils.getNodeIdString(routingTable.getLocalNode().getId()));

        boolean needAnnounce = false;

        synchronized (mytopics) {
            Map<String, SubscribeCallback> subscribeCallbackMap = mytopics.get(topic);
            if (subscribeCallbackMap != null) {
                if(!subscribeCallbackMap.containsKey(serviceName)) {
                    subscribeCallbackMap.put(serviceName, callback);

                }
            } else {
                subscribeCallbackMap = new HashMap<>();
                subscribeCallbackMap.put(serviceName, callback);
                mytopics.put(topic, subscribeCallbackMap);
                needAnnounce = true;
            }
        }

        logger.trace("needAnnounce:{}", needAnnounce);

        if (needAnnounce) {
            announce(topic, true);
            pubSubRouter.join(topic);
        }
    }


    
    private void announce(String topic, boolean sub) {

        TopicMessage.Builder topicMessageBuilder = TopicMessage.newBuilder();
        SubMessage.Builder subMessageBuilder = SubMessage.newBuilder();

        subMessageBuilder.setNodeId(routingTable.getLocalNode().getId());
        subMessageBuilder.setSub(sub);
        subMessageBuilder.setTopic(topic);

        topicMessageBuilder.setFromNodeId(routingTable.getLocalNode().getId());
        topicMessageBuilder.setSubscribe(subMessageBuilder);

        synchronized (peers) {
            CompletableFuture.runAsync(()-> {
                for (Bytes peer : peers) {
                    logger.trace("send announce:{} local:{}", NodeUtils.getNodeIdString(peer.getKey()), NodeUtils.getNodeIdString(routingTable.getLocalNode().getId()));
                    messageRouter.sendRequest(topicMessageBuilder.build(),
                            Collections.singletonList(NodeUtils.toRoutableID(peer, RoutableID.DestinationType.NODEIDTYPE)),
                            MessageRouter.ForwardingOptionType.DIRECT_CONNECTION, false);
                }
            });
        }
    }




    
    public void unSubscribe(String topic, String serviceName) {
        boolean needAnnounce = false;

        synchronized (mytopics) {
            Map<String, SubscribeCallback> subscribeCallbackMap = mytopics.get(topic);
            if (subscribeCallbackMap != null) {
                if(subscribeCallbackMap.containsKey(serviceName)) {
                    subscribeCallbackMap.remove(serviceName);
                    if (subscribeCallbackMap.isEmpty()) {
                        logger.debug("unsubscribe topic:{} and send other peers", topic);
                        needAnnounce = true;
                    }
                }
            }
        }

        if (needAnnounce) {
            announce(topic, false);
            pubSubRouter.leave(topic);
        }
    }

    
    public void publish(String topic, byte[] data) {
        logger.trace("publish:{}", topic);

        EntryMessage.Builder entryMessageBuilder = EntryMessage.newBuilder();

        entryMessageBuilder.setData(ByteString.copyFrom(data));
        entryMessageBuilder.setFromNodeId(routingTable.getLocalNode().getId());
        entryMessageBuilder.setKey(UUID.randomUUID().toString());
        entryMessageBuilder.setTopic(topic);


        pushMsg(entryMessageBuilder.build());








    }


    
    private void pushMsg(EntryMessage entryMessage) {
        logger.trace("push msg topic:{}", entryMessage.getTopic());

        String msgId = msgId(entryMessage);
        if (seenMessage.has(msgId)) {
            logger.trace("haved seen msgid{}", msgId);
            return;
        }

        seenMessage.add(msgId);
        notifyAll(entryMessage.getTopic(), entryMessage.getData().toByteArray());
        pubSubRouter.publish(Bytes.valueOf(entryMessage.getFromNodeId()), entryMessage);

    }

    public static String msgId(EntryMessage entryMessage) {
        return NodeUtils.getNodeIdString(entryMessage.getFromNodeId()) + entryMessage.getKey();
    }


    
    public void notifyAll(String topic, byte[] data) {
        Map<String, SubscribeCallback> callbackMap = mytopics.get(topic);
        if (callbackMap != null) {
            for (SubscribeCallback callback : callbackMap.values()) {
                if (callback != null) {
                    callback.subscribe(topic, data);
                }
            }
        }

    }

    public Set<Bytes> listPeers(String topic) {
        return topics.get(topic);
    }

    public interface TopicValidator {
        boolean validatorMsg(Message message);
    }

    public interface SubscribeCallback {
        void subscribe(String topic, byte[] data);
    }


    
    public void sendMessage(TopicMessage msg, HeaderHelper header){
        logger.trace("receive topic message from:{} has Subscribe:{}", NodeUtils.getNodeIdString(msg.getFromNodeId()), msg.hasSubscribe());

        if (msg.hasSubscribe()) {
            logger.trace("announce msg:{} ", msg.getSubscribe().getSub());
            if (msg.getSubscribe().getSub()){
                synchronized (topics) {
                    Set<Bytes> nodeIds = topics.get(msg.getSubscribe().getTopic());
                    if (nodeIds == null) {

                        nodeIds = new ConcurrentSet<>();
                    }
                    nodeIds.add(Bytes.valueOf(msg.getSubscribe().getNodeId()));
                    topics.put(msg.getSubscribe().getTopic(), nodeIds);
                }
            } else {
                Set<Bytes> nodeIds = topics.get(msg.getSubscribe().getTopic());
                if (nodeIds != null) {
                    nodeIds.remove(msg.getSubscribe().getNodeId());
                }
            }
        }

        if (msg.getPublishedEntryCount() != 0) {
            for (EntryMessage entryMessage : msg.getPublishedEntryList()){
                logger.trace("receive remote:{} have publish message:{}", NodeUtils.getNodeIdString(entryMessage.getFromNodeId()), entryMessage.getKey());
                pushMsg(entryMessage);
            }
        }

        pubSubRouter.handleControl(Bytes.valueOf(msg.getFromNodeId()), msg.getControl());
    }

    
    public boolean isSubscribe(String topic){
        synchronized (mytopics) {
            return mytopics.containsKey(topic);
        }
    }
}
