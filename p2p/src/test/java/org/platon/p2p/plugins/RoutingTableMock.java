package org.platon.p2p.plugins;

import com.google.protobuf.ByteString;
import org.apache.http.util.Asserts;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.plugins.kademlia.Contact;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yangzhou
 * @create 2018-07-30 19:58
 */
public class RoutingTableMock implements RoutingTable {

    private NodeID localNodeId = null;

    public void setNextHops(List<NodeID> nextHops) {
        this.nextHops = nextHops;
    }

    public List<NodeID> getNextHops() {
        return nextHops;
    }

    private List<NodeID> nextHops = null;
    public RoutingTableMock(){
    }

    public RoutingTableMock(String id){
        NodeID nodeID = NodeID.newBuilder()
                .setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(id)))
                .build();

        localNodeId = nodeID;
    }
    @Override
    public NodeID getLocalNode() {
        return localNodeId;
    }

    @Override
    public List<NodeID> getNextHops(NodeID destination) {
        return null;
    }

    @Override
    public List<NodeID> getNextHops(NodeID destination, int num) {
        return null;
    }

    @Override
    public List<NodeID> getNextHops(ResourceID destination) {
        return null;
    }

    @Override
    public List<NodeID> getNextHops(ResourceID destination, int num) {
        return null;
    }

    @Override
    public List<NodeID> getNextHops(RoutableID destination) {
        Asserts.notNull(nextHops, "nextHops is null");
        return nextHops;
    }

    @Override
    public List<NodeID> getNextHops(RoutableID destination, int num) {
        return new LinkedList<>();
    }

    @Override
    public List<NodeID> getNeighbors() {
        return new LinkedList<>();
    }

    @Override
    public List<NodeID> getNeighbors(NodeID id, int num) {
        return null;
    }

    @Override
    public List<NodeID> getNeighbors(ResourceID id, int num) {
        return null;
    }

    @Override
    public List<NodeID> getNeighbors(RoutableID id, int num) {
        return new LinkedList<>();
    }

    @Override
    public List<NodeID> getAllNode() {
        return new LinkedList<>();
    }

    @Override
    public String getEndPoint(ByteString destination) {
        return null;
    }

    @Override
    public NodeID getNodeID(ByteString destination) {
        return null;
    }


    @Override
    public void add(NodeID node) {
        System.out.println("add node:" + NodeUtils.getNodeIdString(node.getId()));
    }

    @Override
    public void del(NodeID node) {
        System.out.println("del node:" + NodeUtils.getNodeIdString(node.getId()));
    }

    @Override
    public String getTableToJson() {
        return "123";
    }

    @Override
    public void setTableFromJson(String json) {
    }

    @Override
    public List<Contact> getAllContacts() {
        return new LinkedList<>();
    }
}
