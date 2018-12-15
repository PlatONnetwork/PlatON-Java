package org.platon.core;

import org.platon.core.block.BlockHeader;
import org.platon.core.block.Block;
import org.platon.core.db.BlockStoreIfc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

public interface Blockchain {

    ImportResult tryToConnect(Block block);

    long getSize();

    BlockSummary add(Block block);

    void storeBlock(Block block, List<TransactionReceipt> receipts);

    Block getBlockByNumber(long blockNumber);

    void setBestBlock(Block block);

    Block getBestBlock();

    boolean hasParentOnTheChain(Block block);

    void close();

    // ==================== for difficulty ================

    void updateTotalDifficulty(Block block);

    BigInteger getTotalDifficulty();

    void setTotalDifficulty(BigInteger totalDifficulty);

    byte[] getBestBlockHash();

    List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty);

    List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty);

    TransactionInfo getTransactionInfo(byte[] hash);

    Block getBlockByHash(byte[] hash);

    void setExitOn(long exitOn);

    byte[] getMinerCoinbase();

    boolean isBlockExist(byte[] hash);

    BlockStoreIfc getBlockStore();

    Block createNewBlock(Block parent, List<Transaction> transactions, List<BlockHeader> uncles);

    Iterator<BlockHeader> getIteratorOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse) ;


}
