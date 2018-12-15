package org.platon.storage;

import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.RandomUtils;
import org.platon.storage.datasource.DbSource;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


public class StorageTest {

    public void updateBatch(DbSource storage, Logger logger) {
        try {
            int batchSize = 100;
            Map<ByteArrayWrapper, byte[]> rows = createBatch(batchSize);
            storage.updateBatch(rows);
            for (Map.Entry<ByteArrayWrapper, byte[]> e : rows.entrySet()) {
                assertArrayEquals(e.getValue(), rows.get(e.getKey()));
            }
        } catch (Exception e) {
            logger.error("batchUpdate error", e);
            assertFalse(true);
        }
    }

    public void put(DbSource storage, Logger logger) {
        try {
            byte[] key = RandomUtils.randomBytes(32);
            byte[] value = RandomUtils.randomBytes(32);
            storage.put(key, value);
            assertArrayEquals(value, (byte[]) storage.get(key));
        } catch (Exception e) {
            logger.error("put error", e);
            assertFalse(true);
        }
    }

    public void keys(DbSource storage, Logger logger) {
        try {
            byte[] key = RandomUtils.randomBytes(32);
            byte[] value = RandomUtils.randomBytes(32);
            storage.put(key, value);
            Set<ByteArrayWrapper> keys = storage.keys();
            assertTrue(keys.contains(new ByteArrayWrapper(key)));
        } catch (Exception e) {
            logger.error("keys error", e);
            assertFalse(true);
        }
    }

    public void delete(DbSource storage, Logger logger) {
        try {
            byte[] key = RandomUtils.randomBytes(32);
            byte[] value = RandomUtils.randomBytes(32);
            storage.put(key, value);
            assertArrayEquals(value, (byte[]) storage.get(key));
            storage.delete(key);
            assertArrayEquals(null, (byte[]) storage.get(key));
        } catch (Exception e) {
            logger.error("delete error", e);
            assertFalse(true);
        }
    }

    public static Map<ByteArrayWrapper, byte[]> createBatch(int batchSize) {
        HashMap<ByteArrayWrapper, byte[]> result = new HashMap<>();
        while (result.size() < batchSize) {
            result.put(new ByteArrayWrapper(RandomUtils.randomBytes(32)), RandomUtils.randomBytes(32));
        }
        return result;
    }

}
