package org.platon.core;

import org.platon.core.block.Block;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;

import java.util.List;

public interface PendingStateIfc {

    /**
     * Adds transactions received from the net to the list of wire transactions <br>
     * Triggers an update of pending state
     *
     * @param transactions txs received from the net
     * @return sublist of transactions with NEW_PENDING status
     */
    List<Transaction> addPendingTransactions(List<Transaction> transactions);

    /**
     * Adds transaction to the list of pending state txs  <br>
     * For the moment this list is populated with txs sent by our peer only <br>
     * Triggers an update of pending state
     */
    void addPendingTransaction(Transaction tx);

    /**
     * It should be called on each block imported as <b>BEST</b> <br>
     * Does several things:
     * <ul>
     *     <li>removes block's txs from pending state and wire lists</li>
     *     <li>removes outdated wire txs</li>
     *     <li>updates pending state</li>
     * </ul>
     *
     * @param block block imported into blockchain as a <b>BEST</b> one
     */
    void processBest(Block block, List<TransactionReceipt> receipts);

    /**
     * @return pending state repository
     */
    Repository getRepository();

    /**
     * @return list of pending transactions
     */
    List<Transaction> getPendingTransactions();
}
