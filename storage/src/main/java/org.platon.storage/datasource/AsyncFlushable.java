package org.platon.storage.datasource;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncFlushable {

    
    void flipStorage() throws InterruptedException;

    
    ListenableFuture<Boolean> flushAsync() throws InterruptedException;
}
