package org.platon.p2p.router;

import com.google.protobuf.Message;
import org.platon.common.cache.DelayCache;
import org.platon.p2p.common.PeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author yangzhou
 * @create 2018-04-26 19:54
 */
@Component
public class RequestManager {

    private static Logger logger = LoggerFactory.getLogger(RequestManager.class);

    private static DelayCache.TimeoutCallbackFunction requestTimeoutCallback = new DelayCache.TimeoutCallbackFunction() {

        @Override
        public void timeout(Object key, Object value) {
            String transactionId = (String)key;
            CompletableFuture<Message> future = (CompletableFuture<Message>)value;
            if (future.isDone()) {
                logger.trace("message is complete before timeout, txId:={}.", transactionId);
                return;
            }
            future.completeExceptionally(new Exception(String.format("Request %s times out", transactionId)));
            logger.warn("Complete pending future exceptionally with timeout, txId:={}.", transactionId);
        }
    };
    private static DelayCache<String, CompletableFuture<Message>> responseFutureCache = new DelayCache<>(requestTimeoutCallback);


    
    public void put(String id, CompletableFuture<Message> responseFuture) {


        responseFutureCache.put(id, responseFuture, PeerConfig.getInstance().getMessageResponseTimeout(), TimeUnit.SECONDS);
        logger.debug("Save pending future, txId:={}.", id);
    }

    public CompletableFuture<Message> remove(String messageTxId){
        return responseFutureCache.remove(messageTxId);
    }


    
    public void handleResponse(String id, Message msg) {



        CompletableFuture<Message> pending = this.remove(id);

        logger.trace("All pending Futures :::::::::::::: " + responseFutureCache.getSize());
        if (pending != null) {
            logger.debug("Complete pending future, txId:={}.", id);
            pending.complete(msg);
        }
    }

    public void handleException(String id, Throwable throwable) {
        logger.trace("Received throwable, txId:={}.", id);
        logger.trace("All pending Futures :::::::::::::: " + responseFutureCache.getSize());


        CompletableFuture<Message> pending = this.remove(id);
        if (pending != null) {
            logger.debug("Complete pending future exceptionally, txId:={}.", id);
            pending.completeExceptionally(throwable);
        }
    }
}
