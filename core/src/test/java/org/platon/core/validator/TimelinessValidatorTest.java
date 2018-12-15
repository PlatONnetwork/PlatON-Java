package org.platon.core.validator;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.core.validator.model.ValidateBlock;

import java.util.HashSet;

import static org.junit.Assert.*;

public class TimelinessValidatorTest {

    private static final byte[]  blockHash = "blockHash".getBytes();
    private static final long ancestorBlockNum = 1L;
    private static final byte[] ancestorBlockHash ="ancestorBlockHash".getBytes();
    private static final byte[] txHash ="txHash".getBytes();
    private static final byte[] txHash2 = "txHash2".getBytes();
    private HashSet<ByteArrayWrapper> txSet;


    @Test
    public void validateBlockHash() {
        TimelinessValidator timelinessValidator = this.addHash();
        Assert.assertTrue(timelinessValidator.validateBlockHash(blockHash,ancestorBlockNum,ancestorBlockHash));
        Assert.assertFalse(timelinessValidator.validateBlockHash(blockHash,ancestorBlockNum+1,ancestorBlockHash));
    }

    @Test
    public void addBlockHash() {
        TimelinessValidator timelinessValidator = this.addHash();
        Assert.assertTrue(timelinessValidator.addBlockHash(blockHash,ancestorBlockNum,ancestorBlockHash,null));
        Assert.assertFalse(timelinessValidator.addBlockHash(blockHash,ancestorBlockNum,ancestorBlockHash,null));
    }

    @Test
    public void addAncestorBlockHash() {
        TimelinessValidator timelinessValidator = this.addHash();
        Assert.assertNotNull(timelinessValidator);
    }

    private TimelinessValidator addHash(){
        TimelinessValidator timelinessValidator = new TimelinessValidator();
        txSet = new HashSet<>();
        txSet.add(new ByteArrayWrapper(txHash));
        if(!timelinessValidator.addAncestorBlockHash(ancestorBlockHash,ancestorBlockNum,null,0,txSet)){
            return null;
        }
        return timelinessValidator;
    }

    @Test
    public void validateTxHash() {
        TimelinessValidator timelinessValidator = this.addHash();
        Assert.assertTrue(timelinessValidator.validateTxHash(txHash2,ancestorBlockNum,ancestorBlockHash));
        Assert.assertFalse(timelinessValidator.validateTxHash(txHash,ancestorBlockNum,ancestorBlockHash));
    }

    @Test
    public void addTxHash() {
        TimelinessValidator timelinessValidator = this.addHash();
        Assert.assertTrue(timelinessValidator.addTxHash(txHash2,ancestorBlockNum,ancestorBlockHash));
        Assert.assertFalse(timelinessValidator.addTxHash(txHash2,ancestorBlockNum,ancestorBlockHash));
    }

}