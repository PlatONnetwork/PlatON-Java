package org.platon.core.transaction;

import org.bouncycastle.util.encoders.Hex;
import org.platon.common.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * transaction poo class
 * Created by alliswell on 2018/7/23.
 */

public class TransactionPool {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPool.class);
    private static volatile TransactionPool instance;
    /* priority queue of the valid transaction */
    private PriorityBlockingQueue<Transaction> queue;
    /* priority queue of the valid transaction */
    private HashSet<byte[]> known;

    private TransactionPool(int initialCapacity) {
        queue = new PriorityBlockingQueue<Transaction>(initialCapacity, new TransactionComparator());
        known = new HashSet<>(initialCapacity);
    }

    public static TransactionPool getInstance() {
        if (instance == null) {
            synchronized (TransactionPool.class) {
                if (instance == null) {
                    instance = new TransactionPool(2048);
                }
            }
        }
        return instance;
    }

    /**
     * inject a transaction to the Q
     *
     * @param tx: Transaction to be injected
     * @return 0:OK, other: failed
     */
    public synchronized int inject(Transaction tx) {
        int ret = checkTx(tx);
        if (0 == ret) {
            queue.put(tx);
            known.add(tx.getHash());
        }
        return ret;
    }

    /**
     * checking if we have known the hash
     *
     * @param hash : hash of the tx
     * @return if we have already received this tx return true
     */
    public boolean isKnown(byte[] hash) {
        return ByteUtil.contains(known, hash);
    }

    /**
     * return the number of tx in queue
     *
     * @return int
     */
    public int pending() {
        return queue.size();
    }

    /**
     * clear the pool
     */
    public void clear() {
        queue.clear();
        known.clear();
    }

    /**
     * drop the tx from the Q
     *
     * @param tx
     */
    public synchronized void drop(Transaction tx) {
        queue.remove(tx);
        known.remove(tx.getHash());
    }

    /**
     * consume the Q
     *
     * @param limit :  max size
     * @param avoid : a set of avoid hash
     * @return ArrayList of Transaction
     */
    public synchronized ArrayList<Transaction> consume(int limit, HashSet<byte[]> avoid) {
        ArrayList<Transaction> ret = new ArrayList<Transaction>();
        int total = 0;
        while (!queue.isEmpty()) {
            Transaction tx = queue.poll();
            if (tx != null && !ByteUtil.contains(avoid, tx.getHash())) {
                ret.add(tx);
                ++total;
            }
            if (total >= limit) {
                break;
            }
        }
        return ret;
    }

    /**
     * check if a Transaction is valid
     *
     * @param tx: Transaction to be checked
     * @return 0:OK, other: failed
     */
    private int checkTx(Transaction tx) {
        if (tx.getEnergonPrice().intValue() > 0) {
            return 0;
        }
        if (queue.contains(tx)) {
            logger.debug("tx[" + Hex.toHexString(tx.getHash()) + "] has already in pool!");
            return 1;
        }

        //TODO: theck the tx's hash in TransactionHashPool
        return -1;
    }
}








