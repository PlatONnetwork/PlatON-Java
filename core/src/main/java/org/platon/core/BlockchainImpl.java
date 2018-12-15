package org.platon.core;

import org.apache.commons.lang3.tuple.Pair;
import org.platon.common.AppenderName;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.ByteComparator;
import org.platon.common.utils.Numeric;
import org.platon.core.block.BlockHeader;
import org.platon.core.config.CommonConfig;
import org.platon.core.config.CoreConfig;
import org.platon.core.listener.PlatonListener;
import org.platon.core.listener.PlatonListenerAdapter;
import org.platon.core.proto.IntMessage;
import org.platon.core.block.Block;
import org.platon.core.datasource.TransactionStore;
import org.platon.core.db.*;
import org.platon.storage.trie.Trie;
import org.platon.storage.trie.TrieImpl;
import org.platon.core.transaction.Bloom;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.crypto.HashUtil;
import org.platon.storage.datasource.inmemory.HashMapDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static java.lang.Runtime.getRuntime;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.emptyList;
import static org.platon.core.Denomination.SZABO;

@Component
public class BlockchainImpl implements Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);
    private static final Logger stateLogger = LoggerFactory.getLogger(AppenderName.APPENDER_STATE);


    private static final long INITIAL_MIN_GAS_PRICE = 10 * SZABO.longValue();
    private static final int MAGIC_REWARD_OFFSET = 8;

    @Autowired
    @Qualifier("defaultRepository")
    private Repository repository;

    @Autowired
    protected BlockStoreIfc blockStore;

    private HeaderStore headerStore = null;

    @Autowired
    private TransactionStore transactionStore;

    @Autowired
    private PendingStateIfc pendingState;

    
    private Block bestBlock;

    
    private BigInteger totalDifficulty = ZERO;

    @Autowired
    private PlatonListener listener;

    @Autowired
    EventDispatchWorker eventDispatchWorker;

    @Autowired
    CommonConfig commonConfig;

    @Autowired
    StateSource stateDataSource;

    @Autowired
    DbFlushManager dbFlushManager;

    CoreConfig config = CoreConfig.getInstance();

    long exitOn = Long.MAX_VALUE;

    public boolean byTest = false;
    private boolean fork = false;

    private byte[] minerCoinbase;
    private byte[] minerExtraData;
    private int UNCLE_LIST_LIMIT;
    private int UNCLE_GENERATION_LIMIT;

    private Stack<State> stateStack = new Stack<>();

    
    public BlockchainImpl() {
    }

    @Autowired
    public BlockchainImpl(final CoreConfig config) {
        this.config = config;
        initConst(config);
    }


    public BlockchainImpl(final BlockStoreIfc blockStore, final Repository repository) {
        this.blockStore = blockStore;
        this.repository = repository;
        this.listener = new PlatonListenerAdapter();

        this.transactionStore = new TransactionStore(new HashMapDB());
        this.eventDispatchWorker = eventDispatchWorker.getDefault();

        initConst(CoreConfig.getInstance());
    }

    public BlockchainImpl withTransactionStore(TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
        return this;
    }

    public BlockchainImpl withPlatonListener(PlatonListener listener) {
        this.listener = listener;
        return this;
    }

    
    private void initConst(CoreConfig config) {
        minerCoinbase = config.getMinerCoinbase();
        minerExtraData = new byte[]{0};
        UNCLE_LIST_LIMIT = 2;
        UNCLE_GENERATION_LIMIT = 7;
    }

    @Override
    public byte[] getBestBlockHash() {
        return getBestBlock().getBlockHeader().getHash();
    }

    @Override
    public long getSize() {
        return bestBlock.getBlockHeader().getNumber() + 1;
    }

    @Override
    public Block getBlockByNumber(long blockNr) {
        return blockStore.getChainBlockByNumber(blockNr);
    }

    @Override
    public TransactionInfo getTransactionInfo(byte[] hash) {

        List<TransactionInfo> infos = transactionStore.get(hash);

        if (infos == null || infos.isEmpty())
            return null;

        TransactionInfo txInfo = null;
        if (infos.size() == 1) {
            txInfo = infos.get(0);
        } else {

            for (TransactionInfo info : infos) {
                Block block = blockStore.getBlockByHash(info.getBlockHash());
                Block mainBlock = blockStore.getChainBlockByNumber(block.getBlockHeader().getNumber());
                if (ByteComparator.equals(info.getBlockHash(), mainBlock.getBlockHeader().getHash())) {
                    txInfo = info;
                    break;
                }
            }
        }
        if (txInfo == null) {
            logger.warn("Can't find block from main chain for transaction " + Numeric.toHexString(hash));
            return null;
        }

        Transaction tx = this.getBlockByHash(txInfo.getBlockHash()).getTransactions().get(txInfo.getIndex());
        txInfo.setTransaction(tx);

        return txInfo;
    }

    @Override
    public Block getBlockByHash(byte[] hash) {
        return blockStore.getBlockByHash(hash);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty) {
        return blockStore.getListHashesEndWith(hash, qty);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty) {
        long bestNumber = bestBlock.getBlockHeader().getNumber();

        if (blockNumber > bestNumber) {
            return emptyList();
        }

        if (blockNumber + qty - 1 > bestNumber) {
            qty = (int) (bestNumber - blockNumber + 1);
        }

        long endNumber = blockNumber + qty - 1;

        Block block = getBlockByNumber(endNumber);

        List<byte[]> hashes = blockStore.getListHashesEndWith(block.getBlockHeader().getHash(), qty);


        Collections.reverse(hashes);

        return hashes;
    }

    
    public static byte[] calcTxTrie(List<Transaction> transactions) {


        Trie txsState = new TrieImpl();

        if (transactions == null || transactions.isEmpty()) {
            return HashUtil.EMPTY_HASH;
        }

        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);


            txsState.put(IntMessage.newBuilder().setData(i).build().toByteArray(), tx.getEncoded());
        }
        return txsState.getRootHash();
    }

    public Repository getRepository() {
        return repository;
    }


    public Repository getRepositorySnapshot() {

        return repository.getSnapshotTo(blockStore.getBestBlock().getBlockHeader().getStateRoot());
    }

    @Override
    public BlockStoreIfc getBlockStore() {
        return blockStore;
    }

    private State pushState(byte[] bestBlockHash) {
        State push = stateStack.push(new State());
        this.bestBlock = blockStore.getBlockByHash(bestBlockHash);
        totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlockHash);
        this.repository = this.repository.getSnapshotTo(this.bestBlock.getBlockHeader().getStateRoot());
        return push;
    }

    private void popState() {
        State state = stateStack.pop();
        this.repository = repository.getSnapshotTo(state.root);
        this.bestBlock = state.savedBest;
        this.totalDifficulty = state.savedTD;
    }

    public void dropState() {
        stateStack.pop();
    }

    private synchronized BlockSummary tryConnectAndFork(final Block block) {

        State savedState = pushState(block.getBlockHeader().getParentHash());
        this.fork = true;

        final BlockSummary summary;
        Repository repo;
        try {


            Block parentBlock = getBlockByHash(block.getBlockHeader().getParentHash());
            repo = repository.getSnapshotTo(parentBlock.getBlockHeader().getStateRoot());
            summary = add(repo, block);
            if (summary == null) {
                return null;
            }
        } catch (Throwable th) {
            logger.error("Unexpected error: ", th);
            return null;
        } finally {
            this.fork = false;
        }

        if (summary.betterThan(savedState.savedTD)) {

            logger.info("Rebranching: {} ~> {}", savedState.savedBest.getShortHash(), block.getShortHash());




            blockStore.reBranch(block);


            this.repository = repo;


            dropState();
        } else {

            popState();
        }

        return summary;
    }

    @Override
    public synchronized ImportResult tryToConnect(final Block block) {



        if (logger.isDebugEnabled()) {
            logger.debug("Try connect block hash: {}, number: {}",
                    Numeric.toHexString(block.getBlockHeader().getHash()).substring(0, 6),
                    block.getBlockHeader().getNumber());
        }


        if (blockStore.getMaxNumber() >= block.getBlockHeader().getNumber()
                && blockStore.isBlockExist(block.getBlockHeader().getHash())) {

            if (logger.isDebugEnabled()) {
                logger.debug("~> Block already exist hash: {}, number: {}",
                        Numeric.toHexString(block.getBlockHeader().getHash()).substring(0, 6),
                        block.getBlockHeader().getNumber());
            }
            return ImportResult.EXIST;
        }

        final ImportResult ret;



        final BlockSummary summary;



        if (bestBlock.isParentOf(block)) {
            recordBlock(block);
            summary = add(repository, block);

            ret = summary == null ? ImportResult.INVALID_BLOCK : ImportResult.IMPORTED_BEST;
        } else {





            if (blockStore.isBlockExist(block.getBlockHeader().getParentHash())) {





                BigInteger oldTotalDiff = getTotalDifficulty();

                recordBlock(block);
                summary = tryConnectAndFork(block);

                ret = summary == null ? ImportResult.INVALID_BLOCK :
                        (summary.betterThan(oldTotalDiff) ? ImportResult.IMPORTED_BEST : ImportResult.IMPORTED_NOT_BEST);
            } else {
                summary = null;
                ret = ImportResult.NO_PARENT;
            }

        }

        if (ret.isSuccessful()) {
            listener.onBlock(summary);
            listener.trace(String.format("Block chain size: [ %d ]", this.getSize()));

            if (ret == ImportResult.IMPORTED_BEST) {
                eventDispatchWorker.invokeLater(() -> pendingState.processBest(block, summary.getReceipts()));
            }
        }

        return ret;
    }

    
    public synchronized Block createNewBlock(Block parent, List<Transaction> txs, List<BlockHeader> uncles) {
        if (parent == null) {
            return null;
        }
        long time = System.currentTimeMillis() / 1000;
        if (parent.getBlockHeader().getTimestamp() >= time) {
            time = parent.getBlockHeader().getTimestamp() + 1;
        }
        return createNewBlock(parent, txs, uncles, time);
    }

    public synchronized Block createNewBlock(Block parent, List<Transaction> txs, List<BlockHeader> uncles, long time) {

        final long blockNumber = parent.getBlockHeader().getNumber() + 1;

        final byte[] extraData = new byte[0];


        Block block = new Block(
                time,
                new byte[32],
                new byte[32],
                parent.getBlockHeader().getHash(),
                new byte[32],
                blockNumber,
                BigInteger.ZERO,
                parent.getBlockHeader().getEnergonCeiling(),
                BigInteger.ZERO,
                extraData
        );
        block.setTransactions(txs);
        block.getBlockHeader().setRoots(
                new byte[]{0},
                new byte[32],
                new byte[32],
                calcTxTrie(txs),
                new byte[32],
                new byte[32],
                new byte[32]
        );

        Repository track = repository.getSnapshotTo(parent.getBlockHeader().getStateRoot());

        BlockSummary summary = applyBlock(track, block);

        List<TransactionReceipt> receipts = summary.getReceipts();
        block.getBlockHeader().setStateRoot(track.getRoot());

        Bloom logBloom = new Bloom();
        for (TransactionReceipt receipt : receipts) {
            logBloom.or(receipt.getBloomFilter());
        }
        block.getBlockHeader().setBloomLog(logBloom.getData());
        block.getBlockHeader().setEnergonUsed(receipts.size() > 0 ? BigInteger.valueOf(receipts.get(receipts.size() - 1).getCumulativeEnergonLong()) : BigInteger.ZERO);
        block.getBlockHeader().setReceiptRoot(calcReceiptsTrie(receipts));

        return block;
    }

    @Override
    public BlockSummary add(Block block) {
        throw new RuntimeException("Not supported");
    }

    public synchronized BlockSummary add(Repository repo, final Block block) {

        BlockSummary summary = addImpl(repo, block);
        if (summary == null) {
            stateLogger.warn("Trying to reimport the block for debug...");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            BlockSummary summary1 = addImpl(repo.getSnapshotTo(getBestBlock().getBlockHeader().getStateRoot()), block);
            stateLogger.warn("Second import trial " + (summary1 == null ? "FAILED" : "OK"));
            if (summary1 != null) {
                if (!byTest) {
                    stateLogger.error("Inconsistent behavior, exiting...");
                    System.exit(-1);
                } else {
                    return summary1;
                }
            }
        }
        return summary;
    }

    public synchronized BlockSummary addImpl(Repository repo, final Block block) {

        if (exitOn < block.getBlockHeader().getNumber()) {
            System.out.print("Exiting after block.number: " + bestBlock.getBlockHeader().getNumber());
            dbFlushManager.flushSync();
            System.exit(-1);
        }
        byte[] origRoot = repo.getRoot();

        if (block == null) {
            return null;
        }

        BlockSummary summary = processBlock(repo, block);
        final List<TransactionReceipt> receipts = summary.getReceipts();


        if (!ByteComparator.equals(block.getBlockHeader().getReceiptRoot(), calcReceiptsTrie(receipts))) {
            logger.warn("Block's given Receipt Hash doesn't match: {} != {}", Numeric.toHexString(block.getBlockHeader().getReceiptRoot()), Numeric.toHexString(calcReceiptsTrie(receipts)));
            logger.warn("Calculated receipts: " + receipts);
            repo.rollback();
            summary = null;
        }

        if (!ByteComparator.equals(block.getBlockHeader().getBloomLog(), calcLogBloom(receipts))) {
            logger.warn("Block's given logBloom Hash doesn't match: {} != {}", Numeric.toHexString(block.getBlockHeader().getBloomLog()), Numeric.toHexString(calcLogBloom(receipts)));
            repo.rollback();
            summary = null;
        }

        if (!ByteComparator.equals(block.getBlockHeader().getStateRoot(), repo.getRoot())) {

            stateLogger.warn("BLOCK: State conflict or received invalid block. block: {} worldstate {} mismatch", block.getBlockHeader().getNumber(), Numeric.toHexString(repo.getRoot()));
            stateLogger.warn("Conflict block dump: {}", Numeric.toHexString(block.encode()));

            repository = repository.getSnapshotTo(origRoot);

            if (!byTest) {

                System.out.println("CONFLICT: BLOCK #" + block.getBlockHeader().getNumber() + ", dump: " + Numeric.toHexString(block.encode()));
                System.exit(1);
            } else {
                summary = null;
            }
        }

        if (summary != null) {
            repo.commit();
            updateTotalDifficulty(block);

            if (!byTest) {
                dbFlushManager.commit(() -> {
                    storeBlock(block, receipts);
                    repository.commit();
                });
            } else {
                storeBlock(block, receipts);
            }
        }

        return summary;
    }

    private boolean needFlushByMemory(double maxMemoryPercents) {
        return getRuntime().freeMemory() < (getRuntime().totalMemory() * (1 - maxMemoryPercents));
    }

    public static byte[] calcReceiptsTrie(List<TransactionReceipt> receipts) {
        Trie receiptsTrie = new TrieImpl();

        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_HASH;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.put(new byte[]{(byte) ((i >> 24) & 0xFF), (byte) ((i >> 16) & 0xFF), (byte) ((i >> 8) & 0xFF), (byte) (i & 0xFF)}, receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.getRootHash();
    }

    private byte[] calcLogBloom(List<TransactionReceipt> receipts) {

        Bloom retBloomFilter = new Bloom();

        if (receipts == null || receipts.isEmpty())
            return retBloomFilter.getData();

        for (TransactionReceipt receipt : receipts) {
            retBloomFilter.or(receipt.getBloomFilter());
        }

        return retBloomFilter.getData();
    }

    public Block getParent(BlockHeader header) {

        return blockStore.getBlockByHash(header.getParentHash());
    }

    
    public static Set<ByteArrayWrapper> getAncestors(BlockStoreIfc blockStore, Block testedBlock, int limitNum, boolean isParentBlock) {
        Set<ByteArrayWrapper> ret = new HashSet<>();
        limitNum = (int) Math.max(0, testedBlock.getBlockHeader().getNumber() - limitNum);
        Block it = testedBlock;
        if (!isParentBlock) {
            it = blockStore.getBlockByHash(it.getBlockHeader().getParentHash());
        }
        while (it != null && it.getBlockHeader().getNumber() >= limitNum) {
            ret.add(new ByteArrayWrapper(it.getBlockHeader().getHash()));
            it = blockStore.getBlockByHash(it.getBlockHeader().getParentHash());
        }
        return ret;
    }

    private BlockSummary processBlock(Repository track, Block block) {

        if (!block.isGenesis() && !config.blockChainOnly()) {
            return applyBlock(track, block);
        } else {
            return new BlockSummary(block, new HashMap<byte[], BigInteger>(), new ArrayList<TransactionReceipt>(), new ArrayList<TransactionExecutionSummary>());
        }
    }

    
    private BlockSummary applyBlock(Repository track, Block block) {

        if (logger.isDebugEnabled()) {
            logger.debug("applyBlock: block: [{}] tx.list: [{}]", block.getBlockHeader().getNumber(), block.getTransactions().size());
        }
        long saveTime = System.nanoTime();
        int i = 1;
        long totalEnergonUsed = 0;
        List<TransactionReceipt> receipts = new ArrayList<>();
        List<TransactionExecutionSummary> summaries = new ArrayList<>();

        for (Transaction tx : block.getTransactions()) {
            stateLogger.debug("apply block: [{}] tx: [{}] ", block.getBlockHeader().getNumber(), i);

            Repository txTrack = track.startTracking();
            

            stateLogger.info("block: [{}] executed tx: [{}] \n  state: [{}]", block.getBlockHeader().getNumber(), i,
                    Numeric.toHexString(track.getRoot()));

            
        }

        Map<byte[], BigInteger> rewards = new HashMap<>();

        stateLogger.info("applied reward for block: [{}]  \n  state: [{}]",
                block.getBlockHeader().getNumber(),
                Numeric.toHexString(track.getRoot()));

        long totalTime = System.nanoTime() - saveTime;
        if (logger.isDebugEnabled()) {
            logger.debug("block: num: [{}] hash: [{}], executed after: [{}]nano", block.getBlockHeader().getNumber(), Numeric.toHexString(block.getBlockHeader().getHash()).substring(0, 6), totalTime);
        }


        return new BlockSummary(block, rewards, receipts, summaries);
    }

    @Override
    public synchronized void storeBlock(Block block, List<TransactionReceipt> receipts) {

        if (fork)
            blockStore.saveBlock(block, totalDifficulty, false);
        else
            blockStore.saveBlock(block, totalDifficulty, true);

        for (int i = 0; i < receipts.size(); i++) {
            transactionStore.put(new TransactionInfo(receipts.get(i), block.getBlockHeader().getHash(), i));
        }

        logger.debug("Block saved: number: {}, hash: {}, TD: {}",
                block.getBlockHeader().getNumber(), block.getBlockHeader().getHash(), totalDifficulty);

        setBestBlock(block);

        if (logger.isDebugEnabled())
            logger.debug("block added to the blockChain: index: [{}]", block.getBlockHeader().getNumber());
        if (block.getBlockHeader().getNumber() % 100 == 0)
            logger.info("*** Last block added [ #{} ]", block.getBlockHeader().getNumber());

    }

    public boolean hasParentOnTheChain(Block block) {
        return getParent(block.getBlockHeader()) != null;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    @Override
    public void setBestBlock(Block block) {
        bestBlock = block;
        repository = repository.getSnapshotTo(block.getBlockHeader().getStateRoot());
    }

    @Override
    public synchronized Block getBestBlock() {


        return bestBlock;
    }

    @Override
    public synchronized void close() {
        blockStore.close();
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    @Override
    public synchronized void updateTotalDifficulty(Block block) {
        totalDifficulty = totalDifficulty.add(block.getTotalDifficulty());
        if (logger.isDebugEnabled()) {
            logger.debug("TD: updated to {}", totalDifficulty);
        }
    }

    @Override
    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    private void recordBlock(Block block) {

        if (!config.isRecordBlocks()) return;

        String dumpDir = config.databaseDir() + "/" + config.dumpDir();

        File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {

            dumpFile.getParentFile().mkdirs();
            if (!dumpFile.exists()) dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            if (bestBlock.getBlockHeader().getNumber() == 0) {
                bw.write(Hex.toHexString(bestBlock.encode()));
                bw.write("\n");
            }

            bw.write(Hex.toHexString(block.encode()));
            bw.write("\n");

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateBlockTotDifficulties(long startFrom) {

        while (true) {
            synchronized (this) {
                ((BlockStoreImpl) blockStore).updateTotDifficulties(startFrom);

                if (startFrom == bestBlock.getBlockHeader().getNumber()) {
                    totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlock.getBlockHeader().getHash());
                }

                if (startFrom == blockStore.getMaxNumber()) {
                    Block bestStoredBlock = bestBlock;
                    BigInteger maxTD = totalDifficulty;


                    for (long num = bestBlock.getBlockHeader().getNumber() + 1; num <= blockStore.getMaxNumber(); num++) {
                        List<Block> blocks = ((BlockStoreImpl) blockStore).getBlocksByNumber(num);
                        for (Block block : blocks) {
                            BigInteger td = blockStore.getTotalDifficultyForHash(block.getBlockHeader().getHash());
                            if (maxTD.compareTo(td) < 0) {
                                maxTD = td;
                                bestStoredBlock = block;
                            }
                        }
                    }

                    if (totalDifficulty.compareTo(maxTD) < 0) {
                        blockStore.reBranch(bestStoredBlock);
                        bestBlock = bestStoredBlock;
                        totalDifficulty = maxTD;
                        repository = repository.getSnapshotTo(bestBlock.getBlockHeader().getStateRoot());
                    }

                    break;
                }
                startFrom++;
            }
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setExitOn(long exitOn) {
        this.exitOn = exitOn;
    }

    public void setMinerCoinbase(byte[] minerCoinbase) {
        this.minerCoinbase = minerCoinbase;
    }

    @Override
    public byte[] getMinerCoinbase() {
        return minerCoinbase;
    }

    public void setMinerExtraData(byte[] minerExtraData) {
        this.minerExtraData = minerExtraData;
    }

    public boolean isBlockExist(byte[] hash) {
        return blockStore.isBlockExist(hash);
    }

    @Override
    public Iterator<BlockHeader> getIteratorOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse) {


        BlockHeader startHeader;
        if (identifier.getHash() != null) {
            startHeader = findHeaderByHash(identifier.getHash());
        } else {
            startHeader = findHeaderByNumber(identifier.getNumber());
        }


        if (startHeader == null) {
            return EmptyBlockHeadersIterator.INSTANCE;
        }

        if (identifier.getHash() != null) {
            BlockHeader mainChainHeader = findHeaderByNumber(startHeader.getNumber());
            if (!startHeader.equals(mainChainHeader)) return EmptyBlockHeadersIterator.INSTANCE;
        }

        return new BlockHeadersIterator(startHeader, skip, limit, reverse);
    }

    
    private BlockHeader findHeaderByNumber(long number) {
        Block block = blockStore.getChainBlockByNumber(number);
        if (block == null) {
            if (headerStore != null) {
                return headerStore.getHeaderByNumber(number);
            } else {
                return null;
            }
        } else {
            return block.getBlockHeader();
        }
    }

    
    private BlockHeader findHeaderByHash(byte[] hash) {
        Block block = blockStore.getBlockByHash(hash);
        if (block == null) {
            if (headerStore != null) {
                return headerStore.getHeaderByHash(hash);
            } else {
                return null;
            }
        } else {
            return block.getBlockHeader();
        }
    }

    static class EmptyBlockHeadersIterator implements Iterator<BlockHeader> {

        final static EmptyBlockHeadersIterator INSTANCE = new EmptyBlockHeadersIterator();

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public BlockHeader next() {
            throw new NoSuchElementException("Nothing left");
        }
    }

    
    class BlockHeadersIterator implements Iterator<BlockHeader> {

        private final BlockHeader startHeader;
        private final int skip;
        private final int limit;
        private final boolean reverse;
        private Integer position = 0;
        private Pair<Integer, BlockHeader> cachedNext = null;

        BlockHeadersIterator(BlockHeader startHeader, int skip, int limit, boolean reverse) {
            this.startHeader = startHeader;
            this.skip = skip;
            this.limit = limit;
            this.reverse = reverse;
        }

        @Override
        public boolean hasNext() {
            if (startHeader == null || position >= limit) {
                return false;
            }

            if (position == 0) {

                cachedNext = Pair.of(0, startHeader);
                return true;
            } else if (cachedNext.getLeft().equals(position)) {

                return true;
            } else {

                BlockHeader prevHeader = cachedNext.getRight();
                long nextBlockNumber;
                if (reverse) {
                    nextBlockNumber = prevHeader.getNumber() - 1 - skip;
                } else {
                    nextBlockNumber = prevHeader.getNumber() + 1 + skip;
                }

                BlockHeader nextHeader = null;
                if (nextBlockNumber >= 0 && nextBlockNumber <= blockStore.getBestBlock().getBlockHeader().getNumber()) {
                    nextHeader = findHeaderByNumber(nextBlockNumber);
                }

                if (nextHeader == null) {
                    return false;
                } else {
                    cachedNext = Pair.of(position, nextHeader);
                    return true;
                }
            }
        }

        @Override
        public BlockHeader next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Nothing left");
            }

            if (cachedNext == null || !cachedNext.getLeft().equals(position)) {
                throw new ConcurrentModificationException("Concurrent modification");
            }
            ++position;

            return cachedNext.getRight();
        }
    }

    private class State {
        byte[] root = repository.getRoot();
        Block savedBest = bestBlock;
        BigInteger savedTD = totalDifficulty;
    }

    public void setHeaderStore(HeaderStore headerStore) {
        this.headerStore = headerStore;
    }
}
