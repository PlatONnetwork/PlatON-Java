package org.platon.storage.datasource;

public class PrefixLookupSource<V> implements Source<byte[], V> {


    private int prefixBytes;
    private DbSource<V> source;

    public PrefixLookupSource(DbSource<V> source, int prefixBytes) {
        this.source = source;
        this.prefixBytes = prefixBytes;
    }

    @Override
    public V get(byte[] key) {
        return source.prefixLookup(key, prefixBytes);
    }

    @Override
    public void put(byte[] key, V val) {
        source.put(key, val);
    }

    @Override
    public void delete(byte[] key) {
        source.delete(key);
    }

    @Override
    public boolean flush() {
        return source.flush();
    }
}
