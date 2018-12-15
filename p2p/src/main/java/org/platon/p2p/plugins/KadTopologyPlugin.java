package org.platon.p2p.plugins;

import com.google.protobuf.Message;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.KadPluginConfig;
import org.platon.p2p.db.DB;
import org.platon.p2p.plugins.kademlia.KademliaHelp;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author yangzhou
 * @create 2018-04-24 15:55
 */
@Component("topologyPlugin")
public class KadTopologyPlugin implements TopologyPlugin {

    private static Logger logger = LoggerFactory.getLogger(KadTopologyPlugin.class);

    @Autowired
    private KadRoutingTable kadRoutingTable;
    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    DB db;



    public void setKadRoutingTable(KadRoutingTable kadRoutingTable) {
        this.kadRoutingTable = kadRoutingTable;
    }


    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }













    public void setDb(DB db) {
        this.db = db;
    }

    private final ScheduledExecutorService expiredRoutingInfoRemover = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService backupRoutable =  Executors.newScheduledThreadPool(1);

    public KadTopologyPlugin() {


    }

    
    @Override
    public void join(NodeID remote) {



        kadRoutingTable.add(remote);

        JoinMessage msg = JoinMessage.newBuilder().setNodeId(kadRoutingTable.getLocalNode()).build();


        RoutableID routableID = RoutableID.newBuilder().setType(RoutableID.DestinationType.NODEIDTYPE)
                .setId(remote.getId()).build();
        List<RoutableID> destinationList = Collections.singletonList(routableID);

        CompletableFuture<Message> future = messageRouter.sendRequest( msg, destinationList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);
        future.thenAcceptAsync(futMsg->{
            JoinRespMessage ansMsg = null;
            try {
                ansMsg = (JoinRespMessage)futMsg;

                List<RoutableID> destList = new ArrayList<>();
                for (NodeID id : ansMsg.getNodeIdList()) {
                    if (!id.getId().equals(kadRoutingTable.getLocalNode().getId()) && !id.getId().equals(remote.getId())) {
                        destList.add(RoutableID.newBuilder().setId(id.getId()).setType(RoutableID.DestinationType.NODEIDTYPE).build());
                    }

                }





                if(destList.size()>0) {
                    messageRouter.sendRequest(msg, destList, MessageRouter.ForwardingOptionType.BROADCAST_CONNECTION, false);
                }
            } catch (Exception e) {
                logger.error("error:", e);
            }
        }, NodeContext.executor);

    }

    
    @Override
    public CompletableFuture<List<NodeID>> query(RoutableID dest) {



        final CompletableFuture<List<NodeID>> res = new CompletableFuture<>();

        QueryMessage msg = QueryMessage.newBuilder().setRoutableId(dest).build();

        CompletableFuture<Message> msgFut = messageRouter.sendRequest(msg,
                Collections.singletonList(dest),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        msgFut.thenAcceptAsync(ans->{
            QueryRespMessage queryAns = (QueryRespMessage)ans;
            res.complete(queryAns.getNodeIdList());
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);

            return null;
        });


        return res;
    }


    @Override
    public int getDistance(RoutableID source, RoutableID dest){
            return KademliaHelp.getDistance(source, dest);
    }

    
    @Override
    public boolean isLocalPeerResponsible(RoutableID dest){

        List<NodeID> node = kadRoutingTable.getNextHops(dest, 1);
        if (node.isEmpty()) return false;
        return node.get(0).getId().equals(dest.getId());
    }

    
    @Override
    public boolean isLocalPeerValidStorage(ResourceID resourceId, boolean isReplica) {
        return true;
    }


    @Override
    public List<NodeID> getReplicaNodes(ResourceID resourceId) {
        return kadRoutingTable.getNeighbors();
    }

    @Override
    public RoutingTable getRoutingTable(){
        return kadRoutingTable;
    }

    
    @Override
    public List<NodeID> getBroadCastNode(RoutableID dest) {
        List<NodeID> nodes = new ArrayList<>();
        for (int i = 0; i < KadPluginConfig.getInstance().getIdLength()*8; i++){
            NodeID nodeID = kadRoutingTable.getBucketOne(i);
            if (nodeID != null) {
                nodes.add(nodeID);
            }
        }
        return nodes;
    }

}
