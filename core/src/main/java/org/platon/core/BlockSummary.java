package org.platon.core;

import org.platon.core.block.Block;
import org.platon.core.transaction.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class BlockSummary {

    private final Block block;
    private final Map<byte[], BigInteger> rewards;
    private final List<TransactionReceipt> receipts;
    private final List<TransactionExecutionSummary> txExecutionSummaries;
    private BigInteger totalDifficulty = BigInteger.ZERO;

    public BlockSummary(Block block, Map<byte[], BigInteger> bigIntegerHashMap,
                        List<TransactionReceipt> transactionReceipts,
                        List<TransactionExecutionSummary> txExecutionSummaries) {

        this.receipts = transactionReceipts;
        this.rewards = bigIntegerHashMap;
        this.block = block;
        this.txExecutionSummaries = txExecutionSummaries;
    }

    public List<TransactionReceipt> getReceipts() {
        return receipts;
    }

    public List<TransactionExecutionSummary> getSummaries() {
        return txExecutionSummaries;
    }


    public Map<byte[], BigInteger> getRewards() {
        return rewards;
    }

    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    public boolean betterThan(BigInteger oldTotDifficulty) {
        return getTotalDifficulty().compareTo(oldTotDifficulty) > 0;
    }
}
