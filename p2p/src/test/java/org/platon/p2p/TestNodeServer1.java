package org.platon.p2p;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.platon.common.config.ConfigProperties;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.session.SayHello;
import org.platon.p2p.router.MessageRouter;

public class TestNodeServer1 {
    public static void main(String[] args){

        /*byte[] byteString = Numeric.hexStringToByteArray("0x506bc1dc099358e5137292f4efdd57e400f29ba5132aa5d12b18dac1c1f6aaba645c0b7b58158babbfa6c6cd5a48aa7340a8749176b120e8516216787a13dc76");

        ByteString bb = ByteString.copyFrom(byteString);
        System.out.println(CodecUtils.toHexString(bb));

        byte[] addressByte = WalletUtil.computeAddress(byteString);

        ByteString b1 = ByteString.copyFrom(addressByte);

        byte[] addressByte2 = WalletUtil.computeAddress(byteString);
        ByteString b2 = ByteString.copyFrom(addressByte2);

        System.out.println(b1.equals(b2));*/

        ConfigProperties.setConfigPath("");
        NodeServer server1 = new NodeServer();
        server1.startup();

        new TestNodeServer1().cmdline();
    }

    private void cmdline(){
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        String line = null;

        do {
            try {
                System.out.print("==> ");
                System.out.flush();
                line = in.readLine();
                if (line == null) {
                    break;
                }

                if (line.startsWith("sayHello")) {
                    String[] params = line.split(" ");
                    sayHello(StringUtils.trim(params[1]), StringUtils.trim(params[2]));
                } else if (line.equals("x")) {

                } else {
                    System.out.println("unknown command `" + line + "'");
                    menu();
                }
            } catch ( Exception ex) {
                ex.printStackTrace();
            }
        }
        while (!line.equals("x"));
    }


    private void sayHello(String nodeId, String hello) {

        byte[] remoteNodeIdBytes = Numeric.hexStringToByteArray(nodeId);
        ByteString remoteNodeId = ByteString.copyFrom(remoteNodeIdBytes);
        NodeID nodeID = NodeID.newBuilder().setId(remoteNodeId).build();

        SayHello sayHello = SayHello.newBuilder().setNodeId(NodeContext.localNodeId).setHello(hello).setFeedback(true).build();

        MessageRouter messageRouter = SpringContextUtil.getBean("messageRouter");


        messageRouter.sendRequest(sayHello, nodeID, MessageRouter.ForwardingOptionType.DIRECT_CONNECTION, false);
    }

    private static void menu() {
        System.out.println(
                "usage:\n" +
                        "sayHello remoteNodeId something: send something to remoteNodeId\n" +
                        "x: exit\n" +
                        "?: help\n");
    }
}
