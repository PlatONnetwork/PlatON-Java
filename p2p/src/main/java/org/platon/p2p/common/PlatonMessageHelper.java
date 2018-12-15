package org.platon.p2p.common;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.crypto.ECKey;
import org.platon.crypto.HashUtil;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.NodeContext;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Body;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.proto.session.CreateSession;

import java.util.List;
import java.util.UUID;

/**
 * <p>PlatonMessageHelper.java</p>
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
 * 2018/8/22, lvxiaoyi, Initial Version.
 */
public class PlatonMessageHelper {
    public static boolean arrived(List<RoutableID> destIdList, ByteString localNodeId){
        return CollectionUtils.isEmpty(destIdList) || existInRoutables(destIdList, localNodeId);
    }

    /*public static PlatonMessageHandler getHandler(String messageName){
        return (PlatonMessageHandler)SpringContextUtil.getBean(messageName + "Handler");
    }*/

/*    public static int decreaseTtl(Header header){
        int counter = header.getTtl();
        if(counter>1){
            Header.newBuilder(header).setTtl(counter--);
            return counter;
        }
        return 0;
    }*/

    /*public static void removeDestNode(Header header, ByteString localNodeId) {
        RoutableID routableId = RoutableID.newBuilder().setType(RoutableID.DestinationType.NODEIDTYPE).setId(localNodeId).build();
        header.getDestList().remove(routableId);
    }*/


/*
    public static boolean viaed(Header header, ByteString localNodeId){
        return viaed(header.getViaList(), localNodeId);
    }
*/


    public static boolean viaed(List<NodeID> viaIDList, ByteString localNodeId){
        return existInNodes(viaIDList, localNodeId);
    }

    public static Header renewHeader(Header header, ByteString localNodeId){
        NodeID localNode = NodeID.newBuilder().setId(localNodeId).build();
        Header.Builder builder = Header.newBuilder(header);

        int idx = header.getViaList().indexOf(localNode);
        if(idx == -1){
            builder.addVia(localNode);
        }

        RoutableID routableId = RoutableID.newBuilder().setType(RoutableID.DestinationType.NODEIDTYPE).setId(localNodeId).build();
        idx = header.getDestList().indexOf(routableId);
        if(idx >= 0){
            builder.removeDest(idx);
        }

        int ttl = header.getTtl();
        if(ttl>0){
            ttl--;
            builder.setTtl(ttl);
        }

        return builder.build();
    }

    public static Header addVia(Header header, ByteString localNodeId){

        NodeID localNode = NodeID.newBuilder().setId(localNodeId).build();

        int idx = header.getViaList().indexOf(localNode);
        if(idx == -1){
            return Header.newBuilder(header).addVia(localNode).build();
        }
        return header;
    }

    public static String getMessageName(Any any){
        return StringUtils.substringAfterLast(any.getTypeUrl(), ".");
    }


    public static PlatonMessage createCreateSession(){
        byte[] messageHash = HashUtil.sha3(NodeContext.localNodeId.toByteArray());
        ECKey ecKey = ECKey.fromPrivate(NodeContext.privateKey);

        CreateSession createSession = CreateSession.newBuilder()
                .setClientNodeId(NodeContext.localNodeId)
                .setEndpoint(NodeContext.getEndpoint())
                .setMessageHash(ByteString.copyFrom(messageHash))
                .setSignature(ByteString.copyFrom(WalletUtil.sign(messageHash, ecKey)))
                .build();

        Body body = Body.newBuilder().setData(Any.pack(createSession)).build();
        return PlatonMessage.newBuilder().setHeader(PlatonMessageHelper.createHeader()).setBody(body).build();
    }

    public static Header createHeader(){
        NodeID viaId = NodeID.newBuilder().setId(NodeContext.localNodeId).build();
        return Header.newBuilder().setTtl(1).setTxId(UUID.randomUUID().toString()).addVia(viaId).build();
    }

    public static Header createHeader(ByteString remoteNodeId){
        RoutableID destId = RoutableID.newBuilder().setId(remoteNodeId).setType(RoutableID.DestinationType.NODEIDTYPE).build();

        NodeID viaId = NodeID.newBuilder().setId(NodeContext.localNodeId).build();

        return Header.newBuilder().setTtl(1).addDest(destId).setTxId(UUID.randomUUID().toString()).addVia(viaId).build();
    }


    public static PlatonMessage createPlatonMessage(ByteString remoteNodeId, com.google.protobuf.Message message){
        Body body = Body.newBuilder().setData(Any.pack(message)).build();
        return PlatonMessage.newBuilder().setHeader(createHeader(remoteNodeId)).setBody(body).build();
    }


    private static boolean existInNodes(List<NodeID> nodeIdList, ByteString localNodeId ){
        if(nodeIdList!=null && nodeIdList.size()>0){
            for(NodeID nodeId : nodeIdList){
                if (localNodeId.equals(nodeId.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean existInRoutables(List<RoutableID> idList, ByteString localNodeId){
        if(idList!=null && idList.size()>0){
            for(RoutableID routableID : idList){
                if(routableID.getType()==RoutableID.DestinationType.NODEIDTYPE) {
                    if (localNodeId.equals(routableID.getId())) {
                        return true;
                    }
                }else if(routableID.getType()==RoutableID.DestinationType.RESOURCEIDTYPE){
                    RoutingTable routingTable = SpringContextUtil.getBean("routingTable");
                    List<NodeID> nodeIDList = routingTable.getNeighbors(routableID, 1);
                    if(CollectionUtils.isNotEmpty(nodeIDList) && localNodeId.equals(nodeIDList.get(0).getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static void main(String[] args){

        NodeID localNode = NodeID.newBuilder().setId(ByteString.copyFrom(Numeric.hexStringToByteArray("0x9f6d816d91405c36d3c408f2bcdae97f5e5df182"))).build();
        NodeID localNode1 = NodeID.newBuilder().setId(ByteString.copyFrom(Numeric.hexStringToByteArray("0x0cfa5fcf00b1f552c070c398bcfc002c8798208a"))).build();
        NodeID localNode2 = NodeID.newBuilder().setId(ByteString.copyFrom(Numeric.hexStringToByteArray("0x282a993adaed9b6dff554e0992d02211604605bf"))).build();

        RoutableID routableID1 = RoutableID.newBuilder().setId(ByteString.copyFrom(Numeric.hexStringToByteArray("0x0cfa5fcf00b1f552c070c398bcfc002c8798208a")))
                .setType(RoutableID.DestinationType.NODEIDTYPE).build();

        RoutableID routableID2 = RoutableID.newBuilder().setId(ByteString.copyFrom(Numeric.hexStringToByteArray("0x282a993adaed9b6dff554e0992d02211604605bf")))
                .setType(RoutableID.DestinationType.NODEIDTYPE).build();


        Header header = Header.newBuilder().setTtl(10).setTxId("txId").setVersion("1.0").addDest(routableID1).addDest(routableID2).addVia(localNode).addVia(localNode1).build();
        /*System.out.println(header.getViaList().size());
        System.out.println(header.toString());

        header = Header.newBuilder(header).addVia(localNode2).build();
        System.out.println(header.getViaList().size());
        System.out.println(header.toString());


        int idx = header.getViaList().indexOf(localNode2);
        if(idx>=0) {
            header = Header.newBuilder(header).removeVia(idx).build();
        }
        System.out.println(header.getViaList().size());
        System.out.println(header.toString());*/


        header = PlatonMessageHelper.renewHeader(header, ByteString.copyFrom(Numeric.hexStringToByteArray("0x282a993adaed9b6dff554e0992d02211604605bf")));

        System.out.println(header.toString());

    }
}
