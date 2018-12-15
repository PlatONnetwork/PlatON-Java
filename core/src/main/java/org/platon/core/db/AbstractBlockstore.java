package org.platon.core.db;

import org.platon.core.block.Block;

public abstract class AbstractBlockstore implements BlockStoreIfc {

    @Override
    public byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash) {
        Block branchBlock = getBlockByHash(branchBlockHash);
        if (branchBlock.getBlockHeader().getNumber() < blockNumber) {
            throw new IllegalArgumentException("Requested block number > branch hash number: " + blockNumber + " < " + branchBlock.getBlockHeader().getNumber());
        }
        while(branchBlock.getBlockHeader().getNumber() > blockNumber) {
            branchBlock = getBlockByHash(branchBlock.getBlockHeader().getParentHash());
        }
        return branchBlock.getBlockHeader().getHash();
    }
}
