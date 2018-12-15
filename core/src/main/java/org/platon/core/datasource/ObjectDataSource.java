package org.platon.core.datasource;

import org.platon.storage.datasource.ReadCache;
import org.platon.storage.datasource.SerializerIfc;
import org.platon.storage.datasource.Source;
import org.platon.storage.datasource.SourceChainBox;

public class ObjectDataSource<V> extends SourceChainBox<byte[], V, byte[], byte[]> {

    private ReadCache<byte[], V> cache;
    private SourceCodec<byte[], V, byte[], byte[]> codec;
    private Source<byte[], byte[]> byteSource;

    public ObjectDataSource(Source<byte[], byte[]> byteSource,
                            SerializerIfc<V, byte[]> serializer,
                            int readCacheEntries) {
        super(byteSource);
        this.byteSource = byteSource;
        add(codec = new SourceCodec<>(byteSource, new Serializers.Identity<byte[]>(), serializer));
        if (readCacheEntries > 0) {
            add(cache = new ReadCache.BytesKey<>(codec).withMaxCapacity(readCacheEntries));
        }
    }
}
