package org.platon.core.mine;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections4.CollectionUtils;
import org.platon.common.AppenderName;
import org.platon.core.Blockchain;
import org.platon.core.ImportResult;
import org.platon.core.PendingStateIfc;
import org.platon.core.PendingStateImpl;
import org.platon.core.block.Block;
import org.platon.core.block.BlockHeader;
import org.platon.core.config.CoreConfig;
import org.platon.core.consensus.ConsensusManager;
import org.platon.core.db.BlockStoreIfc;
import org.platon.core.facade.Platon;
import org.platon.core.facade.PlatonImpl;
import org.platon.core.listener.CompoplexPlatonListener;
import org.platon.core.transaction.Transaction;
import org.platon.core.utils.TransactionSortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

@Component
public class BlockProduceManager {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_MINE);

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private Platon platon;

    private Blockchain blockchain;
    private BlockStoreIfc blockStore;
    private ConsensusManager consensusManager;
    protected PendingStateIfc pendingState;
    private CompoplexPlatonListener listener;
    private CoreConfig config;

    //
    private List<MinerListener> listeners = new CopyOnWriteArrayList<>();

    private BigInteger minEnergonPrice;
    private long minBlockTimeout;

    // for pow alg
    private int cpuThreads;
    private boolean fullMining = true;

    private volatile boolean isLocalMining;
    private Block miningBlock;

    // 挖矿算法
    private volatile MinerAlgorithmIfc externalMiner;
    private Thread minerBlockThread;

    private final Queue<ListenableFuture<MiningResult>> currentMiningTasks = new ConcurrentLinkedQueue<>();

    private long lastBlockMinedTime;
    private int UNCLE_LIST_LIMIT;
    private int UNCLE_GENERATION_LIMIT;

    @Autowired
    public BlockProduceManager(final CoreConfig config, final CompoplexPlatonListener listener,
                               final Blockchain blockchain, final BlockStoreIfc blockStore,
                               final PendingStateIfc pendingState, final ConsensusManager consensusManager) {
        this.listener = listener;
        this.config = config;
        this.blockchain = blockchain;
        this.blockStore = blockStore;
        this.pendingState = pendingState;
        this.consensusManager = consensusManager;
        UNCLE_LIST_LIMIT = 10;
        UNCLE_GENERATION_LIMIT = 10;
        minEnergonPrice = new BigInteger("15000000000");        // 15Gwei

        minBlockTimeout = 0;
        cpuThreads = 4;
        fullMining = true;

        // tips: 启动挖矿主线程
        Runnable mineTask = this::processMineBlock;
        minerBlockThread = new Thread(mineTask, "ProduceBlockThread");
        minerBlockThread.start();
    }

    private void processMineBlock() {

        if (logger.isDebugEnabled()) {
            logger.debug("Produce block thread start...");
        }

        while (!Thread.currentThread().isInterrupted()) {

            try {
                // tips: 提取pending交易，判断是否可以进行挖矿.
                if (!consensusManager.shouldSeal()) {
                    continue;
                }
                if (null == miningBlock) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("~> Restart mining: mining block is null.");
                    }
                } else if (miningBlock.getBlockHeader().getNumber() <= ((PendingStateImpl) pendingState).getBestBlock().getBlockHeader().getNumber()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("~> Restart mining: new best block: " + blockchain.getBestBlock().getShortDescr());
                    }
                } else if (!CollectionUtils.isEqualCollection(miningBlock.getTransactions(), getAllPendingTransactions())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("~> Restart mining: pending transactions changed");
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        String s = "~> onPendingStateChanged() event, but pending Txs the same as in currently mining block: ";
                        for (Transaction tx : getAllPendingTransactions()) {
                            s += "\n    " + tx;
                        }
                        logger.trace(s);
                    }
                }
                startMining();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    break;
                }
                logger.error("~> Error mine block.", e);
            }
        }
    }

    public void setFullMining(boolean fullMining) {
        this.fullMining = fullMining;
    }

    public void setCpuThreads(int cpuThreads) {
        this.cpuThreads = cpuThreads;
    }

    public void setMinEnergonPrice(BigInteger minEnergonPrice) {
        this.minEnergonPrice = minEnergonPrice;
    }

    public void setExternalMiner(MinerAlgorithmIfc miner) {
        externalMiner = miner;
    }

    public void startMining() {
        isLocalMining = true;
        fireMinerStarted(); // fire listener
        logger.info("Miner started...");
        restartMining();
    }

    public void stopMining() {
        isLocalMining = false;
        cancelCurrentBlock();
        fireMinerStopped();
        logger.info("Miner stopped");
    }

    /**
     * @return 返回pendingTxs，从pendingState的池中提取.
     */
    protected List<Transaction> getAllPendingTransactions() {
        TransactionSortedSet ret = new TransactionSortedSet();
        ret.addAll(pendingState.getPendingTransactions());
        Iterator<Transaction> it = ret.iterator();
        while (it.hasNext()) {
            Transaction tx = it.next();
            if (!isAcceptableTx(tx)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Producer excluded the transaction: {}", tx);
                }
                it.remove();
            }
        }
        return new ArrayList<>(ret);
    }

    protected boolean isAcceptableTx(Transaction tx) {
        return minEnergonPrice.compareTo(tx.getEnergonPrice()) <= 0;
    }

    // tips: 取消当前正出块任务
    protected synchronized void cancelCurrentBlock() {
        for (ListenableFuture<MiningResult> task : currentMiningTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
            }
        }
        currentMiningTasks.clear();

        if (miningBlock != null) {
            fireBlockCancelled(miningBlock);
            logger.debug("Tainted block mining cancelled: {}", miningBlock.getShortDescr());
            miningBlock = null;
        }
    }

    protected List<BlockHeader> getUncles(Block mineBest) {
        //todo: 叔区块是否也存在?
        List<BlockHeader> ret = new ArrayList<>();
        return ret;
    }

    /**
     * @return 打包了交易的区块，需要结合共识算法出块.
     */
    protected Block getNewBlockForMining() {
        Block bestBlockChainBlock = blockchain.getBestBlock();
        Block bestPendingStateBlock = ((PendingStateImpl) pendingState).getBestBlock();
        if (null == bestBlockChainBlock) return null;
        if (logger.isDebugEnabled()) {
            logger.debug("~> Get new block for produce, best blocks : PendingState: "
                    + (null == bestPendingStateBlock ? "" : bestPendingStateBlock.getShortDescr() ) +
                    ", BlockChain : " + (null == bestBlockChainBlock ? "" : bestBlockChainBlock.getShortDescr()));
        }

        // tips: 打包交易，生成一个区块.
        Block newMiningBlock = blockchain.createNewBlock(bestPendingStateBlock, getAllPendingTransactions(), getUncles(bestPendingStateBlock));
        return newMiningBlock;
    }

    protected void restartMining() {

        Block newMiningBlock = getNewBlockForMining();
        if (newMiningBlock == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("~> newMiningBlock is null.");
            }
            return;
        }
        synchronized (this) {
            cancelCurrentBlock();
            miningBlock = newMiningBlock;

            // tips: 挖矿动作交由共识主线程去完成，
            if (consensusManager != null && isLocalMining) {
                consensusManager.setListeners(listeners);
                currentMiningTasks.add(consensusManager.mine(cloneBlock(miningBlock)));
            }

            for (final ListenableFuture<MiningResult> task : currentMiningTasks) {
                task.addListener(() -> {
                    try {
                        // 哇, 挖到宝了,出块产生
                        final Block minedBlock = task.get().block;
                        blockMined(minedBlock);
                    } catch (InterruptedException | CancellationException e) {
                        // OK, we've been cancelled, just exit
                    } catch (Exception e) {
                        logger.warn("Exception during mining: ", e);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
        fireBlockStarted(newMiningBlock);
        logger.debug("New block mining started: {}", newMiningBlock.getShortHash());
    }

    /**
     * Block cloning is required before passing block to concurrent miner env.
     * In success result miner will modify this block instance.
     */
    private Block cloneBlock(Block block) {
        return new Block(block.encode());
    }

    protected void blockMined(Block newBlock) throws InterruptedException {
        long t = System.currentTimeMillis();
        if (t - lastBlockMinedTime < minBlockTimeout) {
            long sleepTime = minBlockTimeout - (t - lastBlockMinedTime);
            logger.debug("~> Last block was mined " + (t - lastBlockMinedTime) + " ms ago. Sleeping " +
                    sleepTime + " ms before importing...");
            Thread.sleep(sleepTime);
        }

        fireBlockMined(newBlock);
        logger.info("~> Wow, block mined !!!: {}", newBlock.toString());

        lastBlockMinedTime = t;
        miningBlock = null;
        // cancel all tasks
        cancelCurrentBlock();

        // broadcast the block
        logger.debug("~> Importing newly mined block {} {} ...", newBlock.getShortHash(), newBlock.getBlockHeader().getNumber());
        ImportResult importResult = ((PlatonImpl) platon).addNewMinedBlock(newBlock);
        logger.debug("~> Mined block import result is " + importResult);
    }

    public MinerAlgorithmIfc getConsensusAlgorithm(CoreConfig config) {
        // tips: 依据配置读取不同的共识算法实现
        return new NoMinerAlgorithm();
    }

    public boolean isMining() {
        return isLocalMining || externalMiner != null;
    }

    public void addListener(MinerListener l) {
        listeners.add(l);
    }

    public void removeListener(MinerListener l) {
        listeners.remove(l);
    }

    protected void fireMinerStarted() {
        for (MinerListener l : listeners) {
            l.miningStarted();
        }
    }

    protected void fireMinerStopped() {
        for (MinerListener l : listeners) {
            l.miningStopped();
        }
    }

    protected void fireBlockStarted(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningStarted(b);
        }
    }

    protected void fireBlockCancelled(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningCanceled(b);
        }
    }

    protected void fireBlockMined(Block b) {
        for (MinerListener l : listeners) {
            l.blockMined(b);
        }
    }
}
