package org.platon.p2p.plugins;

import org.platon.p2p.plugins.kademlia.KademliaHelp;
import org.platon.p2p.proto.common.RoutableID;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * @author yangzhou
 * @create 2018-08-17 15:37
 */
public class RoutableIDComparator implements Comparator<RoutableID>  {
    private final BigInteger key;

    /**
     * @param key The NodeId relative to which the distance should be measured.
     */
    public RoutableIDComparator(RoutableID key)
    {
        this.key = KademliaHelp.getInt(key.getId());
    }

    /**
     * Compare two objects which must both be of type <code>Node</code>
     * and determine which is closest to the identifier specified in the
     * constructor.
     *
     * @param n1 Node 1 to compare distance from the key
     * @param n2 Node 2 to compare distance from the key
     */
    @Override
    public int compare(RoutableID n1, RoutableID n2)
    {
        BigInteger b1 = KademliaHelp.getInt(n1.getId());
        BigInteger b2 = KademliaHelp.getInt(n2.getId());

        b1 = b1.xor(key);
        b2 = b2.xor(key);

        return b1.abs().compareTo(b2.abs());
    }
}
