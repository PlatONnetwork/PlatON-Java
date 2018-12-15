package org.platon.p2p;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.platon.common.config.ConfigProperties;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.session.SayHello;
import org.platon.p2p.router.MessageRouter;

public class TestNodeServer2 {
    public static void main(String[] args){
        ConfigProperties.setConfigPath("");
        NodeServer server1 = new NodeServer();
        server1.startup();

        new TestNodeServer2().cmdline();
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
                    // Nothing to do
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
