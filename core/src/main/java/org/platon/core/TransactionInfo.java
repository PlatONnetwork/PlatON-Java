package org.platon.core;

import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;

public class TransactionInfo {

    TransactionReceipt receipt;
    byte[] blockHash;
    byte[] parentBlockHash;
    int index;

    public TransactionInfo(TransactionReceipt receipt, byte[] blockHash, int index) {
        this.receipt = receipt;
        this.blockHash = blockHash;
        this.index = index;
    }

    public TransactionInfo(TransactionReceipt receipt) {
        this.receipt = receipt;
    }

    public void setTransaction(Transaction tx){
        this.receipt.setTransaction(tx);
    }

    public TransactionReceipt getReceipt(){
        return receipt;
    }

    public byte[] getBlockHash() { return blockHash; }

    public byte[] getParentBlockHash() {
        return parentBlockHash;
    }

    public void setParentBlockHash(byte[] parentBlockHash) {
        this.parentBlockHash = parentBlockHash;
    }

    public int getIndex() { return index; }

    public boolean isPending() {
        return blockHash == null;
    }
}
