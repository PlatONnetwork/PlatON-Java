package org.platon.core.db;

import org.platon.core.block.BlockHeader;
import org.platon.core.block.Block;

import java.math.BigInteger;
import java.util.List;

/**
 * @author - Jungle
 * @date 2018/9/3 14:48
 * @version 0.0.1
 */
public interface BlockStoreIfc {

    byte[] getBlockHashByNumber(long blockNumber);


    byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash);

    Block getChainBlockByNumber(long blockNumber);

    Block getBlockByHash(byte[] hash);

    boolean isBlockExist(byte[] hash);

    List<byte[]> getListHashesEndWith(byte[] hash, long qty);

    List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty);

    List<Block> getListBlocksEndWith(byte[] hash, long qty);

    void saveBlock(Block block, BigInteger totalDifficulty, boolean mainChain);

    BigInteger getTotalDifficultyForHash(byte[] hash);

    BigInteger getTotalDifficulty();

    Block getBestBlock();

    long getMaxNumber();

    void flush();

    void reBranch(Block forkBlock);

    void load();

    void close();
}
