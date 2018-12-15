package org.platon.core.transaction;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.util.encoders.Base64;
import org.platon.core.transaction.proto.*;
import org.platon.core.transaction.util.ByteUtil;
import org.platon.crypto.ECKey;
import org.platon.crypto.ECKey.ECDSASignature;
import org.platon.crypto.ECKey.MissingPrivateKeyException;
import org.platon.crypto.HashUtil;
import org.platon.crypto.WalletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * A transaction (formally, T) is a single cryptographically
 * signed instruction sent by an actor external to platon.
 * An external actor can be a person (via a mobile device or desktop computer)
 * or could be from a piece of automated software running on a server.
 * There are six types of transactions:
 * TRANSACTION,            // the normal transaction
 * VOTE,                   // vote
 * CONTRACT_DEPLOY,        // contract deploy
 * CONTRACT_CALL,          // contract call
 * TRANSACTION_MPC,        // MPC and other special transaction
 * QUERY_CALL,             // query call
 * PERMISSION_UPDATE       // permission update
 */
public class Transaction {

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    private static final BigInteger DEFAULT_ENERGON_PRICE = new BigInteger("10000000000000");
    private static final BigInteger DEFAULT_BALANCE_ENERGON = new BigInteger("21000");

    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;

    /* SHA3 hash of the proto3 encoded transaction */
    private byte[] hash;

    /* specify what transaction type is */
    private TransactionType transactionType;

    /* the amount of energon to transfer (calculated as the base unit) */
    private BigInteger value;

    /* the address of the destination account
     * In creation transaction the receive address is - 0 */
    private byte[] receiveAddress;

    private long referenceBlockNum;

    private byte[] referenceBlockHash;

    /* the amount of energon to pay as a transaction fee
     * to the miner for each unit of energon */
    private BigInteger energonPrice;

    /* the amount of "energon" to allow for the computation.
     * energon is the fuel of the computational engine;
     * every computational step taken and every byte added
     * to the state or transaction list consumes some energon. */
    private BigInteger energonLimit;

    /* An unlimited size byte array specifying
     * input [data] of the message call or
     * Initialization code for a new contract */
    private byte[] data;

    /**
     * encode chainId in V (or one of RSV)
     */
    private static final int CHAIN_ID_INC = 35;

    private static final int LOWER_REAL_V = 27;

    private Integer chainId = null;

    /* the elliptic curve signature
     * (including public key recovery bits) */
    private ECDSASignature signature;

    protected byte[] sendAddress;

    /* Tx in encoded form */
    protected byte[] protoEncoded;

    // sha3 hash of the proto buff 3 encoded transaction data without any signature data
    private byte[] rawHash;

    /* Indicates if this transaction has been parsed
     * from the proto-encoded data */
    protected boolean parsed = false;

    public Transaction(byte[] rawData) {
        this.protoEncoded = rawData;
        parsed = false;
    }

    public Transaction(TransactionType type, BigInteger value, byte[] receiveAddress,
                       long referenceBlockNum, byte[] referenceBlockHash, BigInteger energonPrice,
                       BigInteger energonLimit, byte[] data,  Integer chainId) {
        this.transactionType = type;
        this.referenceBlockNum = referenceBlockNum;
        this.referenceBlockHash = referenceBlockHash;
        this.energonPrice = energonPrice;
        this.energonLimit = energonLimit;
        this.receiveAddress = receiveAddress;
        this.value = value;
        if(this.referenceBlockNum < 0) {
            // throw new RuntimeException()?
            this.referenceBlockNum = 0;
        }
        this.data = data;
        this.chainId = chainId;

        if (receiveAddress == null) {
            this.receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }

        parsed = true;
    }

    public Transaction(TransactionType type, BigInteger value, byte[] receiveAddress,
                       long referenceBlockNum, byte[] referenceBlockHash, BigInteger energonPrice,
                       BigInteger energonLimit, byte[] data) {
        this(type, value, receiveAddress, referenceBlockNum, referenceBlockHash, energonPrice, energonLimit, data, null);
    }

    public Transaction(TransactionType type, BigInteger value, byte[] receiveAddress,
                       long referenceBlockNum, byte[] referenceBlockHash, BigInteger energonPrice,
                       BigInteger energonLimit, byte[] data, byte[] r, byte[] s, byte v, Integer chainId) {
        this(type, value, receiveAddress, referenceBlockNum, referenceBlockHash, energonPrice, energonLimit, data, chainId);
        this.signature = ECDSASignature.fromComponents(r, s, v);
    }

    public Transaction(TransactionType type, BigInteger value, byte[] receiveAddress,
                       long referenceBlockNum, byte[] referenceBlockHash, BigInteger energonPrice,
                       BigInteger energonLimit, byte[] data, byte[] r, byte[] s, byte v) {
        this(type, value, receiveAddress, referenceBlockNum, referenceBlockHash, energonPrice, energonLimit, data, r, s, v, null);
    }

    private Integer extractChainIdFromRawSignature(BigInteger bv, byte[] r, byte[] s) {
        if (r == null && s == null) return bv.intValue();
        if (bv.bitLength() > 31) return Integer.MAX_VALUE;
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return null;
        return (int) ((v - CHAIN_ID_INC) / 2);
    }

    private byte getRealV(BigInteger bv) {
        if (bv.bitLength() > 31) return 0;
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return (byte) v;
        byte realV = LOWER_REAL_V;
        int inc = 0;
        if ((int) v % 2 == 0) inc = 1;
        return (byte) (realV + inc);
    }

    public synchronized void verify() {
        protoParse();
        validate();
    }

    public synchronized void protoParse() {
        if (parsed) {
            return;
        }
        try {
            org.platon.core.transaction.proto.Transaction tx = org.platon.core.transaction.proto.Transaction.parseFrom(protoEncoded);

            TransactionBody transactionBase = tx.getBody();

            this.transactionType = transactionBase.getType();
            if(transactionBase.getValue() !=  null){
                this.value = new BigInteger(transactionBase.getValue().toByteArray());
            }

            if(transactionBase.getReceiveAddress() != null) {
                this.receiveAddress = transactionBase.getReceiveAddress().toByteArray();
            }
            this.referenceBlockNum = transactionBase.getReferenceBlockNum();

            if(transactionBase.getReferenceBlockHash() != null) {
                this.referenceBlockHash = transactionBase.getReferenceBlockHash().toByteArray();
            }
            if (transactionBase.getEnergonPrice() != null) {
                this.energonPrice = new BigInteger(transactionBase.getEnergonPrice().toByteArray());
            }
            if(transactionBase.getEnergonLimit() != null) {
                this.energonLimit = new BigInteger(transactionBase.getEnergonLimit().toByteArray());
            }
            if(transactionBase.getData() != null) {
                Any any = transactionBase.getData();
                this.data = any.getValue().toByteArray();
            }
            if (tx.getSignature() != null) {
                byte[] vData =  tx.getSignature().toByteArray();
                ECDSASignature signature = WalletUtil.signToECDSASignature(vData);
                byte[] r = signature.r.toByteArray();
                byte[] s = signature.s.toByteArray();
                BigInteger v = new BigInteger(String.valueOf(signature.v));
                this.chainId = extractChainIdFromRawSignature(v, r, s);
                byte realV = getRealV(BigInteger.valueOf(signature.v));
                signature.v = realV;
                this.signature = signature;
            } else {
                logger.debug("proto buff encoded tx is not signed!");
            }
            this.hash = HashUtil.sha3(transactionBase.toByteArray());
            this.parsed = true;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Proto buff decode transaction exception!");
        }
    }

    private void validate() {
        if (receiveAddress != null && receiveAddress.length != 0 && receiveAddress.length != ADDRESS_LENGTH) {
            throw new RuntimeException("Receive address is not valid");
        }

        if(referenceBlockNum < 0) {
            throw new RuntimeException("reference block number is not valid");
        }

        if (referenceBlockHash != null  && referenceBlockHash.length > HASH_LENGTH) {
            throw new RuntimeException("reference Block Hash is not valid");
        }

        if (energonLimit.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Energon Limit is not valid");
        }

        if (energonPrice.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Energon Price is not valid price");
        }

        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Value is not valid");
        }

        if (getSignature() != null) {
            if (BigIntegers.asUnsignedByteArray(signature.r).length > HASH_LENGTH)
                throw new RuntimeException("Signature R is not valid");
            if (BigIntegers.asUnsignedByteArray(signature.s).length > HASH_LENGTH)
                throw new RuntimeException("Signature S is not valid");
            if (getSender() != null && getSender().length != ADDRESS_LENGTH)
                //todo  need to fix the address (20 bytes)
                //throw new RuntimeException("Sender is not valid");
                logger.error("Error! Sender Address length is not correct");
        }
    }

    public boolean isParsed() {
        return parsed;
    }

    public byte[] getHash() {
        if (!isEmpty(hash)) {
            return hash;
        }
        protoParse();
        getEncoded();
        return hash;
    }

    public byte[] getRawHash() {
        protoParse();
        if (rawHash != null) {
            return rawHash;
        }
        byte[] plainMsg = this.getEncodedRaw();
        return rawHash = HashUtil.sha3(plainMsg);
    }

    public boolean isValueTx() {
        protoParse();
        return value != null;
    }

    public BigInteger getValue() {
        protoParse();
        return value == null ? BigInteger.ZERO : value;
    }

    protected void setValue(BigInteger value) {
        this.value = value;
        parsed = true;
    }

    public byte[] getReceiveAddress() {
        protoParse();
        return receiveAddress;
    }

    protected void setReceiveAddress(byte[] receiveAddress) {
        this.receiveAddress = receiveAddress;
        parsed = true;
    }

    public BigInteger getEnergonPrice() {
        protoParse();
        return energonPrice == null ? BigInteger.ZERO : energonPrice;
    }

    protected void setEnergonPrice(BigInteger energonPrice) {
        this.energonPrice = energonPrice;
        parsed = true;
    }

    public BigInteger getEnergonLimit() {
        protoParse();
        return energonLimit == null ? BigInteger.ZERO : energonLimit;
    }

    protected void setEnergonLimit(BigInteger energonLimit) {
        this.energonLimit = energonLimit;
        parsed = true;
    }

    public long nonZeroDataBytes() {
        if (data == null) return 0;
        int counter = 0;
        for (final byte aData : data) {
            if (aData != 0) ++counter;
        }
        return counter;
    }

    public long zeroDataBytes() {
        if (data == null) return 0;
        int counter = 0;
        for (final byte aData : data) {
            if (aData == 0) ++counter;
        }
        return counter;
    }


    public byte[] getData() {
        protoParse();
        return data;
    }

    protected void setData(byte[] data) {
        this.data = data;
        parsed = true;
    }

    public ECDSASignature getSignature() {
        protoParse();
        return signature;
    }

    public long getReferenceBlockNum() {
        protoParse();
        return referenceBlockNum;
    }

    protected void setReferenceBlockNum(long referenceBlockNum) {
        this.referenceBlockNum = referenceBlockNum;
        parsed = true;
    }

    public byte[] getReferenceBlockHash() {
        protoParse();
        return referenceBlockHash;
    }

    protected void setReferenceBlockHash(byte[] referenceBlockHash){
        this.referenceBlockHash = referenceBlockHash;
        parsed = true;
    }

    public TransactionType getTransactionType() {
        protoParse();
        return transactionType;
    }

    protected  void setTransactionType(TransactionType type) {
        transactionType = type;
        parsed = true;
    }

    public byte[] getContractAddress() {
        if (!isContractCreation()) {
            return null;
        }

        if(this.getSender() == null || this.getHash() == null) {
            return null;
        }

        byte[] addrParameter = new byte[Transaction.HASH_LENGTH + Transaction.ADDRESS_LENGTH];
        System.arraycopy(this.getSender(), 0, addrParameter, 0, Transaction.ADDRESS_LENGTH);
        System.arraycopy(this.getHash(), 0, addrParameter, Transaction.ADDRESS_LENGTH, Transaction.HASH_LENGTH);
        byte[] addrHash = HashUtil.sha3(addrParameter);
        return Arrays.copyOfRange(addrHash, 12, addrHash.length);
    }

    public boolean isContractCreation() {
        protoParse();
        return this.receiveAddress == null || Arrays.equals(this.receiveAddress,ByteUtil.EMPTY_BYTE_ARRAY);
    }

    //todo it seems no need to get ECKey from signature, maybe implement it in ECkey.java if it is needed
//    public ECKey getKey() {
//        byte[] hash = getRawHash();
//        return ECKey.recoverFromSignature(signature.v, signature, hash);
//    }

//    public static ECKey recoverFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
//        final byte[] pubBytes = ECKey.recoverPubBytesFromSignature(recId, sig, messageHash);
//        if (pubBytes == null) {
//            return null;
//        } else {
//            //return ECKey.fromPublicOnly(pubBytes);
//            return new ECKey(null, CURVE.getCurve().decodePoint(pubBytes));
//        }
//    }

    public synchronized byte[] getSender() {
        try {
            if (sendAddress == null && getSignature() != null) {
                sendAddress = WalletUtil.signatureToAddress(getHash(), WalletUtil.signToByteArray(getSignature()));
            }
            return sendAddress;
        } catch (SignatureException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public Integer getChainId() {
        protoParse();
        return chainId == null ? null : (int) chainId;
    }

    public void sign(ECKey key) throws MissingPrivateKeyException {
        this.signature = WalletUtil.signature(this.getRawHash(), key);
        this.protoEncoded = null;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "hash=" + Arrays.toString(hash) +
                ", transactionType=" + transactionType +
                ", value=" + value +
                ", receiveAddress=" + Arrays.toString(receiveAddress) +
                ", referenceBlockNum=" + referenceBlockNum +
                ", referenceBlockHash=" + Arrays.toString(referenceBlockHash) +
                ", energonPrice=" + energonPrice +
                ", energonLimit=" + energonLimit +
                ", data=" + Arrays.toString(data) +
                ", chainId=" + chainId +
                ", signature=" + signature +
                ", sendAddress=" + Arrays.toString(sendAddress) +
                ", protoEncoded=" + Arrays.toString(protoEncoded) +
                ", rawHash=" + Arrays.toString(rawHash) +
                ", parsed=" + parsed +
                '}';
    }

    /**
     * For signatures you have to keep also
     * proto buff encode of the transaction without any signature data
     */
    public byte[] getEncodedRaw() {
        protoParse();

        byte[] protoRaw;

        TransactionBody.Builder transactionBase = TransactionBody.newBuilder();

        transactionBase.setType(transactionType);
        transactionBase.setValue(ByteString.copyFrom(value.toByteArray()));
        transactionBase.setReceiveAddress(ByteString.copyFrom(receiveAddress));
        transactionBase.setReferenceBlockNum(referenceBlockNum);
        transactionBase.setReferenceBlockHash(ByteString.copyFrom(referenceBlockHash));
        transactionBase.setEnergonLimit(ByteString.copyFrom(energonLimit.toByteArray()));
        transactionBase.setEnergonPrice(ByteString.copyFrom(energonPrice.toByteArray()));

        if(data != null && data.length != 0) {
            switch (transactionType) {
                case TRANSACTION_MPC:
                    try {
                        MPCTransactionRequest mpcTransactionRequest = MPCTransactionRequest.parseFrom(data);
                        Any any = Any.pack(mpcTransactionRequest);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto.buff decode MPC transaction request fail!");
                    }
                    break;

                case CONTRACT_DEPLOY:
                    try {
                        ContractDeployRequest contractDeployReq = ContractDeployRequest.parseFrom(data);
                        Any any = Any.pack(contractDeployReq);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto.buff decode contract deploy request fail!");
                    }
                    break;

                case CONTRACT_CALL:
                    try {
                        ContractRequest contractCallRequest = ContractRequest.parseFrom(data);
                        Any any = Any.pack(contractCallRequest);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto.buff decode contract call request fail!");
                    }
                    break;

                default:
                    try {
                        defaultRequestData defaultRequestData = org.platon.core.transaction.proto.defaultRequestData.parseFrom(data);
                        Any any = Any.pack(defaultRequestData);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto.buff decode default Data request fail!");
                    }
            }
        }

//        if(chainId != null) {
//            ECDSASignature newSignature = new ECDSASignature(new BigInteger(ByteUtil.EMPTY_BYTE_ARRAY),
//                    new BigInteger(ByteUtil.EMPTY_BYTE_ARRAY), chainId.byteValue());
//            transactionBase.setSignature(ByteString.copyFrom(newSignature.toByteArray()));
//        }

        protoRaw = transactionBase.build().toByteArray();

        return protoRaw;
    }

    public byte[] getEncoded() {

        if (protoEncoded != null) {
            return protoEncoded;
        }
        org.platon.core.transaction.proto.Transaction.Builder txBuilder = org.platon.core.transaction.proto.Transaction.newBuilder();
        TransactionBody.Builder transactionBase = TransactionBody.newBuilder();

        transactionBase.setType(transactionType);
        transactionBase.setValue(ByteString.copyFrom(value.toByteArray()));
        transactionBase.setReceiveAddress(ByteString.copyFrom(receiveAddress));
        transactionBase.setReferenceBlockNum(referenceBlockNum);
        transactionBase.setReferenceBlockHash(ByteString.copyFrom(referenceBlockHash));
        transactionBase.setEnergonLimit(ByteString.copyFrom(energonLimit.toByteArray()));
        transactionBase.setEnergonPrice(ByteString.copyFrom(energonPrice.toByteArray()));

        if(data != null && data.length != 0) {
            switch (transactionType) {
                case CONTRACT_DEPLOY:
                    try {
                        ContractDeployRequest contractDeployRequest = ContractDeployRequest.parseFrom(data);
                        Any any = Any.pack(contractDeployRequest);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto buff decode contract deploy request fail!");
                    }
                    break;

                case CONTRACT_CALL:
                    try {
                        ContractRequest contractCallRequest = ContractRequest.parseFrom(data);
                        Any any = Any.pack(contractCallRequest);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto buff decode contract call request fail!");
                    }
                    break;

                case TRANSACTION_MPC:
                    try {
                        MPCTransactionRequest mpcTransactionRequest = MPCTransactionRequest.parseFrom(data);
                        Any any = Any.pack(mpcTransactionRequest);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto buff decode MPC transaction request fail!");
                    }
                    break;

                default:
                    try {
                        defaultRequestData defaultRequestData = org.platon.core.transaction.proto.defaultRequestData.parseFrom(data);
                        Any any = Any.pack(defaultRequestData);
                        transactionBase.setData(any);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Proto buff decode default Data request fail!");
                    }
            }
        }

        //todo test and verify this EMPTY_BYTE_ARRAY, and also check if this is really needed?
        if (signature == null) {
            // todo if the signature is null, no need to encode an empty signature
//            byte v = chainId == null ? (ByteUtil.EMPTY_BYTE_ARRAY)[0] : chainId.byteValue();
//            ECDSASignature newSignature = new ECDSASignature(new BigInteger(ByteUtil.EMPTY_BYTE_ARRAY),
//                    new BigInteger(ByteUtil.EMPTY_BYTE_ARRAY), v);
//            transactionBase.setSignature(ByteString.copyFrom(newSignature.toByteArray()));
        } else {
//            int encodeV;
//            if(chainId == null) {
//                encodeV = signature.v;getEncodedRaw
//            } else {
//                encodeV = signature.v - LOWER_REAL_V;
//                encodeV += chainId * 2 + CHAIN_ID_INC;
//            }
            txBuilder.setSignature(ByteString.copyFrom(Base64.encode(signature.toByteArray())));
        }

        txBuilder.setBody(transactionBase.build());
        protoEncoded = txBuilder.build().toByteArray();

        this.hash = HashUtil.sha3(protoEncoded);
        return protoEncoded;
    }

    @Override
    //todo what the purpose of this function?
    public int hashCode() {

        byte[] hash = this.getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Transaction)) return false;
        Transaction tx = (Transaction) obj;

        return tx.hashCode() == this.hashCode();
    }

    /**
     * @deprecated Use {@link Transaction#createDefault(TransactionType, BigInteger, byte[], long, BigInteger, Integer)} instead
     */
    public static Transaction createDefault(TransactionType type, BigInteger amount,
                                            byte[] to, long referenceBlockNum, BigInteger referenceBlockHash){
        return create(type, to, amount, referenceBlockNum, referenceBlockHash, DEFAULT_ENERGON_PRICE, DEFAULT_BALANCE_ENERGON);
    }

    public static Transaction createDefault(TransactionType type, BigInteger amount,
                                            byte[] to, long referenceBlockNum, BigInteger referenceBlockHash, Integer chainId){
        return create(type, to, amount, referenceBlockNum, referenceBlockHash, DEFAULT_ENERGON_PRICE, DEFAULT_BALANCE_ENERGON, chainId);
    }

    /**
     * @deprecated use {@link Transaction#create(TransactionType, byte[],
     *                                      BigInteger, long, BigInteger,
     *                                      BigInteger, BigInteger, Integer)} instead
     */
    public static Transaction create(TransactionType type, byte[] to,
                                     BigInteger amount, long referenceBlockNum, BigInteger referenceBlockHash,
                                     BigInteger energonPrice, BigInteger energonLimit){
        return new Transaction(type,
                amount,
                to,
                referenceBlockNum,
                BigIntegers.asUnsignedByteArray(referenceBlockHash),
                energonPrice,
                energonLimit,
                null);
    }

    public static Transaction create(TransactionType type, byte[] to,
                                     BigInteger amount, long referenceBlockNum, BigInteger referenceBlockHash,
                                     BigInteger energonPrice, BigInteger energonLimit, Integer chainId){
        return new Transaction(type,
                amount,
                to,
                referenceBlockNum,
                BigIntegers.asUnsignedByteArray(referenceBlockHash),
                energonPrice,
                energonLimit,
                null,
                chainId);
    }

//    public static final MemSizeEstimator<Transaction> MemEstimator = tx ->
//            ByteArrayEstimator.estimateSize(tx.hash) +
//            ByteArrayEstimator.estimateSize(tx.nonce) +
//            ByteArrayEstimator.estimateSize(tx.value) +
//            ByteArrayEstimator.estimateSize(tx.energonPrice) +
//            ByteArrayEstimator.estimateSize(tx.energonLimit) +
//            ByteArrayEstimator.estimateSize(tx.data) +
//            ByteArrayEstimator.estimateSize(tx.sendAddress) +
//            ByteArrayEstimator.estimateSize(tx.rlpEncoded) +
//            ByteArrayEstimator.estimateSize(tx.rawHash) +
//            (tx.chainId != null ? 24 : 0) +
//            (tx.signature != null ? 208 : 0) + // approximate size of signature
//            16; // Object header + ref
}
