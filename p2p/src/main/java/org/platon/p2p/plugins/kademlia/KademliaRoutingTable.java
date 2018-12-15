package org.platon.p2p.plugins.kademlia;

import com.google.protobuf.ByteString;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.KadPluginConfig;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;



public class KademliaRoutingTable
{

    private static NodeID localNode;
    private static KademliaBucket[] buckets;



    public KademliaRoutingTable() {

        localNode = NodeID.newBuilder()
                .setId(NodeContext.localNodeId)
                .setEndpoint(NodeContext.getEndpoint())
                .setPubKey(ByteString.copyFrom(NodeContext.publicKey)).build();


        
        this.initialize();

        
        this.insert(localNode);
    }


 /*   public KademliaRoutingTable(NodeID localNode)
    {
        this.localNode = localNode;
        *
        this.initialize();

        *
        this.insert(localNode);
    }*/

    
    public final void initialize() {

        this.buckets = new KademliaBucket[KadPluginConfig.getInstance().getIdLength()*8];
        for (int i = 0; i < KadPluginConfig.getInstance().getIdLength()*8; i++)
        {
            buckets[i] = new KademliaBucket(i);
        }
    }

    
    public synchronized final void insert(Contact c)
    {
        this.buckets[this.getBucketId(c.getNode())].insert(c);
    }

    

    public synchronized final void insert(NodeID n)
    {
        this.buckets[this.getBucketId(n)].insert(n);
    }

    

    public final int getBucketId(NodeID nid)
    {
        int bId = KademliaHelp.getDistance(this.localNode.getId(), nid.getId());

        
        return bId < 0 ? 0 : bId;
    }

    

    public  final List<NodeID> findClosest(ResourceID target, int numNodesRequired) {
        return findClosest(target.getId(), numNodesRequired);
    }

    public  final List<NodeID> findClosest(NodeID target, int numNodesRequired) {
        return findClosest(target.getId(), numNodesRequired);
    }

    public  final List<NodeID> findClosest(RoutableID target, int numNodesRequired) {
        return findClosest(target.getId(), numNodesRequired);
    }


    public synchronized final List<NodeID> findClosest(ByteString target, int numNodesRequired)
    {
        NodeID tagetNodeId = NodeID.newBuilder().setId(target).build();
        TreeSet<NodeID> sortedSet = new TreeSet<>(new KeyComparator(tagetNodeId));
        sortedSet.addAll(this.getAllNodes());

        List<NodeID> closest = new ArrayList<>(numNodesRequired);

        
        int count = 0;
        for (NodeID n : sortedSet)
        {
            closest.add(n);
            if (++count == numNodesRequired)
            {
                break;
            }
        }
        return closest;
    }

    public synchronized final List<NodeID> findClosest(int numNodesRequired)
    {
        return findClosest(localNode.getId(), numNodesRequired);
    }

    
    public synchronized final List<NodeID> getAllNodes()
    {
        List<NodeID> nodes = new ArrayList<>();

        for (KademliaBucket b : this.buckets)
        {
            for (Contact c : b.getContacts())
            {

                if (c.staleCount() < KadPluginConfig.getInstance().getStaleTimes()) {
                    nodes.add(c.getNode());
                }
            }
        }

        return nodes;
    }

    public synchronized final NodeID getBucketOne(int index) {
        if (this.buckets[index].getContacts().isEmpty()) {
            return null;
        }
        return this.buckets[index].getContacts().get(0).getNode();
    }
    
    public final List<Contact> getAllContacts()
    {
        List<Contact> contacts = new ArrayList<>();

        for (KademliaBucket b : this.buckets)
        {
            contacts.addAll(b.getContacts());
        }

        return contacts;
    }

    

    public final KademliaBucket[] getBuckets()
    {
        return this.buckets;
    }

    
    public final void setBuckets(KademliaBucket[] buckets)
    {
        this.buckets = buckets;
    }

    
    public void setUnresponsiveContacts(List<NodeID> contacts)
    {
        if (contacts.isEmpty())
        {
            return;
        }
        for (NodeID n : contacts)
        {
            this.setUnresponsiveContact(n);
        }
    }

    
    public synchronized void setUnresponsiveContact(NodeID n)
    {
        int bucketId = this.getBucketId(n);

        
        this.buckets[bucketId].removeNode(n);
    }

    public synchronized void removeNode(NodeID n) {
        int bucketId = this.getBucketId(n);
        this.buckets[bucketId].removeFromContacts(n);
    }

    @Override
    public synchronized final String toString()
    {
        StringBuilder sb = new StringBuilder("\nPrinting Routing Table Started ***************** \n");
        int totalContacts = 0;
        for (KademliaBucket b : this.buckets)
        {
            if (b.numContacts() > 0)
            {
                totalContacts += b.numContacts();
                sb.append("# nodes in Bucket with depth ");
                sb.append(b.getDepth());
                sb.append(": ");
                sb.append(b.numContacts());
                sb.append("\n");
                sb.append(b.toString());
                sb.append("\n");
            }
        }

        sb.append("\nTotal Contacts: ");
        sb.append(totalContacts);
        sb.append("\n\n");

        sb.append("Printing Routing Table Ended ******************** ");

        return sb.toString();
    }

}

