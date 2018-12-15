package org.platon.core.rpc;

import com.google.protobuf.ByteString;
import org.platon.common.AppenderName;
import org.platon.common.utils.ByteUtil;
import org.platon.common.utils.Numeric;
import org.platon.core.TransactionInfo;
import org.platon.core.block.BlockHeader;
import org.platon.core.config.Constants;
import org.platon.core.exception.OperationException;
import org.platon.core.exception.PlatonException;
import org.platon.core.keystore.Keystore;
import org.platon.core.block.Block;
import org.platon.core.facade.Platon;
import org.platon.core.rpc.model.AccountModel;
import org.platon.core.transaction.LogInfo;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.core.transaction.proto.TransactionBody;
import org.platon.core.transaction.proto.TransactionType;
import org.platon.crypto.ECKey;
import org.platon.crypto.HashUtil;
import org.platon.slice.message.request.TransactionBaseRequest;
import org.platon.slice.message.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProtoRpcImpl implements ProtoRpc {

    private final static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_RPC);

    @Autowired
    private Keystore keystore;

    @Autowired
    private Platon platon;

    private static final String BLOCK_LATEST = "latest";

    final Map<String, String> addresses = new ConcurrentHashMap<>();

    final Map<String, AccountModel> unlockedAccounts = new ConcurrentHashMap<>();

    @Override
    public String atp4Sha3(String data) {
        byte[] result = HashUtil.sha3(data.getBytes());
        return Numeric.toHexString(result);
    }

    @Override
    public String atpProtocolVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SyncingResultResponse atpSyncing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BoolResponse atpMining() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringArrayResponse atpAccounts() {
        String[] accounts = keystore.listStoredKeys();
        return convertArrayToResponse(accounts);
    }

    @Override
    public StringResponse atpBlockNumber() {
        if (logger.isDebugEnabled()) {
            logger.debug("into atpBlockNumber.");
        }
        //long blockNumber = this.platon.getBestBlock().info().getNumber();
        long blockNumber = 0;
        return convertStrToResponse(Numeric.toHexStringWithPrefix(BigInteger.valueOf(blockNumber)));
    }

    @Override
    public StringResponse atpGetBalance(String address, String blockId) {

        Objects.requireNonNull(address, "address is required.");
        blockId = null == blockId ? BLOCK_LATEST : blockId;
        byte[] addrAsByteArray = Numeric.hexStringToByteArray(address);

        //BigInteger balance = platon.getBalance(addrAsByteArray);
        BigInteger balance = BigInteger.ZERO;
        return convertStrToResponse(Numeric.toHexStringWithPrefix(balance));
    }

    @Override
    public StringResponse atpGetLastBalance(String address) {
        return atpGetBalance(address, BLOCK_LATEST);
    }

    @Override
    public StringResponse atpGetBlockTransactionCountByHash(String blockHash) {
        final byte[] bbHash = Numeric.hexStringToByteArray(blockHash);
        Block block = platon.getBlockchain().getBlockByHash(bbHash);
        if(null == block) {
            return null;
        }
        long size = block.getTransactions().size();
        String hexSize = Numeric.toHexStringWithPrefixSafe(BigInteger.valueOf(size));
        return StringResponse.newBuilder().setData(hexSize).build();
    }

    private Block getBlockByHexHash(String blockHash) {
        return null;
    }

    @Override
    public StringResponse atpGetBlockTransactionCountByNumber(String bnOrId) {

        if (logger.isDebugEnabled()) {
            logger.debug("into atpGetBlockTransactionCountByNumber, the bnOrId is : {}", bnOrId);
        }
        List<Transaction> list = getTransactionsByHexBlockId(bnOrId);
        if (list == null) return null;
        long n = list.size();
        return convertStrToResponse(Numeric.toHexStringWithPrefix(BigInteger.valueOf(n)));
    }

    private List<Transaction> getTransactionsByHexBlockId(String id) {
        Block block = getBlockByHexBlockId(id);
        return block != null ? block.getTransactions() : null;
    }

    private Block getBlockByHexBlockId(String id) {
        return null;
    }

    @Override
    public StringResponse atpSign(String addr, String data) {

        return null;
    }

    @Override
    public StringResponse atpSendTransaction(TransactionBaseRequest txRequest) throws OperationException {
        if (logger.isDebugEnabled()) {
            logger.debug("into atpSendTransaction , txRequest : {} ", txRequest);
        }
        if (txRequest.getBody() == null) return null;
        AccountModel account = getAccountFromKeystore(txRequest.getFrom());
        return sendTransaction(txRequest, account);
    }

    private StringResponse sendTransaction(TransactionBaseRequest txRequest, AccountModel account) {

        byte[] valueBytes = txRequest.getBody().getValue().toByteArray();
        final BigInteger valueBigInt = valueBytes != null ? Numeric.toBigInt(valueBytes) : BigInteger.ZERO;
        final byte[] value = null == valueBytes ? ByteUtil.EMPTY_BYTE_ARRAY : valueBytes;

        TransactionBody body = txRequest.getBody();
        TransactionType type = body.getType();

        final Transaction tx = new Transaction(
                type,
                valueBigInt,
                body.getReceiveAddress() != null ? body.getReceiveAddress().toByteArray() : ByteUtil.EMPTY_BYTE_ARRAY,
                body.getReferenceBlockNum(),
                body.getReferenceBlockHash().toByteArray(),
                Numeric.toBigInt(body.getEnergonPrice().toByteArray()),
                body.getEnergonLimit() != null ? Numeric.toBigInt(body.getEnergonLimit().toByteArray()) : BigInteger.valueOf(90_000),
                body.getData() != null ? body.getData().toByteArray() : ByteUtil.EMPTY_BYTE_ARRAY
        );

        // sign message
        tx.sign(account.getEcKey());

        validateAndSubmit(tx);

        return convertStrToResponse(Numeric.toHexString(tx.getHash()));
    }

    @Override
    public StringResponse atpSendFillTransaction(TransactionBaseRequest txRequest) {

        if (logger.isDebugEnabled()) {
            logger.debug("into atpSendFillTransaction.");
        }
        if (null == txRequest.getBody()) throw new IllegalArgumentException("Invalid request params.");

        // TransactionBaseRequest -> TransactionProto
        try {
            org.platon.core.transaction.proto.Transaction pbTx = convertToTxProto(txRequest);
            Transaction tx = new Transaction(pbTx.toByteArray());
            tx.protoParse();
            validateAndSubmit(tx);
            // tips: for test
            //String sender = Numeric.toHexString(tx.getSender());

            return convertStrToResponse(Numeric.toHexString(tx.getHash()));
        } catch (Exception e) {
            logger.error("atpSendFillTransaction throw exception.", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void validateAndSubmit(Transaction tx) {
        platon.submitTransaction(tx);
    }

    @Override
    public StringResponse atpCall(TransactionBaseRequest txRequest) {

        return null;
    }

    @Override
    public BlockResponse atpGetBlockByHash(String blockHash, Boolean fullTransactionObjects) {
        final Block b = getBlockByHexHash(blockHash);
        return getBlockResponse(b, fullTransactionObjects);
    }

    @Override
    public BlockResponse atpGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects) {

        final Block b = getBlockByHexBlockId(bnOrId);
        ;
        return (b == null ? null : getBlockResponse(b, fullTransactionObjects));
    }

    @Override
    public TransactionResponse atpGetTransactionByHash(String transactionHash) {

        if (logger.isTraceEnabled()) {
            logger.trace("into atpGetTransactionByHash(), the transactionHash:{}", transactionHash);
        }

        final byte[] txHash = Numeric.hexStringToByteArray(transactionHash);

        final TransactionInfo txInfo = this.platon.getTransactionInfo(txHash);
        if(txInfo == null){
            return null;
        }
        final Block block = getBlockByHexHash(Numeric.toHexString(txInfo.getBlockHash()));
        final Block mainBlock = getBlockByHexBlockId(Numeric.toHexStringWithPrefix(BigInteger.valueOf(block.getBlockHeader().getNumber())));

        if(!Arrays.equals(block.getBlockHeader().getHash(),mainBlock.getBlockHeader().getHash())){
            return null;
        }

        // convert to transactionResponse
        txInfo.setTransaction(block.getTransactions().get(txInfo.getIndex()));
        return getTxResponse(block, txInfo.getIndex(), txInfo.getReceipt().getTransaction());
    }

    @Override
    public TransactionResponse atpGetTransactionByBlockHashAndIndex(String blockHash, String index) {

        if (logger.isDebugEnabled()) {
            logger.debug("into atpGetTransactionByBlockHashAndIndex , the blockHash : {}, the index: {}", blockHash, index);
        }
        Block b = getBlockByHexHash(blockHash);
        if (b == null) return null;
        int idx = Numeric.toBigInt(index).intValue();
        if (idx >= b.getTransactions().size()) return null;
        Transaction tx = b.getTransactions().get(idx);
        return getTxResponse(b, idx, tx);
    }

    @Override
    public TransactionResponse atpGetTransactionByBlockNumberAndIndex(String bnOrId, String index) {

        if (logger.isDebugEnabled()) {
            logger.debug("into atpGetTransactionByBlockNumberAndIndex, the bnOrId:{} , the index : {} .", bnOrId, index);
        }
        Block b = getBlockByHexBlockId(bnOrId);
        List<Transaction> txs = getTransactionsByHexBlockId(bnOrId);
        if (txs == null) return null;
        int _index = Numeric.toBigInt(index).intValue();
        if (_index >= txs.size()) return null;
        Transaction tx = txs.get(_index);
        return getTxResponse(b, _index, tx);
    }

    @Override
    public TransactionReceiptResponse atpGetTransactionReceipt(String txHash) {

        if (logger.isTraceEnabled()) {
            logger.trace("into atpGetTransactionReceipt , the txHash : {} .", txHash);
        }
        final byte[] hash = Numeric.hexStringToByteArray(txHash);
        final TransactionInfo txInfo = platon.getTransactionInfo(hash);
        if(null == txInfo) {
            return null;
        }

        final Block block = getBlockByHexHash(Numeric.toHexString(txInfo.getBlockHash()));
        final Block mainBlock = getBlockByHexBlockId(Numeric.toHexStringWithPrefix(BigInteger.valueOf(block.getBlockHeader().getNumber())));

        if(!Arrays.equals(block.getBlockHeader().getHash(),mainBlock.getBlockHeader().getHash())){
            return null;
        }

        return getTxReceiptResponse(block, txInfo);
    }

    @Override
    public StringResponse personalNewAccount(String password) {
        if (logger.isTraceEnabled()) {
            logger.trace("into personalNewAccount..");
        }
        ECKey key = new ECKey();
        final AccountModel account = new AccountModel();
        account.init(key);
        final String address = Numeric.toHexString(account.getAddress());
        int i = 1;
        boolean vacant = false;
        String name = null;
        while (!vacant) {
            name = String.format("Account #%s", i);
            if (!addresses.values().contains(name)) {
                vacant = true;
            } else {
                ++i;
            }
        }
        keystore.storeKey(key, password);
        addresses.put(address, name);
        return convertStrToResponse(address);
    }

    @Override
    public StringResponse personalImportRawKey(String keydata, String passphrase) {

        throw new UnsupportedOperationException();
    }

    @Override
    public BoolResponse personalUnlockAccount(String addr, String pass, String duration) {

        if (logger.isDebugEnabled()) {
            logger.debug("into personalUnlockAccount , the addr : {}, the pass : {} , the duration : {}",
                    addr, pass, duration);
        }
        Objects.requireNonNull(addr, "addr is required.");
        Objects.requireNonNull(pass, "password is required.");

        final ECKey key = keystore.loadStoredKey(addr, pass);
        if (null == key) {
            throw new PlatonException("No key was found in keystore for account : " + addr);
        } else {
            logger.info("Found key address is " + Numeric.toHexString(key.getAddress()));
            final AccountModel model = new AccountModel();
            model.init(key);
            logger.info("Found account address is " + Numeric.toHexString(model.getAddress()));
            unlockedAccounts.put(Numeric.toHexString(model.getAddress()).toLowerCase(), model);
            return BoolResponse.newBuilder().setData(true).build();
        }
    }

    @Override
    public BoolResponse personalLockAccount(String address) {

        if (logger.isDebugEnabled()) {
            logger.debug("into personalLockAccount , the address : {} ", address);
        }
        Objects.requireNonNull(address, "address is required.");
        unlockedAccounts.remove(address.toLowerCase());

        return BoolResponse.newBuilder().setData(true).build();
    }

    @Override
    public StringArrayResponse personalListAccounts() {

        if (logger.isDebugEnabled()) {
            logger.debug("into personalListAccounts.");
        }

        String[] keys = keystore.listStoredKeys();
        return convertArrayToResponse(keys);
    }

    @Override
    public StringResponse personalSignAndSendTransaction(TransactionBaseRequest tx, String password) {

        if (logger.isDebugEnabled()) {
            logger.debug("into personalSignAndSendTransaction.");
        }

        Objects.requireNonNull(password, "password is required.");
        final ECKey key = keystore.loadStoredKey(tx.getFrom().toLowerCase(), password);
        if (null == key) throw new PlatonException("No key was found in keystore for account : " + tx.getFrom());
        final AccountModel model = new AccountModel();
        model.init(key);
        return sendTransaction(tx, model);
    }

    public StringArrayResponse convertArrayToResponse(String[] element) {
        StringArrayResponse.Builder builder = StringArrayResponse.newBuilder();
        Arrays.stream(element).forEach(str -> builder.addData(str));
        return builder.build();
    }

    private StringResponse convertStrToResponse(String element) {
        StringResponse.Builder build = StringResponse.newBuilder();
        build.setData(element);
        return build.build();
    }

    protected AccountModel getAccountFromKeystore(String address) throws OperationException{

        final AccountModel model = unlockedAccounts.get(address.toLowerCase());
        if (model != null) {
            return model;
        }

        if (keystore.hasStoredKey(address)) {
            throw new OperationException("Unlocked account is required. Account: " + address, Constants.ERROR__1001_UNLOCK_ACCOUNT);
        } else {
            throw new OperationException("Key not found in keystore", Constants.ERROR__1002_KEY_NOT_FOUND);
        }
    }

    private org.platon.core.transaction.proto.Transaction convertToTxProto(TransactionBaseRequest txRequest) {

        org.platon.core.transaction.proto.Transaction.Builder txBuilder =
                org.platon.core.transaction.proto.Transaction.newBuilder();
        txBuilder.setBody(txRequest.getBody());
        if (null != txRequest.getSignature()) {
            txBuilder.setSignature(ByteString.copyFrom(txRequest.getSignature().getBytes()));
        }
        return txBuilder.build();
    }

    protected BlockResponse getBlockResponse(Block block, boolean fullTx) {
        if (block == null)
            return null;
        //TODO: pow 时 nonce 有意义
        BlockResponse.Builder bloBuild = BlockResponse.newBuilder();

        BlockHeader bh = block.getBlockHeader();
        if (null == bh) return null;

        bloBuild.setBlockNumber(block.getBlockHeader().getNumber());
        if (null != bh.getHash()) bloBuild.setHash(Numeric.toHexString(block.getBlockHeader().getHash()));
        if (null != bh.getParentHash())
            bloBuild.setParentHash(Numeric.toHexString(block.getBlockHeader().getParentHash()));
        if (bh.getBloomLog() != null) bloBuild.setLogsBloom(Numeric.toHexString(bh.getBloomLog()));
        if (null != bh.getTransactionRoot()) bloBuild.setTransactionsRoot(Numeric.toHexString(bh.getTransactionRoot()));
        if (null != bh.getStateRoot()) bloBuild.setStateRoot(Numeric.toHexString(bh.getStateRoot()));
        if (null != bh.getReceiptRoot()) bloBuild.setReceiptsRoot(Numeric.toHexString(bh.getReceiptRoot()));
        if (null != bh.getAuthor()) bloBuild.setMiner(Numeric.toHexString(bh.getAuthor()));

        bloBuild.setSize(block.getBlockSize());
        if (null != bh.getExtraData()) bloBuild.setExtraData(Numeric.toHexString(bh.getExtraData()));
        if (null != bh.getEnergonCeiling())
            bloBuild.setEnergonLimit(ByteString.copyFrom(bh.getEnergonCeiling().toByteArray()));
        if (null != bh.getEnergonUsed())
            bloBuild.setEnergonUsed(ByteString.copyFrom(bh.getEnergonUsed().toByteArray()));
        bloBuild.setTimestamp(bh.getTimestamp());

        // txs
        List<Transaction> txs = block.getTransactions();
        if (null != txs) {
            for (int i = 0; i < txs.size(); i++) {
                bloBuild.setTransactions(i, Numeric.toHexString(txs.get(i).getHash()));
            }
        }

        return bloBuild.build();
    }

    protected TransactionResponse getTxResponse(Block b, int txIndex, Transaction tx) {

        TransactionResponse.Builder txBuilder = TransactionResponse.newBuilder();
        txBuilder.setHash(Numeric.toHexString(tx.getHash()));
        txBuilder.setBlockHash(Numeric.toHexString(b.getBlockHeader().getHash()));
        txBuilder.setBlockNumber(b.getBlockHeader().getNumber());
        txBuilder.setTransactionIndex(txIndex);
        txBuilder.setFrom(Numeric.toHexString(tx.getSender()));
        if (null != tx.getReceiveAddress()) txBuilder.setTo(Numeric.toHexString(tx.getReceiveAddress()));
        txBuilder.setValue(ByteString.copyFrom(tx.getValue().toByteArray()));
        txBuilder.setEnergonPrice(ByteString.copyFrom(tx.getEnergonPrice().toByteArray()));
        txBuilder.setEnergonLimit(ByteString.copyFrom(tx.getEnergonLimit().toByteArray()));
        txBuilder.setInput(Numeric.toHexString(tx.getData()));
        return txBuilder.build();
    }

    protected TransactionReceiptResponse getTxReceiptResponse(Block b, TransactionInfo txInfo) {

        if (null == txInfo) return null;
        TransactionReceipt txReceipt = txInfo.getReceipt();

        TransactionReceiptResponse.Builder txReceiptBuild = TransactionReceiptResponse.newBuilder();
        txReceiptBuild.setTransactionhash(Numeric.toHexString(txReceipt.getTransaction().getHash()));
        txReceiptBuild.setTransactionIndex(txInfo.getIndex());

        if (null != b) {
            txReceiptBuild.setBlockHash(Numeric.toHexString(b.getBlockHeader().getHash()));
            txReceiptBuild.setBlockNumber(b.getBlockHeader().getNumber());
        }
        txReceiptBuild.setFrom(Numeric.toHexString(txReceipt.getTransaction().getSender()));
        txReceiptBuild.setTo(Numeric.toHexString(txReceipt.getTransaction().getReceiveAddress()));
        //if(null != txReceipt.getTransaction().get){}

        // logs
        List<LogInfo> logInfos = txReceipt.getLogInfoList();
        if (null != logInfos) {
            for (int i = 0; i < logInfos.size(); i++) {
                LogInfo logInfo = logInfos.get(i);

                LogEntry.Builder logBuilder = LogEntry.newBuilder();
                logBuilder.setBlockHash(Numeric.toHexString(b.getBlockHeader().getHash()));
                logBuilder.setAddress(Numeric.toHexString(logInfo.getAddress()));
                logBuilder.setLogIndex(Numeric.toHexStringWithPrefix(BigInteger.valueOf(i)));
                logBuilder.setData(Numeric.toHexString(logInfo.getData()));

                for (int n = 0; n < logInfo.getTopics().size(); n++) {
                    logBuilder.setTopics(n, Numeric.toHexString(logInfo.getTopics().get(n).getData()));
                }
                if (b != null) logBuilder.setBlockNumber(b.getBlockHeader().getNumber());
                logBuilder.setTransactionIndex(txInfo.getIndex());
                logBuilder.setTransactionHash(Numeric.toHexString(txReceipt.getTransaction().getHash()));
                txReceiptBuild.setLogs(i, logBuilder.build());
            }
        }

        return txReceiptBuild.build();
    }

}
