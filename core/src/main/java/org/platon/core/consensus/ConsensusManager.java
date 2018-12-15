package org.platon.core.consensus;

import com.google.common.util.concurrent.ListenableFuture;
import org.platon.common.AppenderName;
import org.platon.core.block.Block;
import org.platon.core.block.BlockHeader;
import org.platon.core.config.CoreConfig;
import org.platon.core.mine.MinerAlgorithmIfc;
import org.platon.core.mine.MinerListener;
import org.platon.core.mine.MiningResult;
import org.platon.core.mine.NoMinerAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ConsensusManager {

    private final static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_CONSENSUS);

    private final CoreConfig config;

    private MinerAlgorithmIfc minerAlgorithm;

    private Thread consensusThread;

    @Autowired
    public ConsensusManager(CoreConfig config){
        this.config = config;
        this.minerAlgorithm = getMinerAlgorithm(config);
        Runnable consensusTask = this::processConsensus;
        consensusThread = new Thread(consensusTask);
        consensusThread.start();
    }

    private void processConsensus(){
        if (logger.isDebugEnabled()) {
            logger.debug("~> process consensus thread start.");
        }
        while(!Thread.currentThread().isInterrupted()){

            try {

            }catch (Exception e){
                if(e instanceof InterruptedException){
                    break;
                }
                logger.error("~> Error process consensus.", e);
            }
        }
    }

    public ListenableFuture<MiningResult> mine(Block block) {
        return this.minerAlgorithm.mine(block);
    }

    public boolean validate(BlockHeader blockHeader) {
        return this.minerAlgorithm.validate(blockHeader);
    }

    public void setListeners(Collection<MinerListener> listeners) {
        this.minerAlgorithm.setListeners(listeners);
    }

    public boolean shouldSeal() {
        return this.minerAlgorithm.shouldSeal();
    }

    public byte[] headHash() {
        return this.minerAlgorithm.headHash();
    }

    public byte[] currentIrbHash() {
        return this.minerAlgorithm.currentIrbHash();
    }

    public byte[] lastIrbHash() {
        return this.minerAlgorithm.lastIrbHash();
    }

    public void generateSeal(BlockHeader header) {
        this.minerAlgorithm.generateSeal(header);
    }

    private MinerAlgorithmIfc getMinerAlgorithm(CoreConfig config){
        return new NoMinerAlgorithm();
    }
}
