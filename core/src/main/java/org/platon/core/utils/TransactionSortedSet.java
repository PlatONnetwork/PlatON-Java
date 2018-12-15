package org.platon.core.utils;

import org.platon.common.utils.ByteComparator;
import org.platon.core.transaction.Transaction;

import java.util.TreeSet;

public class TransactionSortedSet extends TreeSet<Transaction> {
    public TransactionSortedSet() {
        super((tx1, tx2) -> {
            long refBlockNumDiff = tx1.getReferenceBlockNum() - tx2.getReferenceBlockNum();
            if (refBlockNumDiff != 0) {
                return refBlockNumDiff > 0 ? 1 : -1;
            }
            return ByteComparator.compareTo(tx1.getHash(), 0, 32, tx2.getHash(), 0, 32);
        });
    }
}