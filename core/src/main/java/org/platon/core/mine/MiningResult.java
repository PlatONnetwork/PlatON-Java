package org.platon.core.mine;

import org.platon.core.block.Block;

public class MiningResult {

    public final long nonce;

    public final byte[] digest;

    public final Block block;

    public MiningResult(long nonce, byte[] digest, Block block) {
        this.nonce = nonce;
        this.digest = digest;
        this.block = block;
    }

    public long getNonce() {
        return nonce;
    }

    public byte[] getDigest() {
        return digest;
    }

    public Block getBlock() {
        return block;
    }
}