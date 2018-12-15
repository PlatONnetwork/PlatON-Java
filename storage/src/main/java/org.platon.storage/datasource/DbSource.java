package org.platon.storage.datasource;

import java.util.Set;

public interface DbSource<V> extends BatchSource<byte[], V> {

    void setName(String name);

    void open() throws RuntimeException;

    void open(DbSettings settings) throws RuntimeException;

    boolean isAlive();

    void reset();

    void close() throws Exception;

    V prefixLookup(byte[] key, int prefixBytes) throws RuntimeException;

    String getName();

    Set<byte[]> keys() throws RuntimeException;
}
