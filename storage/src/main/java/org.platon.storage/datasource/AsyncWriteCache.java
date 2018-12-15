package org.platon.storage.datasource;

import com.google.common.util.concurrent.*;
import org.platon.common.AppenderName;
import org.platon.storage.utils.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AsyncWriteCache<Key, Value> extends AbstractCachedSource<Key, Value>
        implements AsyncFlushable {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_DB);

    private static ListeningExecutorService flushExecutor = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("AsyncWriteCacheThread - %d").build()));

    protected volatile WriteCache<Key, Value> curCache;
    protected WriteCache<Key, Value> flushingCache;

    private ListenableFuture<Boolean> lastFlush = Futures.immediateFuture(false);

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final AutoLock rLock = new AutoLock(rwLock.readLock());
    private final AutoLock wLock = new AutoLock(rwLock.writeLock());

    private String name = "<null>";

    public AsyncWriteCache(Source<Key, Value> source) {
        super(source);
        flushingCache = createCache(source);
        flushingCache.setFlushSource(true);
        curCache = createCache(flushingCache);
    }

    protected abstract WriteCache<Key, Value> createCache(Source<Key, Value> source);

    @Override
    public Collection<Key> getModified() {
        try (AutoLock l = rLock.lock()) {
            return curCache.getModified();
        }
    }

    @Override
    public boolean hasModified() {
        try (AutoLock l = rLock.lock()) {
            return curCache.hasModified();
        }
    }

    @Override
    public void put(Key key, Value val) {
        try (AutoLock l = rLock.lock()) {
            curCache.put(key, val);
        }
    }

    @Override
    public void delete(Key key) {
        try (AutoLock l = rLock.lock()) {
            curCache.delete(key);
        }
    }

    @Override
    public Value get(Key key) {
        try (AutoLock l = rLock.lock()) {
            return curCache.get(key);
        }
    }

    @Override
    public synchronized boolean flush() {
        try {
            flipStorage();
            flushAsync();
            return flushingCache.hasModified();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    Entry<Value> getCached(Key key) {
        return curCache.getCached(key);
    }

    @Override
    public synchronized void flipStorage() throws InterruptedException {

        try {
            if (!lastFlush.isDone())
                logger.debug("AsyncWriteCache (" + name + "): waiting for previous flush to complete");
            lastFlush.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        try (AutoLock l = wLock.lock()) {
            flushingCache.cache = curCache.cache;
            curCache = createCache(flushingCache);
        }
    }

    public synchronized ListenableFuture<Boolean> flushAsync() throws InterruptedException {
        if (logger.isDebugEnabled()) {
            logger.debug("AsyncWriteCache (" + name + "): flush submitted");
        }
        lastFlush = flushExecutor.submit(() -> {
            if (logger.isDebugEnabled()) {
                logger.debug("AsyncWriteCache (" + name + "): flush started");
            }
            long s = System.currentTimeMillis();
            boolean ret = flushingCache.flush();
            if (logger.isDebugEnabled()) {
                logger.debug("AsyncWriteCache (" + name + "): flush completed in " + (System.currentTimeMillis() - s) + " ms");
            }
            return ret;
        });
        return lastFlush;
    }

    @Override
    public long estimateCacheSize() {


        return (long) (curCache.estimateCacheSize() * 2.0);
    }

    @Override
    protected synchronized boolean flushImpl() {
        return false;
    }

    public AsyncWriteCache<Key, Value> withName(String name) {
        this.name = name;
        return this;
    }
}
