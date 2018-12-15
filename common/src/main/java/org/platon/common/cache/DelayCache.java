package org.platon.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lvxy
 * @version 0.0.1
 * @date 2018/8/28 13:43
 */
public class DelayCache <K, V> {
    private static Logger logger = LoggerFactory.getLogger(DelayCache.class);

    private ConcurrentMap<K, V> cache = new ConcurrentHashMap<K, V>();

    private DelayQueue<DelayItem> q = new DelayQueue<DelayItem>();

    private TimeoutCallbackFunction timeoutCallback;

    private Thread daemonThread;

    public DelayCache(TimeoutCallbackFunction timeoutCallback) {
        this();
        this.timeoutCallback = timeoutCallback;
    }
    public DelayCache() {

        Runnable daemonTask = new Runnable() {
            public void run() {
                daemonCheck();
            }
        };

        daemonThread = new Thread(daemonTask);
        daemonThread.setDaemon(true);
        daemonThread.setName("Cache Daemon");
        daemonThread.start();
    }

    public int getSize(){
        return cache.size();
    }

    private void daemonCheck() {
        for (;;) {
            try {
                DelayItem delayItem = q.take();
                V value = cache.remove(delayItem.getItem());
                if (timeoutCallback != null) {
                    //V value = cache.get((K)delayItem.getItem());
                    timeoutCallback.timeout(delayItem.getItem(), value);
                }
                //cache.remove((K)delayItem.getItem());
            } catch (InterruptedException e) {
                logger.error("error:", e);
                break;
            }
        }
    }

    public synchronized void put(K key, V value, long time, TimeUnit unit) {
        V oldValue =  cache.putIfAbsent(key, value);
        if (oldValue != null) {
            q.remove(new DelayItem(key, time, unit));
        }
        q.put(new DelayItem(key, time, unit));
    }


    public synchronized V remove(K key) {
        V oldValue =  cache.remove(key);
        q.remove(new DelayItem(key, 1, TimeUnit.SECONDS));
        return oldValue;
    }

    public V get(K key) {
        return cache.get(key);
    }


    public void setTimeoutCallback(TimeoutCallbackFunction timeoutCallback) {
        this.timeoutCallback = timeoutCallback;
    }

    public interface TimeoutCallbackFunction<K, V> {
        void timeout(K key, V value);
    }


    public static void main(String[] args) throws InterruptedException {
        DelayCache<String, Integer> testCache = new DelayCache<>();
        testCache.setTimeoutCallback((key, value)->{
                System.out.println("key:" + key);
                System.out.println("value:" + value);
            });

        testCache.put("key1", 1, 3, TimeUnit.SECONDS);

        testCache.remove("key1");

        TimeUnit.SECONDS.sleep(100);


    }


}
