package org.platon.p2p.redir;

import org.apache.commons.codec.binary.Hex;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBException;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.proto.storage.StoredData;
import org.platon.p2p.pubsub.PubSub;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;



public class ServiceDiscoveryUtil {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ServiceDiscoveryUtil.class);

    private static final long LIFETIME = 100000;



    
    public static void storeLocalMessage(DB db, PubSub pubSub, ReDiRMessage msg, long now) {


        StoredData.Builder storedDataBuilder = StoredData.newBuilder();




        storedDataBuilder.setKey(msg.getResourceId().getId());
        storedDataBuilder.setValue(ProtoBufHelper.encodeIceData(msg.getEntry()));
        storedDataBuilder.setStorageTime(now);
        storedDataBuilder.setLifeTime(LIFETIME);


        byte[] storeBytes = ProtoBufHelper.encodeIceData(storedDataBuilder.build()).toByteArray();

        try {
            db.hset(msg.getResourceId().getId().toByteArray(), msg.getEntry().getFrom().getBytes(), storeBytes);
        } catch (DBException e) {
            logger.error("error:", e);
        }


        CompletableFuture.runAsync(()-> {
            pubSub.publish(Hex.encodeHexString(msg.getResourceId().getId().toByteArray()), storeBytes);
        });
    }


}
