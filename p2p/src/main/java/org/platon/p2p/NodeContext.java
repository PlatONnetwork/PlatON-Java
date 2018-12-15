package org.platon.p2p;

import com.google.protobuf.ByteString;
import org.platon.crypto.ECKey;
import org.platon.p2p.proto.common.NodeID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/5/17, lvxiaoyi, Initial Version.
 */
public class NodeContext {

    private static Logger logger = LoggerFactory.getLogger(NodeContext.class);

    private static Map<String, Integer> relayMap = new ConcurrentHashMap<>();

    public static String host;
    public static int port;

    public static ByteString localNodeId;
    public static ByteString address;

    public static byte[] publicKey;
    public static byte[] privateKey;
    public static ECKey ecKey;

    public static long timeIntervalForDuplicatedMessage;

    public static Executor executor = Executors.newCachedThreadPool();


    public static String getEndpoint(){
        return host + ":" + port;
    }
    public static NodeID getNodeID(){
        return NodeID.newBuilder().setId(localNodeId).setEndpoint(host +":"+port).build();
    }


}
