package org.platon.core.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.core.transaction.proto.TransactionReceiptProto;
import org.platon.core.transaction.util.ByteUtil;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.platon.core.transaction.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.platon.core.transaction.util.ByteUtil.toHexString;

/**
 * The transaction receipt is a tuple of three items
 * comprising the transaction, together with the post-transaction state,
 * and the cumulative energon used in the block containing the transaction receipt
 * as of immediately after the transaction has happened,
 */
public class TransactionReceipt {

    private Transaction transaction;

    private byte[] stateRoot = EMPTY_BYTE_ARRAY;
    private byte[] cumulativeEnergon = EMPTY_BYTE_ARRAY;
    private Bloom bloomFilter = new Bloom();
    private List<LogInfo> logInfoList = new ArrayList<>();

    private byte[] energonUsed = EMPTY_BYTE_ARRAY;
    private byte[] executionResult = EMPTY_BYTE_ARRAY;
    private String error = "";

    /* Tx Receipt in encoded form */
    private byte[] protoEncoded;

    public TransactionReceipt() {
    }

    public TransactionReceipt(byte[] protoTransactionReceipt) {

        try {
            TransactionReceiptProto.TransactionReceiptBase transactionReceiptBase
                    = TransactionReceiptProto.TransactionReceiptBase.parseFrom(protoTransactionReceipt);

            this.energonUsed = transactionReceiptBase.getEnergonUsed().toByteArray();
            this.cumulativeEnergon = transactionReceiptBase.getCumulativeEnergon().toByteArray();
            this.stateRoot = transactionReceiptBase.getStateRoot().toByteArray();
            this.bloomFilter = new Bloom(transactionReceiptBase.getBloomFilter().toByteArray());
            this.executionResult = transactionReceiptBase.getExecutionResult().toByteArray();

            for (int i =  0; i < transactionReceiptBase.getLogsCount(); i++) {
                byte[] logs = transactionReceiptBase.getLogs(i).toByteArray();
                LogInfo logInfo = new LogInfo(logs);
                logInfoList.add(logInfo);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Proto buff decode transactionReceipt exception!");
        }

        protoEncoded = protoTransactionReceipt;
    }


    public TransactionReceipt(byte[] stateRoot, byte[] cumulativeEnergon,
                              Bloom bloomFilter, List<LogInfo> logInfoList) {
        this.stateRoot = stateRoot;
        this.cumulativeEnergon = cumulativeEnergon;
        this.bloomFilter = bloomFilter;
        this.logInfoList = logInfoList;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public byte[] getCumulativeEnergon() {
        return cumulativeEnergon;
    }

    public byte[] getEnergonUsed() {
        return energonUsed;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public long getCumulativeEnergonLong() {
        return new BigInteger(1, cumulativeEnergon).longValue();
    }


    public Bloom getBloomFilter() {
        return bloomFilter;
    }

    public void setBloomFilter(Bloom bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public boolean isValid() {
        return ByteUtil.byteArrayToLong(energonUsed) > 0;
    }

    public boolean isSuccessful() {
        return error.isEmpty();
    }

    public String getError() {
        return error;
    }

    /**
     *  Used for Receipt trie hash calculation. Should contain only the following items encoded:
     *  [postTxState, cumulativeGas, bloomFilter, logInfoList]
     */
    public byte[] getReceiptTrieEncoded() {
        return getEncoded(true);
    }

    /**
     * Used for serialization, contains all the receipt data encoded
     */
    public byte[] getEncoded() {
        if (protoEncoded == null) {
            protoEncoded = getEncoded(false);
        }

        return protoEncoded;
    }

    public byte[] getEncoded(boolean receiptTrie) {
        TransactionReceiptProto.TransactionReceiptBase.Builder transactionReceipt =
                TransactionReceiptProto.TransactionReceiptBase.newBuilder();

        transactionReceipt.setCumulativeEnergon(ByteString.copyFrom(cumulativeEnergon));
        transactionReceipt.setStateRoot(ByteString.copyFrom(stateRoot));
        transactionReceipt.setBloomFilter(ByteString.copyFrom(bloomFilter.getData()));

        if(logInfoList != null && logInfoList.size() > 0) {
            for (int i = 0; i < logInfoList.size(); i++) {
                transactionReceipt.addLogs(logInfoList.get(i).getEncodedBuilder());
            }
        }

        //todo fix me here, is it correct?
        if(receiptTrie) {
            transactionReceipt.setEnergonUsed(ByteString.copyFrom(energonUsed));
            transactionReceipt.setExecutionResult(ByteString.copyFrom(executionResult));
        }

        return transactionReceipt.build().toByteArray();
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
        protoEncoded = null;
    }

    public void setTxStatus(boolean success) {
        this.stateRoot = success ? new byte[]{1} : new byte[0];
        protoEncoded = null;
    }

    public boolean hasTxStatus() {
        return stateRoot != null && stateRoot.length <= 1;
    }

    public boolean isTxStatusOK() {
        return stateRoot != null && stateRoot.length == 1 && stateRoot[0] == 1;
    }

    public void setCumulativeEnergon(long cumulativeEnergon) {
        this.cumulativeEnergon = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(cumulativeEnergon));
        protoEncoded = null;
    }

    public void setCumulativeEnergon(byte[] cumulativeEnergon) {
        this.cumulativeEnergon = cumulativeEnergon;
        protoEncoded = null;
    }

    public void setEnergonUsed(byte[] energonUsed) {
        this.energonUsed = energonUsed;
        protoEncoded = null;
    }

    public void setEnergonUsed(long energonUsed) {
        this.energonUsed = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(energonUsed));
        protoEncoded = null;
    }

    public void setExecutionResult(byte[] executionResult) {
        this.executionResult = executionResult;
        protoEncoded = null;
    }

    public void setError(String error) {
        this.error = error == null ? "" : error;
    }

    public void setLogInfoList(List<LogInfo> logInfoList) {
        if (logInfoList == null) return;
        this.logInfoList = logInfoList;

        for (LogInfo loginfo : logInfoList) {
            bloomFilter.or(loginfo.getBloom());
        }
        protoEncoded = null;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        if (transaction == null) throw new NullPointerException("Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");
        return transaction;
    }

    @Override
    public String toString() {

        // todo: fix that

        return "TransactionReceipt[" +
                "\n  , " + (hasTxStatus() ? ("txStatus=" + (isTxStatusOK() ? "OK" : "FAILED"))
                                        : ("postTxState=" + toHexString(stateRoot))) +
                "\n  , cumulativeEnergon=" + toHexString(cumulativeEnergon) +
                "\n  , energonUsed=" + toHexString(energonUsed) +
                "\n  , error=" + error +
                "\n  , executionResult=" + toHexString(executionResult) +
                "\n  , bloom=" + bloomFilter.toString() +
                "\n  , logs=" + logInfoList +
                ']';
    }

//    public long estimateMemSize() {
//        return MemEstimator.estimateSize(this);
//    }

//    public static final MemSizeEstimator<TransactionReceipt> MemEstimator = receipt -> {
//        if (receipt == null) {
//            return 0;
//        }
//        long logSize = receipt.logInfoList.stream().mapToLong(LogInfo.MemEstimator::estimateSize).sum() + 16;
//        return (receipt.transaction == null ? 0 : Transaction.MemEstimator.estimateSize(receipt.transaction)) +
//                (receipt.postTxState == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.postTxState)) +
//                (receipt.cumulativeGas == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.cumulativeGas)) +
//                (receipt.gasUsed == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.gasUsed)) +
//                (receipt.executionResult == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.executionResult)) +
//                ByteArrayEstimator.estimateSize(receipt.rlpEncoded) +
//                Bloom.MEM_SIZE +
//                receipt.error.getBytes().length + 40 +
//                logSize;
//    };
}
