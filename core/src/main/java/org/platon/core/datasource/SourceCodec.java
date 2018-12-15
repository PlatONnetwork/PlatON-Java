package org.platon.core.datasource;

import org.platon.storage.datasource.AbstractChainedSource;
import org.platon.storage.datasource.SerializerIfc;
import org.platon.storage.datasource.Source;

public class SourceCodec<Key, Value, SourceKey, SourceValue>
        extends AbstractChainedSource<Key, Value, SourceKey, SourceValue> {

    protected SerializerIfc<Key, SourceKey> keySerializer;
    protected SerializerIfc<Value, SourceValue> valSerializer;

    public SourceCodec(Source<SourceKey, SourceValue> src, SerializerIfc<Key, SourceKey> keySerializer, SerializerIfc<Value, SourceValue> valSerializer) {
        super(src);
        this.keySerializer = keySerializer;
        this.valSerializer = valSerializer;
        setFlushSource(true);
    }

    @Override
    public void put(Key key, Value val) {
        getSource().put(keySerializer.serialize(key), valSerializer.serialize(val));
    }

    @Override
    public Value get(Key key) {
        return valSerializer.deserialize(getSource().get(keySerializer.serialize(key)));
    }

    @Override
    public void delete(Key key) {
        getSource().delete(keySerializer.serialize(key));
    }

    @Override
    public boolean flushImpl() {
        return false;
    }

    /**
     * Shortcut class when only value conversion is required
     */
    public static class ValueOnly<Key, Value, SourceValue> extends SourceCodec<Key, Value, Key, SourceValue> {
        public ValueOnly(Source<Key, SourceValue> src, SerializerIfc<Value, SourceValue> valSerializer) {
            super(src, new Serializers.Identity<Key>(), valSerializer);
        }
    }

    /**
     * Shortcut class when only key conversion is required
     */
    public static class KeyOnly<Key, Value, SourceKey> extends SourceCodec<Key, Value, SourceKey, Value> {
        public KeyOnly(Source<SourceKey, Value> src, SerializerIfc<Key, SourceKey> keySerializer) {
            super(src, keySerializer, new Serializers.Identity<Value>());
        }
    }

    /**
     * Shortcut class when only value conversion is required and keys are of byte[] type
     */
    public static class BytesKey<Value, SourceValue> extends ValueOnly<byte[], Value, SourceValue> {
        public BytesKey(Source<byte[], SourceValue> src, SerializerIfc<Value, SourceValue> valSerializer) {
            super(src, valSerializer);
        }
    }
}
