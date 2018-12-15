package org.platon.p2p.plugins.kademlia;

import org.platon.p2p.proto.common.NodeID;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;



public class Contact implements Comparable<Contact>
{

    private NodeID n;



    private long lastSeen;

    
    private int staleCount;

    
    public Contact(){

    }
    public Contact(NodeID n)
    {
        this.n = n;
        this.lastSeen = System.currentTimeMillis() / 1000L;
    }

    public NodeID getNode()
    {
        return this.n;
    }

    public void setNode(NodeID n) {
        this.n = n;
    }

    
    public void setSeenNow()
    {
        this.lastSeen = System.currentTimeMillis() / 1000L;
    }

    
    public long lastSeen()
    {
        return this.lastSeen;
    }

    
    public void incrementStaleCount()
    {
        staleCount++;
    }

    
    public int staleCount()
    {
        return this.staleCount;
    }

    
    public void resetStaleCount()
    {
        this.staleCount = 0;
    }

    @Override
    public int compareTo(Contact o)
    {
        BigInteger from = new BigInteger(this.getNode().getId().toByteArray());
        BigInteger dest = new BigInteger(o.getNode().getId().toByteArray());
        return from.compareTo(dest);






    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Arrays.equals(this.getNode().getId().toByteArray(), ((Contact)o).getNode().getId().toByteArray());
    }

    @Override
    public int hashCode() {

        return Objects.hash(n.getId());
    }
}


