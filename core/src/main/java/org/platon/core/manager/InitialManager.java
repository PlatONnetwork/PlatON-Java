package org.platon.core.manager;

import org.platon.common.AppenderName;
import org.platon.common.utils.Numeric;
import org.platon.core.*;
import org.platon.core.block.Block;
import org.platon.core.block.GenesisBlock;
import org.platon.core.config.CoreConfig;
import org.platon.core.config.SystemConfig;
import org.platon.core.db.BlockStoreIfc;
import org.platon.core.db.DbFlushManager;
import org.platon.core.listener.CompoplexPlatonListener;
import org.platon.core.listener.PlatonListener;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.storage.trie.TrieImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author - Jungle
 * @date 2018/9/6 11:02
 * @version 0.0.1
 */
@Component
public class InitialManager {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    @Autowired
    private PendingStateIfc pendingState;

    @Autowired
    private EventDispatchWorker eventDispatchThread;

    @Autowired
    private DbFlushManager dbFlushManager;

    @Autowired
    private ApplicationContext ctx;

    private CoreConfig config;

    private PlatonListener listener;

    private Blockchain blockchain;

    private Repository repository;

    private BlockStoreIfc blockStore;

    private SystemConfig systemConfig;

    @Autowired
    public InitialManager(final CoreConfig config, final Repository repository,
                          final PlatonListener listener, final Blockchain blockchain,
                          final BlockStoreIfc blockStore, final SystemConfig systemConfig) {
        this.listener = listener;
        this.blockchain = blockchain;
        this.repository = repository;
        this.blockStore = blockStore;
        this.config = config;
        this.systemConfig = systemConfig;
        loadBlockchain();
    }

    public void addListener(PlatonListener listener) {
        logger.info("~> Platon listener added success.");
        ((CompoplexPlatonListener) this.listener).addListener(listener);
    }

    public PlatonListener getListener() {
        return listener;
    }

    public Repository getRepository() {
        return (Repository) repository;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public BlockStoreIfc getBlockStore() {
        return blockStore;
    }

    public PendingStateIfc getPendingState() {
        return pendingState;
    }

    public void loadBlockchain() {

        if (blockStore.getBestBlock() == null) {

            logger.info("~> DB is empty - adding Genesis");
            GenesisBlock genesis = systemConfig.getGenesisBlock(repository);
            repository.commit();

            blockStore.saveBlock(genesis, genesis.getBlockHeader().getDifficulty(), true);
            blockchain.setBestBlock(genesis);
            blockchain.setTotalDifficulty(genesis.getBlockHeader().getDifficulty());

            listener.onBlock(new BlockSummary(genesis, new HashMap<byte[], BigInteger>(),
                    new ArrayList<TransactionReceipt>(), new ArrayList<TransactionExecutionSummary>()));

            logger.info("~> Load Genesis block complete...");

        } else {
            Block bestBlock = blockStore.getBestBlock();

            blockchain.setBestBlock(bestBlock);
            BigInteger totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlock.getBlockHeader().getHash());
            blockchain.setTotalDifficulty(totalDifficulty);

            logger.info("~> Loaded up to block [{}] totalDifficulty [{}] with stateRoot [{}]",
                    blockchain.getBestBlock().getBlockHeader().getNumber(),
                    blockchain.getTotalDifficulty().toString(),
                    Numeric.toHexString(blockchain.getBestBlock().getBlockHeader().getStateRoot()));
        }
        if (!Arrays.equals(blockchain.getBestBlock().getBlockHeader().getStateRoot(), TrieImpl.EMPTY_TRIE_HASH)) {
            this.repository.syncToRoot(blockchain.getBestBlock().getBlockHeader().getStateRoot());
        }
    }



    public void close() {
        logger.info("~> close: shutting down event dispatch thread used by EventBus ...");
        eventDispatchThread.shutdown();
        logger.info("~> close: closing Blockchain instance ...");
        blockchain.close();
        logger.info("~> close: closing main repository ...");
        repository.close();
        logger.info("~> close: database flush manager ...");
        dbFlushManager.close();
    }

}
