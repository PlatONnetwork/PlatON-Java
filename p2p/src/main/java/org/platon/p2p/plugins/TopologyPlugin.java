package org.platon.p2p.plugins;

import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author yangzhou
 * @create 2018-04-24 15:53
 */
public interface TopologyPlugin {

    void join(NodeID remote);
    CompletableFuture<List<NodeID>> query(RoutableID dest);

    int getDistance(RoutableID source, RoutableID dest);

    boolean isLocalPeerResponsible(RoutableID dest);

    boolean isLocalPeerValidStorage(ResourceID resourceId, boolean isReplica);

    List<NodeID> getReplicaNodes(ResourceID resourceId);

    RoutingTable getRoutingTable();

    List<NodeID> getBroadCastNode(RoutableID dest);
}
