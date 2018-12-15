package org.platon.p2p.redir;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Body;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.proto.redir.ReDiRFindMessage;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.router.MessageRouterMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ReDiRClusterRequestCallback {
    private static Logger logger = LoggerFactory.getLogger(ReDiRClusterRequestCallback.class);

    private static Map<String, CompletableFuture<Message>> futureMap = new ConcurrentHashMap<>();

    private ReDiRClusterNode self = null;

    private Map<Bytes, ReDiRClusterNode> clusterNodeMap = new HashMap<>();

    public ReDiRClusterRequestCallback(ReDiRClusterNode self, List<ReDiRClusterNode> reDiRClusterNodeList) {
        this.self = self;
        for (ReDiRClusterNode reDiRClusterNode : reDiRClusterNodeList) {
            clusterNodeMap.put(Bytes.valueOf(reDiRClusterNode.routingTable.getLocalNode().getId()), reDiRClusterNode);
        }
    }

    public static void addFuture(String transactionId, CompletableFuture<Message> future) {
        futureMap.put(transactionId, future);
    }

    public static CompletableFuture<Message> getFuture(String transactionId) {
        return futureMap.get(transactionId);
    }


    public class ClusterPublishAndFindCallback implements MessageRouterMock.RequestMessageCallback {


        @Override
        public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn) {
            Asserts.check(dest.size() == 1, "dest list size expected:1");

            logger.trace("sendRequest localnode:{}", NodeUtils.getNodeIdString(self.routingTable.getLocalNode().getId()));


            String transactionId = UUID.randomUUID().toString();


            Header.Builder header = Header.newBuilder().addAllDest(dest).setTxId(transactionId).addVia(self.routingTable.getLocalNode());


            List<NodeID> peerList = self.routingTable.getNextHops( dest.get(0));

            for (NodeID nodeId : peerList) {
                clusterNodeMap.get(nodeId).messageRouter.sendFrowardRequest(msg, dest, type, isReturn, HeaderHelper.build(header.build()));
            }

            CompletableFuture<Message> future = new CompletableFuture<>();

            futureMap.put(transactionId, future);

            return future;
        }
    }


    public class ClusterForwardRequestCallback implements MessageRouterMock.ForwardRequestMessageCallback {

        @Override
        public void sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn, HeaderHelper header) {


            PlatonMessage platonMessage = PlatonMessage.newBuilder()
                    .setBody(Body.newBuilder().setData(Any.pack(msg)))
                    .setHeader(header.getHeader()).build();


            if (self.serviceDiscoveryManager.reDiRMessageHook.isNeedProcess(platonMessage) || self.routingTable.getNextHops() == null) {
                CompletableFuture.runAsync(()-> {
                    if (ProtoBufHelper.getFullName(msg.getClass()).compareTo("ReDiRMessage") == 0) {
                        self.serviceDiscoveryManager.publish((ReDiRMessage) msg, header);
                    } else {
                        self.serviceDiscoveryManager.discovery((ReDiRFindMessage) msg, header);
                    }
                });
            } else {
                List<NodeID> nextHops = self.serviceDiscoveryManager.reDiRForwardMessageHook.nextHops(header.getHeader(), Any.pack(msg));
                if (nextHops == null || nextHops.isEmpty()) {
                    nextHops = self.serviceDiscoveryManager.routingTable.getNextHops(dest.get(0));
                }
                for (NodeID nodeId : nextHops) {
                    CompletableFuture.runAsync(()-> {
                        clusterNodeMap.get(nodeId).messageRouter.sendFrowardRequest(msg, dest, type, isReturn, header);
                    });
                }
            }
        }
    }

    public class ClusterResponseCallback implements MessageRouterMock.ResponseMessageCallback {

        @Override
        public void sendResponse(Message msg, String transactionID, List<RoutableID> dest, MessageRouter.ForwardingOptionType type) {

            CompletableFuture<Message> future = futureMap.get(transactionID);
            Asserts.notNull(future, "traunasaction nodeId: " + transactionID + " future is null");
            future.complete(msg);
        }

    }

}
