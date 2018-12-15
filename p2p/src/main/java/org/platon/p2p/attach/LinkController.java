package org.platon.p2p.attach;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.plugins.TopologyPlugin;
import org.platon.p2p.proto.attach.AttachMessage;
import org.platon.p2p.proto.attach.AttachRespMessage;
import org.platon.p2p.proto.attach.PingMessage;
import org.platon.p2p.proto.attach.PongMessage;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author Jungle
 * @create 2018-05-07 16:10
 */
@Component("linkController")
public class LinkController {

    private static Logger logger = LoggerFactory.getLogger(LinkController.class);

    @Autowired
    MessageRouter messageRouter;

    @Autowired
    TopologyPlugin plugin;

    @Autowired
    RoutingTable routingTable;


    public void setPlugin(TopologyPlugin plugin) {
        this.plugin = plugin;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }



    public CompletableFuture<NodeID> attach(ByteString remoteNodeId) {
        logger.trace("remote nodeId:" + remoteNodeId);
        NodeID remoteNode = NodeID.newBuilder().setId(remoteNodeId).build();



        return this.attach(remoteNode, routingTable.getLocalNode());
    }

    public CompletableFuture<NodeID> attach(String remoteNodeId) {
        logger.trace("remote nodeId:" + remoteNodeId);
        NodeID remoteNode = NodeID.newBuilder().setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(remoteNodeId))).build();



        return this.attach(remoteNode, routingTable.getLocalNode());
    }


    public CompletableFuture<NodeID> attach(NodeID dest, NodeID local) {
        logger.debug("Attach dest node:{}, local node:{}", dest.toString(), local.toString());
        final CompletableFuture<NodeID> respFut = new CompletableFuture<>();
        AttachMessage attachMessage = AttachMessage.newBuilder()
                .setNodeId(local).build();



        CompletableFuture<com.google.protobuf.Message> fut = messageRouter.sendRequest(attachMessage, Collections.singletonList(NodeUtils.toRoutableID(dest)), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);
        fut.thenAcceptAsync(futMsg -> {
            AttachRespMessage attachRespMsg = (AttachRespMessage)futMsg;
            plugin.getRoutingTable().add(attachRespMsg.getNodeId());
            respFut.complete(attachRespMsg.getNodeId());
        }, NodeContext.executor).exceptionally(throwable ->{
            respFut.completeExceptionally(throwable);
            return null;
        });

        return respFut;
    }

    
    public CompletableFuture<PongMessage> ping(NodeID nodeid) {
        logger.trace("ping node nodeId:{}", nodeid);
        final CompletableFuture<PongMessage> pongFut = new CompletableFuture<>();
        PingMessage pingMsg = PingMessage.newBuilder().setPayloadLength(32).build();

        CompletableFuture<Message> fut = messageRouter.sendRequest(pingMsg,
               Collections.singletonList(NodeUtils.toRoutableID(nodeid)), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);
        fut.thenAcceptAsync(futMsg -> {
            pongFut.complete((PongMessage)futMsg);
        }, NodeContext.executor).exceptionally(throwable ->{
            logger.warn("ping exception:{}", throwable.getMessage());
            pongFut.completeExceptionally(throwable);
            return null;
        });
        return pongFut;
    }
}
