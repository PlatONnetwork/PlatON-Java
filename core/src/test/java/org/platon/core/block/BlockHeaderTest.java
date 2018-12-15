//package org.platon.core.block;
//
//import org.junit.After;
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
///**
// * Created by alliswell on 2018/8/8.
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = DefaultConfig.class)
//public class BlockHeaderTest {
//
//	@Autowired
//	Chain chain;
//
//	private Block block;
//
//	private BlockHeader blockHeader;
//
//	@Before
//	public void setUp() throws Exception {
//		block = chain.getBlock(0);
//		blockHeader = block.info();
//	}
//
//	@Test
//	public void testParse() {
//
//		byte[]  headerBytes = block.info().encode();
//		blockHeader = new BlockHeader(headerBytes);
//
//		Assert.assertEquals(blockHeader, block.info());
//	}
//
//	@Test
//	public void testPropagatedBy() {
//		BlockHeader sonHeader = new BlockHeader();
//		sonHeader.propagatedBy(blockHeader);
//
//		Assert.assertEquals(blockHeader.getNumber()+1, sonHeader.getNumber());
//	}
//
//	@Test
//	public void testSetRoots() {
//		BlockHeader sonHeader = new BlockHeader();
//		sonHeader.setRoots(blockHeader.getStateRoot(),blockHeader.getPermissionRoot(),
//				blockHeader.getDposRoot(), blockHeader.getTransactionRoot(), blockHeader.getTransferRoot(),
//				blockHeader.getVotingRoot(), blockHeader.getReceiptRoot());
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		System.out.println("BlockHeader test OK!");
//	}
//}