package org.platon.p2p.plugins.kademlia;

import org.platon.p2p.proto.common.NodeID;

import java.math.BigInteger;
import java.util.Comparator;


public class KeyComparator implements Comparator<NodeID>
{

    private final BigInteger key;

    
    public KeyComparator(NodeID key)
    {
        this.key = KademliaHelp.getInt(key.getId());
    }

    
    @Override
    public int compare(NodeID n1, NodeID n2)
    {
        BigInteger b1 = KademliaHelp.getInt(n1.getId());
        BigInteger b2 = KademliaHelp.getInt(n2.getId());

        b1 = b1.xor(key);
        b2 = b2.xor(key);

        return b1.abs().compareTo(b2.abs());
    }
}

