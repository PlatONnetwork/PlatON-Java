package org.platon.core.datasource;

import org.apache.commons.collections4.map.LRUMap;
import org.platon.common.AppenderName;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.ByteComparator;
import org.platon.core.TransactionInfo;
import org.platon.core.codec.TransactionInfoCodec;
import org.platon.core.proto.TransactionInfoMessage;
import org.platon.core.proto.TransactionInfoMessageList;
import org.platon.storage.datasource.SerializerIfc;
import org.platon.storage.datasource.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;


public class TransactionStore extends ObjectDataSource<List<TransactionInfo>> {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_DB);

    
    private final LRUMap<ByteArrayWrapper, Object> lastSavedTxHash = new LRUMap<>(5000);

    private final Object object = new Object();

    private final static SerializerIfc<List<TransactionInfo>, byte[]> serializer =
            new SerializerIfc<List<TransactionInfo>, byte[]>() {

                @Override
                public byte[] serialize(List<TransactionInfo> object) {


                    TransactionInfoMessageList.Builder messageList = TransactionInfoMessageList.newBuilder();
                    for (int i = 0; i < object.size(); i++) {
                        TransactionInfoMessage txInfoMessage = TransactionInfoCodec.encode(object.get(i));
                        messageList.addTxInfoList(txInfoMessage);
                    }
                    return messageList.build().toByteArray();
                }

                @Override
                public List<TransactionInfo> deserialize(byte[] stream) {
                    try {
                        if (stream == null) return null;
                        TransactionInfoMessageList infoMessageList = TransactionInfoMessageList.parseFrom(stream);
                        List<TransactionInfoMessage> list = infoMessageList.getTxInfoListList();
                        List<TransactionInfo> ret = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            ret.add(TransactionInfoCodec.decode(list.get(i)));
                        }
                        return ret;
                    } catch (Exception e) {

                        logger.error("TransactionStore. deserialize fail.", e);
                    }
                    return null;
                }
            };

    
    public boolean put(TransactionInfo tx) {
        byte[] txHash = tx.getReceipt().getTransaction().getHash();

        List<TransactionInfo> existingInfos = null;
        synchronized (lastSavedTxHash) {
            if (lastSavedTxHash.put(new ByteArrayWrapper(txHash), object) != null || !lastSavedTxHash.isFull()) {
                existingInfos = get(txHash);
            }
        }



        if (existingInfos == null) {
            existingInfos = new ArrayList<>();
        } else {
            for (TransactionInfo info : existingInfos) {
                if (ByteComparator.equals(info.getBlockHash(), tx.getBlockHash())) {
                    return false;
                }
            }
        }
        existingInfos.add(tx);
        put(txHash, existingInfos);

        return true;
    }

    public TransactionInfo get(byte[] txHash, byte[] blockHash) {
        List<TransactionInfo> existingInfos = get(txHash);
        for (TransactionInfo info : existingInfos) {
            if (ByteComparator.equals(info.getBlockHash(), blockHash)) {
                return info;
            }
        }
        return null;
    }

    public TransactionStore(Source<byte[], byte[]> src) {
        super(src, serializer, 500);
    }

    @PreDestroy
    public void close() {
    }
}
