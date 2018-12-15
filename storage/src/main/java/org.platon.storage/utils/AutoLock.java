package org.platon.storage.utils;

import java.util.concurrent.locks.Lock;

public final class AutoLock implements AutoCloseable {

    private final Lock lock;

    public AutoLock(Lock l) {
        this.lock = l;
    }

    public final AutoLock lock() {
        this.lock.lock();
        return this;
    }

    public final void close() {
        this.lock.unlock();
    }
}
