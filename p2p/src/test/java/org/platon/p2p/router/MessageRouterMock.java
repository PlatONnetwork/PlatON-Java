package org.platon.p2p.router;

import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.proto.common.RoutableID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class MessageRouterMock extends MessageRouter {

    Logger logger = LoggerFactory.getLogger(MessageRouterMock.class);

    public final Map<String, Integer> requestCount = new HashMap<>();

    private final Map<String, Map<RoutableID, List<Message>>> requestMessage = new HashMap<>();

    private final Map<String, RequestMessageCallback> requestMessageCallback = new HashMap<>();
    private final Map<String, ResponseMessageCallback> responseMessageCallback = new HashMap<>();
    private final Map<String, ForwardRequestMessageCallback> forwardRequestMessageCallbackHashMap = new HashMap<>();

    public void addForwardRequestCallback(String methodId, ForwardRequestMessageCallback callback) {
        forwardRequestMessageCallbackHashMap.put(methodId, callback);
    }

    public void addRequestCallback(String methodId, RequestMessageCallback callback) {
        requestMessageCallback.put(methodId, callback);
    }

    public void addResponseCallback(String methodId, ResponseMessageCallback callback) {
        responseMessageCallback.put(methodId, callback);
    }

    public String methodId(Message msg) {
        return ProtoBufHelper.getFullName(msg.getClass());
    }

    public void clearRequest(String methodId) {

        synchronized (requestCount) {
            requestCount.remove(methodId);
            requestCount.put(methodId, 0);
        }
    }

    public Integer getRequestCount(String methodId) {
        Integer count = null;
        synchronized (requestCount) {
            count = requestCount.get(methodId);
        }
        return count == null ? 0 : count;
    }

    public void dumpRequestMessage(){
        for (Map.Entry<String, Map<RoutableID, List<Message>>> m : requestMessage.entrySet()) {
            logger.trace("method:{}" , m.getKey());
            for (Map.Entry<RoutableID, List<Message>> r : m.getValue().entrySet()) {
                logger.trace("RoutableID:{}", NodeUtils.getNodeIdString(r.getKey().getId().toByteArray()));
                for (Message message : r.getValue()) {
                    logger.trace("message:{}", message.toString());
                }
            }
        }
    }

    public Map<RoutableID, List<Message>> getMessage(String methodId){
        return requestMessage.get(methodId);
    }

    @Override
    public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, ForwardingOptionType type, boolean isReturn) {

        String methodId = ProtoBufHelper.getFullName(msg.getClass());

        recordCount(methodId);
        recordMessage(methodId, msg, dest);

        RequestMessageCallback callback = requestMessageCallback.get(methodId);

        if (callback != null) {
            return callback.sendRequest(msg, dest, type, isReturn);
        }

        if (isReturn) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("none callback"));
            return future;
        }

        return null;
    }

    private void recordCount(String methodId){
        synchronized (requestCount) {

            Integer count = requestCount.get(methodId);
            if (count == null) {
                requestCount.putIfAbsent(methodId, 1);
            } else {
                requestCount.put(methodId, count+1);
            }
        }

    }
    private void recordMessage(String methodId, Message msg, List<RoutableID> dest){
        synchronized (requestMessage) {
            Map<RoutableID, List<Message>> peerMessage = requestMessage.get(methodId);
            if (peerMessage == null) {
                peerMessage = new HashMap<>();
                requestMessage.put(methodId, peerMessage);
            }

            for (RoutableID routableID : dest) {
                    List<Message> listMessage = peerMessage.get(routableID);
                    if (listMessage == null) {
                        listMessage = new ArrayList<>();
                    }

                    listMessage.add(msg);
                    peerMessage.put(routableID, listMessage);



            }
        }
    }

    public void sendFrowardRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn, HeaderHelper header) {
        String methodId = methodId(msg);
        ForwardRequestMessageCallback callback = forwardRequestMessageCallbackHashMap.get(methodId);

        Asserts.notNull(callback, "no callback deal methodid:" + methodId);
        if (callback != null) {
            callback.sendRequest(msg, dest, type, isReturn, header);
        }


    }

    @Override
    public void sendResponse(Message msg, String transactionID, List<RoutableID> dest, MessageRouter.ForwardingOptionType type) {
        String methodId = methodId(msg);
        ResponseMessageCallback callback = responseMessageCallback.get(methodId);

        Asserts.notNull(callback, "no callback deal methodid:" + methodId);
        if (callback != null) {
            callback.sendResponse(msg, transactionID, dest, type);
        }


    }









    @Override
    public void handleResponse(String id, Message msg) {

    }
    @Override
    public void handleException(String id, Throwable throwable) {

    }


    public interface RequestMessageCallback {
        CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn);
    }

    public interface ForwardRequestMessageCallback {
        void sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn, HeaderHelper header);
    }

    public interface ResponseMessageCallback {
        void sendResponse(Message msg, String transactionID, List<RoutableID> dest, MessageRouter.ForwardingOptionType type);
    }
}
