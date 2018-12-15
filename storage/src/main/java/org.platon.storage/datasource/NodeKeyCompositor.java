package org.platon.storage.datasource;

import org.platon.crypto.HashUtil;

import static java.lang.System.arraycopy;

public class NodeKeyCompositor implements SerializerIfc<byte[], byte[]> {

    public static final int HASH_LEN = 32;
    public static final int PREFIX_BYTES = 16;
    private byte[] addrHash;

    public NodeKeyCompositor(byte[] addrOrHash) {
        this.addrHash = addrHash(addrOrHash);
    }

    @Override
    public byte[] serialize(byte[] key) {
        return composeInner(key, addrHash);
    }

    @Override
    public byte[] deserialize(byte[] stream) {
        return stream;
    }

    public static byte[] compose(byte[] key, byte[] addrOrHash) {
        return composeInner(key, addrHash(addrOrHash));
    }

    private static byte[] composeInner(byte[] key, byte[] addrHash) {

        validateKey(key);

        byte[] derivative = new byte[key.length];
        arraycopy(key, 0, derivative, 0, PREFIX_BYTES);
        arraycopy(addrHash, 0, derivative, PREFIX_BYTES, PREFIX_BYTES);

        return derivative;
    }

    private static void validateKey(byte[] key) {
        if (key.length != HASH_LEN)
            throw new IllegalArgumentException("Key is not a hash code");
    }

    private static byte[] addrHash(byte[] addrOrHash) {
        return addrOrHash.length == HASH_LEN ? addrOrHash : HashUtil.sha3(addrOrHash);
    }
}
