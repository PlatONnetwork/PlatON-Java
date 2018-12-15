package org.platon.p2p.common;


import com.google.protobuf.ByteString;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.platon.p2p.plugins.kademlia.KeyComparator;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.common.RoutableID;

import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * <p>NodeUtils.java</p>
 * <p/>
 * <p>Description:</p>
 * <p/>
 * <p>Copyright (c) 2017. juzix.io. All rights reserved.</p>
 * <p/>
 *
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/5/14, lvxiaoyi, Initial Version.
 */
public class    NodeUtils {

    public static byte[] getNodeIdBytes(String nodeId){
        if(nodeId==null){
            throw new IllegalArgumentException();
        }
        try {
            return Hex.decodeHex(nodeId.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getNodeIdString(byte[] nodeIdBytes){
        if(nodeIdBytes==null){
            throw new IllegalArgumentException();
        }
        return Hex.encodeHexString(nodeIdBytes);
    }

    public static String getNodeIdString(ByteString nodeIdBytes){
        if(nodeIdBytes==null){
            throw new IllegalArgumentException();
        }
        return Hex.encodeHexString(nodeIdBytes.toByteArray());
    }


    public static boolean equals(NodeID src, NodeID dest) {
        return Arrays.equals(src.getId().toByteArray(), dest.getId().toByteArray());
    }

    public static void main(String[] args){
        String hexString = "74657374206e6f646531";//"test node1"

        byte[] hexBytes = NodeUtils.getNodeIdBytes(hexString);

        String hexString2 = NodeUtils.getNodeIdString(hexBytes);

        System.out.println("hexString:" + hexString2);

    }

    public static NodeID closestNode(RoutableID target, Set<NodeID> peers) {
        TreeSet<NodeID> sortedSet = new TreeSet<>(new KeyComparator(toNodeID(target)));
        sortedSet.addAll(peers);
        return sortedSet.first();
    }

    public static Bytes closestNode(Bytes target, Set<Bytes> peers) {
        TreeSet<Bytes> sortedSet = new TreeSet<>(Bytes.newBytesComparator(target));
        sortedSet.addAll(peers);
        return sortedSet.first();
    }

    public static NodeID toNodeID(RoutableID routableId) {
        return NodeID.newBuilder().setId(routableId.getId()).build();
    }

    public static RoutableID toRoutableID(NodeID nodeId) {
        return RoutableID.newBuilder().setId(nodeId.getId()).setType(RoutableID.DestinationType.NODEIDTYPE).build();
    }

    public static RoutableID toRoutableID(ResourceID resourceId) {
        return RoutableID.newBuilder().setId(resourceId.getId()).setType(RoutableID.DestinationType.RESOURCEIDTYPE).build();
    }

    public static RoutableID toRoutableID(ByteString byteString) {
        return RoutableID.newBuilder().setId(byteString).setType(RoutableID.DestinationType.RESOURCEIDTYPE).build();
    }

    public static RoutableID toRoutableID(Bytes bytes, RoutableID.DestinationType type) {
        return RoutableID.newBuilder().setId(ByteString.copyFrom(bytes.getKey())).setType(type).build();
    }

    public static List<RoutableID> nodeIdListToRoutableIdList(List<NodeID> nodeIdList){
        List<RoutableID> routableIdList = new ArrayList<>();
        for (NodeID nodeId : nodeIdList) {
            routableIdList.add(RoutableID.newBuilder().setId(nodeId.getId()).setType(RoutableID.DestinationType.NODEIDTYPE).build());
        }
        return routableIdList;
    }

    public static String base64Endpoint(String endpoint){
        return Base64.encodeBase64String(endpoint.getBytes(StandardCharsets.UTF_8));
    }
}
