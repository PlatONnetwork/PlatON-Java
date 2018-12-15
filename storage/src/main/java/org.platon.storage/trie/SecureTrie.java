package org.platon.storage.trie;

import org.platon.storage.datasource.Source;

import static org.platon.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.platon.crypto.HashUtil.sha3;

public class SecureTrie extends TrieImpl {

    public SecureTrie(Source<byte[], byte[]> cache) {
        super(cache, true, 32);
    }

    public SecureTrie(Source<byte[], byte[]> cache, byte[] root) {
        super(cache, true, 32, root);
    }

    public SecureTrie(){}

    @Override
    public byte[] get(byte[] key) {
        return super.get(sha3(key));
    }

    @Override
    public void put(byte[] key, byte[] value) {
        super.put(sha3(key), value);
    }

    @Override
    public void delete(byte[] key) {
        put(key, EMPTY_BYTE_ARRAY);
    }
}
