package org.platon.p2p.test.proto;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import org.apache.commons.lang3.StringUtils;
import org.platon.p2p.proto.attach.AttachMessage;
import org.platon.p2p.proto.attach.AttachProtos;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Body;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;


public class AnyTest {

    public static void main(String[] args){
        AnyTest anyTest = new AnyTest();

        try {
            anyTest.testAny();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testAny() throws Exception {
        RoutableID destID = RoutableID.newBuilder().setId(ByteString.copyFromUtf8("1122")).setType(RoutableID.DestinationType.NODEIDTYPE).build();
        NodeID viaID = NodeID.newBuilder().setId(ByteString.copyFromUtf8("3344")).build();

        Header header = Header.newBuilder().setTxId("txId").setTtl(10).addDest(destID).addVia(viaID).setMsgType("Ping").build();

        AttachMessage attachMessage = AttachMessage.newBuilder().setNodeId(viaID).build();

        Body message = Body.newBuilder().setData(Any.pack(attachMessage)).build();

        PlatonMessage platonMessage = PlatonMessage.newBuilder().setHeader(header).setBody(message).build();


        byte[] msgBytes = platonMessage.toByteArray();

        PlatonMessage platonMessage1 = PlatonMessage.parseFrom(msgBytes);
        System.out.println("header-msgType:" + platonMessage1.getHeader().getMsgType());
        System.out.println("header-txId:" + platonMessage1.getHeader().getTxId());
        System.out.println("header-ttl:" + platonMessage1.getHeader().getTtl());
        platonMessage1.getHeader().getDestList().forEach(nodeID -> {
            System.out.println("header-destNodeId:" + nodeID);
        });

        platonMessage1.getHeader().getViaList().forEach(nodeID -> {
            System.out.println("header-viaNodeId:" + nodeID);
        });

        Any any =  platonMessage1.getBody().getData();

        String messageName = StringUtils.substringAfter(any.getTypeUrl(), "/");
        System.out.println("body-messageFullName:" + messageName);



        Class clz = Class.forName(messageName);

        Object test = platonMessage1.getBody().getData().unpack(clz);
        System.out.println("test:" + test);


        

    }

}
