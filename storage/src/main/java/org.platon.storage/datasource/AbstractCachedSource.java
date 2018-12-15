package org.platon.storage.datasource;

public abstract class AbstractCachedSource<Key, Value>
        extends AbstractChainedSource<Key, Value, Key, Value>
        implements CachedSource<Key, Value> {

    private final Object lock = new Object();

    
    public interface Entry<V> {
        V value();
    }

    static final class SimpleEntry<V> implements Entry<V> {
        private V val;

        public SimpleEntry(V val) {
            this.val = val;
        }

        public V value() {
            return val;
        }
    }

    protected MemSizeEstimator<Key> keySizeEstimator;
    protected MemSizeEstimator<Value> valueSizeEstimator;
    private int size = 0;

    public AbstractCachedSource(Source<Key, Value> source) {
        super(source);
    }

    
    abstract Entry<Value> getCached(Key key);

    
    protected void cacheAdded(Key key, Value value) {
        synchronized (lock) {
            if (keySizeEstimator != null) {
                size += keySizeEstimator.estimateSize(key);
            }
            if (valueSizeEstimator != null) {
                size += valueSizeEstimator.estimateSize(value);
            }
        }
    }

    
    protected void cacheRemoved(Key key, Value value) {
        synchronized (lock) {
            if (keySizeEstimator != null) {
                size -= keySizeEstimator.estimateSize(key);
            }
            if (valueSizeEstimator != null) {
                size -= valueSizeEstimator.estimateSize(value);
            }
        }
    }

    
    protected void cacheCleared() {
        synchronized (lock) {
            size = 0;
        }
    }

    
    public AbstractCachedSource<Key, Value> withSizeEstimators(MemSizeEstimator<Key> keySizeEstimator, MemSizeEstimator<Value> valueSizeEstimator) {
        this.keySizeEstimator = keySizeEstimator;
        this.valueSizeEstimator = valueSizeEstimator;
        return this;
    }

    @Override
    public long estimateCacheSize() {
        return size;
    }
}
