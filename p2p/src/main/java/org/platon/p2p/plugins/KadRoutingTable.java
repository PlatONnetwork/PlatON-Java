package org.platon.p2p.plugins;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import org.apache.commons.collections.CollectionUtils;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.plugins.kademlia.Contact;
import org.platon.p2p.plugins.kademlia.KademliaRoutingTable;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yangzhou
 * @create 2018-04-24 15:56
 */
@Component("routingTable")
public class KadRoutingTable implements RoutingTable {

    private static Logger logger = LoggerFactory.getLogger(KadRoutingTable.class);


    
    private static KademliaRoutingTable table;
    private static NodeID localNode;
    private static final int numNodesRequired = 5;


    public KadRoutingTable() {
        localNode = NodeID.newBuilder()
                .setId(NodeContext.localNodeId)
                .setEndpoint(NodeContext.getEndpoint())
                .setPubKey(ByteString.copyFrom(NodeContext.publicKey)).build();


        this.table = new KademliaRoutingTable();
        this.table.insert(localNode);
    }

    
    @Override
    public NodeID getLocalNode() {
        List<NodeID> nodeIDList = this.table.findClosest(localNode,1);
        if(CollectionUtils.isNotEmpty(nodeIDList)){
            return nodeIDList.get(0);
        }
        return localNode;
    }

    
    @Override
    public List<NodeID> getNextHops(NodeID destination) {
        return getNextHops(destination, numNodesRequired);
    }

    
    @Override
    public List<NodeID> getNextHops(NodeID destination, int num) {
        List<NodeID> nodeList = table.findClosest(destination, num);
        if (localNode.getId().equals(destination.getId())){
            nodeList.remove(localNode);
        }
        return nodeList;
    }

    
    @Override
    public List<NodeID> getNextHops(ResourceID destination) {
        return getNextHops(destination, numNodesRequired);
    }

    
    @Override
    public List<NodeID> getNextHops(ResourceID destination, int num) {
        List<NodeID> nodeList = table.findClosest(destination, num);
        return nodeList;
    }

    
    @Override
    public List<NodeID> getNextHops(RoutableID destination){
        return getNextHops(destination, numNodesRequired);
    }

    
    @Override
    public List<NodeID> getNextHops(RoutableID destination, int num){
        List<NodeID> nodeList = table.findClosest(destination, num);
        if (destination.getType() == RoutableID.DestinationType.NODEIDTYPE) {
            nodeList.remove(localNode);
        }
        return nodeList;
    }

    
    @Override
    public String getEndPoint(ByteString destination) {
        List<NodeID> nodeList = table.findClosest(destination, 1);
        if (!nodeList.isEmpty() && nodeList.get(0).getId().equals(destination)) {
            return nodeList.get(0).getEndpoint();
        }
        return "";
    }

    
    @Override
    public List<NodeID> getNeighbors(){

        List<NodeID> nodeList = table.findClosest(numNodesRequired);
        nodeList.remove(localNode);
        return nodeList;
    }

    
    @Override
    public List<NodeID> getNeighbors(NodeID id, int num) {
        return table.findClosest(id, num);
    }

    
    @Override
    public List<NodeID> getNeighbors(ResourceID id, int num) {
        return table.findClosest(id, num);
    }

    
    @Override
    public List<NodeID> getNeighbors(RoutableID id, int num){
        return table.findClosest(id, num);
    }

    
    @Override
    public void add(NodeID node) {
        if (!node.getId().isEmpty() && !node.getEndpoint().isEmpty()) {
            logger.debug("Add node {}/{} to local router table", CodecUtils.toHexString(node.getId()), node.getEndpoint());
            table.insert(node);
        }
    }

    
    @Override
    public void del(NodeID node) {
        table.removeNode(node);
    }

     
    public void setUnresponsiveNode(NodeID node) {
        table.setUnresponsiveContact(node);
    }

    
    @Override
    public List<NodeID> getAllNode() {
        return table.getAllNodes();
    }

    
    @Override
    public NodeID getNodeID(ByteString destination) {
        List<NodeID> nodeList = table.findClosest(destination, 1);
        if (!nodeList.isEmpty() && nodeList.get(0).getId().equals(destination)) {
            return nodeList.get(0);
        }
        return null;
    }

    
    public NodeID getBucketOne(int i) {
        return table.getBucketOne(i);
    }

    
    @Override
    public String getTableToJson() {
        return JSON.toJSONString(table.getAllContacts());
    }

    
    @Override
    public void setTableFromJson(String json) {
        List<Contact> contacts = JSON.parseArray(json, Contact.class);
        for (Contact c : contacts) {
            if (!localNode.getId().equals(c.getNode().getId())) {
                table.insert(c);
            }
        }
    }

    
    @Override
    public List<Contact> getAllContacts() {
        return table.getAllContacts();
    }

}