package org.platon.core.db;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.AppenderName;
import org.platon.common.utils.ByteComparator;
import org.platon.common.utils.Numeric;
import org.platon.core.block.BlockHeader;
import org.platon.core.proto.BlockInfo;
import org.platon.core.proto.BlockInfoList;
import org.platon.core.block.Block;
import org.platon.core.datasource.DataSourceArray;
import org.platon.core.datasource.ObjectDataSource;
import org.platon.storage.datasource.SerializerIfc;
import org.platon.storage.datasource.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigInteger.ZERO;
import static org.bouncycastle.util.Arrays.areEqual;


public class BlockStoreImpl extends AbstractBlockstore {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);


    Source<byte[], byte[]> indexDS;
    DataSourceArray<List<BlockInfo>> index;

    Source<byte[], byte[]> blocksDS;
    ObjectDataSource<Block> blocks;

    public BlockStoreImpl() {
    }

    public void init(Source<byte[], byte[]> index, Source<byte[], byte[]> blocks) {
        indexDS = index;
        this.index = new DataSourceArray<>(new ObjectDataSource<>(index, BLOCK_INFO_SERIALIZER,512));
        this.blocksDS = blocks;
        this.blocks = new ObjectDataSource<>(blocks, new SerializerIfc<Block, byte[]>() {
            @Override
            public byte[] serialize(Block block) {
                return block.encode();
            }

            @Override
            public Block deserialize(byte[] bytes) {
                return bytes == null ? null : new Block(bytes);
            }
        },256);
    }

    public synchronized Block getBestBlock() {

        Long maxLevel = getMaxNumber();
        if (maxLevel < 0) return null;

        Block bestBlock = getChainBlockByNumber(maxLevel);
        if (bestBlock != null) return bestBlock;





        while (bestBlock == null) {
            --maxLevel;
            bestBlock = getChainBlockByNumber(maxLevel);
        }
        return bestBlock;
    }

    public synchronized byte[] getBlockHashByNumber(long blockNumber) {
        Block chainBlock = getChainBlockByNumber(blockNumber);
        return chainBlock == null ? null : chainBlock.getBlockHeader().getHash();
    }

    @Override
    public synchronized void flush() {
        blocks.flush();
        index.flush();
        blocksDS.flush();
        indexDS.flush();
    }

    @Override
    public synchronized void saveBlock(Block block, BigInteger totalDifficulty, boolean mainChain) {
        addInternalBlock(block, totalDifficulty, mainChain);
    }

    private void addInternalBlock(Block block, BigInteger totalDifficulty, boolean mainChain) {

        List<BlockInfo> blockInfos = block.getBlockHeader().getNumber() >= index.size() ? null : index.get((int) block.getBlockHeader().getNumber());
        blockInfos = blockInfos == null ? new ArrayList<BlockInfo>() : blockInfos;

        BlockInfo.Builder blockInfo = BlockInfo.newBuilder();
        blockInfo.setTotalDifficulty(ByteString.copyFrom(totalDifficulty.toByteArray()));
        blockInfo.setHash(ByteString.copyFrom(block.getBlockHeader().getHash()));

        blockInfo.setMainChain(mainChain);

        putBlockInfo(blockInfos, blockInfo.build());
        index.set((int) block.getBlockHeader().getNumber(), blockInfos);

        blocks.put(block.getBlockHeader().getHash(), block);
    }

    private void putBlockInfo(List<BlockInfo> blockInfos, BlockInfo blockInfo) {
        for (int i = 0; i < blockInfos.size(); i++) {
            BlockInfo curBlockInfo = blockInfos.get(i);
            if (ByteComparator.equals(curBlockInfo.getHash().toByteArray(), blockInfo.getHash().toByteArray())) {
                blockInfos.set(i, blockInfo);
                return;
            }
        }
        blockInfos.add(blockInfo);
    }

    public synchronized List<Block> getBlocksByNumber(long number) {

        List<Block> result = new ArrayList<>();

        if (number >= index.size()) {
            return result;
        }


        List<BlockInfo> blockInfos = index.get((int) number);

        if (blockInfos == null) {
            return result;
        }

        for (BlockInfo blockInfo : blockInfos) {

            byte[] hash = blockInfo.getHash().toByteArray();
            Block block = blocks.get(hash);

            result.add(block);
        }
        return result;
    }

    @Override
    public synchronized Block getChainBlockByNumber(long number) {
        if (number >= index.size()) {
            return null;
        }

        List<BlockInfo> blockInfos = index.get((int) number);

        if (blockInfos == null) {
            return null;
        }

        for (BlockInfo blockInfo : blockInfos) {

            if (blockInfo.getMainChain()) {

                byte[] hash = blockInfo.getHash().toByteArray();
                return blocks.get(hash);
            }
        }

        return null;
    }

    @Override
    public synchronized Block getBlockByHash(byte[] hash) {
        return blocks.get(hash);
    }

    @Override
    public synchronized boolean isBlockExist(byte[] hash) {
        return blocks.get(hash) != null;
    }

    @Override
    public synchronized BigInteger getTotalDifficultyForHash(byte[] hash) {
        Block block = this.getBlockByHash(hash);
        if (block == null) return ZERO;

        Long level = block.getBlockHeader().getNumber();
        List<BlockInfo> blockInfos = index.get(level.intValue());
        for (BlockInfo blockInfo : blockInfos) {
            if (areEqual(blockInfo.getHash().toByteArray(), hash)) {
                return Numeric.toBigInt(blockInfo.getTotalDifficulty().toByteArray());
            }
        }

        return ZERO;
    }

    @Override
    public synchronized BigInteger getTotalDifficulty() {
        long maxNumber = getMaxNumber();

        List<BlockInfo> blockInfos = index.get((int) maxNumber);
        for (BlockInfo blockInfo : blockInfos) {
            if (blockInfo.getMainChain()) {
                return Numeric.toBigInt(blockInfo.getTotalDifficulty().toByteArray());
            }
        }

        while (true) {
            --maxNumber;
            List<BlockInfo> infos = getBlockInfoForLevel(maxNumber);

            for (BlockInfo blockInfo : infos) {
                if (blockInfo.getMainChain()) {
                    return Numeric.toBigInt(blockInfo.getTotalDifficulty().toByteArray());
                }
            }
        }
    }

    public synchronized void updateTotDifficulties(long index) {
        List<BlockInfo> level = getBlockInfoForLevel(index);
        for (BlockInfo blockInfo : level) {

            BlockInfo.Builder blockInfoBuilder = blockInfo.toBuilder();
            Block block = getBlockByHash(blockInfo.getHash().toByteArray());
            List<BlockInfo> parentInfos = getBlockInfoForLevel(index - 1);
            BlockInfo parentInfo = getBlockInfoForHash(parentInfos, block.getBlockHeader().getParentHash());

            BigInteger parentTotalDifficulty = Numeric.toBigInt(parentInfo.getTotalDifficulty().toByteArray());
            BigInteger blockTotalDifficulty = Numeric.toBigInt(parentInfo.getTotalDifficulty().toByteArray());
            BigInteger finalTotalDifficulty = parentTotalDifficulty.add(blockTotalDifficulty);
            blockInfoBuilder.setTotalDifficulty(ByteString.copyFrom(finalTotalDifficulty.toByteArray()));
        }
        this.index.set((int) index, level);
    }

    @Override
    public synchronized long getMaxNumber() {

        Long bestIndex = 0L;

        if (index.size() > 0) {
            bestIndex = (long) index.size();
        }

        return bestIndex - 1L;
    }

    @Override
    public synchronized List<byte[]> getListHashesEndWith(byte[] hash, long number) {

        List<Block> blocks = getListBlocksEndWith(hash, number);
        List<byte[]> hashes = new ArrayList<>(blocks.size());

        for (Block b : blocks) {
            hashes.add(b.getBlockHeader().getHash());
        }

        return hashes;
    }

    @Override
    public synchronized List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty) {

        List<Block> blocks = getListBlocksEndWith(hash, qty);
        List<BlockHeader> headers = new ArrayList<>(blocks.size());

        for (Block b : blocks) {
            headers.add(b.getBlockHeader());
        }

        return headers;
    }

    @Override
    public synchronized List<Block> getListBlocksEndWith(byte[] hash, long qty) {
        return getListBlocksEndWithInner(hash, qty);
    }

    private List<Block> getListBlocksEndWithInner(byte[] hash, long qty) {

        Block block = this.blocks.get(hash);

        if (block == null) return new ArrayList<>();

        List<Block> blocks = new ArrayList<>((int) qty);

        for (int i = 0; i < qty; ++i) {
            blocks.add(block);
            block = this.blocks.get(block.getBlockHeader().getParentHash());
            if (block == null) break;
        }

        return blocks;
    }

    @Override
    public synchronized void reBranch(Block forkBlock) {

        Block bestBlock = getBestBlock();

        long maxLevel = Math.max(bestBlock.getBlockHeader().getNumber(), forkBlock.getBlockHeader().getNumber());


        long currentLevel = maxLevel;
        Block forkLine = forkBlock;
        if (forkBlock.getBlockHeader().getNumber() > bestBlock.getBlockHeader().getNumber()) {

            while (currentLevel > bestBlock.getBlockHeader().getNumber()) {
                List<BlockInfo> blocks = getBlockInfoForLevel(currentLevel);
                BlockInfo blockInfo = getBlockInfoForHash(blocks, forkLine.getBlockHeader().getHash());

                BlockInfo.Builder blockInfoBuilder = blockInfo.toBuilder();
                if (blockInfo != null) {
                    blockInfoBuilder.setMainChain(true);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                forkLine = getBlockByHash(forkLine.getBlockHeader().getParentHash());
                --currentLevel;
            }
        }

        Block bestLine = bestBlock;
        if (bestBlock.getBlockHeader().getNumber() > forkBlock.getBlockHeader().getNumber()) {

            while (currentLevel > forkBlock.getBlockHeader().getNumber()) {

                List<BlockInfo> blocks = getBlockInfoForLevel(currentLevel);
                BlockInfo blockInfo = getBlockInfoForHash(blocks, bestLine.getBlockHeader().getHash());
                BlockInfo.Builder blockInfoBuilder = blockInfo.toBuilder();
                if (blockInfo != null) {
                    blockInfoBuilder.setMainChain(false);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                bestLine = getBlockByHash(bestLine.getBlockHeader().getParentHash());
                --currentLevel;
            }
        }


        while (!bestLine.isEqual(forkLine)) {

            List<BlockInfo> levelBlocks = getBlockInfoForLevel(currentLevel);
            BlockInfo bestInfo = getBlockInfoForHash(levelBlocks, bestLine.getBlockHeader().getHash());
            BlockInfo.Builder bestInfoBuilder = bestInfo.toBuilder();
            if (bestInfo != null) {
                bestInfoBuilder.setMainChain(false);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }
            BlockInfo forkInfo = getBlockInfoForHash(levelBlocks, forkLine.getBlockHeader().getHash());
            BlockInfo.Builder forkInfoBuilder = forkInfo.toBuilder();
            if (forkInfo != null) {
                forkInfoBuilder.setMainChain(true);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }
            bestLine = getBlockByHash(bestLine.getBlockHeader().getParentHash());
            forkLine = getBlockByHash(forkLine.getBlockHeader().getParentHash());

            --currentLevel;
        }
    }

    public synchronized List<byte[]> getListHashesStartWith(long number, long maxBlocks) {

        List<byte[]> result = new ArrayList<>();

        int i;
        for (i = 0; i < maxBlocks; ++i) {
            List<BlockInfo> blockInfos = index.get((int) number);
            if (blockInfos == null) break;

            for (BlockInfo blockInfo : blockInfos)

                if (blockInfo.getMainChain()) {
                    result.add(blockInfo.getHash().toByteArray());
                    break;
                }

            ++number;
        }
        maxBlocks -= i;

        return result;
    }


    public static final SerializerIfc<List<BlockInfo>, byte[]> BLOCK_INFO_SERIALIZER = new SerializerIfc<List<BlockInfo>, byte[]>() {

        @Override
        public byte[] serialize(List<BlockInfo> value) {

            BlockInfoList.Builder blockInfoListBuilder = BlockInfoList.newBuilder();

            for (BlockInfo blockInfo : value) {

                BigInteger blockTotalDifficulty = Numeric.toBigInt(blockInfo.getTotalDifficulty().toByteArray());


                    
                blockInfoListBuilder.addBlockInfoList(blockInfo);
            }
            return blockInfoListBuilder.build().toByteArray();
        }

        @Override
        public List<BlockInfo> deserialize(byte[] bytes) {
            if (bytes == null) return null;
            try {
                BlockInfoList pbBlockInfoList = BlockInfoList.parseFrom(bytes);
                List<BlockInfo> blockInfoList = pbBlockInfoList.getBlockInfoListList();
                return blockInfoList;
            } catch (InvalidProtocolBufferException e) {

                throw new RuntimeException(e);
            }
        }
    };

    public synchronized void printChain() {

        Long number = getMaxNumber();

        for (int i = 0; i < number; ++i) {
            List<BlockInfo> levelInfos = index.get(i);

            if (levelInfos != null) {
                System.out.print(i);
                for (BlockInfo blockInfo : levelInfos) {
                    if (blockInfo.getMainChain())
                        System.out.print(" [" + shortHash(blockInfo.getHash().toByteArray()) + "] ");
                    else
                        System.out.print(" " + shortHash(blockInfo.getHash().toByteArray()) + " ");
                }
                System.out.println();
            }
        }
    }

    private synchronized List<BlockInfo> getBlockInfoForLevel(long level) {
        return index.get((int) level);
    }

    private synchronized void setBlockInfoForLevel(long level, List<BlockInfo> infos) {
        index.set((int) level, infos);
    }

    private static BlockInfo getBlockInfoForHash(List<BlockInfo> blocks, byte[] hash) {

        for (BlockInfo blockInfo : blocks)
            if (areEqual(hash, blockInfo.getHash().toByteArray())) return blockInfo;

        return null;
    }

    @Override
    public synchronized void load() {
    }

    @Override
    public synchronized void close() {
    }

    public static String shortHash(byte[] hash) {
        return Numeric.toHexString(hash).substring(0, 6);
    }
}
