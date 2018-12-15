package org.platon.p2p.plugins;

import com.google.protobuf.ByteString;
import org.platon.p2p.plugins.kademlia.Contact;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;

import java.util.List;

/**
 * @author yangzhou
 * @create 2018-04-24 15:55
 */
public interface RoutingTable {

    /**
     * @return the the next hops node-ids
     */
    NodeID getLocalNode();
    List<NodeID> getNextHops(NodeID destination);
    List<NodeID> getNextHops(NodeID destination, int num);

    List<NodeID> getNextHops(ResourceID destination);
    List<NodeID> getNextHops(ResourceID destination, int num);

    List<NodeID> getNextHops(RoutableID destination);
    List<NodeID> getNextHops(RoutableID destination, int num);

    List<NodeID> getNeighbors();
    List<NodeID> getNeighbors(NodeID id, int num);
    List<NodeID> getNeighbors(ResourceID id, int num);
    List<NodeID> getNeighbors(RoutableID id, int num);

    List<NodeID> getAllNode();

    String getEndPoint(ByteString destination);

    NodeID getNodeID(ByteString destination);

    void add(NodeID node);
    void del(NodeID node);
    String getTableToJson();

    void setTableFromJson(String json);

    List<Contact> getAllContacts();
}