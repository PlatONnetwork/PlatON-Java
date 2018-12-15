package org.platon.storage.datasource.leveldb;

import org.junit.*;
import org.platon.common.utils.ByteComparator;
import org.platon.crypto.HashUtil;
import org.platon.storage.datasource.DbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Ignore
public class LevelDBSourceTest {

    private Logger logger = LoggerFactory.getLogger("leveldbtest");

    private DbSource<byte[]> dbSource;

    @Before
    public void before(){
        String dir = System.getProperty("user.dir");
        dbSource = new LevelDBSource("testdb", dir);
        dbSource.reset();
        dbSource.open();
    }

    @Test
    public void testUpdateBatch() {
        final int dataSize = 50;
        Map<byte[], byte[]> rows = createData(dataSize);
        dbSource.updateBatch(rows);

        Assert.assertEquals(dataSize, dbSource.keys().size());
        try {
            dbSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPutAndDelete(){


        byte[] key = randomBytes(32);
        byte[] value = HashUtil.sha3(new byte[]{1, 2});
        dbSource.put(key, value);
        Assert.assertNotNull(dbSource.get(key));


        byte[] res = dbSource.get(key);
        Assert.assertTrue(ByteComparator.equals(value, res));


        dbSource.delete(key);
        Assert.assertNull(dbSource.get(key));
    }

    static Map<byte[], byte[]> createData(int size){
        Map<byte[], byte[]> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            result.put(randomBytes(32), randomBytes(32));
        }
        return result;
    }

    static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    @After
    public void after(){
        try{
            if (dbSource != null) {
                dbSource.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}