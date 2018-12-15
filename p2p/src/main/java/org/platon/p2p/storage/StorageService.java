package org.platon.p2p.storage;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.tuple.Pair;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.storage.*;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author yangzhou
 * @create 2018-04-26 14:46
 */
@Component("storageService")
public class StorageService {

    private static Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private DB db;

    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    private RoutingTable routingTable;

    public void setDb(DB db) {
        this.db = db;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    @PlatonMessageType("SetStoreMessage")
    public void set(SetStoreMessage setStoreMessage, HeaderHelper header){
        logger.trace(setStoreMessage.toString());


        boolean needReplica = false;

        List<RoutableID> replicaNode = new ArrayList<>();
        List<RoutableID> destList = new ArrayList<>();

        if (setStoreMessage.getReplica() == 0) {
            List<NodeID> nodeIDList = routingTable.getNextHops(
                    RoutableID.newBuilder().setId(setStoreMessage.getResourceId().getId())
                            .setType(RoutableID.DestinationType.NODEIDTYPE).build(), 4);

            destList.addAll(NodeUtils.nodeIdListToRoutableIdList(nodeIDList));
            needReplica = true;
        }

        if(!replicaNode.isEmpty()) {
            replicaNode.add(NodeUtils.toRoutableID(routingTable.getLocalNode()));
            setStoreMessage.toBuilder().addAllReplicaNodeId(replicaNode);
        }

        ByteString out = ProtoBufHelper.encodeIceData(toStoreDataEntry(setStoreMessage));

        SetStoreRespMessage.Builder statusMessageBuilder = SetStoreRespMessage.newBuilder().setStatus(StoreStatus.SUCCESS);

        try {


            List<RoutableID> lastReplicaNodes = new ArrayList<>();
            StoreDataEntry lastStoreEntry = getReplicaStoreMessage(setStoreMessage.getResourceId().toByteArray());
            if (lastStoreEntry != null) {
                lastReplicaNodes = lastStoreEntry.getReplicaNodeIdList();
            }


            db.set(setStoreMessage.getResourceId().toByteArray(), out.toByteArray());


            if (needReplica && !destList.isEmpty()) {
                setStoreMessage.toBuilder().setReplica(1);
                messageRouter.sendRequest(
                        setStoreMessage, destList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, false);
            }


            lastReplicaNodes.removeAll(replicaNode);
            if (!lastReplicaNodes.isEmpty() && lastStoreEntry.getReplica() == 0) {
                List<RoutableID> removeDestList = new ArrayList<>();
                removeDestList.addAll(lastReplicaNodes);
                DelStoreMessage.Builder delStoreMessage = DelStoreMessage.newBuilder();
                delStoreMessage.setResourceId(setStoreMessage.getResourceId());
                messageRouter.sendRequest(delStoreMessage.build(), removeDestList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, false);
            }

        } catch (Exception e) {
            statusMessageBuilder.setStatus(StoreStatus.FAILED);
            statusMessageBuilder.setError(e.getMessage());
            logger.error("error:", e);
        }
        messageRouter.sendResponse(
                statusMessageBuilder.build(), header.txId(), header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }

    @PlatonMessageType("SetStoreRespMessage")
    public void setResp(SetStoreRespMessage msg, HeaderHelper header) {
        messageRouter.handleResponse(header.txId(), msg);
    }

    @PlatonMessageType("GetStoreMessage")
    public void get(GetStoreMessage msg, HeaderHelper header) {

        GetStoreRespMessage.Builder getStoreRespMsgBuilder = GetStoreRespMessage.newBuilder();
        try {

            byte[] value = db.get(msg.getResourceId().getId().toByteArray());


            StoreDataEntry storeDataEntry = ProtoBufHelper.decodeIceData(value, StoreDataEntry.parser());

            StoredData data = storeDataEntry.getStoredData();


            if (data != null) {
                long now = System.currentTimeMillis();
                if (data.getLifeTime() != -1 && data.getStorageTime() + data.getLifeTime() < now) {
                    db.hdel(msg.getResourceId().getId().toByteArray(), data.getKey().toByteArray());
                } else {
                    getStoreRespMsgBuilder.setStoredData(data);
                }
            }
            logger.trace("get data:" + getStoreRespMsgBuilder.build());

        } catch (Exception e) {
            logger.error("error:", e);
        } finally {
            messageRouter.sendResponse(
                    getStoreRespMsgBuilder.build(),header.txId(), header.viaToDest(), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
        }

    }

    @PlatonMessageType("GetStoreRespMessage")
    public void getResp(GetStoreRespMessage msg, HeaderHelper header){
        System.out.println("getResp:" + msg.toString());
        messageRouter.handleResponse(header.txId(), msg);
    }

    @PlatonMessageType("DelStoreMessage")
    public void del(DelStoreMessage msg, HeaderHelper header){
        DelStoreRespMessage.Builder delStoreRespMsgBuilder = DelStoreRespMessage.newBuilder().setStatus(StoreStatus.SUCCESS);

        try {

            StoreDataEntry lastStoreEntry = getReplicaStoreMessage(msg.getResourceId().getId().toByteArray());

            db.del(msg.getResourceId().getId().toByteArray());

            if (lastStoreEntry != null
                    && !lastStoreEntry.getReplicaNodeIdList().isEmpty()
                    && lastStoreEntry.getReplica() == 0) {

                List<RoutableID> removeDestList = new ArrayList<>();
                removeDestList.addAll(lastStoreEntry.getReplicaNodeIdList());
                DelStoreMessage.Builder removeMsgBuilder = DelStoreMessage.newBuilder();
                removeMsgBuilder.setResourceId(msg.getResourceId());
                messageRouter.sendRequest(
                        removeMsgBuilder.build(), removeDestList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, false);
            }
        } catch (Exception e) {
            delStoreRespMsgBuilder.setStatus(StoreStatus.FAILED);
            delStoreRespMsgBuilder.setError(e.getMessage());
            logger.error("error:", e);
        }

        messageRouter.sendResponse(
                delStoreRespMsgBuilder.build(),
                header.txId(),
                header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }

    private StoreDataEntry getReplicaStoreMessage(byte[] id) {
        StoreDataEntry storeDataEntry = null;
        try {
            byte[] lastValue = db.get(id);
            if (lastValue != null) {

                storeDataEntry = ProtoBufHelper.decodeIceData(lastValue, StoreDataEntry.parser());

            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return storeDataEntry;
    }

    private HashStoreDataEntry getReplicaHashStoreMessage(byte[] name, byte[] key) {


        HashStoreDataEntry hashStoreDataEntry = null;
        try {
            byte[] lastValue = db.hget(name, key);

            if (lastValue != null) {
                hashStoreDataEntry = ProtoBufHelper.decodeIceData(lastValue, HashStoreDataEntry.parser());
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return hashStoreDataEntry;
    }

    @PlatonMessageType("DelStoreRespMessage")
    public void delResp(DelStoreRespMessage msg, HeaderHelper header) {
        messageRouter.handleResponse(header.txId(), msg);
    }

    @PlatonMessageType("HSetStoreMessage")
    public void hset(HSetStoreMessage msg, HeaderHelper header){

        boolean needReplica = false;

        List<NodeID> replicaNode = new ArrayList<>();
        List<RoutableID> destList =new ArrayList<>();
        if (msg.getReplica() == 0) {
            replicaNode = routingTable.getNextHops(msg.getResourceId(), 4);
            replicaNode.remove(routingTable.getLocalNode());
            destList.addAll(NodeUtils.nodeIdListToRoutableIdList(replicaNode));
            needReplica = true;
        }

        if(!replicaNode.isEmpty()){
            replicaNode.add(routingTable.getLocalNode());
            msg.toBuilder().addAllReplicaNodeId(NodeUtils.nodeIdListToRoutableIdList(replicaNode));
        }


        byte[] out = ProtoBufHelper.encodeIceData(toHashStoreDataEntry(msg)).toByteArray();

        HSetStoreRespMessage.Builder hSetStoreRespMsgBuilder = HSetStoreRespMessage.newBuilder().setStatus(StoreStatus.SUCCESS);
        try {


            List<RoutableID> lastReplicaNodes = new ArrayList<>();
            HashStoreDataEntry lastStoreMsg = getReplicaHashStoreMessage(msg.getResourceId().getId().toByteArray(), msg.getKey().toByteArray());
            if (lastStoreMsg != null) {
                lastReplicaNodes = lastStoreMsg.getReplicaNodeIdList();
            }

            db.hset(msg.getResourceId().getId().toByteArray(), msg.getKey().toByteArray(), out);


            if (needReplica && !destList.isEmpty()) {
                msg.toBuilder().setReplica(1);
                messageRouter.sendRequest(
                        msg, destList, MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, false);
            }


            lastReplicaNodes.removeAll(replicaNode);
            if (!lastReplicaNodes.isEmpty() && lastStoreMsg.getReplica() == 0) {
                List<RoutableID> removeDestList = new ArrayList<>();
                removeDestList.addAll(lastReplicaNodes);
                HDelStoreMessage.Builder removeMsgBuilder = HDelStoreMessage.newBuilder();
                removeMsgBuilder.setResourceId(msg.getResourceId());
                messageRouter.sendRequest(
                        removeMsgBuilder.build(),
                        removeDestList,
                        MessageRouter.ForwardingOptionType.FORWARD_CONNECTION,
                        false);
            }

        } catch (Exception e) {
            hSetStoreRespMsgBuilder.setStatus(StoreStatus.FAILED);
            hSetStoreRespMsgBuilder.setError(e.getMessage());
            logger.error("error:", e);
        }

        messageRouter.sendResponse(
                hSetStoreRespMsgBuilder.build(), header.txId(), header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }

    @PlatonMessageType("HSetStoreRespMessage")
    public void hsetResp(HSetStoreRespMessage msg, HeaderHelper header) {

        messageRouter.handleResponse(header.txId(), msg);
    }

    @PlatonMessageType("HGetStoreMessage")
    public void hget(HGetStoreMessage msg, HeaderHelper header){
        HGetStoreRespMessage.Builder hGetStoreMsgBuilder = HGetStoreRespMessage.newBuilder();


        try {

            byte[] value = db.hget(msg.getResourceId().getId().toByteArray(), msg.getKey().toByteArray());
            StoredData data = ProtoBufHelper.decodeIceData(value, HashStoreDataEntry.parser()).getStoredData();

            if (data != null) {
                long now = System.currentTimeMillis();
                if (data.getLifeTime() != -1 && data.getStorageTime() + data.getLifeTime() < now) {
                    db.hdel(msg.getResourceId().getId().toByteArray(), data.getKey().toByteArray());
                } else {
                    data.toBuilder().setKey(msg.getKey());
                    hGetStoreMsgBuilder.addStoredData(data);
                }
            }

        } catch (Exception e) {
            logger.error("error:", e);
        } finally {
            messageRouter.sendResponse(hGetStoreMsgBuilder.build(), header.txId(), header.viaToDest(), MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);

        }
    }

    @PlatonMessageType("HGetAllStoreMessage")
    public void hgetAll(HGetAllStoreMessage msg, HeaderHelper header){
        HGetAllStoreRespMessage.Builder hGetAllStoreRespMsgBuilder = HGetAllStoreRespMessage.newBuilder();

        try {

            List<Pair<byte[], byte[]>> res = db.hgetAll(msg.getResourceId().getId().toByteArray());
            List<StoredData> storeList = new ArrayList<>();
            for (Map.Entry<byte[], byte[]> m : res) {
                StoredData data = ProtoBufHelper.decodeIceData(m.getValue(), HashStoreDataEntry.parser()).getStoredData();
                if (data != null) {
                    long now = System.currentTimeMillis();
                    if (data.getLifeTime() != -1 && data.getStorageTime() + data.getLifeTime() < now) {
                        db.hdel(msg.getResourceId().getId().toByteArray(), data.getKey().toByteArray());
                    } else {
                        data.toBuilder().setKey(ByteString.copyFrom(m.getKey())).build();
                        storeList.add(data);
                    }
                }
            }
            hGetAllStoreRespMsgBuilder.addAllStoredData(storeList);
        } catch (Exception e) {
            logger.error("error:", e);
        } finally {
            messageRouter.sendResponse(
                    hGetAllStoreRespMsgBuilder.build(),
                    header.txId(),
                    header.viaToDest(),
                    MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);

        }

    }

    @PlatonMessageType("HGetStoreRespMessage")
    public void hgetResp(HGetStoreRespMessage msg, HeaderHelper header){
        messageRouter.handleResponse(header.txId(), msg);
    }

    @PlatonMessageType("HDelStoreMessage")
    public void hdel(HDelStoreMessage msg, HeaderHelper header){
        HDelStoreRespMessage.Builder hDelStoreRespMsgBuilder = HDelStoreRespMessage.newBuilder().setStatus(StoreStatus.SUCCESS);

        try {

            HashStoreDataEntry lastStoreMessage = getReplicaHashStoreMessage(msg.getResourceId().getId().toByteArray(), msg.getKey().toByteArray());
            db.hdel(msg.getResourceId().getId().toByteArray(), msg.getKey().toByteArray());

            if (lastStoreMessage != null && !lastStoreMessage.getReplicaNodeIdList().isEmpty() && lastStoreMessage.getReplica() == 0) {

                HDelStoreMessage removeMsg = HDelStoreMessage.newBuilder().setResourceId(msg.getResourceId()).setKey(msg.getKey()).build();

                messageRouter.sendRequest(
                        removeMsg,
                        lastStoreMessage.getReplicaNodeIdList(),
                        MessageRouter.ForwardingOptionType.FORWARD_CONNECTION,
                        false);
            }
        } catch (Exception e) {
            hDelStoreRespMsgBuilder.setStatus(StoreStatus.FAILED);
            hDelStoreRespMsgBuilder.setError(e.getMessage());
            logger.error("error:", e);
        }

        messageRouter.sendResponse(
                hDelStoreRespMsgBuilder.build(), header.txId(), header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }

    @PlatonMessageType("HDelStoreRespMessage")
    public void hdelResp(HDelStoreRespMessage msg, HeaderHelper header) {
        messageRouter.handleResponse(header.txId(), msg);
    }


    private StoreDataEntry toStoreDataEntry(SetStoreMessage setStoreMessage) {
        return StoreDataEntry.newBuilder()
                .setReplica(setStoreMessage.getReplica())
                .addAllReplicaNodeId(setStoreMessage.getReplicaNodeIdList())
                .setResourceId(setStoreMessage.getResourceId())
                .setStoredData(setStoreMessage.getStoredData())
                .build();
    }

    private HashStoreDataEntry toHashStoreDataEntry(HSetStoreMessage hSetStoreMessage) {
        return HashStoreDataEntry.newBuilder()
                .setReplica(hSetStoreMessage.getReplica())
                .addAllReplicaNodeId(hSetStoreMessage.getReplicaNodeIdList())
                .setResourceId(hSetStoreMessage.getResourceId())
                .setStoredData(hSetStoreMessage.getStoredData())
                .setKey(hSetStoreMessage.getKey())
                .build();
    }


}

