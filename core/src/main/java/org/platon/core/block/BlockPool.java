package org.platon.core.block;

import org.platon.common.AppenderName;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.Numeric;
import org.platon.storage.utils.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alliswell on 2018/7/25.
 */
public class BlockPool {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    // lock
    private ReentrantLock rawLock = new ReentrantLock();
    private ReentrantLock validLock = new ReentrantLock();
    private ReentrantLock forkLock = new ReentrantLock();
    private ReentrantLock signLock = new ReentrantLock();

    /**
     * raw blocks to be checked
     */
    private final ArrayBlockingQueue<Block> rawBlocks;

    /**
     * blocks have been checked and confirmed
     */
    private final ArrayBlockingQueue<Block> validBlocks;

    /**
     * blocks have been checked but not confirmed
     */
    private final ArrayBlockingQueue<Block> forkBlocks;

    /**
     * map of hash->signature count
     */
    private final ConcurrentHashMap<ByteArrayWrapper, Set<byte[]>> signCount;

    public BlockPool(int capacity) {
        // init queue
        rawBlocks = new ArrayBlockingQueue<>(capacity);
        validBlocks = new ArrayBlockingQueue<>(capacity);
        forkBlocks = new ArrayBlockingQueue<>(capacity);
        signCount = new ConcurrentHashMap<>(capacity);
    }

    public void injectRawBlock(Block block) {
        try(AutoLock lock = new AutoLock(rawLock)){
            if (rawBlocks.contains(block)) {
                logger.error("the block had already in pool!  hash:" + Numeric.toHexString(block.getBlockHeader().getHash()));
                return;
            }
            rawBlocks.offer(block);
        }
    }

    public Block pollRawBlock() {
        try(AutoLock l = new AutoLock(rawLock)){
            return rawBlocks.poll();
        }
    }

    public void injectValidBlock(Block block) {
        try(AutoLock l = new AutoLock(validLock)){
            if (validBlocks.contains(block)) {
                validBlocks.remove(block);
            }
            validBlocks.offer(block);
        }
    }

    public void injectForkBlock(Block block) {
        try(AutoLock l = new AutoLock(forkLock)){
            if (forkBlocks.contains(block)) {
                forkBlocks.remove(block);
            }
            forkBlocks.offer(block);
        }
    }

    public Block pollValidBlock() {
        try(AutoLock l = new AutoLock(validLock)){
            return validBlocks.poll();
        }
    }

    public Block pollForkBlock() {
        try(AutoLock l = new AutoLock(forkLock)){
            Block b = forkBlocks.poll();
            return b;
        }
    }

    public void addSignatures(byte[] hash, byte[] sig) {
        Set<byte[]> sigsSet = new HashSet<>();
        try(AutoLock l = new AutoLock(signLock)){
            if (signCount.containsKey(hash)) {
                sigsSet = signCount.get(hash);
            }
            sigsSet.add(sig);
            signCount.put(new ByteArrayWrapper(hash), sigsSet);
        }
    }

    public boolean hasRawBlocks() {
        return !rawBlocks.isEmpty();
    }

    public boolean hasValidBlocks() {
        return !validBlocks.isEmpty();
    }

    public int getSignaturesCount(byte[] hash) {
        try(AutoLock l = new AutoLock(signLock)){
            Set<byte[]> signatures = signCount.get(hash);
            return signatures.size();
        }
    }

    public synchronized void clear() {
        try(AutoLock l = new AutoLock(rawLock)) {
            rawBlocks.clear();
        }
        try(AutoLock l = new AutoLock(forkLock)) {
            forkBlocks.clear();
        }
        try(AutoLock l = new AutoLock(validLock)) {
            validBlocks.clear();
        }
        try (AutoLock l = new AutoLock(signLock)){
            signCount.clear();
        }
    }
}
