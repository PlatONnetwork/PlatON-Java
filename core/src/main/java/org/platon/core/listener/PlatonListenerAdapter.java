package org.platon.core.listener;

import org.platon.core.block.Block;
import org.platon.core.BlockSummary;
import org.platon.core.PendingStateIfc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;

import java.util.List;

public class PlatonListenerAdapter implements PlatonListener {

    @Override
    public void trace(String output) {

    }

    @Override
    public void onBlock(BlockSummary blockSummary) {

    }

    @Override
    public void onPeerDisconnect(String host, long port) {

    }

    @Override
    public void onPendingTransactionsReceived(List<Transaction> transactions) {

    }

    @Override
    public void onPendingStateChanged(PendingStateIfc pendingState) {

    }

    @Override
    public void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {

    }

    @Override
    public void onNoConnections() {

    }

    @Override
    public void onVMTraceCreated(String transactionHash, String trace) {

    }
}
