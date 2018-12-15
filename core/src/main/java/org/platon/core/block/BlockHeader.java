package org.platon.core.block;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.core.block.proto.BlockHeaderProto;
import org.platon.crypto.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * the block's head
 * Created by alliswell on 2018/7/26.
 */
public class BlockHeader {

    private static final Logger logger = LoggerFactory.getLogger(BlockHeader.class);

    /**
     * the block product time in millionseconds
     **/
    private long timestamp;

    /**
     * address of the author
     **/
    private byte[] author;

    /**
     * father block's hash
     **/
    private byte[] parentHash;

    /**
     * root of the state MPT
     **/
    private byte[] stateRoot;

    /**
     * root of the permission MPT
     **/
    private byte[] permissionRoot;

    /**
     * root of the dpos list MPT
     **/
    private byte[] dposRoot;

    /**
     * root of the transactions MPT
     **/
    private byte[] transactionRoot;

    /**
     * root of the transfer MPT
     **/
    private byte[] transferRoot;

    /**
     * root of the voting MPT
     **/
    private byte[] votingRoot;

    /**
     * root of the receipt MPT
     **/
    private byte[] receiptRoot;

    /**
     * log bloom
     **/
    private byte[] bloomLog;

    private long number;

    private BigInteger energonUsed;

    private BigInteger energonCeiling;

    private BigInteger difficulty;

    private byte[] extraData;

    private byte[] coinbase;

    /**
     * sha3 of the whole Block
     **/
    private byte[] hash;

    public BlockHeader() {
    }

    public BlockHeader(long timestamp, byte[] author, byte[] coinbase, byte[] parentHash, byte[] bloomLog, long number, BigInteger energonUsed, BigInteger energonCeiling, BigInteger difficulty, byte[] extraData) {
        this.timestamp = timestamp;
        this.author = author;
        this.coinbase = coinbase;
        this.parentHash = parentHash;
        this.bloomLog = bloomLog;
        this.number = number;
        this.energonUsed = energonUsed;
        this.energonCeiling = energonCeiling;
        this.difficulty = difficulty;
        this.extraData = extraData;
    }

    public BlockHeader(byte[] bytes) {
        parse(bytes);
    }

    public static byte[] headerHashFromBlock(byte[] _block) {
        return new byte[0];
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getAuthor() {
        return author;
    }

    public void setAuthor(byte[] author) {

        this.author = author;
    }

    public byte[] getParentHash() {
        if (null == parentHash || 0 == parentHash.length) {
            return HashUtil.EMPTY_HASH;
        }
        return parentHash;
    }

    public void setParentHash(byte[] parentHash) {
        this.parentHash = parentHash;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public void setPermissionRoot(byte[] permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    public void setDposRoot(byte[] dposRoot) {
        this.dposRoot = dposRoot;
    }

    public void setTransactionRoot(byte[] transactionRoot) {
        this.transactionRoot = transactionRoot;
    }

    public void setTransferRoot(byte[] transferRoot) {
        this.transferRoot = transferRoot;
    }

    public void setVotingRoot(byte[] votingRoot) {
        this.votingRoot = votingRoot;
    }

    public void setReceiptRoot(byte[] receiptRoot) {
        this.receiptRoot = receiptRoot;
    }

    public byte[] getPermissionRoot() {
        return permissionRoot;
    }

    public byte[] getDposRoot() {
        return dposRoot;
    }

    public byte[] getTransactionRoot() {
        return transactionRoot;
    }

    public byte[] getTransferRoot() {
        return transferRoot;
    }

    public byte[] getVotingRoot() {
        return votingRoot;
    }

    public byte[] getReceiptRoot() {
        return receiptRoot;
    }

    public byte[] getBloomLog() {
        return bloomLog;
    }

    public void setBloomLog(byte[] bloomLog) {
        this.bloomLog = bloomLog;
    }

    public BigInteger getEnergonUsed() {
        return energonUsed;
    }

    public void setEnergonUsed(BigInteger energonUsed) {
        this.energonUsed = energonUsed;
    }

    public BigInteger getEnergonCeiling() {
        return energonCeiling;
    }

    public void setEnergonCeiling(BigInteger energonCeiling) {
        this.energonCeiling = energonCeiling;
    }

    public BigInteger getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(BigInteger difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public int hashCode() {
        if (null == hash || 0 == hash.length) {
            hash = getHash();
        }
        return Objects.hash(hash);
    }

    public byte[] getHash() {
        if (null != hash) {
            return hash;
        }
        hash = HashUtil.sha3(encode());
        return hash;
    }

    public byte[] getExtraData() {
        return extraData;
    }

    public void setExtraData(byte[] extraData) {
        this.extraData = extraData;
    }

    public void verify(BlockHeader parent, byte[] block) {

    }

    public void setRoots(byte[] stateRoot, byte[] permissionRoot,
                         byte[] dposRoot, byte[] transactionRoot, byte[] transferRoot,
                         byte[] votingRoot, byte[] receiptRoot) {
        if (null == stateRoot || 0 == stateRoot.length) {
            logger.error("state root is invalid!");
        }
        if (null == permissionRoot || 0 == permissionRoot.length) {
            logger.error("permission root is invalid!");
        }
        if (null == dposRoot || 0 == dposRoot.length) {
            logger.error("dpos root is invalid!");
        }
        if (null == transactionRoot || 0 == transactionRoot.length) {
            logger.error("transaction root is invalid!");
        }
        if (null == transferRoot || 0 == transferRoot.length) {
            logger.error("transfer root is invalid!");
        }
        if (null == votingRoot || 0 == votingRoot.length) {
            logger.error("voting root is invalid!");
        }
        if (null == receiptRoot || 0 == receiptRoot.length) {
            logger.error("receipt root is invalid!");
        }
        this.stateRoot = stateRoot;
        this.permissionRoot = permissionRoot;
        this.dposRoot = dposRoot;
        this.transactionRoot = transactionRoot;
        this.transferRoot = transferRoot;
        this.votingRoot = votingRoot;
        this.receiptRoot = receiptRoot;
    }

    public void propagatedBy(BlockHeader parent) {
        stateRoot = parent.getStateRoot();
        permissionRoot = parent.getPermissionRoot();
        dposRoot = parent.getDposRoot();
        transactionRoot = parent.getTransactionRoot();
        transferRoot = parent.getTransferRoot();
        votingRoot = parent.getVotingRoot();
        receiptRoot = parent.getReceiptRoot();

        number = parent.getNumber() + 1;
        parentHash = parent.getHash();
        energonCeiling = parent.energonCeiling;
        difficulty = new BigInteger("1");
        energonUsed = new BigInteger("0");
        timestamp = System.currentTimeMillis();
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public byte[] getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(byte[] coinbase) {
        this.coinbase = coinbase;
    }

    public byte[] encode() {
        BlockHeaderProto.BlockHeader.Builder headerBuilder = BlockHeaderProto.BlockHeader.newBuilder();

        headerBuilder.setTimestamp(this.timestamp);
        headerBuilder.setNumber(this.number);
        if (null == this.parentHash ||
                null == this.stateRoot ||
                null == this.transactionRoot ||
                null == this.receiptRoot) {
            return null;
        }
        headerBuilder.setParentHash(ByteString.copyFrom(this.parentHash));
        headerBuilder.setStateRoot(ByteString.copyFrom(this.stateRoot));
        headerBuilder.setTransactionRoot(ByteString.copyFrom(this.transactionRoot));
        headerBuilder.setReceiptRoot(ByteString.copyFrom(this.receiptRoot));
        if (null != this.permissionRoot) {
            headerBuilder.setPermissionRoot(ByteString.copyFrom(this.permissionRoot));
        }
        if (null != this.dposRoot) {
            headerBuilder.setDposRoot(ByteString.copyFrom(this.dposRoot));
        }
        if (null != this.transferRoot) {
            headerBuilder.setTransferRoot(ByteString.copyFrom(this.transferRoot));
        }
        if (null != this.votingRoot) {
            headerBuilder.setVotingRoot(ByteString.copyFrom(this.votingRoot));
        }
        if (null != this.bloomLog) {
            headerBuilder.setBloomLog(ByteString.copyFrom(this.bloomLog));
        }
        if (null != this.energonUsed) {
            headerBuilder.setEnergonUsed(ByteString.copyFrom(this.energonUsed.toByteArray()));
        }
        if (null != this.energonCeiling) {
            headerBuilder.setEnergonCeiling(ByteString.copyFrom(this.energonCeiling.toByteArray()));
        }
        if (null != this.difficulty) {
            headerBuilder.setDifficulty(ByteString.copyFrom(this.difficulty.toByteArray()));
        }
        if (null != this.extraData) {
            headerBuilder.setExtraData(ByteString.copyFrom(this.extraData));
        }
        if (null != this.author) {
            headerBuilder.setAuthor(ByteString.copyFrom(this.author));
        }
        if (null != this.coinbase) {
            headerBuilder.setCoinbase(ByteString.copyFrom(this.coinbase));
        }

        return headerBuilder.build().toByteArray();
    }

    private void parse(byte[] bytes) {
        try {
            BlockHeaderProto.BlockHeader blockHeader = BlockHeaderProto.BlockHeader.parseFrom(bytes);

            this.timestamp = blockHeader.getTimestamp();
            this.number = blockHeader.getNumber();
            this.parentHash = blockHeader.getParentHash().toByteArray();
            this.stateRoot = blockHeader.getStateRoot().toByteArray();
            this.permissionRoot = blockHeader.getPermissionRoot().toByteArray();
            this.dposRoot = blockHeader.getDposRoot().toByteArray();
            this.transactionRoot = blockHeader.getTransactionRoot().toByteArray();
            this.transferRoot = blockHeader.getTransferRoot().toByteArray();
            this.votingRoot = blockHeader.getVotingRoot().toByteArray();
            this.receiptRoot = blockHeader.getReceiptRoot().toByteArray();
            this.bloomLog = blockHeader.getBloomLog().toByteArray();
            this.energonUsed = new BigInteger(blockHeader.getEnergonUsed().toByteArray());
            this.energonCeiling = new BigInteger(blockHeader.getEnergonCeiling().toByteArray());
            this.difficulty = new BigInteger(blockHeader.getDifficulty().toByteArray());
            this.extraData = blockHeader.getExtraData().toByteArray();
            this.author = blockHeader.getAuthor().toByteArray();
            this.coinbase = blockHeader.getCoinbase().toByteArray();

            this.hash = HashUtil.sha3(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockHeader)) return false;
        BlockHeader bh = (BlockHeader) other;

        return this.timestamp == bh.getTimestamp() &&
                Arrays.equals(this.author, bh.getAuthor()) &&
                Arrays.equals(this.coinbase, bh.getCoinbase()) &&
                Arrays.equals(this.parentHash, bh.getParentHash()) &&
                Arrays.equals(this.stateRoot, bh.getStateRoot()) &&
                Arrays.equals(this.permissionRoot, bh.getPermissionRoot()) &&
                Arrays.equals(this.dposRoot, bh.getDposRoot()) &&
                Arrays.equals(this.transactionRoot, bh.getTransactionRoot()) &&
                Arrays.equals(this.transferRoot, bh.getTransferRoot()) &&
                Arrays.equals(this.votingRoot, bh.getVotingRoot()) &&
                Arrays.equals(this.receiptRoot, bh.getReceiptRoot()) &&
                Arrays.equals(this.bloomLog, bh.getBloomLog()) &&
                Arrays.equals(this.extraData, bh.getExtraData()) &&
                Arrays.equals(this.hash, bh.getHash()) &&
                this.number == bh.getNumber() &&
                this.energonUsed.equals(bh.getEnergonUsed()) &&
                this.energonCeiling.equals(bh.getEnergonCeiling()) &&
                this.difficulty.equals(bh.getDifficulty());
    }

}
