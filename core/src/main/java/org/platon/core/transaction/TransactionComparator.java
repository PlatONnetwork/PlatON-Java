package org.platon.core.transaction;

/**
 * Created by alliswell on 2018/7/24.
 */
public class TransactionComparator implements java.util.Comparator<Transaction> {
    @Override
    public int compare(Transaction t1, Transaction t2) {
        return t1.getEnergonPrice().subtract(t2.getEnergonPrice()).intValue();
    }
}
