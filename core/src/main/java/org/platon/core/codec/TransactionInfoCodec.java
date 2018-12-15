package org.platon.core.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.core.TransactionInfo;
import org.platon.core.proto.TransactionInfoMessage;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.core.transaction.proto.TransactionReceiptProto;

/**
 * @author jungle
 * @version 0.0.1
 * @date 2018/9/4 8:41
 */
public final class TransactionInfoCodec {

    public static TransactionInfoMessage encode(TransactionInfo txInfo) {
        if (null == txInfo) {
            return null;
        }
        TransactionInfoMessage.Builder txInfoBuilder = TransactionInfoMessage.newBuilder();
        txInfoBuilder.setBlockHash(ByteString.copyFrom(txInfo.getBlockHash()));
        TransactionReceiptProto.TransactionReceiptBase base = TransactionReceiptCodec.encode(txInfo.getReceipt());
        if (base != null) {
            txInfoBuilder.setReceipt(base);
        }
        if (txInfo.getParentBlockHash() != null) {
            txInfoBuilder.setParentBlockHash(ByteString.copyFrom(txInfo.getParentBlockHash()));
        }
        txInfoBuilder.setIndex(txInfo.getIndex());
        return txInfoBuilder.build();
    }

    public static TransactionInfo decode(TransactionInfoMessage txInfoMessage) {
        return decode(txInfoMessage.toByteArray());
    }

    public static TransactionInfo decode(byte[] pbTransactionReceipt) {
        try {
            TransactionInfoMessage txInfoMessage = TransactionInfoMessage.parseFrom(pbTransactionReceipt);
            TransactionReceiptProto.TransactionReceiptBase base = txInfoMessage.getReceipt();
            TransactionReceipt txReceipt = TransactionReceiptCodec.decode(base);

            TransactionInfo txInfo = new TransactionInfo(txReceipt, txInfoMessage.getBlockHash().toByteArray(), (int) txInfoMessage.getIndex());
            return txInfo;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Proto buff decode TransactionInfo exception!");
        }
    }
}
