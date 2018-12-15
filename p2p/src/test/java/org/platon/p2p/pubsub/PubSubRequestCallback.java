package org.platon.p2p.pubsub;

import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.router.MessageRouterMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author yangzhou
 * @create 2018-08-02 16:44
 */
public class PubSubRequestCallback {
    private static Logger logger = LoggerFactory.getLogger(PubSubRouter.class);
    public class ClusterCallback implements MessageRouterMock.RequestMessageCallback {

        Map<String, PubSub> pubSubMap = new HashMap<>();

        public void setPubSubMap(String id, PubSub pubSub) {
            pubSubMap.put(id, pubSub);
        }

        @Override
        public CompletableFuture<Message> sendRequest(Message msg, List<RoutableID> dest, MessageRouter.ForwardingOptionType type, boolean isReturn) {
            Asserts.check(dest.size() == 1, "dest list size expected:1");

            String id = NodeUtils.getNodeIdString(dest.get(0).getId());
            CompletableFuture.runAsync(()-> {
                try {
                    logger.trace("ClusterCallback request nodeId:{}", id);

                    PubSub pubSub = pubSubMap.get(id);
                    Asserts.notNull(pubSub, id + " pubsub is null");
                    logger.trace("ClusterCallback request 2 nodeId:" + id);
                    pubSub.sendMessage((TopicMessage) msg, null);
                }catch (Exception e) {
                    logger.error("nodeId:{} error:{}", id, e.getMessage());
                }
            }).exceptionally(e->{
                logger.error("ERROR:" + e);

                return null;});
            return null;
        }
    }
}
