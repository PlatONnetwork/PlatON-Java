package org.platon.core.consensus;

import org.platon.common.utils.SpringContextUtil;
import org.platon.core.Worker;
import org.platon.core.block.Block;
import org.platon.core.block.BlockHeader;
import org.platon.core.block.BlockPool;
import org.platon.core.config.CommonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alliswell on 2018/7/25.
 */

public abstract class BaseConsensus extends Worker {

    private static final Logger logger = LoggerFactory.getLogger(BaseConsensus.class);
    protected BlockPool blockPool;
    private CommonConfig commonConfig;

    public BaseConsensus(BlockPool blockPool) {
        this.blockPool = blockPool;
    }

    public abstract boolean shouldSeal();

    public abstract byte[] headHash();
    public abstract long headNumber();

    public abstract byte[] currentIrbHash();

    public abstract long currentIrbNumber();

    public abstract byte[] lastIrbHash();

    public abstract long lastIrbNumber();

    public abstract void generateSeal(BlockHeader header);

    /**
     * Determines whether the block should be written to db
     *
     * @param blockHash block's hash
     * @return
     */
    public abstract boolean shouldCommit(byte[] blockHash);

    /**
     * process the raw block
     *
     * @param block
     */
    protected abstract void processRawBlock(Block block);

    /**
     * mining
     */
    protected abstract void mine();

    @Override
    protected void beforeWorking() {
        commonConfig = (CommonConfig) SpringContextUtil.getBean(CommonConfig.class);
        setIdleInMills(20);
    }

    @Override
    public void doWork() {

        if (null == blockPool) {
            logger.error("the blockPool is null!");
            return;
        }

        while (blockPool.hasRawBlocks()) {
            Block block = blockPool.pollRawBlock();
            processRawBlock(block);
        }
        mine();

        try {
            synchronized (blockPool) {
                if (!blockPool.hasRawBlocks()) {
                    logger.debug("there is no raw blocks in queue!");
                    blockPool.wait();
                }
            }

        } catch (InterruptedException ie) {
            ie.printStackTrace();
            logger.error(ie.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
