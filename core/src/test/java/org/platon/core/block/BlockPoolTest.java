//package org.platon.core.block;
//
//import org.bouncycastle.util.encoders.Hex;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.platon.core.chain.Chain;
//import org.platon.core.config.DefaultConfig;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = DefaultConfig.class)
//public class BlockPoolTest {
//
//    @Autowired
//    Chain chain;
//
//    private BlockPool pool = new BlockPool(100);
//
//    private Block genesisBlock;
//
//    @Before
//    public void setUp() throws Exception {
//        genesisBlock = chain.genesisBlock();
//    }
//
//    @Test
//    public void injectRaw() {
//        pool.clear();
//        Block block = new Block();
//        block.populateFromParent(genesisBlock.info());
//        pool.injectRaw(block);
//    }
//
//    @Test
//    public void pollRaw() {
//        injectRaw();
//        Block block = pool.pollRaw();
//        Assert.assertNotNull(block);
//    }
//
//    @Test
//    public void injectValid() {
//        pool.clear();
//        Block block = new Block();
//        block.populateFromParent(genesisBlock.info());
//        pool.injectValid(block);
//    }
//
//    @Test
//    public void injectFork() {
//        pool.clear();
//        Block block = new Block();
//        block.populateFromParent(genesisBlock.info());
//        pool.injectFork(block);
//    }
//
//    @Test
//    public void pollValid() {
//        injectValid();
//        Block block = pool.pollValid();
//        Assert.assertNotNull(block);
//    }
//
//    @Test
//    public void pollFork() {
//        injectFork();
//        Block block = pool.pollFork();
//        Assert.assertNotNull(block);
//    }
//
//    @Test
//    public void collectSignatures() {
//        byte[] sig = Hex.decode("0a791201011a2166473538557a77664b7a41565877594e614e55766b4c534550");
//        pool.collectSignatures(genesisBlock.info().getHash(), sig);
//    }
//}