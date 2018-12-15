package org.platon.storage.datasource;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import org.platon.storage.utils.AutoLock;
import org.platon.common.utils.ByteArrayMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WriteCache<Key, Value> extends AbstractCachedSource<Key, Value> {

    
    public enum CacheType {
        
        SIMPLE,
        
        COUNTING
    }

    private static abstract class CacheEntry<V> implements Entry<V>{


        static final Object UNKNOWN_VALUE = new Object();

        V value;
        int counter = 0;

        protected CacheEntry(V value) {
            this.value = value;
        }

        protected abstract void deleted();

        protected abstract void added();

        protected abstract V getValue();

        @Override
        public V value() {
            V v = getValue();
            return v == UNKNOWN_VALUE ? null : v;
        }
    }

    private static final class SimpleCacheEntry<V> extends CacheEntry<V> {
        public SimpleCacheEntry(V value) {
            super(value);
        }

        public void deleted() {
            counter = -1;
        }

        public void added() {
            counter = 1;
        }

        @Override
        public V getValue() {
            return counter < 0 ? null : value;
        }
    }

    private static final class CountCacheEntry<V> extends CacheEntry<V> {
        public CountCacheEntry(V value) {
            super(value);
        }

        public void deleted() {
            counter--;
        }

        public void added() {
            counter++;
        }

        @Override
        public V getValue() {



            return value;
        }
    }

    private final boolean isCounting;

    protected volatile Map<Key, CacheEntry<Value>> cache = new HashMap<>();

    protected ReadWriteUpdateLock rwuLock = new ReentrantReadWriteUpdateLock();
    protected AutoLock readLock = new AutoLock(rwuLock.readLock());
    protected AutoLock writeLock = new AutoLock(rwuLock.writeLock());
    protected AutoLock updateLock = new AutoLock(rwuLock.updateLock());

    private boolean checked = false;

    public WriteCache(Source<Key, Value> src, CacheType cacheType) {
        super(src);
        this.isCounting = cacheType == CacheType.COUNTING;
    }

    public WriteCache<Key, Value> withCache(Map<Key, CacheEntry<Value>> cache) {
        this.cache = cache;
        return this;
    }

    @Override
    public Collection<Key> getModified() {
        try (AutoLock l = readLock.lock()){
            return cache.keySet();
        }
    }

    @Override
    public boolean hasModified() {
        return !cache.isEmpty();
    }

    private CacheEntry<Value> createCacheEntry(Value val) {
        if (isCounting) {
            return new CountCacheEntry<>(val);
        } else {
            return new SimpleCacheEntry<>(val);
        }
    }

    @Override
    public void put(Key key, Value val) {
        checkByteArrKey(key);
        if (val == null)  {
            delete(key);
            return;
        }


        try (AutoLock l = writeLock.lock()){
            CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(val);
                CacheEntry<Value> oldVal = cache.put(key, curVal);
                if (oldVal != null) {

                    cacheRemoved(key, oldVal.value == unknownValue() ? null : oldVal.value);
                }
                cacheAdded(key, curVal.value);
            }


            curVal.value = val;
            curVal.added();
        }
    }

    @Override
    public Value get(Key key) {
        checkByteArrKey(key);
        try (AutoLock l = readLock.lock()){
            CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                return getSource() == null ? null : getSource().get(key);
            } else {
                Value value = curVal.getValue();
                if (value == unknownValue()) {
                    return getSource() == null ? null : getSource().get(key);
                } else {
                    return value;
                }
            }
        }
    }

    @Override
    public void delete(Key key) {
        checkByteArrKey(key);
        try (AutoLock l = writeLock.lock()){
            CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(getSource() == null ? null : unknownValue());
                CacheEntry<Value> oldVal = cache.put(key, curVal);
                if (oldVal != null) {
                    cacheRemoved(key, oldVal.value);
                }
                cacheAdded(key, curVal.value == unknownValue() ? null : curVal.value);
            }
            curVal.deleted();
        }
    }

    @Override
    public boolean flush() {
        boolean ret = false;
        try (AutoLock l = updateLock.lock()){
            for (Map.Entry<Key, CacheEntry<Value>> entry : cache.entrySet()) {
                if (entry.getValue().counter > 0) {
                    for (int i = 0; i < entry.getValue().counter; i++) {
                        getSource().put(entry.getKey(), entry.getValue().value);
                    }
                    ret = true;
                } else if (entry.getValue().counter < 0) {
                    for (int i = 0; i > entry.getValue().counter; i--) {
                        getSource().delete(entry.getKey());
                    }
                    ret = true;
                }
            }
            if (flushSource) {
                getSource().flush();
            }
            try (AutoLock l1 = writeLock.lock()){
                cache.clear();
                cacheCleared();
            }
            return ret;
        }
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }

    private Value unknownValue() {
        return (Value) CacheEntry.UNKNOWN_VALUE;
    }

    public Entry<Value> getCached(Key key) {
        try (AutoLock l = readLock.lock()){
            CacheEntry<Value> entry = cache.get(key);
            if (entry == null || entry.value == unknownValue()) {
                return null;
            }else {
                return entry;
            }
        }
    }




    private void checkByteArrKey(Key key) {
        if (checked) return;

        if (key instanceof byte[]) {
            if (!(cache instanceof ByteArrayMap)) {
                throw new RuntimeException("Wrong map/set for byte[] key");
            }
        }
        checked = true;
    }

    public long debugCacheSize() {
        long ret = 0;
        for (Map.Entry<Key, CacheEntry<Value>> entry : cache.entrySet()) {
            ret += keySizeEstimator.estimateSize(entry.getKey());
            ret += valueSizeEstimator.estimateSize(entry.getValue().value());
        }
        return ret;
    }

    
    public static class BytesKey<V> extends WriteCache<byte[], V> implements CachedSource.BytesKey<V> {

        public BytesKey(Source<byte[], V> src, CacheType cacheType) {
            super(src, cacheType);
            withCache(new ByteArrayMap<CacheEntry<V>>());
        }
    }
}
