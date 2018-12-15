package org.platon.core.mine;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.platon.core.block.Block;

import java.util.concurrent.Callable;

public class MineTask extends CompositeFuture<MiningResult> {

    private Block block;
    private int nThreads;
    private Callable<MiningResult> miner;
    private ListeningExecutorService executor;

    public MineTask(Block block, int nThreads, Callable<MiningResult> miner,
                    ListeningExecutorService executor) {
        this.block = block;
        this.nThreads = nThreads;
        this.miner = miner;
        this.executor = executor;
    }

    public MineTask submit() {
        for (int i = 0; i < nThreads; i++) {
            ListenableFuture<MiningResult> f = executor.submit(miner);
            add(f);
        }
        return this;
    }

    @Override
    protected void postProcess(MiningResult result) {


    }
}