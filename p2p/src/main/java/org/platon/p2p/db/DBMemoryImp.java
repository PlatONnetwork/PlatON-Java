package org.platon.p2p.db;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;


@Component("db")
public class DBMemoryImp implements DB{
    Map<ByteArrayWrapper, ByteArrayWrapper> storage;

    Map<ByteArrayWrapper, Map<ByteArrayWrapper, ByteArrayWrapper>> setStorage;

    Map<ByteArrayWrapper, Map<BigInteger, List<ByteArrayWrapper>>> zsetScoreStorage;
    Map<ByteArrayWrapper, Map<ByteArrayWrapper, BigInteger>> zsetStorage;



    public static class ByteArrayWrapper {
        private final byte[] data;

        public ByteArrayWrapper(byte[] data)
        {
            if (data == null)
            {
                throw new NullPointerException();
            }
            this.data = data;
        }

        public static ByteArrayWrapper valueOf(byte[] data) {
            return new ByteArrayWrapper(data);
        }
        byte[] toBytes() {
            return data;
        }
        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof ByteArrayWrapper))
            {
                return false;
            }
            return Arrays.equals(data, ((ByteArrayWrapper)other).data);
        }

        @Override
        public int hashCode()
        {
            return Arrays.hashCode(data);
        }
    }
    public DBMemoryImp() {
        storage = new HashMap<>();

        setStorage = new HashMap<>();

        zsetStorage = new HashedMap();
        zsetScoreStorage = new HashedMap();
    }

    public synchronized void set(final byte[] key, final byte[] value) throws DBException{
        
        storage.put(ByteArrayWrapper.valueOf(key), ByteArrayWrapper.valueOf(value));
    }

    public synchronized byte[] get(final byte[] key) throws DBException {
        ByteArrayWrapper value =  storage.get(ByteArrayWrapper.valueOf(key));
        return value == null ? null : value.toBytes();
    }

    public synchronized void del(final byte[] key) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);

        if (storage.containsKey(wrapperKey)) {
            storage.remove(wrapperKey);
        }

    }

    public synchronized void hset(final byte[] name, final byte[] key, final byte[] value) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperValue = ByteArrayWrapper.valueOf(value);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);

        if (!setStorage.containsKey(wrapperName)) {
            Map<ByteArrayWrapper, ByteArrayWrapper> data = new HashMap<>();
            data.put(wrapperKey, wrapperValue);
            setStorage.put(wrapperName, data);
        }
        setStorage.get(wrapperName).put(wrapperKey, wrapperValue);

    }
    public synchronized byte[] hget(final byte[] name, final byte[] key) throws DBException{
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);
        if (!setStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }

        ByteArrayWrapper value = setStorage.get(wrapperName).get(wrapperKey);
        return value == null ? null : value.toBytes();
    }

    public synchronized List<Pair<byte[], byte[]>> hgetAll(final byte[] name) throws DBException {
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);
        if (!setStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }
        List<Pair<byte[], byte[]>> res = new ArrayList<>();
        for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> n : setStorage.get(wrapperName).entrySet()) {
            res.add(Pair.of(n.getKey().toBytes(), n.getValue().toBytes()));
        }
        return res;
    }

    public synchronized void hdel(final byte[] name, final byte[] key) throws DBException{
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);
        if (!setStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }
        if (setStorage.containsKey(wrapperName)) {
            setStorage.get(wrapperName).remove(wrapperKey);

        }
    }

    public synchronized long hsize(final byte[] name) throws DBException {
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);
        if (!setStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }
        return setStorage.get(wrapperName).size();
    }

    public synchronized void zset(final byte[] name, final byte[] key, final byte[] score) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);
        BigInteger bigIntegerScore = new BigInteger(score);



        if (!zsetScoreStorage.containsKey(wrapperName)) {

            Map<ByteArrayWrapper, BigInteger> keyMap = new HashMap<>();
            Map<BigInteger, List<ByteArrayWrapper>> scoreMap = new TreeMap<>();
            List<ByteArrayWrapper> keyList = new ArrayList<>();
            keyMap.put(wrapperKey, bigIntegerScore);
            keyList.add(wrapperKey);
            scoreMap.put(bigIntegerScore, keyList);
            zsetScoreStorage.put(wrapperName, scoreMap);
            zsetStorage.put(wrapperName, keyMap);
        } else {
            if (zsetStorage.get(wrapperName).containsKey(wrapperKey)) {

                BigInteger old = zsetStorage.get(wrapperName).get(wrapperKey);
                zsetScoreStorage.get(wrapperName).get(old).remove(wrapperKey);
            } else {
                zsetStorage.get(wrapperName).put(wrapperKey, bigIntegerScore);
            }

            if (!zsetScoreStorage.get(wrapperName).containsKey(bigIntegerScore)) {
                List<ByteArrayWrapper> keyList = new ArrayList<>();
                keyList.add(wrapperKey);
                zsetScoreStorage.get(wrapperName).put(bigIntegerScore, keyList);
            } else {
                zsetScoreStorage.get(wrapperName).get(bigIntegerScore).add(wrapperKey);
            }
        }
    }

    public synchronized void zdel(final byte[] name, final byte[] key) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);

        if (!zsetStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }

        if (zsetStorage.get(wrapperName).containsKey(wrapperKey)) {
            BigInteger old = zsetStorage.get(wrapperName).get(wrapperKey);
            zsetScoreStorage.get(wrapperName).get(old).remove(wrapperKey);
            zsetStorage.get(wrapperName).remove(wrapperKey);
        }
    }

    public synchronized byte[] zget(final byte[] name, final byte[] key) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);

        if (!zsetStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }

        if (zsetStorage.get(wrapperName).containsKey(wrapperKey)) {
            return zsetStorage.get(wrapperName).get(wrapperKey).toByteArray();
        }
        return null;
    }

    public synchronized long zrank(final byte[] name, final byte[] key) throws DBException {
        ByteArrayWrapper wrapperKey = ByteArrayWrapper.valueOf(key);
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);

        if (!zsetStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }

        int pos = 0;
        if (zsetStorage.get(wrapperName).containsKey(wrapperKey)) {
            BigInteger old = zsetStorage.get(wrapperName).get(wrapperKey);

            Iterator<BigInteger> iter = zsetScoreStorage.get(wrapperName).keySet().iterator();
            while (iter.hasNext()) {
                BigInteger n = iter.next();
                if (old != n) {
                    pos += 1;
                } else {
                    break;
                }

            }
        }
        return pos;
    }

    public synchronized long zsize(final byte[] name) throws DBException {
        ByteArrayWrapper wrapperName = ByteArrayWrapper.valueOf(name);

        if (!zsetStorage.containsKey(wrapperName)){
            throw new DBException("key is not in Set");
        }
        return zsetStorage.get(wrapperName).size();
    }

}
