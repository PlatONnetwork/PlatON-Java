package org.platon.common.utils;

import java.io.Serializable;
import java.util.Arrays;

import static org.platon.common.utils.Numeric.toHexString;


public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

    private final byte[] data;
    private int hashCode = 0;

    public ByteArrayWrapper(byte[] data) {
        if (null == data) {
            throw new NullPointerException("data required.");
        }
        this.data = data;
        // hashCode用于比较
        this.hashCode = Arrays.hashCode(data);
    }

    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }
        byte[] otherData = ((ByteArrayWrapper) other).getData();
        return ByteComparator.compareTo(
                data, 0, data.length,
                otherData, 0, otherData.length) == 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        return ByteComparator.compareTo(
                data, 0, data.length,
                o.getData(), 0, o.getData().length);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return toHexString(data);
    }
}
