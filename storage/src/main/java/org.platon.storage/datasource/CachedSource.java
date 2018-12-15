package org.platon.storage.datasource;

import java.util.Collection;

public interface CachedSource<Key, Value> extends Source<Key, Value> {

    
    Source<Key, Value> getSource();

    
    Collection<Key> getModified();

    
    boolean hasModified();

    
    long estimateCacheSize();

    
    interface BytesKey<Value> extends CachedSource<byte[], Value> {}
}
