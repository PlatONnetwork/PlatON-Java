package org.platon.p2p.pubsub;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.apache.http.util.Asserts;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.platon.common.cache.DelayCache;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBMemoryImp;
import org.platon.p2p.plugins.RoutingTableMock;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.pubsub.EntryMessage;
import org.platon.p2p.proto.pubsub.SubMessage;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.platon.p2p.router.MessageRouterMock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-31 14:16
 */
@RunWith(PowerMockRunner.class)
public class PubSubRouterTest {


        private static Logger logger = null;
        @InjectMocks
        private PubSub pubSub = new PubSub();


        private MessageRouterMock messageRouter;
        private RoutingTableMock routingTable;
        private DB db;

        private PubSubSessionNotify notify;

        private PubSubRouter pubSubRouterMock;
        private DelayCache<String, EntryMessage> msgCacheMock;
        private Map<String, Set<Bytes>> meshMock;
        private Map<String, Set<Bytes>> fanoutMock;

        private  static final String[] nodeStringList = {"e238a6077ffa4e93b943", "e238a6077ffa4e93b944", "e238a6077ffa4e93b945", "e238a6077ffa4e93b946"};

        private List<Bytes> nodeList;


        @BeforeClass
        public static void setLogger()
        {
            logger = LoggerFactory.getLogger(PubSubRouterTest.class);
        }

        @Before
        public void init(){
            //create other peers
            initNodeList();

            messageRouter = new MessageRouterMock();
            routingTable = new RoutingTableMock("e238a6077ffa4e93b943");
            db = new DBMemoryImp();
            pubSub = new PubsubMock(routingTable, messageRouter);

            mockPubSubRouter();



            notify = new PubSubSessionNotify(pubSub, routingTable.getLocalNode());
        }

        private void initNodeList() {
            nodeList = new ArrayList<>();
            for (String nodeString : nodeStringList) {
                nodeList.add(Bytes.valueOf(NodeUtils.getNodeIdBytes(nodeString)));
            }
        }

        private void createSession() {
            for (Bytes nodeid : nodeList) {
                notify.create(ByteString.copyFrom(nodeid.getKey()));
            }
        }

        private void removeSession() {
            for (Bytes nodeid : nodeList) {
                notify.close(ByteString.copyFrom(nodeid.getKey()));
            }
        }



        private void mockPubSubRouter() {
            try {
                pubSubRouterMock = new PubSubRouter();
                pubSubRouterMock.setMessageRouter(messageRouter);
                pubSubRouterMock.attach(pubSub);

                msgCacheMock = new DelayCache<>();
                meshMock = new ConcurrentHashMap<>();
                fanoutMock = new ConcurrentHashMap<>();

                Field pubSubRouterField = pubSub.getClass().getDeclaredField("pubSubRouter");
                Field meshFiled = pubSubRouterMock.getClass().getDeclaredField("mesh");
                Field fanoutFiled = pubSubRouterMock.getClass().getDeclaredField("fanout");
                Field msgCacheFiled = pubSubRouterMock.getClass().getDeclaredField("msgCache");

                pubSubRouterField.setAccessible(true);
                meshFiled.setAccessible(true);
                fanoutFiled.setAccessible(true);
                msgCacheFiled.setAccessible(true);


                meshFiled.set(pubSubRouterMock, meshMock);
                fanoutFiled.set(pubSubRouterMock, fanoutMock);
                msgCacheFiled.set(pubSubRouterMock, msgCacheMock);
                pubSubRouterField.set(pubSub, pubSubRouterMock);

            } catch (Exception e) {
                logger.error("error:", e);
            }
        }



        private void subscribe() {
            List<TopicMessage> topicMessageList = new ArrayList<>();


            for (Bytes nodeId : nodeList) {
                TopicMessage.Builder topicMessageBuilder = TopicMessage.newBuilder();
                topicMessageBuilder.setFromNodeId(ByteString.copyFrom(nodeId.getKey()));
                SubMessage.Builder subMessageBuilder = SubMessage.newBuilder();
                subMessageBuilder.setTopic("hello");
                subMessageBuilder.setSub(true);
                subMessageBuilder.setNodeId(ByteString.copyFrom(nodeId.getKey()));

                topicMessageBuilder.setSubscribe(subMessageBuilder);

                topicMessageList.add(topicMessageBuilder.build());
            }
            for (TopicMessage topicMessage : topicMessageList) {
                pubSub.sendMessage(topicMessage, null);
            }
        }

        @Test
        public void testControlMessage() throws InterruptedException {
            subscribe();

            List<TopicMessage> topicMessageList = new ArrayList<>();
            for (Bytes nodeId : nodeList) {
                TopicMessage.Builder topicMessageBuilder = TopicMessage.newBuilder();
                topicMessageBuilder.setFromNodeId(ByteString.copyFrom(nodeId.getKey()));
                EntryMessage.Builder entryMessage = EntryMessage.newBuilder();
                entryMessage.setFromNodeId(ByteString.copyFrom(nodeId.getKey()));
                entryMessage.setKey("123");
                entryMessage.setTopic("hello");
                entryMessage.setData(ByteString.copyFrom("world".getBytes()));

                topicMessageBuilder.addPublishedEntry(entryMessage);

                topicMessageList.add(topicMessageBuilder.build());
            }

            for (TopicMessage topicMessage : topicMessageList) {
                pubSub.sendMessage(topicMessage, null);
            }


            Thread.sleep(2000);
            Map<RoutableID, List<Message>> messageMap = messageRouter.getMessage(ProtoBufHelper.getFullName(TopicMessage.class));
            for (Map.Entry<RoutableID, List<Message>> x: messageMap.entrySet()) {
                Asserts.check(x.getValue().size() == nodeList.size(), "send request size error, expected:" + nodeList.size());
            }

        }

}


