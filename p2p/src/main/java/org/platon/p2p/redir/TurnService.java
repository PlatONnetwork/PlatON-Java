package org.platon.p2p.redir;

import org.springframework.stereotype.Component;

/**
 * @author yangzhou
 * @create 2018-05-11 17:36
 */
@Component("turnService")
public class TurnService extends ReDiR{

//
//    private static String SERVICENAME = "turn";
//
//    public TurnService() {
//        KeySelector selector = new KeySelector();
//
//        selector.setBranchingFactor(ReDiRConfig.getInstance().getBranchingFactor(SERVICENAME));
//        selector.setStartLevel(ReDiRConfig.getInstance().getStartLevel(SERVICENAME));
//        selector.setLowestKey(ReDiRConfig.getInstance().getLowestKey(SERVICENAME));
//        selector.setHighestKey(ReDiRConfig.getInstance().getHighestKey(SERVICENAME));
//        selector.setNamespace(SERVICENAME);
//        selector.setLevel(ReDiRConfig.getInstance().getLevel(SERVICENAME));
//        setSelector(selector);
//    }
//
//    static private byte[] encodeValue(NodeID nodeId) {
//        byte[] encode = new byte[1 + nodeId.nodeId.length + nodeId.getEndpoint().length()];
//        ByteBuffer buffer = ByteBuffer.wrap(encode);
//        buffer.addSessionFuture((byte)nodeId.nodeId.length);
//        buffer.addSessionFuture(nodeId.getId());
//        buffer.addSessionFuture(nodeId.getEndpoint().getBytes());
//
//        return encode;
//    }
//
//    static private NodeID decodeValue(byte[] decode) {
//        NodeID nodeID = new NodeID();
//        ByteBuffer buffer = ByteBuffer.wrap(decode);
//        byte idLen  = buffer.get();
//        byte[] nodeId = new byte[idLen];
//        buffer.get(nodeId);
//        nodeID.setId(nodeId);
//        if (buffer.hasRemaining()){
//            byte[] endpoint = new byte[buffer.remaining()];
//            buffer.get(endpoint);
//            nodeID.setEndpoint(new String(endpoint));
//            return nodeID;
//        }
//        return null;
//    }
//
//    public void registerService(NodeID nodeID) throws ExecutionException, InterruptedException {
//        if (nodeID.getEndpoint().isEmpty()) {
//            return;
//        }
//
//        ReDiRKindData registerData = new ReDiRKindData();
//        registerData.setValue(encodeValue(nodeID));
//        registerData.setId(nodeID);
//        registerData.setKey(nodeID.nodeId);
//        registerService(registerData);
//    }
//
//    public CompletableFuture<NodeID> findService(NodeID nodeId) {
//        final CompletableFuture<NodeID> node  = new CompletableFuture<>();
//        CompletableFuture<List<ReDiRKindData>> fut = findService(new BigInteger(nodeId.getId()));
//
//        fut.thenAcceptAsync(futMsg -> {
//            NodeID nodeID = decodeValue(futMsg.get(0).getValue());
//            if (nodeID == null) {
//                node.completeExceptionally(new Exception("node nodeId parse failed"));
//            } else {
//                node.complete(nodeID);
//            }
//
//        }, NodeContext.executor).exceptionally(throwable->{
//            node.completeExceptionally(throwable);
//            return null;
//        });
//
//        return node;
//    }
//
//    public CompletableFuture<NodeID> findOne() {
//        final CompletableFuture<NodeID> futNode = new CompletableFuture<>();
//        CompletableFuture<Pair<byte[], byte[]>> fut = findAnyoneService();
//
//        fut.thenAcceptAsync(pair -> {
//            NodeID nodeID = decodeValue(pair.getValue());
//            if (nodeID == null) {
//                futNode.completeExceptionally(new Exception("node nodeId parse failed"));
//            } else {
//                futNode.complete(nodeID);
//            }
//        }, NodeContext.executor).exceptionally(throwable->{
//            futNode.completeExceptionally(throwable);
//            return null;
//        });
//        return futNode;
//    }

}
