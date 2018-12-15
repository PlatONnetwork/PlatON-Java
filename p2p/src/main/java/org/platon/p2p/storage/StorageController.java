package org.platon.p2p.storage;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.apache.commons.lang3.tuple.Pair;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.storage.*;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author yangzhou
 * @create 2018-05-04 16:42
 */
@Component("storageController")
public class StorageController {
    private static Logger logger = LoggerFactory.getLogger(StorageController.class);

    @Autowired
    private MessageRouter messageRouter;

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public CompletableFuture<SetStoreRespMessage> set(final ResourceID key, final byte[] value) {
        return set(key, value, -1);
    }

    public CompletableFuture<SetStoreRespMessage> set(final ResourceID key, final byte[] value, long lifetime) {
        final CompletableFuture<SetStoreRespMessage> res = new CompletableFuture<>();
        SetStoreMessage.Builder setStoreMsgBuilder = SetStoreMessage.newBuilder();

        setStoreMsgBuilder.setResourceId(key);

        StoredData.Builder storedDataBuilder = StoredData.newBuilder();
        storedDataBuilder.setLifeTime(lifetime);
        storedDataBuilder.setStorageTime(System.currentTimeMillis());
        storedDataBuilder.setValue(ByteString.copyFrom(value));
        setStoreMsgBuilder.setStoredData(storedDataBuilder);

        CompletableFuture<Message> futMsg = messageRouter.sendRequest(
                setStoreMsgBuilder.build(), Collections.singletonList(NodeUtils.toRoutableID(key)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        futMsg.thenAcceptAsync(ansMsg->{
            res.complete((SetStoreRespMessage)ansMsg);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<byte[]> get(final ResourceID key) {
        final CompletableFuture<byte[]> res = new CompletableFuture<byte[]>();
        GetStoreMessage.Builder storeMessageBuilder = GetStoreMessage.newBuilder();
        storeMessageBuilder.setResourceId(key);

        StoredData.Builder storedDataBuilder = StoredData.newBuilder();

        CompletableFuture<Message> fut = messageRouter.sendRequest(storeMessageBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(key)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        fut.thenAcceptAsync(futMsg->{
            GetStoreRespMessage storeMsg = (GetStoreRespMessage)futMsg;
            if (!storeMsg.hasStoredData()) {
                res.complete(null);
            } else {
                res.complete(storeMsg.getStoredData().getValue().toByteArray());
            }
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<DelStoreRespMessage> del(final ResourceID key) {
        final CompletableFuture<DelStoreRespMessage> res = new CompletableFuture<>();

        DelStoreMessage.Builder delStoreMsgBuilder = DelStoreMessage.newBuilder();
        delStoreMsgBuilder.setResourceId(key);


        CompletableFuture<Message> futMsg = messageRouter.sendRequest(delStoreMsgBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(key)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION,
                true);

        futMsg.thenAcceptAsync(ansMsg->{
            res.complete((DelStoreRespMessage)ansMsg);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<HSetStoreRespMessage> hset(final ResourceID name, final byte[] key, final byte[] value) {
        return hset(name, key, value, -1);
    }

    public CompletableFuture<HSetStoreRespMessage> hset(final ResourceID name, final byte[] key, final byte[] value, long lifetime) {
        final CompletableFuture<HSetStoreRespMessage> res = new CompletableFuture<>();

        HSetStoreMessage.Builder hSetStoreMsgBuilder = HSetStoreMessage.newBuilder();
        hSetStoreMsgBuilder.setResourceId(name);
        hSetStoreMsgBuilder.setKey(ByteString.copyFrom(key));
        StoredData.Builder dataBuilder = StoredData.newBuilder();

        dataBuilder.setValue(ByteString.copyFrom(value));
        dataBuilder.setLifeTime(lifetime);
        dataBuilder.setStorageTime(System.currentTimeMillis());
        hSetStoreMsgBuilder.setStoredData(dataBuilder);
        CompletableFuture<Message> futMsg = messageRouter.sendRequest( hSetStoreMsgBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(name)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        futMsg.thenAcceptAsync(ansMsg->{
            res.complete((HSetStoreRespMessage)ansMsg);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<byte[]> hget(final ResourceID name, final byte[] key) {
        final CompletableFuture<byte[]> res = new CompletableFuture<>();
        HGetStoreMessage.Builder hGetStoreMsgBuilder = HGetStoreMessage.newBuilder();
        hGetStoreMsgBuilder.setResourceId(name);
        hGetStoreMsgBuilder.setKey(ByteString.copyFrom(key));
        StoredData.Builder dataBuilder = StoredData.newBuilder();
        dataBuilder.setStorageTime(System.currentTimeMillis());
        CompletableFuture<Message> fut = messageRouter.sendRequest( hGetStoreMsgBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(name)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        fut.thenAcceptAsync(futMsg -> {
            HGetStoreRespMessage hGetStoreRespMsg = (HGetStoreRespMessage)futMsg;
            if (hGetStoreRespMsg.getStoredDataList().isEmpty()){
                res.complete(null);
            }
            res.complete(hGetStoreRespMsg.getStoredDataList().get(0).getValue().toByteArray());
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<List<Pair<byte[], byte[]>>> hgetAll(final ResourceID name) {
        final CompletableFuture<List<Pair<byte[], byte[]>>> res = new CompletableFuture<>();

        HGetAllStoreMessage.Builder hGetAllStoreMsgBuilder = HGetAllStoreMessage.newBuilder();
        hGetAllStoreMsgBuilder.setResourceId(name);
        StoredData.Builder data = StoredData.newBuilder();
        data.setStorageTime(System.currentTimeMillis());
        CompletableFuture<Message> fut = messageRouter.sendRequest(
                hGetAllStoreMsgBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(name)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION,
                true);

        fut.thenAcceptAsync(futMsg -> {
            HGetAllStoreRespMessage hGetAllStoreRespMsg = (HGetAllStoreRespMessage)futMsg;
            List<Pair<byte[], byte[]>> kvList = new ArrayList<>();
            for (StoredData v : hGetAllStoreRespMsg.getStoredDataList()){
                kvList.add(Pair.of(v.getKey().toByteArray(), v.getValue().toByteArray()));
            }
            res.complete(kvList);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

    public CompletableFuture<HDelStoreRespMessage> hdel(final ResourceID name, final byte[] key) {
        final CompletableFuture<HDelStoreRespMessage> res = new CompletableFuture<>();

        HDelStoreMessage.Builder hDelStoreMsgBuilder = HDelStoreMessage.newBuilder();
        hDelStoreMsgBuilder.setResourceId(name);
        hDelStoreMsgBuilder.setKey(ByteString.copyFrom(key));
        StoredData.Builder data = StoredData.newBuilder();

        data.setStorageTime(System.currentTimeMillis());
        CompletableFuture<Message> futMsg = messageRouter.sendRequest(hDelStoreMsgBuilder.build(),
                Collections.singletonList(NodeUtils.toRoutableID(name)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION,
                true);

        futMsg.thenAcceptAsync(ansMsg->{
            res.complete((HDelStoreRespMessage)ansMsg);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });
        return res;
    }

}
