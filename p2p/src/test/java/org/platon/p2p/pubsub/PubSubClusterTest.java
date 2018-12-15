package org.platon.p2p.pubsub;

import com.google.protobuf.ByteString;
import org.apache.http.util.Asserts;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBMemoryImp;
import org.platon.p2p.plugins.RoutingTableMock;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.platon.p2p.router.MessageRouterMock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;






@RunWith(PowerMockRunner.class)
public class PubSubClusterTest {
    private static Logger logger = null;

    private  static final String[] nodeStringList = {"e238a6077ffa4e93b943", "e238a6077ffa4e93b944", "e238a6077ffa4e93b945", "e238a6077ffa4e93b946", "e238a6077ffa4e93b947"};

    MessageRouterMock messageRouter = null;

    DB db = null;


    private PubSub pubSubA = null;
    private PubSubSessionNotify notifyA = null;
    private RoutingTableMock routingTableMockA = null;

    private PubSub pubSubB = null;
    private PubSubSessionNotify notifyB = null;
    private RoutingTableMock routingTableMockB = null;

    private PubSub pubSubC = null;
    private PubSubSessionNotify notifyC = null;
    private RoutingTableMock routingTableMockC = null;

    private PubSub pubSubD = null;
    private PubSubSessionNotify notifyD = null;
    private RoutingTableMock routingTableMockD = null;

    private PubSub pubSubE = null;
    private PubSubSessionNotify notifyE = null;
    private RoutingTableMock routingTableMockE = null;

    private List<NodeID> nodeList = null;

    @BeforeClass
    public static void setLogger() throws MalformedURLException
    {
        logger = LoggerFactory.getLogger(PubsubTest.class);
    }


    @Before
    public void init() throws Exception {



        initNodeList();

        messageRouter = new MessageRouterMock();

        db = new DBMemoryImp();








    }

    private void initPubSub() throws Exception {
        PubSubRequestCallback pubSubRequestCallback = new PubSubRequestCallback();
        PubSubRequestCallback.ClusterCallback clusterCallback = pubSubRequestCallback.new ClusterCallback();


        routingTableMockA = new RoutingTableMock(nodeStringList[0]);
        pubSubA = new PubsubMock(routingTableMockA, messageRouter);
        clusterCallback.setPubSubMap(nodeStringList[0], pubSubA);
        notifyA = new PubSubSessionNotify(pubSubA, nodeList.get(0));

        routingTableMockB = new RoutingTableMock(nodeStringList[1]);
        pubSubB = new PubsubMock(routingTableMockB, messageRouter);
        clusterCallback.setPubSubMap(nodeStringList[1], pubSubB);
        notifyB = new PubSubSessionNotify(pubSubB, nodeList.get(1));




        routingTableMockC = new RoutingTableMock(nodeStringList[2]);
        pubSubC = new PubsubMock(routingTableMockC, messageRouter);
        clusterCallback.setPubSubMap(nodeStringList[2], pubSubC);
        notifyC = new PubSubSessionNotify(pubSubC, nodeList.get(2));

        routingTableMockD = new RoutingTableMock(nodeStringList[3]);
        pubSubD =  new PubsubMock(routingTableMockD, messageRouter);
        clusterCallback.setPubSubMap(nodeStringList[3], pubSubD);
        notifyD = new PubSubSessionNotify(pubSubD, nodeList.get(3));


        routingTableMockE = new RoutingTableMock(nodeStringList[4]);
        pubSubE = new PubsubMock(routingTableMockE, messageRouter);

        clusterCallback.setPubSubMap(nodeStringList[4], pubSubE);
        notifyE = new PubSubSessionNotify(pubSubE, nodeList.get(4));


        notifyA.create(routingTableMockB.getLocalNode().getId());

        notifyB.create(routingTableMockA.getLocalNode().getId());
        notifyB.create(routingTableMockC.getLocalNode().getId());

        notifyC.create(routingTableMockB.getLocalNode().getId());
        notifyC.create(routingTableMockD.getLocalNode().getId());

        notifyD.create(routingTableMockC.getLocalNode().getId());

        notifyE.create(routingTableMockD.getLocalNode().getId());

        messageRouter.addRequestCallback(ProtoBufHelper.getFullName(TopicMessage.class), clusterCallback);
    }




    private void initNodeList() {
        nodeList = new ArrayList<>();
        for (String nodeString : nodeStringList) {
            NodeID nodeId = NodeID.newBuilder().setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(nodeString))).build();
            nodeList.add(nodeId);
        }
    }


    @Test
    public void testSendMessage() throws Exception {
        initPubSub();
        PubSubClusterCallbackMock pubSubClusterCallbackMockA = new PubSubClusterCallbackMock(nodeStringList[0]);
        PubSubClusterCallbackMock pubSubClusterCallbackMockB = new PubSubClusterCallbackMock(nodeStringList[1]);
        PubSubClusterCallbackMock pubSubClusterCallbackMockC = new PubSubClusterCallbackMock(nodeStringList[2]);
        PubSubClusterCallbackMock pubSubClusterCallbackMockD = new PubSubClusterCallbackMock(nodeStringList[3]);
        PubSubClusterCallbackMock pubSubClusterCallbackMockE = new PubSubClusterCallbackMock(nodeStringList[4]);


        pubSubA.subscribe("hello", ProtoBufHelper.getFullName(TopicMessage.class), pubSubClusterCallbackMockA);
        pubSubB.subscribe("hello", ProtoBufHelper.getFullName(TopicMessage.class), pubSubClusterCallbackMockB);
        pubSubC.subscribe("hello", ProtoBufHelper.getFullName(TopicMessage.class), pubSubClusterCallbackMockC);
        pubSubD.subscribe("hello", ProtoBufHelper.getFullName(TopicMessage.class), pubSubClusterCallbackMockD);

        Thread.sleep(100);
        int sendCount = 200;
        String prefix = "world+";
        for (int i = 0; i < sendCount; i++) {

            Thread.sleep(10);
            String value = prefix+i;
            try {
                pubSubA.publish("hello", value.getBytes());
            } catch (Exception e) {
                logger.error("publish:", e);
            }

        }


        Thread.sleep(1000);


        Asserts.check(pubSubClusterCallbackMockA.subMessage.size() == sendCount,
                "pubsubA receive count:" + pubSubClusterCallbackMockA.subMessage.size() + " expected:" + sendCount +
                        " lost:" + pubSubClusterCallbackMockA.findLost(0, sendCount, prefix));

        Asserts.check(pubSubClusterCallbackMockB.subMessage.size() == sendCount,
                "pubsubB receive count:" + pubSubClusterCallbackMockB.subMessage.size() + " expected:" + sendCount +
                        " lost:" + pubSubClusterCallbackMockB.findLost(0, sendCount, prefix));

        Asserts.check(pubSubClusterCallbackMockC.subMessage.size() == sendCount,
                "pubsubC receive count:" + pubSubClusterCallbackMockC.subMessage.size() + " expected:" + sendCount +
                        " lost:" + pubSubClusterCallbackMockC.findLost(0, sendCount, prefix));

        Asserts.check(pubSubClusterCallbackMockD.subMessage.size() == sendCount,
                "pubsubD receive count:" + pubSubClusterCallbackMockD.subMessage.size() + " expected:" + sendCount +
                        " lost:" + pubSubClusterCallbackMockD.findLost(0, sendCount, prefix));

        pubSubE.subscribe("hello", ProtoBufHelper.getFullName(TopicMessage.class), pubSubClusterCallbackMockE);

        logger.trace("waiting......");
        CompletableFuture<Void> x = CompletableFuture.runAsync(()->{
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        x.get();

        logger.trace("pubsubeE:{}", pubSubClusterCallbackMockE.subMessage.size());
        Asserts.check(pubSubClusterCallbackMockE.subMessage.size() == sendCount,
                "pubsubE receive count:" + pubSubClusterCallbackMockE.subMessage.size() + " expected:" + sendCount +
                        " lost:" + pubSubClusterCallbackMockE.findLost(0, sendCount, prefix));

    }



    public class PubSubClusterCallbackMock implements PubSub.SubscribeCallback {

        Set<String> subMessage = ConcurrentHashMap.newKeySet();
        String nodeId = null;
        public PubSubClusterCallbackMock(String nodeId){
            this.nodeId = nodeId;
        }

        public String findLost(int start, int end, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < end; i++) {
                String key = prefix +i;
                if (!subMessage.contains(key)){
                    stringBuilder.append(key);
                    stringBuilder.append(",");
                }
            }
            return stringBuilder.toString();
        }
        @Override
        public void subscribe(String topic, byte[] data) {

            logger.trace("{} receive new data topic:{} data:{}", nodeId, topic, new String(data));
            subMessage.add(new String(data));

        }
    }
}
