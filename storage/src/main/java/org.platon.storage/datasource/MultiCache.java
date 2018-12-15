package org.platon.storage.datasource;

public abstract class MultiCache<V extends CachedSource> extends ReadWriteCache.BytesKey<V> {

    public MultiCache(Source<byte[], V> src) {
        super(src, WriteCache.CacheType.SIMPLE);
    }

    
    @Override
    public synchronized V get(byte[] key) {
        AbstractCachedSource.Entry<V> ownCacheEntry = getCached(key);
        V ownCache = ownCacheEntry == null ? null : ownCacheEntry.value();
        if (ownCache == null) {
            V v = getSource() != null ? super.get(key) : null;
            ownCache = create(key, v);
            put(key, ownCache);
        }
        return ownCache;
    }

    
    @Override
    public synchronized boolean flushImpl() {
        boolean ret = false;
        for (byte[] key: writeCache.getModified()) {
            V value = super.get(key);
            if (value == null) {

                ret |= flushChild(key, value);
                if (getSource() != null) {
                    getSource().delete(key);
                }
            } else if (value.getSource() != null){
                ret |= flushChild(key, value);
            } else {
                getSource().put(key, value);
                ret = true;
            }
        }
        return ret;
    }

    
    protected boolean flushChild(byte[] key, V childCache) {
        return childCache != null ? childCache.flush() : true;
    }

    
    protected abstract V create(byte[] key, V srcCache);
}
