package org.platon.p2p.pubsub;

import org.apache.http.util.Asserts;
import org.junit.Test;

/**
 * @author yangzhou
 * @create 2018-07-31 13:47
 */
public class TimeCacheTest {

    @Test
    public void testTimeCache() throws InterruptedException {
        TimeCache timeCache = new TimeCache(1000);

        for (int i = 0; i < 1000; i++) {
            timeCache.add(String.valueOf(i));
        }

        Thread.sleep(1001);
        timeCache.sweep();

        for (int i = 0; i < 1000; i++) {
            Asserts.check(!timeCache.has(String.valueOf(i)), "has expired element:" + i);
        }

        for (int i = 1001; i < 2000; i++) {
            timeCache.add(String.valueOf(i));
        }

        for (int i = 1001; i < 2000; i++) {
            Asserts.check(timeCache.has(String.valueOf(i)), "no has element:" + i);

        }
    }
}
