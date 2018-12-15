package org.platon.core.rpc;

import org.platon.core.exception.OperationException;
import org.platon.slice.message.request.TransactionBaseRequest;
import org.platon.slice.message.response.*;


public interface ProtoRpc {

    String atp4Sha3(String data);

    String atpProtocolVersion();

    SyncingResultResponse atpSyncing();

    BoolResponse atpMining();

    StringArrayResponse atpAccounts();

    StringResponse atpBlockNumber();

    StringResponse atpGetBalance(String address, String blockId);

    StringResponse atpGetLastBalance(String address);

    StringResponse atpGetBlockTransactionCountByHash(String blockHash);

    StringResponse atpGetBlockTransactionCountByNumber(String bnOrId);

    StringResponse atpSign(String addr, String data);

    StringResponse atpSendTransaction(TransactionBaseRequest txRequest) throws OperationException;

    StringResponse atpSendFillTransaction(TransactionBaseRequest txRequest);

    
    StringResponse atpCall(TransactionBaseRequest txRequest);

    BlockResponse atpGetBlockByHash(String blockHash, Boolean fullTransactionObjects);

    BlockResponse atpGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects);

    TransactionResponse atpGetTransactionByHash(String transactionHash);

    TransactionResponse atpGetTransactionByBlockHashAndIndex(String blockHash, String index);

    TransactionResponse atpGetTransactionByBlockNumberAndIndex(String bnOrId, String index);

    TransactionReceiptResponse atpGetTransactionReceipt(String txHash);

    StringResponse personalNewAccount(String password);

    StringResponse personalImportRawKey(String keydata, String passphrase);

    BoolResponse personalUnlockAccount(String addr, String pass, String duration);

    BoolResponse personalLockAccount(String address);

    StringArrayResponse personalListAccounts();

    StringResponse personalSignAndSendTransaction(TransactionBaseRequest tx, String password);
}
