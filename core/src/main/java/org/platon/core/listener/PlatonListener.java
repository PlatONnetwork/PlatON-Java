package org.platon.core.listener;

import org.platon.core.block.Block;
import org.platon.core.BlockSummary;
import org.platon.core.PendingStateIfc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;

import java.util.List;

public interface PlatonListener {

    void trace(String output);

    void onBlock(BlockSummary blockSummary);

    void onPeerDisconnect(String host, long port);

    void onPendingTransactionsReceived(List<Transaction> transactions);

    /**
     * PendingState changes on either new pending transaction or new best block receive
     * When a new transaction arrives it is executed on top of the current pending state
     * When a new best block arrives the PendingState is adjusted to the new Repository state
     * and all transactions which remain pending are executed on top of the new PendingState
     */
    void onPendingStateChanged(PendingStateIfc pendingState);

    /**
     * Is called when PendingTransaction arrives, executed or dropped and included to a block
     *
     * @param txReceipt Receipt of the tx execution on the current PendingState
     * @param state Current state of pending tx
     * @param block The block which the current pending state is based on (for PENDING tx state)
     *              or the block which tx was included to (for INCLUDED state)
     */
    void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block);

    //void onSyncDone(SyncState state);

    void onNoConnections();

    void onVMTraceCreated(String transactionHash, String trace);

    //todo: 待确定
    //void onTransactionExecuted(TransactionExecutionSummary summary);

}
