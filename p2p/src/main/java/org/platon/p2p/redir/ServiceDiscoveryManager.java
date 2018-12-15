package org.platon.p2p.redir;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.platon.common.cache.DelayCache;
import org.platon.p2p.ForwardMessageHook;
import org.platon.p2p.MessageHook;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBException;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.redir.*;
import org.platon.p2p.proto.storage.StoredData;
import org.platon.p2p.pubsub.PubSub;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author yangzhou
 * @create 2018-07-25 17:09
 */
@Component("serviceDiscoveryManager")
public class ServiceDiscoveryManager {

    private static Logger logger = LoggerFactory.getLogger(ServiceDiscoveryManager.class);

    private static final int rePublishDuration = 100000;


    private ServiceDiscoverySubCallback subCallback = new ServiceDiscoverySubCallback();


    private DelayCache<String, ServiceEntry> repeatEntry = new DelayCache<>();

    @Autowired
    PubSub pubSub;

    @Autowired
    DB db;

    @Autowired
    MessageRouter messageRouter;

    @Autowired
    ReDiR reDiR;

    @Autowired
    RoutingTable routingTable;

    ReDiRForwardMessageHook reDiRForwardMessageHook = null;
    ReDiRMessageHook reDiRMessageHook = null;

    public ServiceDiscoveryManager(){
        reDiRForwardMessageHook = new ReDiRForwardMessageHook(this);
        reDiRMessageHook = new ReDiRMessageHook(this);

        ForwardMessageHook.add("ServiceDiscovery", reDiRForwardMessageHook);
        MessageHook.add("ServiceDiscovery", reDiRMessageHook);
    }


    public void setPubSub(PubSub pubSub) {
        this.pubSub = pubSub;
    }

    public void setDb(DB db) {
        this.db = db;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }


    
    public void publish(ServiceEntry entry, boolean repeat){

        KeySelector selector = KeySelectorFactory.get(entry.getServiceType());


        ResourceID resourceId = selector.getResourceID(selector.getStartLevel(), entry.getSourceKey());
        if (resourceId == null) {
            logger.error("resource nodeId is null");
            throw new RuntimeException("resource nodeId is null please check sourceKey");
        }


        if (repeat) {
            repeatEntry.put(serviceEntryId(entry), entry, rePublishDuration, TimeUnit.MILLISECONDS);
        }


        pubSub.subscribe(NodeUtils.getNodeIdString(resourceId.getId()), PubSub.class.getName(), subCallback);


        reDiR.registerService(entry, selector);

    }

    public CompletableFuture<List<ServiceEntry>> find(String serviceType, String key) {
        KeySelector selector = KeySelectorFactory.get(serviceType);
        return reDiR.findService(key, selector);
    }



    
    private String serviceEntryId(ServiceEntry entry) {

        return entry.getFrom() + entry.getServiceType();
    }


    private class ServiceDiscoveryManagerDelayCacheCallback implements DelayCache.TimeoutCallbackFunction {

        @Override
        public void timeout(Object key, Object value) {




            publish((ServiceEntry) value, true);

        }
    }



























    

    public void publish(ReDiRMessage msg, HeaderHelper header){
        logger.debug("receive redir resource nodeId:{}",NodeUtils.getNodeIdString(msg.getResourceId().getId()));


        long now = System.currentTimeMillis();



        ServiceDiscoveryUtil.storeLocalMessage(db, pubSub, msg, now);


        ReDiRRespMessage.Builder reDiRRespMessageBuilder = ReDiRRespMessage.newBuilder();
        try {

            List<Pair<byte[], byte[]>> data = db.hgetAll(msg.getResourceId().getId().toByteArray());
            Set<String> keys = new HashSet<>();
            for (Pair<byte[], byte[]> e : data) {
                StoredData store = ProtoBufHelper.decodeIceData(e.getValue(), StoredData.parser());

                ServiceEntry entry = ProtoBufHelper.decodeIceData(store.getValue(), ServiceEntry.parser());
                if (store == null || store.getStorageTime() + store.getLifeTime() < now) {


                    logger.trace("del key:{}", Hex.encodeHexString(e.getKey()));
                    db.hdel(msg.getResourceId().getId().toByteArray(), entry.getFrom().getBytes());
                } else {
                    logger.trace("publish return key:" + Hex.encodeHexString(e.getKey()));
                    keys.add(entry.getSourceKey());
                }
            }
            reDiRRespMessageBuilder.addAllKey(keys);
        } catch (DBException e) {
            logger.error("error:", e);
        }

        messageRouter.sendResponse(reDiRRespMessageBuilder.build(),
                header.txId(),
                header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);

    }






    
    public void subscribe(String topic, byte[] data) {
        logger.trace("subscribe :{} storage local", topic);

        StoredData store = ProtoBufHelper.decodeIceData(data, StoredData.parser());
        ServiceEntry entry = ProtoBufHelper.decodeIceData(store.getValue(), ServiceEntry.parser());
        try {
            db.hset(NodeUtils.getNodeIdBytes(topic), entry.getFrom().getBytes(), data);
        } catch (DBException e) {
            logger.error("error:", e);
        }
    }





    
    public void publishResp(ReDiRRespMessage msg, HeaderHelper header){
        messageRouter.handleResponse(header.txId(), msg);
    }

    
    public void discovery(ReDiRFindMessage msg, HeaderHelper header) {

        ReDiRFindRespMessage.Builder reDiRFindRespMessageBuilder = ReDiRFindRespMessage.newBuilder();
        List<String> keyList = new ArrayList<>();
        List<ServiceEntry> entryList = new ArrayList<>();
        try {
            logger.trace("hgetall:" +NodeUtils.getNodeIdString(msg.getResourceId().getId()));
            List<Pair<byte[], byte[]>> data = db.hgetAll(msg.getResourceId().getId().toByteArray());
            long now = System.currentTimeMillis();

            for (Pair<byte[], byte[]> m : data) {

                StoredData store = ProtoBufHelper.decodeIceData(m.getValue(), StoredData.parser());
                ServiceEntry entry = ProtoBufHelper.decodeIceData(store.getValue(), ServiceEntry.parser());
                if (store.getStorageTime() + store.getLifeTime() < now) {
                    db.hdel(msg.getResourceId().getId().toByteArray(), entry.getFrom().getBytes());
                } else {
                    if (entry.getSourceKey().equals(msg.getFindKey())) {
                        entryList.add(entry);
                    } else {
                        keyList.add(entry.getSourceKey());
                    }
                }
            }

            reDiRFindRespMessageBuilder.setResourceId(msg.getResourceId());
            if (entryList.isEmpty()) {
                reDiRFindRespMessageBuilder.addAllKey(keyList);
            } else {
                reDiRFindRespMessageBuilder.addAllEntry(entryList);
            }

        } catch (DBException e) {
            logger.error("error:", e);
        }

        messageRouter.sendResponse(reDiRFindRespMessageBuilder.build(),
                header.txId(),
                header.viaToDest(),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION);
    }

    
    public void discoveryResp(ReDiRFindRespMessage msg, HeaderHelper header){
        logger.debug("discover resp:{}", msg.toString());
        messageRouter.handleResponse(header.txId(), msg);
    }
}
