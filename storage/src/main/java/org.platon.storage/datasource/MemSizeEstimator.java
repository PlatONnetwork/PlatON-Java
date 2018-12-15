package org.platon.storage.datasource;

public interface MemSizeEstimator<E> {

    long estimateSize(E e);

    MemSizeEstimator<byte[]> ByteArrayEstimator = bytes -> {

        return bytes == null ? 0 : bytes.length + 16;
    };

}
