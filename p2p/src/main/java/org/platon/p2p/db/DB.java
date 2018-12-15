package org.platon.p2p.db;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;



public interface DB {

    final static int BigIntegerLength = 20;
    void set(final byte[] key, final byte[] value) throws DBException;
    byte[] get(final byte[] key) throws DBException;
    void del(final byte[] key) throws DBException;

    void hset(final byte[] name, final byte[] key, final byte[] value) throws DBException;
    byte[] hget(final byte[] name, final byte[] key) throws DBException;
    List<Pair<byte[], byte[]>> hgetAll(final byte[] name) throws DBException;
    void hdel(final byte[] name, final byte[] key) throws DBException;
    long hsize(final byte[] name)throws DBException;




    void zset(final byte[] name, final byte[] key, final byte[] score)throws DBException;
    void zdel(final byte[] name, final byte[] key) throws DBException;
    byte[] zget(final byte[] name, final byte[] key) throws DBException;
    long zrank(final byte[] name, final byte[] key) throws DBException;
    long zsize(final byte[] name) throws DBException;


}