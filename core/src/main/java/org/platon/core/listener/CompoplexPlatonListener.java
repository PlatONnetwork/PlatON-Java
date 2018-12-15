package org.platon.core.listener;

import org.platon.core.block.Block;
import org.platon.core.BlockSummary;
import org.platon.core.EventDispatchWorker;
import org.platon.core.PendingStateIfc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component(value = "platonListener")
public class CompoplexPlatonListener implements PlatonListener {

    @Autowired
    private EventDispatchWorker eventDispatchThread = EventDispatchWorker.getDefault();
    
    private List<PlatonListener> listeners = new CopyOnWriteArrayList<>();

    
    public void addListener(PlatonListener listener) {
        listeners.add(listener);
    }

    
    public void removeListener(PlatonListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void trace(final String output) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "trace") {
                @Override
                public void run() {
                    listener.trace(output);
                }
            });
        }
    }

    @Override
    public void onBlock(final BlockSummary blockSummary) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onBlock") {
                @Override
                public void run() {
                    listener.onBlock(blockSummary);
                }
            });
        }
    }

    @Override
    public void onPeerDisconnect(final String host, final long port) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onPeerDisconnect") {
                @Override
                public void run() {
                    listener.onPeerDisconnect(host, port);
                }
            });
        }
    }

    @Override
    public void onPendingTransactionsReceived(final List<Transaction> transactions) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onPendingTransactionsReceived") {
                @Override
                public void run() {
                    listener.onPendingTransactionsReceived(transactions);
                }
            });
        }
    }

    @Override
    public void onPendingStateChanged(final PendingStateIfc pendingState) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onPendingStateChanged") {
                @Override
                public void run() {
                    listener.onPendingStateChanged(pendingState);
                }
            });
        }
    }

    @Override
    public void onNoConnections() {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onNoConnections") {
                @Override
                public void run() {
                    listener.onNoConnections();
                }
            });
        }
    }

    @Override
    public void onVMTraceCreated(final String transactionHash, final String trace) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onVMTraceCreated") {
                @Override
                public void run() {
                    listener.onVMTraceCreated(transactionHash, trace);
                }
            });
        }
    }

    @Override
    public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state,
                                           final Block block) {
        for (final PlatonListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableWrapper(listener, "onPendingTransactionUpdate") {
                @Override
                public void run() {
                    listener.onPendingTransactionUpdate(txReceipt, state, block);
                }
            });
        }
    }
}
