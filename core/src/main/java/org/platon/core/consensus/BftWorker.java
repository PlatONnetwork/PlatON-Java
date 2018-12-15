package org.platon.core.consensus;

import org.platon.common.utils.ByteUtil;
import org.platon.core.block.Block;
import org.platon.core.block.BlockHeader;
import org.platon.core.block.BlockPool;
import org.platon.p2p.router.MessageRouter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Vector;

/**
 * Created by alliswell on 2018/7/25.
 */
public class BftWorker extends BaseConsensus {

    @Autowired
    MessageRouter messageRouter;

    private DposFace dpos;
    private byte[] myAddress;

    public BftWorker(BlockPool blockPool, byte[] minerAddr) {
        super(blockPool);
        dpos = new DposFace();
        myAddress = minerAddr;
    }

    private boolean iAmPrimary() {
        return dpos.isPrimary(myAddress);
    }

    @Override
    protected void beforeWorking() {
        super.beforeWorking();
    }

    @Override
    public boolean shouldSeal() {

        if (!iAmPrimary()) {
            return false;
        }

        long now = System.currentTimeMillis();
        Vector<byte[]> primaryNodes = dpos.getCurrentPrimary();

        primaryNodes.forEach(node -> {

        });
        ByteUtil.contains(dpos.getCurrentPrimary(), myAddress);
        return false;
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

    }

    @Override
    public boolean shouldCommit(byte[] blockHash) {
        int sigsCount = blockPool.getSignaturesCount(blockHash);
        int requiredSigsCount = dpos.getCurrentPrimary().size() * 2 / 3 + 1;
        return sigsCount >= requiredSigsCount;
    }

    @Override
    protected void processRawBlock(Block block) {

    }

    @Override
    protected void mine() {

    }
}
