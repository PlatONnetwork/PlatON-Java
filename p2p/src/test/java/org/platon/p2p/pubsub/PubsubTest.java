package org.platon.p2p.pubsub;

import com.google.protobuf.ByteString;
import org.apache.http.util.Asserts;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBMemoryImp;
import org.platon.p2p.plugins.RoutingTableMock;
import org.platon.p2p.proto.pubsub.SubMessage;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.platon.p2p.router.MessageRouterMock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-30 16:47
 */

@RunWith(PowerMockRunner.class)
public class PubsubTest {

    private static Logger logger = null;
    @InjectMocks
    private PubsubMock pubSub = null;


    MessageRouterMock messageRouter = null;
    RoutingTableMock routingTable = null;
    DB db = null;

    PubSubSessionNotify notify = null;

    Set<Bytes> peersMock = null;
    Map<String, Map<String, PubSub.SubscribeCallback>> myTopicsMock = null;
    Map<String, Set<Bytes>> topicsMock = null;


    private  static final String[] nodeStringList = {"e238a6077ffa4e93b943", "e238a6077ffa4e93b944", "e238a6077ffa4e93b945", "e238a6077ffa4e93b946"};

    private List<Bytes> nodeList = null;


    @BeforeClass
    public static void setLogger() throws MalformedURLException
    {
        logger = LoggerFactory.getLogger(PubsubTest.class);
    }

    @Before
    public void init(){
        //create other peers
        initNodeList();

        messageRouter = new MessageRouterMock();
        routingTable = new RoutingTableMock("e238a6077ffa4e93b942");
        db = new DBMemoryImp();
        pubSub = new PubsubMock(routingTable, messageRouter);

        //mock PubSub's members
        mockPeers();
        mockMyTopics();
        mockTopic();


        notify = new PubSubSessionNotify(pubSub, routingTable.getLocalNode());
    }

    private void initNodeList() {
        nodeList = new ArrayList<>();
        for (String nodeString : nodeStringList) {
            nodeList.add(Bytes.valueOf(NodeUtils.getNodeIdBytes(nodeString)));
        }
    }


    private void mockPeers() {
        try {
            Field peersField = pubSub.getClass().getSuperclass().getDeclaredField("peers");
            peersMock = new HashSet<>();
            peersField.setAccessible(true);

            peersField.set(pubSub, peersMock);
        } catch (Exception e) {
            logger.error("error:", e);
        }
    }

    private void mockMyTopics() {
        try {
            Field peersField = pubSub.getClass().getSuperclass().getDeclaredField("mytopics");
            myTopicsMock = new HashMap<>();
            peersField.setAccessible(true);

            peersField.set(pubSub, myTopicsMock);
        } catch (Exception e) {
            logger.error("error:", e);
        }
    }

    private void mockTopic() {
        try {
            Field peersField = pubSub.getClass().getSuperclass().getDeclaredField("topics");
            topicsMock = new ConcurrentHashMap<>();
            peersField.setAccessible(true);

            peersField.set(pubSub, topicsMock);
        } catch (Exception e) {
            logger.error("error:", e);
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


    @Test
    public void testSessionNotify() {
        createSession();
        Set<Bytes> createdPeers = peersMock;
        Asserts.check(createdPeers.size() == nodeList.size(), "create session error peers size error expected:" + nodeList.size());

        for (Bytes nodeId : nodeList) {
            Asserts.check(createdPeers.contains(nodeId), "pubsub not contains " + nodeId);
        }

        removeSession();

        Asserts.check(createdPeers.size() == 0, "create session error peers size error expected:0");

        for (Bytes nodeId : nodeList) {
            Asserts.check(!createdPeers.contains(nodeId), "pubsub not contains " + nodeId);
        }
    }


    @Test
    public void testSubscribe() throws InterruptedException {
        createSession();
       pubSub.subscribe("hello", "world", null);

       Thread.sleep(1000);
       int count = messageRouter.getRequestCount(ProtoBufHelper.getFullName(TopicMessage.class));

        Asserts.check(  count == nodeList.size(),
                "send subscribe request count error count:" + count + "  , expected:"+nodeList.size());
        messageRouter.clearRequest(ProtoBufHelper.getFullName(TopicMessage.class));
        removeSession();
    }



    @Test
    public void testSendMessageSubscribe() {
        createSession();

        List<TopicMessage> topicMessageList = new ArrayList<>();

        for (Bytes nodeID : nodeList) {
            TopicMessage.Builder topicMessageBuilder = TopicMessage.newBuilder();
            topicMessageBuilder.setFromNodeId(ByteString.copyFrom(nodeID.getKey()));
            SubMessage.Builder subMessageBuilder = SubMessage.newBuilder();
            subMessageBuilder.setTopic("hello");
            subMessageBuilder.setSub(true);
            subMessageBuilder.setNodeId(ByteString.copyFrom(nodeID.getKey()));

            topicMessageBuilder.setSubscribe(subMessageBuilder);

            topicMessageList.add(topicMessageBuilder.build());
        }
        for (TopicMessage topicMessage : topicMessageList) {
            pubSub.sendMessage(topicMessage, null);
        }

        Set<Bytes> nodeIDSet = topicsMock.get("hello");
        for (Bytes nodeId : nodeList) {
            Asserts.check(nodeIDSet.contains(nodeId), "topics error expected:" + nodeId);
        }
    }


    @Test
    public void testControlMessage(){


    }




}
