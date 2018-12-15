package org.platon.core.mine;

import org.platon.core.block.Block;


public interface MinerListener {

    void miningStarted();

    void miningStopped();

    void blockMiningStarted(Block block);

    void blockMined(Block block);

    void blockMiningCanceled(Block block);
}
