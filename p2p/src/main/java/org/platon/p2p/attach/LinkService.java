package org.platon.p2p.attach;

import org.apache.commons.lang3.RandomUtils;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.plugins.TopologyPlugin;
import org.platon.p2p.proto.attach.AttachMessage;
import org.platon.p2p.proto.attach.AttachRespMessage;
import org.platon.p2p.proto.attach.PingMessage;
import org.platon.p2p.proto.attach.PongMessage;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yangzhou
 * @create 2018-04-27 11:55
 */
@Component("linkService")
public class LinkService {

    private static Logger logger = LoggerFactory.getLogger(LinkService.class);

    @Autowired
    private TopologyPlugin plugin;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    private RoutingTable routingTable;


    public void setPlugin(TopologyPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }





    @PlatonMessageType("AttachMessage")
    public void attach(AttachMessage msg, HeaderHelper header){
        logger.trace("Received attach request message:{}", msg.toString());



        plugin.getRoutingTable().add(msg.getNodeId());



        AttachRespMessage attachMessage = AttachRespMessage.newBuilder().setNodeId(routingTable.getLocalNode()).build();


        messageRouter.sendResponse(attachMessage, header.txId(),
                header.viaToDest(), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }


    @PlatonMessageType("AttachRespMessage")
    public void attachResp(AttachRespMessage msg, HeaderHelper header){
        logger.trace("Received attach response message:{}", msg);
        messageRouter.handleResponse(header.txId(), msg);
    }


    @PlatonMessageType("PingMessage")
    public void ping(PingMessage msg,  HeaderHelper header){
        logger.trace("Received ping request message:{}", msg.toString());
        PongMessage pongMessage = PongMessage.newBuilder()
                .setResponseId(RandomUtils.nextLong())
                .setResponseTime(System.currentTimeMillis())
                .build();

        messageRouter.sendResponse(pongMessage, header.txId(),
                header.viaToDest(), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }


    @PlatonMessageType("PingMessage")
    public void pingResp(PongMessage msg, HeaderHelper header){
        logger.trace("Received ping response message:{}", msg);
        messageRouter.handleResponse(header.txId(), msg);
    }
}
