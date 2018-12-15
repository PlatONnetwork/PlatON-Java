package org.platon.core.consensus;

import org.platon.core.block.Block;
import org.platon.core.block.BlockHeader;
import org.platon.core.block.BlockPool;

/**
 * Created by alliswell on 2018/7/25.
 */
public class NoProofWorker extends BaseConsensus {

    public NoProofWorker(BlockPool blockPool) {
        super(blockPool);
    }

    @Override
    public boolean shouldSeal() {
        return true;
    }

    @Override
    public byte[] headHash() {
        return new byte[0];
    }

    @Override
    public long headNumber() {
        return 0;
    }

    @Override
    public byte[] currentIrbHash() {
        return new byte[0];
    }

    @Override
    public long currentIrbNumber() {
        return 0;
    }

    @Override
    public byte[] lastIrbHash() {
        return new byte[0];
    }

    @Override
    public long lastIrbNumber() {
        return 0;
    }

    @Override
    public void generateSeal(BlockHeader header) {
        System.out.println("controller submit a block, hash=" + header.getHash());
    }

    @Override
    public boolean shouldCommit(byte[] blockHash) {
        return true;
    }

    @Override
    protected void processRawBlock(Block block) {

    }

    @Override
    protected void mine() {

    }
}
