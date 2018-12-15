package org.platon.core.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.core.transaction.Bloom;
import org.platon.core.transaction.LogInfo;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.core.transaction.proto.TransactionReceiptProto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jungle
 * @version 0.0.1
 * @date 2018/9/4 8:41
 */
public final class TransactionReceiptCodec {

    public static TransactionReceiptProto.TransactionReceiptBase encode(TransactionReceipt txReceipt) {
        if (null == txReceipt) {
            return null;
        }
        TransactionReceiptProto.TransactionReceiptBase.Builder txReceiptPb =
                TransactionReceiptProto.TransactionReceiptBase.newBuilder();

        txReceiptPb.setCumulativeEnergon(ByteString.copyFrom(txReceipt.getCumulativeEnergon()));
        txReceiptPb.setStateRoot(ByteString.copyFrom(txReceipt.getStateRoot()));
        txReceiptPb.setBloomFilter(ByteString.copyFrom(txReceipt.getBloomFilter().getData()));
        List<LogInfo> logInfoList = txReceipt.getLogInfoList();
        if (logInfoList != null && logInfoList.size() > 0) {
            for (int i = 0; i < logInfoList.size(); i++) {
                txReceiptPb.addLogs(logInfoList.get(i).getEncodedBuilder());
            }
        }

        if (null != txReceipt.getEnergonUsed()) {
            txReceiptPb.setEnergonUsed(ByteString.copyFrom(txReceipt.getEnergonUsed()));
        }
        if (null != txReceipt.getExecutionResult()) {
            txReceiptPb.setExecutionResult(ByteString.copyFrom(txReceipt.getExecutionResult()));
        }
        return txReceiptPb.build();
    }

    public static TransactionReceipt decode(TransactionReceiptProto.TransactionReceiptBase receiptBase) {
        return decode(receiptBase.toByteArray());
    }

    public static TransactionReceipt decode(byte[] protoTransactionReceipt) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        try {
            TransactionReceiptProto.TransactionReceiptBase transactionReceiptBase
                    = TransactionReceiptProto.TransactionReceiptBase.parseFrom(protoTransactionReceipt);

            txReceipt.setEnergonUsed(transactionReceiptBase.getEnergonUsed().toByteArray());
            txReceipt.setCumulativeEnergon(transactionReceiptBase.getCumulativeEnergon().toByteArray());
            txReceipt.setStateRoot(transactionReceiptBase.getStateRoot().toByteArray());

            txReceipt.setBloomFilter(new Bloom(transactionReceiptBase.getBloomFilter().toByteArray()));
            txReceipt.setExecutionResult(transactionReceiptBase.getExecutionResult().toByteArray());

            List<LogInfo> logInfoList = new ArrayList<>();
            for (int i = 0; i < transactionReceiptBase.getLogsCount(); i++) {
                byte[] logs = transactionReceiptBase.getLogs(i).toByteArray();
                LogInfo logInfo = new LogInfo(logs);
                logInfoList.add(logInfo);
            }
            txReceipt.setLogInfoList(logInfoList);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Proto buff decode transactionReceipt exception!");
        }
        return txReceipt;
    }
}
