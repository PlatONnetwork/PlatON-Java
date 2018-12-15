package org.platon.storage.datasource;

import org.junit.Assert;
import org.junit.Test;
import org.platon.common.utils.ByteComparator;
import org.platon.common.wrapper.DataWord;
import org.platon.crypto.HashUtil;
import org.platon.storage.datasource.inmemory.HashMapDB;

import java.math.BigInteger;

public class WriteCacheTest {

    public byte[] toKey(int intKey){
        return HashUtil.sha3(BigInteger.valueOf(intKey).toByteArray());
    }

    public byte[] toV(int intV) {
        return DataWord.of(intV).getData();
    }

    @Test
    public void test01() {

        Source<byte[], byte[]> dbSource = new HashMapDB<>();
        WriteCache<byte[], byte[]> writeCache = new WriteCache.BytesKey<>(dbSource, WriteCache.CacheType.SIMPLE);
        
        for (int i = 0; i < 1_000; ++i) {
            writeCache.put(toKey(i), DataWord.of(i).getData());
        }
        
        Assert.assertTrue(ByteComparator.equals(DataWord.of(0).getData(), writeCache.getCached(toKey(0)).value()));
        Assert.assertTrue(ByteComparator.equals(DataWord.of(999).getData(), writeCache.getCached(toKey(999)).value()));

        writeCache.flush();


        Assert.assertNull(writeCache.getCached(toKey(0)));
        Assert.assertNull(writeCache.getCached(toKey(999)));


        Assert.assertTrue(ByteComparator.equals(DataWord.of(0).getData(), writeCache.get(toKey(0))));
        Assert.assertTrue(ByteComparator.equals(DataWord.of(999).getData(), writeCache.get(toKey(999))));

        writeCache.put(toKey(0),toV(111));
        Assert.assertTrue(ByteComparator.equals(toV(111), writeCache.getCached(toKey(0)).value()));

        writeCache.delete(toKey(0));

        Assert.assertTrue(null == writeCache.getCached(toKey(0)) || null == writeCache.getCached(toKey(0)).value());
        writeCache.flush();
        Assert.assertNull(dbSource.get(toKey(0)));

    }

}
