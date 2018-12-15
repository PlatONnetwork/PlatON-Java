package org.platon.p2p.redir;


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
import org.platon.p2p.proto.redir.ReDiRFindMessage;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.proto.redir.ServiceEntry;
import org.platon.p2p.pubsub.PubSub;
import org.platon.p2p.pubsub.PubSubSessionNotify;
import org.platon.p2p.pubsub.PubsubMock;
import org.platon.p2p.router.MessageRouterMock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RunWith(PowerMockRunner.class)
public class ReDiRClusterTest {
    private static Logger logger = null;
    private  static final String[] nodeStringList = {"e238a6077ffa4e93b943", "e238a6077ffa4e93b944", "e238a6077ffa4e93b945", "e238a6077ffa4e93b946", "e238a6077ffa4e93b947"};
    private List<NodeID> nodeList = null;

    MessageRouterMock messageRouter = null;



    private List<ReDiRClusterNode> rediRClusterNodeList = new LinkedList<>();

    @BeforeClass
    public static void setLogger() throws MalformedURLException
    {
        logger = LoggerFactory.getLogger(ReDiRClusterTest.class);
    }

    @Before
    public void init() {



        initNodeList();

        messageRouter = new MessageRouterMock();
    }

    @Test
    public void testReDiRCluster() throws InterruptedException, ExecutionException {
        initCluster();


        for (int i = rediRClusterNodeList.size()-2; i >= 0; i--) {
            rediRClusterNodeList.get(i).notify.create(rediRClusterNodeList.get(i+1).routingTable.getLocalNode().getId());
            rediRClusterNodeList.get(i).routingTable.setNextHops(Collections.singletonList(rediRClusterNodeList.get(i+1).routingTable.getLocalNode()));
        }


        for (int i = 0; i < rediRClusterNodeList.size(); i++) {
            ReDiRClusterRequestCallback reDiRClusterRequestCallback = new ReDiRClusterRequestCallback(rediRClusterNodeList.get(i), rediRClusterNodeList);
            ReDiRClusterRequestCallback.ClusterPublishAndFindCallback clusterPublishAndFindCallback = reDiRClusterRequestCallback.new ClusterPublishAndFindCallback();
            ReDiRClusterRequestCallback.ClusterForwardRequestCallback clusterForwardRequestCallback = reDiRClusterRequestCallback.new ClusterForwardRequestCallback();
            ReDiRClusterRequestCallback.ClusterResponseCallback clusterResponseCallback = reDiRClusterRequestCallback.new ClusterResponseCallback();


            rediRClusterNodeList.get(i).messageRouter.addRequestCallback(ProtoBufHelper.getFullName(ReDiRMessage.class), clusterPublishAndFindCallback);
            rediRClusterNodeList.get(i).messageRouter.addRequestCallback(ProtoBufHelper.getFullName(ReDiRFindMessage.class), clusterPublishAndFindCallback);
            rediRClusterNodeList.get(i).messageRouter.addForwardRequestCallback(ProtoBufHelper.getFullName(ReDiRMessage.class), clusterForwardRequestCallback);
            rediRClusterNodeList.get(i).messageRouter.addForwardRequestCallback(ProtoBufHelper.getFullName(ReDiRFindMessage.class), clusterForwardRequestCallback);
            rediRClusterNodeList.get(i).messageRouter.addResponseCallback(ProtoBufHelper.getFullName(ReDiRFindMessage.class), clusterResponseCallback);
        }





        ServiceEntry entry = ServiceEntry.newBuilder()
                .setServiceType("hashrate")
                .setDescribe("hello world")
                .setSourceKey("64")
                .setFrom("3056301006072a8648c9")
                .setUrl("www.platon.network").build();

        rediRClusterNodeList.get(0).serviceDiscoveryManager.publish(entry, false);

        Thread.sleep(1000);


        CompletableFuture<List<ServiceEntry>> future =  rediRClusterNodeList.get(0).serviceDiscoveryManager.find("hashrate", "64");

        List<ServiceEntry> entryList = future.get();

        Asserts.check(entryList.size() == 1, "");
    }

    private void initCluster() {
        for (String nodeString : nodeStringList) {

            NodeID nodeId = NodeID.newBuilder()
                    .setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(nodeString)))
                    .build();
            nodeList.add(nodeId);
            RoutingTableMock routingTableMock = new RoutingTableMock(nodeString);

            MessageRouterMock messageRouterMock = new MessageRouterMock();
            PubsubMock pubsubMock = new PubsubMock(routingTableMock, messageRouterMock);
            DB db = new DBMemoryImp();
            ServiceDiscoveryManager serviceDiscoveryManagerMock =
                    new ServiceDiscoverManagerMock(pubsubMock, messageRouterMock, db, routingTableMock);

            PubSubSessionNotify notify = new PubSubSessionNotify(pubsubMock, nodeId);
            ReDiRClusterNode rediRClusterNode = new ReDiRClusterNode(pubsubMock, db, messageRouterMock, serviceDiscoveryManagerMock, notify, routingTableMock);
            rediRClusterNodeList.add(rediRClusterNode);
        }
    }


    private void initNodeList() {
        nodeList = new ArrayList<>();
        for (String nodeString : nodeStringList) {
            NodeID nodeId = NodeID.newBuilder()
                    .setId(ByteString.copyFrom(NodeUtils.getNodeIdBytes(nodeString)))
                    .build();
            nodeList.add(nodeId);
        }
    }


    public class ReDiRClusterSubscribeCallbackMock implements PubSub.SubscribeCallback {

        String nodeId = null;
        public ReDiRClusterSubscribeCallbackMock(String nodeId){
            this.nodeId = nodeId;
        }


        @Override
        public void subscribe(String topic, byte[] data) {

            logger.trace("{} receive new data topic:{} data:{}", nodeId, topic, new String(data));
        }
    }
}
