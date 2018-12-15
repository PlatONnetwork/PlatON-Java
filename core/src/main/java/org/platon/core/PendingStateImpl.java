package org.platon.core;

import org.apache.commons.collections4.map.LRUMap;
import org.platon.common.AppenderName;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.Numeric;
import org.platon.core.config.CommonConfig;
import org.platon.core.config.CoreConfig;
import org.platon.core.listener.PendingTransactionState;
import org.platon.core.listener.PlatonListener;
import org.platon.core.block.Block;
import org.platon.core.datasource.TransactionStore;
import org.platon.core.db.BlockStoreIfc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PendingStateImpl implements PendingStateIfc {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PENDING);

    @Autowired
    private CoreConfig config = CoreConfig.getInstance();

    @Autowired
    CommonConfig commonConfig;

    @Autowired
    private PlatonListener listener;

    @Autowired
    private BlockchainImpl blockchain;

    @Autowired
    private BlockStoreIfc blockStore;

    @Autowired
    private TransactionStore transactionStore;

    private final List<PendingTransaction> pendingTransactions = new ArrayList<>();



    private final Map<ByteArrayWrapper, Object> receivedTxs = new LRUMap<>(100000);
    private final Object dummyObject = new Object();


    private Repository pendingState;

    private Block best = null;

    @Autowired
    public PendingStateImpl(final PlatonListener listener) {
        this.listener = listener;
    }

    public void init() {
        this.pendingState = getOrigRepository().startTracking();
    }

    private Repository getOrigRepository() {
        return blockchain.getRepositorySnapshot();
    }

    @Override
    public synchronized Repository getRepository() {
        if (pendingState == null) {
            init();
        }
        return pendingState;
    }

    @Override
    public synchronized List<Transaction> getPendingTransactions() {
        List<Transaction> txs = pendingTransactions
                .stream()
                .map(PendingTransaction::getTransaction)
                .collect(Collectors.toList());
        return txs;
    }

    public Block getBestBlock() {
        if (best == null) {
            best = blockchain.getBestBlock();
        }
        return best;
    }

    private boolean addNewTxIfNotExist(Transaction tx) {
        return receivedTxs.put(new ByteArrayWrapper(tx.getHash()), dummyObject) == null;
    }

    @Override
    public void addPendingTransaction(Transaction tx) {
        addPendingTransactions(Collections.singletonList(tx));
    }

    @Override
    public synchronized List<Transaction> addPendingTransactions(List<Transaction> transactions) {


        int unknownTx = 0;
        List<Transaction> newPending = new ArrayList<>();
        for (Transaction tx : transactions) {

            if (!addNewTxIfNotExist(tx)) {
                continue;
            }
            if (!addPendingTransactionImpl(tx)) {
                continue;
            }
            newPending.add(tx);
            unknownTx++;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("~> Pending transaction list added: total: {}, new: {}, valid (added to pending): {} (current #of known txs: {})",
                    transactions.size(), unknownTx, newPending, receivedTxs.size());
        }
        if (!newPending.isEmpty()) {
            listener.onPendingTransactionsReceived(newPending);

            listener.onPendingStateChanged(PendingStateImpl.this);
        }
        return newPending;
    }

    private void fireTxUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("PendingTransactionUpdate: (Tot: %3s) %12s : %s %8s    %s [%s]",
                    getPendingTransactions().size(),
                    state, Numeric.toHexString(txReceipt.getTransaction().getSender()).substring(0, 8),
                    txReceipt.getTransaction().getReferenceBlockNum(),
                    block.getShortDescr(), txReceipt.getError()));
        }
        listener.onPendingTransactionUpdate(txReceipt, state, block);
    }


    private boolean addPendingTransactionImpl(final Transaction tx) {


        TransactionReceipt newReceipt = new TransactionReceipt();
        newReceipt.setTransaction(tx);

        String err = validate(tx);

        TransactionReceipt txReceipt;
        if (err != null) {
            txReceipt = createDroppedReceipt(tx, err);
            fireTxUpdate(txReceipt, PendingTransactionState.DROPPED, getBestBlock());
        } else {
            pendingTransactions.add(new PendingTransaction(tx, getBestBlock().getBlockHeader().getNumber()));
            fireTxUpdate(newReceipt, PendingTransactionState.NEW_PENDING, getBestBlock());
        }
        return err != null;
    }

    private TransactionReceipt createDroppedReceipt(Transaction tx, String error) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setTransaction(tx);
        txReceipt.setError(error);
        return txReceipt;
    }


    private String validate(Transaction tx) {
        try {
            tx.verify();
        } catch (Exception e) {
            return String.format("Invalid transaction: %s", e.getMessage());
        }
        if (config.getMineMinEnergonPrice().compareTo(tx.getEnergonPrice()) > 0) {
            return "Too low gas price for transaction: " + tx.getEnergonPrice();
        }
        return null;
    }


    private Block findCommonAncestor(Block b1, Block b2) {
        while (!b1.isEqual(b2)) {

            if (b1.getBlockHeader().getNumber() >= b2.getBlockHeader().getNumber()) {
                b1 = blockchain.getBlockByHash(b1.getBlockHeader().getParentHash());
            }

            if (b1.getBlockHeader().getNumber() < b2.getBlockHeader().getNumber()) {
                b2 = blockchain.getBlockByHash(b2.getBlockHeader().getParentHash());
            }
            if (b1 == null || b2 == null) {
                throw new RuntimeException("Pending state can't find common ancestor: one of blocks has a gap");
            }
        }
        return b1;
    }

    @Override
    public synchronized void processBest(Block newBlock, List<TransactionReceipt> receipts) {



        if (getBestBlock() != null && !getBestBlock().isParentOf(newBlock)) {





            Block commonAncestor = findCommonAncestor(getBestBlock(), newBlock);

            if (logger.isDebugEnabled()) {
                logger.debug("New best block from another fork: "
                        + newBlock.getShortDescr() + ", old best: " + getBestBlock().getShortDescr()
                        + ", ancestor: " + commonAncestor.getShortDescr());
            }

            Block rollback = getBestBlock();
            while (!rollback.isEqual(commonAncestor)) {
                List<PendingTransaction> blockTxs = new ArrayList<>();
                for (Transaction tx : rollback.getTransactions()) {
                    logger.trace("Returning transaction back to pending: " + tx);
                    blockTxs.add(new PendingTransaction(tx, commonAncestor.getBlockHeader().getNumber()));
                }

                pendingTransactions.addAll(0, blockTxs);


                rollback = blockchain.getBlockByHash(rollback.getBlockHeader().getParentHash());
            }


            pendingState = getOrigRepository().getSnapshotTo(commonAncestor.getBlockHeader().getStateRoot()).startTracking();


            Block main = newBlock;
            List<Block> mainFork = new ArrayList<>();
            while (!main.isEqual(commonAncestor)) {
                mainFork.add(main);
                main = blockchain.getBlockByHash(main.getBlockHeader().getParentHash());
            }


            for (int i = mainFork.size() - 1; i >= 0; i--) {
                processBestInternal(mainFork.get(i), null);
            }
        } else {
            logger.debug("PendingStateImpl.processBest: " + newBlock.getShortDescr());
            processBestInternal(newBlock, receipts);
        }

        best = newBlock;

        pendingState = getOrigRepository().startTracking();

        listener.onPendingStateChanged(PendingStateImpl.this);
    }

    private void processBestInternal(Block block, List<TransactionReceipt> receipts) {

        clearPending(block, receipts);

        clearOutdated(block.getBlockHeader().getNumber());
    }

    private void clearOutdated(final long blockNumber) {



        List<PendingTransaction> outdated = new ArrayList<>();
        long txOutdatedThreshold = 128;
        for (PendingTransaction tx : pendingTransactions) {
            if (blockNumber - tx.getBlockNumber() > txOutdatedThreshold) {
                outdated.add(tx);

                fireTxUpdate(createDroppedReceipt(tx.getTransaction(),
                        "Tx was not included into last " + txOutdatedThreshold + " blocks"),
                        PendingTransactionState.DROPPED, getBestBlock());
            }
        }

        if (outdated.isEmpty()) {
            return;
        }

        if (logger.isDebugEnabled()) {
            for (PendingTransaction tx : outdated) {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "Clear outdated pending transaction, block.number: [{}] hash: [{}]",
                            tx.getBlockNumber(),
                            Numeric.toHexString(tx.getHash())
                    );
                }
            }
        }
        pendingTransactions.removeAll(outdated);
    }

    private void clearPending(Block block, List<TransactionReceipt> receipts) {

        for (int i = 0; i < block.getTransactions().size(); i++) {
            Transaction tx = block.getTransactions().get(i);
            PendingTransaction pend = new PendingTransaction(tx);

            if (pendingTransactions.remove(pend)) {
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Clear pending transaction, hash: [{}]", Numeric.toHexString(tx.getHash()));
                    }
                    TransactionReceipt receipt;
                    if (receipts != null) {
                        receipt = receipts.get(i);
                    } else {
                        TransactionInfo info = getTransactionInfo(tx.getHash(), block.getBlockHeader().getHash());
                        receipt = info.getReceipt();
                    }
                    fireTxUpdate(receipt, PendingTransactionState.INCLUDED, block);
                } catch (Exception e) {
                    logger.error("Exception creating onPendingTransactionUpdate (block: " + block.getShortDescr() + ", tx: " + i, e);
                }
            }
        }
    }

    private TransactionInfo getTransactionInfo(byte[] txHash, byte[] blockHash) {
        TransactionInfo info = transactionStore.get(txHash, blockHash);
        Transaction tx = blockchain.getBlockByHash(info.getBlockHash()).getTransactions().get(info.getIndex());
        info.getReceipt().setTransaction(tx);
        return info;
    }


    private Block createFakePendingBlock() {
        // creating fake lightweight calculated block with no hashes calculations
        Block block = new Block(
                best.getBlockHeader().getTimestamp() + 1,   // timestamp
                new byte[32],   // author
                new byte[32],                       // coinbase
                best.getBlockHeader().getHash(),    // parentHash
                new byte[32],                       // bloomLog
                best.getBlockHeader().getNumber() + 1,  // number
                BigInteger.ZERO,                    // energonUsed
                BigInteger.valueOf(Long.MAX_VALUE), // energonCeiling
                BigInteger.ZERO,    // difficulty
                new byte[0]         // extraData
        );
        block.getBlockHeader().setRoots(
                new byte[32],       // stateRoot
                new byte[32],       // permissionRoot
                new byte[32],       // dposRoot
                new byte[32],       // transactionRoot
                new byte[32],       // transferRoot
                new byte[32],       // votingRoot
                new byte[32]        // receiptRoot
        );
        return block;
    }

    @Autowired
    public void setBlockchain(BlockchainImpl blockchain) {
        this.blockchain = blockchain;
        this.blockStore = blockchain.getBlockStore();
        //this.programInvokeFactory = blockchain.getProgramInvokeFactory();
        this.transactionStore = blockchain.getTransactionStore();
    }
}
