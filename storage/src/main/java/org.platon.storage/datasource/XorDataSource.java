package org.platon.storage.datasource;

import org.platon.common.utils.ByteUtil;

public class XorDataSource<V> extends AbstractChainedSource<byte[], V, byte[], V> {

    private byte[] subKey;

    public XorDataSource(Source<byte[], V> source, byte[] subKey) {
        super(source);
        this.subKey = subKey;
    }

    private byte[] convertKey(byte[] key) {
        return ByteUtil.xorAlignRight(key, subKey);
    }

    @Override
    public V get(byte[] key) {
        return getSource().get(convertKey(key));
    }

    @Override
    public void put(byte[] key, V value) {
        getSource().put(convertKey(key), value);
    }

    @Override
    public void delete(byte[] key) {
        getSource().delete(convertKey(key));
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }
}
