package org.platon.p2p.session;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.platon.common.cache.DelayCache;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.*;
import org.platon.p2p.attach.LinkController;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.common.PeerConfig;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.plugins.TopologyPlugin;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.session.SayHello;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SignatureException;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/4/28, lvxiaoyi, Initial Version.
 */
@Component("sessionManager")
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    @Autowired
    private LinkController linkController;

    @Autowired
    private TopologyPlugin topologyPlugin;

    @Autowired
    private RoutingTable routingTable;

    @Autowired
    private MessageRouter messageRouter;

    private int createSessionTimeoutInSeconds = PeerConfig.getInstance().getCreateSessionTimeout();

    private static DelayCache.TimeoutCallbackFunction<ByteString, CompletableFuture<Session>> createSessionFutureTimeoutCallback = new DelayCache.TimeoutCallbackFunction<ByteString, CompletableFuture<Session>>() {

        @Override
        public void timeout(ByteString remoteNodeId, CompletableFuture<Session> sessionFuture) {
            logger.error("cannot connect to remote Node Id:={}", CodecUtils.toHexString(remoteNodeId));

            sessionFuture.completeExceptionally(new Exception(String.format("cannot connect to remote Node Id:=%s", CodecUtils.toHexString(remoteNodeId))));
        }
    };


    private static DelayCache<ByteString, CompletableFuture<Session>> createSessionFutureCache = new DelayCache<>(createSessionFutureTimeoutCallback);

    public void addSessionFuture(ByteString remoteNodeId, CompletableFuture<Session> sessionFuture){
        logger.debug("===addSessionFuture, remoteNodeId:={}", CodecUtils.toHexString(remoteNodeId));

        createSessionFutureCache.put(remoteNodeId, sessionFuture, createSessionTimeoutInSeconds, TimeUnit.SECONDS);
    }

    public CompletableFuture<Session> removeSessionFuture(ByteString remoteNodeId){
        logger.debug("===removeSessionFuture, remoteNodeId:={}", CodecUtils.toHexString(remoteNodeId));

        return createSessionFutureCache.remove(remoteNodeId);
    }

    public void completeSessionFutureExceptionally(ByteString remoteNodeId, Throwable e){
        logger.debug("===completeSessionFutureExceptionally, remoteNodeId:={}", CodecUtils.toHexString(remoteNodeId));

        CompletableFuture<Session> sessionFuture = createSessionFutureCache.remove(remoteNodeId);
        sessionFuture.completeExceptionally(e);
    }



    private static ConcurrentMap<ByteString, Session> sessionMap = new ConcurrentHashMap<>();

    public Session getSession(ByteString nodeId){
        return sessionMap.get(nodeId);
    }

    public LinkController getLinkController() {
        return linkController;
    }

    public void setLinkController(LinkController linkController) {
        this.linkController = linkController;
    }

    public TopologyPlugin getTopologyPlugin() {
        return topologyPlugin;
    }

    public void setTopologyPlugin(TopologyPlugin topologyPlugin) {
        this.topologyPlugin = topologyPlugin;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }


    public void createSession(ByteString remoteNodeId, ByteString remoteNodePubKey,  Channel channel){
        Session session = new Session();
        session.setRemoteNodeId(remoteNodeId);
        session.setConnection(channel);
        session.setTimestamp(new Date());
        sessionMap.putIfAbsent(remoteNodeId, session);


        CompletableFuture<Session>  sessionCompletableFuture = this.removeSessionFuture(remoteNodeId);
        if(sessionCompletableFuture!=null){
            sessionCompletableFuture.complete(session);
        }
    }

    
    public void handleCreateSessionRequest(ByteString remoteNodeId, String remoteEndpoint, ByteString messageHash, ByteString signature, Channel channel){
        logger.trace("entry acceptConnect(): {}, {}", remoteNodeId, remoteEndpoint);

        Session session = sessionMap.get(remoteNodeId);
        try {
            if (session == null) {


                byte[] pubKey = new byte[0];
                try {
                    pubKey = WalletUtil.signatureToPubKeyBytes(messageHash.toByteArray(), signature.toByteArray());
                } catch (SignatureException e) {



                    channel.close();
                    throw new Exception("Verify signature error", e);
                }

                byte[] tempRemoteNodeId = WalletUtil.computeAddress(pubKey);

                if (!ByteUtils.equals(remoteNodeId.toByteArray(), tempRemoteNodeId)) {
                    logger.error("remote node id error");
                    channel.close();
                    throw new Exception("remote node id error");

                }


                ((EccDecoder)channel.pipeline().get("eccDecoder")).setRemoteNodeId(remoteNodeId);
                ((EccEncoder)channel.pipeline().get("eccEncoder")).setRemoteNodeId(remoteNodeId);
                ((EccEncoder)channel.pipeline().get("eccEncoder")).setRemoteNodePubKey(ByteString.copyFrom(pubKey));
                ((NodeServerChannelHandler)channel.pipeline().get("nodeServerChannelHandler")).setRemoteNodeId(remoteNodeId);

                session = new Session();
                session.setConnection(channel);
                session.setTimestamp(new Date());



                sessionMap.put(remoteNodeId, session);
                SessionNotify.createNotify(remoteNodeId);


                NodeID remoteNode = NodeID.newBuilder().setId(remoteNodeId).setEndpoint(remoteEndpoint).setPubKey(ByteString.copyFrom(pubKey)).build();
                NodeID temp = routingTable.getNodeID(remoteNodeId);
                if (temp == null) {
                    routingTable.add(remoteNode);
                }
            }
        }catch (Exception e){
            logger.error("Exception:", e);
        }


        CompletableFuture<Session> sessionFuture = this.removeSessionFuture(remoteNodeId);
        if(sessionFuture!=null){
            logger.debug("回调sessionFuture.complete(session)");
            sessionFuture.complete(session);
        }
        logger.trace("exit acceptConnect()");
    }


    
    public void refreshSession(String nodeId){
        Session session = sessionMap.get(nodeId);
        if(session!=null){
            session.refresh();
        }
    }

    
    public void closeSession(ByteString remoteNodeId){

        Session session = sessionMap.remove(remoteNodeId);
        if(session!=null){

            session.destroy();
            session=null;
        }
        SessionNotify.closeNotify(remoteNodeId);
    }


    
    public CompletableFuture<Session> getSession(ByteString remoteNodeId, boolean needAttach){
        final CompletableFuture<Session> sessionFuture = new CompletableFuture<>();

        logger.debug("to get session for {}, attach flat:{}", CodecUtils.toHexString(remoteNodeId), needAttach);
        Session session = sessionMap.get(remoteNodeId);
        try {
            if (session != null) {
                logger.debug("session exists.");
                sessionFuture.complete(session);
            }else {
                logger.debug("no session exists.");
                CompletableFuture<Session> sessionFutureExisting = createSessionFutureCache.get(remoteNodeId);
                if(sessionFutureExisting!=null){
                    logger.debug("session future exists, just return this future.");
                    return sessionFutureExisting;
                }

                logger.debug("try to create session.");


                NodeID remoteNode = topologyPlugin.getRoutingTable().getNodeID(remoteNodeId);
                if (remoteNode != null
                        && remoteNode.getId().equals(remoteNodeId)
                        && StringUtils.isNotBlank(remoteNode.getEndpoint())
                        && remoteNode.getPubKey()!=null
                        && !remoteNode.getPubKey().isEmpty()) {
                    logger.debug("try connecting to remote node id:={}, endpoint:={}", CodecUtils.toHexString(remoteNode.getId()), remoteNode.getEndpoint());


                    this.addSessionFuture(remoteNodeId, sessionFuture);


                    CompletableFuture<Boolean> connectFuture = connect(remoteNode.getId(), remoteNode.getEndpoint(), remoteNode.getPubKey());
                    connectFuture.thenAcceptAsync(success->{
                        if(!success){
                            if (!needAttach) {
                                logger.debug("cannot connect to remote node, and needn't try to attach it.");
                                this.removeSessionFuture(remoteNodeId);
                                sessionFuture.complete(null);
                            } else {
                                CompletableFuture<NodeID> attachFuture = linkController.attach(remoteNodeId);
                                attachFuture.thenAcceptAsync(new AttachConsumer(false), NodeContext.executor)
                                        .exceptionally(e -> {
                                            logger.error("exception in attaching", e);
                                            this.completeSessionFutureExceptionally(remoteNodeId, e);
                                            sessionFuture.complete(null);

                                            return null;
                                        });
                            }
                        }
                    });
                }else{
                    logger.warn("there's no detailed info of the remote node.");
                    if (needAttach){
                        this.addSessionFuture(remoteNodeId, sessionFuture);

                        CompletableFuture<NodeID> attachFuture = linkController.attach(remoteNodeId);
                        attachFuture.thenAcceptAsync(new AttachConsumer(true), NodeContext.executor)
                                .exceptionally(e -> {
                                    logger.error("exception in attaching", e);
                                    this.completeSessionFutureExceptionally(remoteNodeId, e);
                                    return null;
                                });
                    }else{
                        sessionFuture.complete(null);
                    }
                }
            }
        }catch (Exception e){
            sessionFuture.completeExceptionally(e);
        }
        return sessionFuture;
    }

    public void sayHello(ByteString nodeId, String hello, boolean feedback) {
        logger.debug("received message:={} from:={}", hello, CodecUtils.toHexString(nodeId));
        if(feedback){


            SayHello sayHello = SayHello.newBuilder().setNodeId(NodeContext.localNodeId).setHello("FeedBack:"+hello).build();

            NodeID remoteNodeId = NodeID.newBuilder().setId(nodeId).build();

            messageRouter.sendRequest(sayHello, remoteNodeId, MessageRouter.ForwardingOptionType.DIRECT_CONNECTION, false);
        }
    }

    private class AttachConsumer implements Consumer<NodeID> {
        private boolean tryAgain;

        public AttachConsumer(boolean tryAgain) {
            this.tryAgain = tryAgain;
        }

        @Override
        public void accept(NodeID remoteNode) {
            if(remoteNode!=null) {
                logger.debug("retrieved the remote node info, remoteNode:= {})", remoteNode.toString());
                if(tryAgain) {
                    connect(remoteNode.getId(), remoteNode.getEndpoint(), remoteNode.getPubKey());
                }
            }
        }
    }



    
    public Session getSessionRandom(){
        Set<Map.Entry<ByteString, Session>> entrySet = sessionMap.entrySet();
        int size = entrySet.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for(Map.Entry<ByteString, Session> entry : entrySet){
            if (i == item) {
                return entry.getValue();
            }
            i++;
        }
        return null;
    }


    
    private CompletableFuture<Boolean> connect(ByteString remoteNodeId, String remoteEndpoint, ByteString remoteNodePubKey){
        return CompletableFuture.supplyAsync(()-> {
            String[] addresses = remoteEndpoint.split(":");
            return new NodeClient().connect(remoteNodeId, addresses[0], Integer.parseInt(addresses[1]), remoteNodePubKey);
        }, NodeContext.executor);
    }
}
