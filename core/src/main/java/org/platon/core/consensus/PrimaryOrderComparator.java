package org.platon.core.consensus;

import java.math.BigInteger;

/**
 * compare primary node's address
 *
 * @author alliswell
 * @version 0.0.1
 * @date 2018/8/28 19:56
 */
public class PrimaryOrderComparator implements java.util.Comparator<byte[]> {

    /**
     * seed of comparator
     */
    private byte[] seed;

    public PrimaryOrderComparator(byte[] seed) {
        this.seed = seed;
    }

    @Override
    public int compare(byte[] n1, byte[] n2) {
        BigInteger first = new BigInteger(n1).xor(new BigInteger(seed));
        BigInteger second = new BigInteger(n2).xor(new BigInteger(seed));
        return first.subtract(second).intValue();
    }
}
