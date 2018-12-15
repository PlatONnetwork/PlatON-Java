package org.platon.storage.trie;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.platon.common.utils.RandomUtils;
import org.platon.storage.datasource.CachedSource;
import org.platon.storage.datasource.Source;
import org.platon.storage.datasource.WriteCache;
import org.platon.storage.datasource.inmemory.HashMapDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrieTest {

    private Logger logger = LoggerFactory.getLogger("trieTest");

    private Source<byte[], byte[]> dbSource;
    private CachedSource.BytesKey<byte[]> trieCache;
    private TrieImpl stateTrie;

    @Before
    public void before() {
        dbSource = new HashMapDB<>();
        trieCache = new WriteCache.BytesKey<>(dbSource, WriteCache.CacheType.COUNTING);
        stateTrie = new TrieImpl(trieCache, true, 1024);
    }

    public void reset(boolean useSPV) {
        dbSource = new HashMapDB<>();
        trieCache = new WriteCache.BytesKey<>(dbSource, WriteCache.CacheType.COUNTING);
        stateTrie = new TrieImpl(trieCache, useSPV, 1024);
    }

    @Test
    public void put() {


        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();
        this.initTrie(stateTrie, 100, 100, 1000, dataMap, keyList);
        logger.debug("trie结构 : " + stateTrie.dumpStructure());
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);
            Assert.assertArrayEquals(dataMap.get(key), stateTrie.get(key));
        }
    }

    @Test
    public void delete() {

        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();
        this.initTrie(stateTrie, 100, 100, 1000, dataMap, keyList);
        for (int i = 99; i >= 0; i--) {
            int index = (int) (Math.random() * i);
            byte[] key = keyList.get(index);
            stateTrie.delete(key);
            keyList.remove(index);
            logger.debug("第{}次:{}", i, keyList.size());
            Assert.assertNull(stateTrie.get(key));
        }
        logger.debug("trie结构:" + stateTrie.dumpStructure());
    }

    @Test
    public void getRootHash() {

        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();
        this.initTrie(stateTrie, 100, 100, 1000, dataMap, keyList);
        logger.debug("trie结构:" + stateTrie.dumpStructure());


        byte[] hash = stateTrie.getRootHash();


        reset(false);
        stateTrie.put(keyList.get(1), dataMap.get(keyList.get(1)));
        logger.debug("newTrie结构:" + stateTrie.dumpStructure());
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);
            if(i == 1){
                Assert.assertArrayEquals(dataMap.get(key), stateTrie.get(key));
            }
        }
    }

    @Test
    public void spvGetAndVerify() {

        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();

        int dataSize = 200;
        int maxKeyLen = 1000000;
        int maxValLen = 500;

        for (int i = 0; i < dataSize; i++) {

            byte[] key = RandomUtils.randomBytes(RandomUtils.randomInt(maxKeyLen) + 1);

            byte[] value = RandomUtils.randomBytes(RandomUtils.randomInt(maxValLen) + 1);
            dataMap.put(key, value);
            keyList.add(key);
            stateTrie.put(key, value);
        }

        byte[] rootHash = stateTrie.getRootHash();
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);
            byte[] spvHash = stateTrie.bcSHA3Digest256(stateTrie.get(key));
            byte[] spvEncodeData = stateTrie.spvEncodeHashList(key);
            byte[] caculedRootHash;
            try {
                TrieProto.NodeBase nodeBase = TrieProto.NodeBase.parseFrom(spvEncodeData);
                caculedRootHash = nodeBase.getHash().toByteArray();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                throw new RuntimeException("Decode proto data error ");
            }
            Assert.assertArrayEquals(rootHash, caculedRootHash);
            boolean flag = stateTrie.spvVerifyHashList(spvHash, spvEncodeData);
            Assert.assertTrue(flag);
        }
    }

    private Trie initTrie(TrieImpl stateTrie, int dataSize, int maxKeyLen, int maxValLen, Map<byte[], byte[]> dataMap, List<byte[]> keyList) {
        for (int i = 0; i < dataSize; i++) {

            byte[] key = RandomUtils.randomBytes(RandomUtils.randomInt(maxKeyLen));

            byte[] value = RandomUtils.randomBytes(RandomUtils.randomInt(maxValLen));
            dataMap.put(key, value);
            keyList.add(key);
            stateTrie.put(key, value);
        }
        return stateTrie;
    }




    @Test
    public void putHashMap() {


        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();

        logger.debug("trie结构:" + stateTrie.dumpStructure());
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);



            Assert.assertArrayEquals(dataMap.get(key), stateTrie.get(key));
        }
    }

    @Test
    public void deleteHashMap() {

        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();
        this.initTrieHashMap(100, 100, 1000, dataMap, keyList);
        for (int i = 99; i >= 0; i--) {
            int index = (int) (Math.random() * i);
            byte[] key = keyList.get(index);
            stateTrie.delete(key);
            keyList.remove(index);
            logger.debug("第{}次:{} ", i, keyList.size());
            Assert.assertNull(stateTrie.get(key));
        }
        logger.debug("trie结构:" + stateTrie.dumpStructure());
    }

    @Test
    public void getRootHashMapHash() {


        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();
        this.initTrieHashMap(100, 100, 1000, dataMap, keyList);
        logger.debug("trie结构:" + stateTrie.dumpStructure());


        byte[] hash = stateTrie.getRootHash();


        reset(true);
        for (int i = 0; i < keyList.size(); i++) {
            stateTrie.put(keyList.get(i), dataMap.get(keyList.get(i)));
        }
        logger.debug("newTrie结构:" + stateTrie.dumpStructure());
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);
            Assert.assertArrayEquals(dataMap.get(key), stateTrie.get(key));
        }

        byte[] newHash = stateTrie.getRootHash();

        Assert.assertArrayEquals(hash, stateTrie.getRootHash());
    }

    @Test
    public void spvGetAndVerifyHashMap() {

        Map<byte[], byte[]> dataMap = new HashMap<>();
        List<byte[]> keyList = new ArrayList<>();

        int dataSize = 200;
        int maxKeyLen = 1000000;
        int maxValLen = 500;
        TrieImpl trie = new TrieImpl();
        for (int i = 0; i < dataSize; i++) {

            byte[] key = RandomUtils.randomBytes(RandomUtils.randomInt(maxKeyLen) + 1);

            byte[] value = RandomUtils.randomBytes(RandomUtils.randomInt(maxValLen) + 1);
            dataMap.put(key, value);
            trie.put(key, value);
            keyList.add(key);
        }

        byte[] rootHash = trie.getRootHash();
        for (int i = 0; i < keyList.size(); i++) {
            byte[] key = keyList.get(i);
            byte[] spvHash = trie.bcSHA3Digest256(trie.get(key));
            byte[] spvEncodeData = trie.spvEncodeHashList(key);
            byte[] caculedRootHash;
            try {
                TrieProto.NodeBase nodeBase = TrieProto.NodeBase.parseFrom(spvEncodeData);
                caculedRootHash = nodeBase.getHash().toByteArray();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                throw new RuntimeException("Decode proto data error");
            }
            Assert.assertArrayEquals(rootHash, caculedRootHash);
            boolean flag = trie.spvVerifyHashList(spvHash, spvEncodeData);
            Assert.assertTrue(flag);
        }
    }

    private Trie initTrieHashMap(int dataSize, int maxKeyLen, int maxValLen, Map<byte[], byte[]> dataMap, List<byte[]> keyList) {
        for (int i = 0; i < dataSize; i++) {

            byte[] key = RandomUtils.randomBytes(RandomUtils.randomInt(maxKeyLen) + 1);

            byte[] value = RandomUtils.randomBytes(RandomUtils.randomInt(maxValLen) + 2);
            dataMap.put(key, value);
            keyList.add(key);
            stateTrie.put(key, value);
        }
        return stateTrie;
    }
}