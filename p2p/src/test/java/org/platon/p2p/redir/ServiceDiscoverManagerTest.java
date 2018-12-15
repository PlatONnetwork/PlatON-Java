package org.platon.p2p.redir;

import com.google.protobuf.ByteString;
import org.apache.http.util.Asserts;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.platon.common.cache.DelayCache;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBMemoryImp;
import org.platon.p2p.plugins.RoutingTableMock;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.pubsub.EntryMessage;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.proto.redir.ServiceEntry;
import org.platon.p2p.pubsub.*;
import org.platon.p2p.router.MessageRouterMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;



@RunWith(PowerMockRunner.class)
@PrepareForTest({KeySelectorFactory.class})
public class ServiceDiscoverManagerTest {

    private static Logger logger = null;
    private ServiceDiscoveryManager serviceDiscoveryManager = null;


    private PubSub pubSub;



    private MessageRouterMock messageRouter = null;
    private RoutingTableMock routingTable = null;
    private DB db = null;


    private PubSubSessionNotify notify = null;

    private PubSubRouter pubSubRouterMock = null;
    private DelayCache<String, EntryMessage> msgCacheMock = null;
    private Map<String, Set<NodeID>> meshMock = null;
    private Map<String, Set<NodeID>> fanoutMock = null;

    private  static final String[] nodeStringList = {"e238a6077ffa4e93b943", "e238a6077ffa4e93b944", "e238a6077ffa4e93b945", "e238a6077ffa4e93b946"};

    private List<NodeID> nodeList = null;


    @BeforeClass
    public static void setLogger() throws MalformedURLException
    {
        logger = LoggerFactory.getLogger(PubSubRouterTest.class);
    }

    @Before
    public void init(){

        initNodeList();

        messageRouter = new MessageRouterMock();
        routingTable = new RoutingTableMock("3056301006072a8648c9");
        db = new DBMemoryImp();


        KeySelector selector = new KeySelector.Builder().
                branchingFactor(4).
                namespace("hashrate").
                level(4).
                lowestKey(BigInteger.valueOf(0)).
                highestKey(BigInteger.valueOf(1024)).
                startLevel(2).
                keyAlgorithmFunction(KeyAlgorithm.get("hashrate")).build();


        PowerMockito.mockStatic(KeySelectorFactory.class);

        pubSub = new PubsubMock(routingTable, messageRouter);






        serviceDiscoveryManager = new ServiceDiscoverManagerMock(pubSub, messageRouter, db, routingTable);

        PowerMockito.when(KeySelectorFactory.get("hashrate")).thenReturn(selector);

    }

    private void initNodeList() {
        nodeList = new ArrayList<>();
        for (String nodeString : nodeStringList) {
            NodeID nodeId = NodeID.newBuilder()
            .setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(nodeString))).build();
            nodeList.add(nodeId);
        }
    }

    private void createSession() {
        for (NodeID nodeid : nodeList) {
            notify.create(nodeid.getId());
        }
    }

    private void removeSession() {
        for (NodeID nodeid : nodeList) {
            notify.close(nodeid.getId());
        }
    }



    @Test
    public void testPublish() {
        ReDiRRequestCallback reDiRRequestCallback = new ReDiRRequestCallback();
        ReDiRRequestCallback.PublishCallback publishCallback = reDiRRequestCallback.new PublishCallback();
        ServiceEntry entry = ServiceEntry.newBuilder()
                .setServiceType("hashrate")
                .setDescribe("hello world")
                .setSourceKey("64")
                .setFrom("3056301006072a8648c9")
                .setUrl("www.platon.network")
                .build();

        BigInteger key = new BigInteger("64");
        System.out.println("---------------------------------------------::::: " + key.intValue());
        KeySelector selector = KeySelectorFactory.get("hashrate");

        messageRouter.addRequestCallback(ProtoBufHelper.getFullName(ReDiRMessage.class), publishCallback);

        serviceDiscoveryManager.publish(entry, false);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        Integer count = messageRouter.getRequestCount(ProtoBufHelper.getFullName(ReDiRMessage.class));

        Asserts.check( count== 4, "receive redir publish message error received count:" + count + " expected count:4");
    }


    private void createKeyList(ReDiRRequestCallback.FindCallback callback, KeySelector selector, String sourceKey) {
        ResourceID resourceId = selector.getResourceID(selector.getStartLevel(), sourceKey);
        List<String> keyList = new ArrayList<>();
        keyList.add("0");
        keyList.add("128");
        keyList.add("256");
        keyList.add("512");
        callback.setKeyList(Bytes.valueOf(resourceId.getId()), keyList);

        ResourceID resourceId2 = selector.getResourceID(selector.getStartLevel()-1, sourceKey);

        List<String> keyList2 = new ArrayList<>();
        keyList2.add("0");
        keyList2.add("53");
        keyList2.add("47");
        keyList2.add("68");
        callback.setKeyList(Bytes.valueOf(resourceId2.getId()), keyList2);

        ResourceID resourceId3 = selector.getResourceID(selector.getStartLevel()+1, sourceKey);

        List<String> keyList3 = new ArrayList<>();
        keyList3.add("0");
        keyList3.add("53");
        keyList3.add("47");
        keyList3.add("68");
        callback.setKeyList(Bytes.valueOf(resourceId3.getId()), keyList3);
    }

    private void createEntryList(ReDiRRequestCallback.FindCallback callback, KeySelector selector, String sourceKey) {
        ResourceID resourceId = selector.getResourceID(selector.getStartLevel()+1, sourceKey);
        List<ServiceEntry> entryList = new ArrayList<>();



        ServiceEntry entry = ServiceEntry.newBuilder()
                .setServiceType("hashrate")
                .setDescribe("hello world")
                .setSourceKey("20")
                .setFrom("3056301006072a8648c9")
                .setUrl("www.platon.network")
                .build();


        ServiceEntry entry2 = ServiceEntry.newBuilder()
                .setServiceType("hashrate")
                .setDescribe("hello world")
                .setSourceKey("64")
                .setFrom("3056301006072a8648ca")
                .setUrl("www.platon.network")
                .build();

        entryList.add(entry);
        entryList.add(entry2);

        callback.setEntryList(Bytes.valueOf(resourceId.getId()), entryList);
    }

    @Test
    public void testFind() throws ExecutionException, InterruptedException {
            KeySelector selector = KeySelectorFactory.get("hashrate");

            String sourceKey = "64";

            ReDiRRequestCallback reDiRRequestCallback = new ReDiRRequestCallback();
            ReDiRRequestCallback.FindCallback findCallback = reDiRRequestCallback.new FindCallback();

            createKeyList(findCallback, selector, sourceKey);
            createEntryList(findCallback, selector, sourceKey);


            messageRouter.addRequestCallback(ProtoBufHelper.getFullName(ReDiRMessage.class), findCallback);

            CompletableFuture<List<ServiceEntry>> future=  serviceDiscoveryManager.find("hashrate", sourceKey);

            List<ServiceEntry> entryList = future.get();

            Asserts.notNull(entryList, "find error entry list is null");

            Integer count = messageRouter.getRequestCount(ProtoBufHelper.getFullName(ReDiRMessage.class));

            Asserts.check( count== 2, "receive redir discovery message error" + "received count:" + count + "expected count:4");
    }





}
