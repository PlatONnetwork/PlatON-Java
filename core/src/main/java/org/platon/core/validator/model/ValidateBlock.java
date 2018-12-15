package org.platon.core.validator.model;

import org.platon.common.utils.ByteArrayWrapper;

import java.util.HashSet;

/**
 * ValidateBlock
 *
 * @author yanze
 * @desc block model for validate
 * @create 2018-08-08 11:09
 **/
public class ValidateBlock {

    private byte[] blockHash;
    private long ancestorBlockNum;
    private byte[] ancestorBlockHash;
    private HashSet<ByteArrayWrapper> txHashSet;

    public ValidateBlock(byte[] blockHash, long ancestorBlockNum, byte[] ancestorBlockHash, HashSet<ByteArrayWrapper> txHashSet) {
        this.blockHash = blockHash;
        this.ancestorBlockNum = ancestorBlockNum;
        this.ancestorBlockHash = ancestorBlockHash;
        this.txHashSet = txHashSet;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(byte[] blockHash) {
        this.blockHash = blockHash;
    }

    public long getAncestorBlockNum() {
        return ancestorBlockNum;
    }

    public void setAncestorBlockNum(long ancestorBlockNum) {
        this.ancestorBlockNum = ancestorBlockNum;
    }

    public byte[] getAncestorBlockHash() {
        return ancestorBlockHash;
    }

    public void setAncestorBlockHash(byte[] ancestorBlockHash) {
        this.ancestorBlockHash = ancestorBlockHash;
    }

    public HashSet<ByteArrayWrapper> getTxHashSet() {
        return txHashSet;
    }

    public void setTxHashSet(HashSet<ByteArrayWrapper> txHashSet) {
        this.txHashSet = txHashSet;
    }

    public ValidateBlock clone(){
        return new ValidateBlock(this.blockHash,this.ancestorBlockNum,this.ancestorBlockHash,this.txHashSet);
    }
}
