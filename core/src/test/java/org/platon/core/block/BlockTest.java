//package org.platon.core.block;
//
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.platon.common.utils.Numeric;
//import org.platon.core.chain.Chain;
//import org.platon.core.config.DefaultConfig;
//import org.platon.core.state.State;
//import org.platon.core.transaction.TransactionPool;
//import org.platon.core.transaction.TransactionPoolTest;
//import org.platon.core.transaction.TransactionReceipt;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import java.math.BigInteger;
//import java.util.ArrayList;
//
///** test Block class
// * Created by alliswell on 2018/8/8.
// */
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = DefaultConfig.class)
//public class BlockTest {
//
//	@Autowired
//	Chain chain;
//
//	private byte[] blockHash = "".getBytes();
//	private byte[] address = Numeric.hexStringToByteArray("0xa3ae7747a0690701cc84b453524fa7c99afcd8ac");
//
//	private Block block;
//
//	@Before
//	public void setUp() throws Exception {
//		block = chain.getBlock(0);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		System.out.println("Block test OK!");
//	}
//
//	@Test
//	public void testGetBalance() {
//
//		BigInteger balance = block.getBalance(address);
//		System.out.println("[fG58UzwfKzAVXwYNaNUvkLSEPqYSkR25o]'s balance = " + balance.toString());
//	}
//
//	@Test
//	public void testInfo() {
//		BlockHeader header = block.info();
//		Assert.assertNotNull(header);
//	}
//
//	@Test
//	public void testGetState() {
//		State state = block.getState();
//		BigInteger balance = state.balance(address);
//		System.out.println("balance = " + balance);
//	}
//
//	@Test
//	public void testPopulateFromParent() {
//		Block sonBlock = new Block();
//		sonBlock.populateFromParent(block.info());
//	}
//
//	@Test
//	public void testBuildFromTransactionPool() {
//		TransactionPool pool = TransactionPool.getInstance();
//		pool.inject(TransactionPoolTest.getTx1());
//		pool.inject(TransactionPoolTest.getTx2());
//
//		Block sonBlock = new Block();
//		sonBlock.populateFromParent(block.info());
//		ArrayList<TransactionReceipt> receipts = sonBlock.buildFromTransactionPool(chain, pool);
//		Assert.assertTrue(receipts.size() > 0);
//	}
//
//	@Test
//	public void testSeal() {
//		TransactionPool pool = TransactionPool.getInstance();
//		pool.inject(TransactionPoolTest.getTx1());
//		pool.inject(TransactionPoolTest.getTx2());
//
//		Block sonBlock = new Block();
//		sonBlock.populateFromParent(block.info());
//		ArrayList<TransactionReceipt> receipts = sonBlock.buildFromTransactionPool(chain, pool);
//
//		sonBlock.seal(chain, new byte[0]);
//		Assert.assertNotNull(sonBlock.info());
//	}
//}