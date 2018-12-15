package org.platon.core.mine;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.platon.core.block.BlockHeader;
import org.platon.core.block.Block;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NoMinerAlgorithm implements MinerAlgorithmIfc {

    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(8, 8, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                    new ThreadFactoryBuilder().setNameFormat("hash-pool-%d").build()));

    private int cpuThreads;
    private boolean fullMining = true;

    public NoMinerAlgorithm() {
        cpuThreads = 4;
        fullMining = true;
    }

    @Override
    public ListenableFuture<MiningResult> mine(Block block) {

        return new MineTask(block, 1, new Callable<MiningResult>() {
            @Override
            public MiningResult call() throws Exception {
                return new MiningResult(0, new byte[]{0}, block);
            }
        }, executor).submit();
    }

    @Override
    public boolean validate(BlockHeader blockHeader) {
        return blockHeader != null;
    }

    @Override
    public void setListeners(Collection<MinerListener> listeners) {

    }

    @Override
    public boolean shouldSeal() {
        return true;
    }

    @Override
    public byte[] headHash() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    @Override
    public byte[] currentIrbHash() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    @Override
    public byte[] lastIrbHash() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    @Override
    public void generateSeal(BlockHeader header) {
        throw new UnsupportedOperationException("Not Supported.");
    }
}
