package org.platon.p2p.common;

import com.google.protobuf.ByteString;
import org.platon.p2p.plugins.kademlia.KademliaHelp;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author yangzhou
 * @create 2018-08-22 16:45
 */
public class Bytes implements Comparable<Bytes> {
    private final byte[] key;


    public static class BytesComparator implements Comparator<Bytes>
    {

        private final BigInteger key;

        /**
         * @param key The NodeId relative to which the distance should be measured.
         */
        public BytesComparator(Bytes key)
        {
            this.key = KademliaHelp.getInt(key);
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
        public int compare(Bytes n1, Bytes n2)
        {
            BigInteger b1 = KademliaHelp.getInt(n1);
            BigInteger b2 = KademliaHelp.getInt(n2);

            b1 = b1.xor(key);
            b2 = b2.xor(key);

            return b1.abs().compareTo(b2.abs());
        }
    }

    public static BytesComparator newBytesComparator(Bytes key) {
        return new BytesComparator(key);
    }


    /**
     * @param key The NodeId relative to which the distance should be measured.
     */
    public Bytes(byte[] key) {
        this.key = key;
    }

    public static Bytes valueOf(byte[] key){
        return new Bytes(key);
    }
    public static Bytes valueOf(ByteString key){
        return new Bytes(key.toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bytes that = (Bytes) o;
        return Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    public byte[] getKey() {
        return key;
    }


    @Override
    public int compareTo(Bytes o) {
        return new BigInteger(key).compareTo(new BigInteger(1, o.getKey()));
    }



}
