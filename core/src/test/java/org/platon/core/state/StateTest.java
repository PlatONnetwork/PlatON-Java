//package org.platon.core.state;
//
//import org.bouncycastle.util.encoders.Hex;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.platon.common.utils.Numeric;
//import org.platon.core.Account;
//import org.platon.core.block.Block;
//import org.platon.core.block.BlockHeader;
//import org.platon.core.chain.Chain;
//import org.platon.core.config.CommonConfig;
//import org.platon.core.config.DefaultConfig;
//import org.platon.core.transaction.Transaction;
//import org.platon.core.transaction.TransactionPoolTest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import java.math.BigInteger;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = DefaultConfig.class)
//public class StateTest {
//
//    @Autowired
//    private Chain chain;
//
//    @Autowired
//    private CommonConfig commonConfig;
//
//    private State state;
//
//    private Block genesisBlock;
//
//    private byte[] address = Numeric.hexStringToByteArray("0xa3ae7747a0690701cc84b453524fa7c99afcd8ac");
//
//    @Before
//    public void setUp() {
//        genesisBlock = chain.genesisBlock();
//        state = chain.genesisBlock().getState();
//    }
//
//    @Test
//    public void balance() {
//        BigInteger balance = state.balance(address);
//        Assert.assertNotNull(balance);
//    }
//
//    @Test
//    public void setStorage() {
//        byte[] key = Hex.decode("0a791201011a2166473538557a77664b7a41565877594e614e55766b4c534550");
//        byte[] value = "0a791201011a2166473538557a77664b7a41565877594e614e55766b4c534550".getBytes();
//        state.setStorage(address, key, value);
//    }
//
//    @Test
//    public void executeTransaction() {
//        Transaction tx = TransactionPoolTest.getTx1();
//        BlockHeader header = new BlockHeader();
//        header.propagatedBy(commonConfig.systemConfig().getGenesisBlock().info());
//        state.executeTransaction(header, chain.lastHashes(), new byte[0], tx);
//    }
//
//    @Test
//    public void transferBalance() {
//        byte[] receiver = Numeric.hexStringToByteArray("0xa3ae7747a0690701cc84b453524fa7c99afcd8ac");
//        BigInteger value = BigInteger.TEN;
//
//        state.transferBalance(address, receiver, value);
//
//        Assert.assertTrue(-1 != state.balance(receiver).compareTo(BigInteger.TEN));
//    }
//
//    @Test
//    public void commit() {
//        transferBalance();
//        state.commit();;
//    }
//
//    @Test
//    public void rollback() {
//        transferBalance();
//        state.rollback(0);;
//    }
//
//    @Test
//    public void setRoot() {
//        state.setRoot(commonConfig.systemConfig().getGenesisBlock().getState().getRoot());
//    }
//
//    @Test
//    public void getRoot() {
//        byte[] root = state.getRoot();
//        Assert.assertNotNull(root);
//    }
//
//    @Test
//    public void createAccount() {
//        byte[] addr = Numeric.hexStringToByteArray("0xa3ae7747a0690701cc84b453524fa7c99afcd8ac");
//        Account account = state.createAccount(addr);
//        Assert.assertNotNull(account);
//    }
//}