package org.platon.core.validator;

import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.LRUHashMap;
import org.platon.core.config.CoreConfig;
import org.platon.core.validator.model.ValidateBlock;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * TimelinessValidator
 *
 * @author yanze
 * @desc check block and tx is valid
 * @create 2018-08-08 10:58
 **/
public class TimelinessValidator {

    private LRUHashMap<Long,ValidateBlock> ancestorBlockCache;
    //TODO pending block and tx may be merge with pool
    private LRUHashMap<ByteArrayWrapper,ValidateBlock> pendingBlockCache;
    private ArrayDeque<ByteArrayWrapper> pendingTxCache;
    private long maxAncestorBlockNum;

    public TimelinessValidator() {
        this.ancestorBlockCache = new LRUHashMap<>(Long.parseLong(CoreConfig.getInstance().ancestorBlockCacheSize()));
        this.pendingBlockCache = new LRUHashMap<>(Long.parseLong(CoreConfig.getInstance().pendingBlockCacheSize()));
        this.pendingTxCache = new ArrayDeque<>(Integer.parseInt(CoreConfig.getInstance().pendingTxCacheSize()));
        this.maxAncestorBlockNum = 0;
    }

    public TimelinessValidator(Map<Long,ValidateBlock> map, long maxAncestorBlockNum) {
        this.ancestorBlockCache = new LRUHashMap<>(Long.parseLong(CoreConfig.getInstance().ancestorBlockCacheSize()));
        this.pendingBlockCache = new LRUHashMap<>(Long.parseLong(CoreConfig.getInstance().pendingBlockCacheSize()));
        this.pendingTxCache = new ArrayDeque<>(Integer.parseInt(CoreConfig.getInstance().pendingTxCacheSize()));
        for(Long num : map.keySet()){
            ValidateBlock temp = map.get(num);
            //deep clone
            ancestorBlockCache.put(num,temp.clone());
        }
        this.maxAncestorBlockNum = maxAncestorBlockNum;
    }


    /**
     * validate block hash
     * @param blockHash
     * @param ancestorBlockNum
     * @param ancestorBlockHash
     * @return
     */
    public boolean validateBlockHash(byte[] blockHash,long ancestorBlockNum,byte[] ancestorBlockHash){
        if(maxAncestorBlockNum == 0 && blockHash != null
                && ancestorBlockNum == 0 && ancestorBlockHash == null){
            return true;
        }
        ValidateBlock ancestorBlock = ancestorBlockCache.get(ancestorBlockNum);
        //validate ancestorBlockNum、ancestorBlockHash
        if(ancestorBlock == null||ancestorBlock.getBlockHash().length==0
                || !Arrays.equals(ancestorBlock.getBlockHash(),ancestorBlockHash)){
            return false;
        }
        //validate blockHash step1:check blockHash is exist in ancestorBlock
        for(long i = ancestorBlockNum;i <= maxAncestorBlockNum;i++){
            if(Arrays.equals(ancestorBlockCache.get(i).getBlockHash(),blockHash)){
                return false;
            }
        }
        //validate blockHash step2:check blockHash is exist in pendingBlock
        ValidateBlock pendingBlock = pendingBlockCache.get(new ByteArrayWrapper(blockHash));
        if(pendingBlock != null){
            return false;
        }
        return true;
    }

    /**
     * add block hash in pending block cache
     * @param blockHash
     * @param ancestorBlockNum
     * @param ancestorBlockHash
     * @param txSet
     * @return
     */
    public boolean addBlockHash(byte[] blockHash, long ancestorBlockNum, byte[] ancestorBlockHash, HashSet<ByteArrayWrapper> txSet){
        if(this.validateBlockHash(blockHash,ancestorBlockNum,ancestorBlockHash)){
            ValidateBlock pendingBlock = new ValidateBlock(blockHash,ancestorBlockNum,ancestorBlockHash,txSet);
            pendingBlockCache.put(new ByteArrayWrapper(blockHash),pendingBlock);
            return true;
        }
        return false;
    }
    /**
     * add block hash in ancestor block cache
     * @param blockHash
     * @param blockNum
     * @param ancestorBlockNum
     * @param ancestorBlockHash
     * @param txSet
     * @return
     */
    public boolean addAncestorBlockHash(byte[] blockHash,long blockNum,byte[] ancestorBlockHash,long ancestorBlockNum,HashSet<ByteArrayWrapper> txSet){
        if(pendingBlockCache.get(blockHash) == null){
            if(!validateBlockHash(blockHash,ancestorBlockNum,ancestorBlockHash)){
                return false;
            }
        }
        ValidateBlock block = new ValidateBlock(blockHash, ancestorBlockNum, ancestorBlockHash, txSet);
        pendingBlockCache.remove(blockHash);
        //??? did I add method about pendingTxCache.remove
        ancestorBlockCache.put(blockNum, block);
        maxAncestorBlockNum = blockNum;
        return true;
    }

    /**
     * validate tx hash
     * @param txHash
     * @param ancestorBlockNum
     * @param ancestorBlockHash
     * @return
     */
    public boolean validateTxHash(byte[] txHash,long ancestorBlockNum,byte[] ancestorBlockHash){
        if(maxAncestorBlockNum == 0 && txHash != null
                && ancestorBlockNum == 0 && ancestorBlockHash == null){
            return true;
        }
        ValidateBlock ancestorBlock = ancestorBlockCache.get(ancestorBlockNum);
        //validate ancestorBlockNum、ancestorBlockHash
        if(ancestorBlock == null||ancestorBlock.getBlockHash().length==0
                || !Arrays.equals(ancestorBlock.getBlockHash(),ancestorBlockHash)){
            return false;
        }
        //validate pengdingTxcache exist
        if(txHash == null || pendingTxCache.contains(new ByteArrayWrapper(txHash))){
            return false;
        }
        //validate ancestorBlock tx exist
        for (long i = ancestorBlockNum;i <= maxAncestorBlockNum;i++){
            ValidateBlock blockTemp = ancestorBlockCache.get(i);
            if(blockTemp.getTxHashSet().contains(new ByteArrayWrapper(txHash))){
                return false;
            }
        }
        return true;
    }

    /**
     * add tx hash in pending tx cache
     * @param txHash
     * @param ancestorBlockNum
     * @param ancestorBlockHash
     * @return
     */
    public boolean addTxHash(byte[] txHash,long ancestorBlockNum,byte[] ancestorBlockHash){
        if(this.validateTxHash(txHash,ancestorBlockNum,ancestorBlockHash)){
            pendingTxCache.add(new ByteArrayWrapper(txHash));
            return true;
        }
        return false;
    }
}
