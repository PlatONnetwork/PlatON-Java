package org.platon.p2p.router;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.platon.p2p.ForwardMessageHook;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.common.PlatonMessageHelper;
import org.platon.p2p.plugins.TopologyPlugin;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.session.Session;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * @author yangzhou
 * @create 2018-04-26 16:55
 */
@Component("messageRouter")
public class MessageRouter {

    private static Logger logger = LoggerFactory.getLogger(MessageRouter.class);

    @Autowired
    private TopologyPlugin topologyPlugin;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RequestManager requestManager;

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public void setTopologyPlugin(TopologyPlugin topologyPlugin) {
        this.topologyPlugin = topologyPlugin;
    }

    
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    
    public enum ForwardingOptionType {
        
        UNKNOWN_OPTION((byte)0),
        

        DIRECT_CONNECTION((byte)1),
        
        FORWARD_CONNECTION((byte)2),

        
        BROADCAST_CONNECTION((byte)3);

        final byte code;
        private ForwardingOptionType(byte code) {
            this.code = code;
        }

        public static ForwardingOptionType valueOf(byte code) {
            for (ForwardingOptionType t : EnumSet.allOf(ForwardingOptionType.class)) {
                if (t.code == code) {
                    return t;
                }
            }
            return UNKNOWN_OPTION;
        }
    }


    public CompletableFuture<Message> sendRequest(Message msg, NodeID dest, ForwardingOptionType type, boolean isReturn) {
        List<RoutableID> destList = new ArrayList<>();
        RoutableID routableID = RoutableID.newBuilder().setId(dest.getId()).setType(RoutableID.DestinationType.NODEIDTYPE).build();
        destList.add(routableID);
        return this.sendRequest(msg, destList, type, isReturn);
    }

    public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, ForwardingOptionType type, boolean isReturn) {

        String transactionID = UUID.randomUUID().toString();




        Header header = Header.newBuilder().setTxId(transactionID)
                .addVia(topologyPlugin.getRoutingTable().getLocalNode())
                .addAllDest(dest)
                .setTtl(30)
                .setMsgType(msg.getClass().getSimpleName())
                .build();


        if (isReturn) {
            final CompletableFuture<Message> msgFut = new CompletableFuture<Message>();
            requestManager.put(transactionID, msgFut);
            sendMessage(msg, header, dest, type);

            return msgFut;
        }
        sendMessage(msg, header, dest, type);



        return null;
    }

    public void sendResponse(Message msg, String transactionID, List<RoutableID> dest, ForwardingOptionType type) {
        Header header = Header.newBuilder().setTxId(transactionID)
                .addVia(topologyPlugin.getRoutingTable().getLocalNode())
                .addAllDest(dest)
                .setTtl(30)
                .setMsgType(msg.getClass().getSimpleName())
                .build();

        sendMessage(msg, header, dest, type);
    }

    private class CreateSessionConsumer implements Consumer<Session> {
        private PlatonMessage platonMessage;
        private RoutableID origDestNode;
        private ForwardingOptionType type;
        private AtomicInteger counter;

        public CreateSessionConsumer(PlatonMessage platonMessage, RoutableID origDestNode, ForwardingOptionType type, int counter) {
            this.platonMessage = platonMessage;
            this.origDestNode = origDestNode;
            this.type = type;
            this.counter = new AtomicInteger(counter);
        }

        @Override
        public void accept(Session session) {

            logger.debug("开始消费sessionFuture回调...");

            if (type == ForwardingOptionType.DIRECT_CONNECTION) {
                if (session == null) {
                    logger.warn("Cannot connect to dest node, fail to send message to it directly.");
                } else {
                    sendMessage(session, platonMessage);
                }
            } else if (type == ForwardingOptionType.FORWARD_CONNECTION) {
                int count = counter.decrementAndGet();
                if (session == null) {
                    if (count == 0) {

                        topologyPlugin.query(origDestNode);

                        session = sessionManager.getSessionRandom();
                        sendMessage(session, platonMessage);
                    }
                } else {
                    sendMessage(session, platonMessage);
                }
            }
        }
    }


    private void sendMessage(Message msg, Header header, List<RoutableID> dest, ForwardingOptionType type){

        Any any = Any.pack(msg);
        PlatonMessage platonMessage = PlatonMessage.newBuilder().setHeader(header).setBody(org.platon.p2p.proto.platon.Body.newBuilder().setData(any)).build();

        String txId = header.getTxId();
        if (type == ForwardingOptionType.DIRECT_CONNECTION) {
            logger.debug("DIRECT_CONNECTION message:{}", platonMessage);
            assert (dest.size() == 1);
            for (RoutableID routableID : dest) {
                if (routableID.getType() == RoutableID.DestinationType.NODEIDTYPE) {
                    logger.debug("DIRECT_CONNECTION:Get session from Node:{}", CodecUtils.toHexString(routableID.getId()));

                    CompletableFuture<Session> sessionFut = sessionManager.getSession(routableID.getId(), true);

                    sessionFut.thenAcceptAsync(
                            new CreateSessionConsumer(platonMessage, routableID, ForwardingOptionType.DIRECT_CONNECTION, 1),
                            NodeContext.executor).exceptionally(throwable -> {
                        handleException(txId, throwable);
                        return null;
                    });
                }
            }
        } else if (type == ForwardingOptionType.FORWARD_CONNECTION || type == ForwardingOptionType.BROADCAST_CONNECTION) {
            Asserts.check(!dest.isEmpty(), "Destination list is empty");
            RoutableID destNode = dest.get(0);
            List<NodeID> nextNodes = null;
            if(type == ForwardingOptionType.FORWARD_CONNECTION) {
                logger.debug("FORWARD_CONNECTION message:{}", platonMessage);
                nextNodes = topologyPlugin.getRoutingTable().getNextHops(destNode);
            }else{
                logger.debug("BROADCAST_CONNECTION message:{}", platonMessage);
                nextNodes = topologyPlugin.getBroadCastNode(destNode);
            }
            logger.debug("nextNodes:::::::::{}", nextNodes.size() );
            if(nextNodes!=null && nextNodes.size()>0){
                Consumer<Session> consumer = new CreateSessionConsumer(platonMessage, destNode, ForwardingOptionType.FORWARD_CONNECTION, nextNodes.size());
                for (NodeID next : nextNodes) {
                    if(next.getId().equals(NodeContext.localNodeId)){

                        continue;
                    }

                    CompletableFuture<Session> sessionFut = sessionManager.getSession(next.getId(), false);

                    sessionFut.thenAcceptAsync(consumer, NodeContext.executor)
                            .exceptionally(throwable -> {


                                logger.error("Exception", throwable);
                        return null;
                    });

                }
            }
        } else {
            return;
        }
    }



    public void forwardPlatonMessage(Header header, Any any, ByteString localNodeId){

        header = PlatonMessageHelper.renewHeader(header, localNodeId);

        PlatonMessage platonMessage = PlatonMessage.newBuilder().setHeader(header).setBody(org.platon.p2p.proto.platon.Body.newBuilder().setData(any)).build();



        List<NodeID> nextHops = ForwardMessageHook.nextHops(header, any);
        if (nextHops == null || nextHops.isEmpty()) {
            nextHops = topologyPlugin.getRoutingTable().getNextHops(header.getDest(0));
        }

        logger.debug("forward message:{} to next hops:{}", platonMessage, nextHops);

        AtomicInteger counter = new AtomicInteger(nextHops.size());
        for (NodeID nodeId : nextHops) {

            if (PlatonMessageHelper.viaed(header.getViaList(), localNodeId)) {
                continue;
            }
            sendMessage(nodeId, platonMessage, false);
        }
    }


    private void sendMessage(NodeID nodeId, PlatonMessage platonMessage, boolean needAttach){
        logger.debug("send message:{} to node:={}", platonMessage, nodeId);
        CompletableFuture<Session> getSessionFuture = sessionManager.getSession(nodeId.getId(), needAttach);
        getSessionFuture.thenAcceptAsync(session -> {
            if (session != null) {
                sendMessage(session, platonMessage);
            }else{





            }
        }, NodeContext.executor);
    }

    private void sendMessage(Session session, PlatonMessage platonMessage){
        if(session!=null && session.getConnection()!=null && session.getConnection().isActive()){
            logger.debug("session:{}", session);
            session.getConnection().writeAndFlush(platonMessage);
        }
    }


    

    public void handleResponse(String id, Message msg) {
        requestManager.handleResponse(id, msg);
    }

    public void handleException(String id, Throwable throwable) {
        requestManager.handleException(id, throwable);
    }



    






















}
