package org.platon.core.facade;

import org.platon.core.TransactionInfo;
import org.platon.core.listener.PlatonListener;
import org.platon.core.Blockchain;
import org.platon.core.transaction.Transaction;

import java.util.concurrent.Future;

/**
 * @author - Jungle
 * @date 2018/9/13 14:14
 * @version 0.0.1
 */
public interface Platon {

    Future<Transaction> submitTransaction(Transaction transaction);

    TransactionInfo getTransactionInfo(byte[] hash);

    Blockchain getBlockchain();

    void addListener(PlatonListener listener);

    void close();
}
