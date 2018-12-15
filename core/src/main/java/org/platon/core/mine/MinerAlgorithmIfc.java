package org.platon.core.mine;

import com.google.common.util.concurrent.ListenableFuture;
import org.platon.core.block.BlockHeader;
import org.platon.core.block.Block;

import java.util.Collection;

/**
 * @author - Jungle
 * @date 2018/9/5 10:55
 * @version 0.0.1
 */
public interface MinerAlgorithmIfc {

    /**
     * Starts mining the block. On successful mining the Block is update with necessary nonce and hash.
     * @return MiningResult Future object. The mining can be canceled via this Future. The Future is complete
     * when the block successfully mined.
     */
    ListenableFuture<MiningResult> mine(Block block);

    /**
     * Validates the Proof of Work for the block
     */
    boolean validate(BlockHeader blockHeader);

    /**
     * Passes {@link MinerListener}'s to miner
     */
    void setListeners(Collection<MinerListener> listeners);

    boolean shouldSeal();

    byte[] headHash();

    byte[] currentIrbHash();

    byte[] lastIrbHash();

    void generateSeal(BlockHeader header);

}
