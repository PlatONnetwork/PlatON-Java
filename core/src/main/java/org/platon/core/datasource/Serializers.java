package org.platon.core.datasource;

import org.platon.common.wrapper.DataWord;
import org.platon.core.Account;
import org.platon.core.block.BlockHeader;
import org.platon.core.codec.DataWordCodec;
import org.platon.storage.datasource.SerializerIfc;

public class Serializers {

    /**
     *  No conversion
     */
    public static class Identity<T> implements SerializerIfc<T, T> {
        @Override
        public T serialize(T object) {
            return object;
        }
        @Override
        public T deserialize(T stream) {
            return stream;
        }
    }

    public final static SerializerIfc<Account, byte[]> AccountStateSerializer = new SerializerIfc<Account, byte[]>() {
        @Override
        public byte[] serialize(Account object) {
            return object.getEncoded();
        }

        @Override
        public Account deserialize(byte[] stream) {
            return stream == null || stream.length == 0 ? null : new Account(stream);
        }
    };

    public final static SerializerIfc<DataWord, byte[]> StorageKeySerializer = new SerializerIfc<DataWord, byte[]>() {
        @Override
        public byte[] serialize(DataWord object) {
            return object.getData();
        }

        @Override
        public DataWord deserialize(byte[] stream) {
            return DataWord.of(stream);
        }
    };

    public final static SerializerIfc<DataWord, byte[]> StorageValueSerializer = new SerializerIfc<DataWord, byte[]>() {

        @Override
        public byte[] serialize(DataWord object) {
            return DataWordCodec.encode(object).toByteArray();
        }

        @Override
        public DataWord deserialize(byte[] stream) {
            if (stream == null || stream.length == 0) return null;
            return DataWordCodec.decode(stream);
        }
    };

    /*public final static SerializerIfc<Value, byte[]> TrieNodeSerializer = new SerializerIfc<Value, byte[]>() {
        @Override
        public byte[] serialize(Value object) {
            return object.asBytes();
        }

        @Override
        public Value deserialize(byte[] stream) {
            return new Value(stream);
        }
    };*/


    public final static SerializerIfc<BlockHeader, byte[]> BlockHeaderSerializer = new SerializerIfc<BlockHeader, byte[]>() {
        @Override
        public byte[] serialize(BlockHeader object) {
            return object == null ? null : object.encode();
        }

        @Override
        public BlockHeader deserialize(byte[] stream) {
            return stream == null ? null : new BlockHeader(stream);
        }
    };

    /**
     * AS IS serializer (doesn't change anything)
     */
    public final static SerializerIfc<byte[], byte[]> AsIsSerializer = new SerializerIfc<byte[], byte[]>() {
        @Override
        public byte[] serialize(byte[] object) {
            return object;
        }

        @Override
        public byte[] deserialize(byte[] stream) {
            return stream;
        }
    };
}
