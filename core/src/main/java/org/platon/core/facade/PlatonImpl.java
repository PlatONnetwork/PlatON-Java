package org.platon.core.facade;

import org.platon.common.AppenderName;
import org.platon.core.TransactionInfo;
import org.platon.core.listener.PlatonListener;
import org.platon.core.Blockchain;
import org.platon.core.ImportResult;
import org.platon.core.PendingStateIfc;
import org.platon.core.block.Block;
import org.platon.core.manager.InitialManager;
import org.platon.core.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
public class PlatonImpl implements Platon, SmartLifecycle {

    private Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    @Autowired
    private InitialManager initialManager;

    @Autowired
    PendingStateIfc pendingState;

    @Autowired
    ApplicationContext context;

    
    public ImportResult addNewMinedBlock(Block block) {
        ImportResult importResult = initialManager.getBlockchain().tryToConnect(block);
        if (logger.isDebugEnabled()) {
            logger.debug("~> PlatonImpl.addNewMinedBlock(), importBlock complete.");
        }
        if (importResult == ImportResult.IMPORTED_BEST) {

        }
        return importResult;
    }

    @Override
    public Future<Transaction> submitTransaction(Transaction transaction) {

        pendingState.addPendingTransaction(transaction);
        return CompletableFuture.completedFuture(transaction);
    }

    @Override
    public TransactionInfo getTransactionInfo(byte[] hash) {
        return initialManager.getBlockchain().getTransactionInfo(hash);
    }

    @Override
    public Blockchain getBlockchain() {
        return initialManager.getBlockchain();
    }

    @Override
    public void addListener(PlatonListener listener) {
        initialManager.addListener(listener);
    }

    @Override
    public void close() {
        logger.info("Close application context.");
        ((AbstractApplicationContext) getApplicationContext()).close();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        logger.info("Shutting down Platon instance...");
        callback.run();
    }

    @Override
    public void start() {
        logger.info("Call start of PlatonImpl instance.");
    }

    @Override
    public void stop() {
        logger.info("Call stop of PlatonImpl instance.");
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

}
