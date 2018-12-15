package org.platon.p2p.plugins;

import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.plugin.JoinMessage;
import org.platon.p2p.proto.plugin.JoinRespMessage;
import org.platon.p2p.proto.plugin.QueryMessage;
import org.platon.p2p.proto.plugin.QueryRespMessage;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

;


/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/5/15, lvxiaoyi, Initial Version.
 */
@Component("plugin")
public class Plugin {

    private static Logger logger = LoggerFactory.getLogger(Plugin.class);



    @Autowired
    private KadRoutingTable routingTable;

    @Autowired
    private MessageRouter messageRouter;


    public void setRoutingTable(KadRoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    
    @PlatonMessageType("JoinMessage")
    public void join(JoinMessage msg, HeaderHelper header){



        List<NodeID> neighbors = routingTable.getNeighbors(msg.getNodeId(), 10);
        routingTable.add(msg.getNodeId());
        JoinRespMessage ans = JoinRespMessage.newBuilder().addAllNodeId(neighbors).build();


        messageRouter.sendResponse(ans, header.txId(),
                Collections.singletonList(RoutableID.newBuilder().setType(RoutableID.DestinationType.NODEIDTYPE).setId(msg.getNodeId().getId()).build()),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);

    }



    
    @PlatonMessageType("JoinRespMessage")
    public void joinResp(JoinRespMessage msg, HeaderHelper header){


        for (NodeID id : msg.getNodeIdList()) {
            routingTable.add(id);
        }
        messageRouter.handleResponse(header.txId(), msg);

    }



    
    @PlatonMessageType("QueryMessage")
    public void query(QueryMessage msg, HeaderHelper header){


        List<NodeID> neighbors = routingTable.getNeighbors(msg.getRoutableId(), 10);
        QueryRespMessage ans = QueryRespMessage.newBuilder().addAllNodeId(neighbors).build();
        List<RoutableID> destList = new ArrayList<>();
        for (NodeID nodeId : header.viaList()) {
            destList.add(RoutableID.newBuilder().setId(nodeId.getId()).setType(RoutableID.DestinationType.NODEIDTYPE).build());
        }

        messageRouter.sendResponse(ans, header.txId(),
                destList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);

    }


    
    @PlatonMessageType("QueryRespMessage")
    public void queryResp(QueryRespMessage msg, HeaderHelper header){
        logger.debug("route query response neighbor:", msg.toString());


        for (NodeID id : msg.getNodeIdList()) {
            routingTable.add(id);
        }
        messageRouter.handleResponse(header.txId(), msg);

    }

}
