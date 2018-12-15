package org.platon.p2p.redir;

import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.redir.ReDiRFindRespMessage;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.proto.redir.ReDiRRespMessage;
import org.platon.p2p.proto.redir.ServiceEntry;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.router.MessageRouterMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class ReDiRRequestCallback {
    private static Logger logger = LoggerFactory.getLogger(ReDiRRequestCallback.class);
    public  class PublishCallback implements MessageRouterMock.RequestMessageCallback {



        Map<Bytes, List<String>> resourceIDListMap = new HashMap<>();

        public PublishCallback(){

        }

        public void setKeyList(Bytes resourceId, List<String> currentKeyList) {
            resourceIDListMap.put(resourceId, currentKeyList);
        }

        @Override
        public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn) {
            logger.trace("request message:{}", msg.toString());

            Asserts.check(dest.size() == 1, "dest list size error, expected:1");
            Asserts.check(dest.get(0).getType() == RoutableID.DestinationType.RESOURCEIDTYPE, "dest list nodeId error, expected:ResourceID");

            ReDiRRespMessage keyMsg = ReDiRRespMessage.newBuilder().build();
            List<String> keyList = new ArrayList<>();
            keyList.add(((ReDiRMessage)msg).getEntry().getSourceKey());
            if (resourceIDListMap.get(dest.get(0)) != null) {
                keyList.addAll(resourceIDListMap.get(Bytes.valueOf(dest.get(0).getId())));
            }

            keyMsg.toBuilder().addAllKey(keyList);
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.complete(keyMsg);
            return future;
        }
    }

    public class FindCallback implements MessageRouterMock.RequestMessageCallback {

        Map<Bytes, List<String>> resourceIDListMap = new HashMap<>();
        Map<Bytes, List<ServiceEntry>> resourceIDEntryListMap = new HashMap<>();

        public FindCallback(){

        }

        public void setKeyList(Bytes resourceId, List<String> currentKeyList) {
            resourceIDListMap.put(resourceId, currentKeyList);
        }

        public void setEntryList(Bytes resourceId, List<ServiceEntry> entryList) {
            resourceIDEntryListMap.put(resourceId, entryList);
        }

        @Override
        public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn) {
            logger.trace("request message:{}", msg.toString());

            Asserts.check(dest.size() == 1, "dest list size error, expected:1");
            Asserts.check(dest.get(0).getType() == RoutableID.DestinationType.RESOURCEIDTYPE, "dest list nodeId error, expected:ResourceID");

            ReDiRFindRespMessage reDiRFindListMessage = ReDiRFindRespMessage.newBuilder().build();

            reDiRFindListMessage.toBuilder().setResourceId(ResourceID.newBuilder().setId(dest.get(0).getId()));



            List<ServiceEntry> mockEntryList = resourceIDEntryListMap.get(reDiRFindListMessage.getResourceId());
            List<String> mockKeyList = resourceIDListMap.get(reDiRFindListMessage.getResourceId());
            if (mockEntryList != null) {
                reDiRFindListMessage.toBuilder().addAllEntry(mockEntryList);
            } else {
                reDiRFindListMessage.toBuilder().addAllKey(mockKeyList);
            }

            CompletableFuture<Message> future = new CompletableFuture<>();
            future.complete(reDiRFindListMessage);
            return future;
        }
    }

}
