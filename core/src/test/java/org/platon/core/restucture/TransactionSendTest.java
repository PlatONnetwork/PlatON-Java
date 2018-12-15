package org.platon.core.restucture;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigFactory;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.platon.common.utils.Numeric;
import org.platon.core.TransactionInfo;
import org.platon.core.block.proto.BlockHeaderProto;
import org.platon.core.config.CoreConfig;
import org.platon.core.facade.Platon;
import org.platon.core.facade.PlatonFactory;
import org.platon.core.facade.PlatonImpl;
import org.platon.core.keystore.FileSystemKeystore;
import org.platon.core.keystore.Keystore;
import org.platon.core.rpc.ProtoRpc;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.TransactionReceipt;
import org.platon.core.transaction.proto.ContractRequest;
import org.platon.core.transaction.proto.TransactionBody;
import org.platon.core.transaction.proto.TransactionType;
import org.platon.crypto.ECKey;
import org.platon.crypto.HashUtil;
import org.platon.crypto.WalletUtil;
import org.platon.slice.message.request.TransactionBaseRequest;
import org.platon.slice.message.response.BlockResponse;
import org.platon.slice.message.response.StringResponse;
import org.platon.slice.message.response.TransactionReceiptResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;

public class TransactionSendTest {

    @Test
    public void testMain() throws Exception {
        System.out.println("Start Platonj for test...");
        PlatonImpl platon = (PlatonImpl) PlatonFactory.createPlaton(TestConfig.class);
        TestRunner test = platon.getApplicationContext().getBean(TestRunner.class);
        test.runTest();
    }

    private static class TestConfig {

        private final String config =
                "database.dir = db-01 \n" +
                        "database.reset = yes \n" +
                        "platon.keystore.dir = /c/users/jungle/Desktop/keystore \n";

        @Autowired
        ApplicationContext ctx;

        @Bean
        public CoreConfig configProperties() {
            CoreConfig configProperties = CoreConfig.getInstance();
            configProperties.overrideParams(ConfigFactory.parseString(config.replaceAll("'", "\"")));
            return configProperties;
        }

        @Bean
        @Primary
        public Platon platon() {
            return new PlatonImpl();
        }

        @Bean
        @Primary
        public Keystore keystore() {
            return new FileSystemKeystore();
        }

        @Bean
        public TestRunner testRunner() {
            return new TestRunner();
        }

        public ApplicationContext getCtx() {
            return ctx;
        }
    }

    static class TestRunner {

        private final static BigInteger RETURN_BALANCE = BigInteger.valueOf(1111);
        private final static long RETURN_BLOCK_NUM = 1;
        private final static String RETURN_BLOCK_HASH = "ox1a1a1a1a";
        private final static String RETURN_TX_HASH = "0x2b2b2b2b2b2b2b2b";
        private final static BigInteger PRIVATE_KEY = new BigInteger("32650665894031292597140147254420778587605881617997786858854485181389018874929");
        private final static ECKey EC_KEY = ECKey.fromPrivate(PRIVATE_KEY);
        private final static String ADDRESS = Numeric.toHexString(EC_KEY.getAddress());
        private final static String RECEIVER = "fG58UzwfKzAVXwYNaNUvkLSEPqYSkR240";
        private final static byte[] EMPTY_BYTES = new byte[]{};
        @Autowired
        private ProtoRpc protoRpc;
        @Autowired
        private Keystore keystore;
        @Autowired
        private Platon platon;

        private Transaction createTx() {
            Transaction tx = new Transaction(
                    TransactionType.TRANSACTION,
                    BigInteger.valueOf(100),
                    Numeric.hexStringToByteArray(ADDRESS),
                    RETURN_BLOCK_NUM,
                    Numeric.hexStringToByteArray(RETURN_BLOCK_HASH),
                    BigInteger.valueOf(1000000),
                    BigInteger.valueOf(2000000),
                    new byte[]{},
                    -1);
            return tx;
        }

        private TransactionInfo createTxInfo() {
            TransactionReceipt txReceipt = new TransactionReceipt(
                    EMPTY_BYTES,
                    EMPTY_BYTES,
                    null, null);
            txReceipt.setTransaction(createTx());

            TransactionInfo txInfo = new TransactionInfo(
                    txReceipt,
                    Numeric.hexStringToByteArray(RETURN_BLOCK_HASH),
                    0);

            return txInfo;
        }

        private BlockHeaderProto.BlockHeader createBlockHeader() {
            BlockHeaderProto.BlockHeader.Builder headerBuilder = BlockHeaderProto.BlockHeader.newBuilder();
            headerBuilder.setTimestamp(System.currentTimeMillis() / 1000);
            headerBuilder.setBloomLog(ByteString.EMPTY);
            headerBuilder.setNumber(RETURN_BLOCK_NUM);
            headerBuilder.setParentHash(ByteString.EMPTY);
            return headerBuilder.build();
        }


        private TransactionBaseRequest createTxRequest() throws Exception {

            // build tx
            ContractRequest.Builder ctrReqBuild = ContractRequest.newBuilder();
            ctrReqBuild.setOperation("call");
            ctrReqBuild.setClassName("MyHello");
            ctrReqBuild.setData(ByteString.EMPTY);

            TransactionBody.Builder bodyBuild = TransactionBody.newBuilder();
            bodyBuild.setType(TransactionType.TRANSACTION);
            bodyBuild.setValue(ByteString.copyFrom(BigInteger.TEN.toByteArray()));
            bodyBuild.setReceiveAddress(ByteString.copyFrom(Numeric.hexStringToByteArray(ADDRESS)));
            bodyBuild.setReferenceBlockNum(RETURN_BLOCK_NUM);
            bodyBuild.setReferenceBlockHash(ByteString.copyFrom(Numeric.hexStringToByteArray(RETURN_BLOCK_HASH)));
            bodyBuild.setEnergonPrice(ByteString.copyFrom(BigInteger.valueOf(11000).toByteArray()));
            bodyBuild.setEnergonLimit(ByteString.copyFrom(BigInteger.valueOf(20000).toByteArray()));
            bodyBuild.setData(Any.pack(ctrReqBuild.build()));

            TransactionBody body = bodyBuild.build();

            TransactionBaseRequest.Builder baseBuild = TransactionBaseRequest.newBuilder();
            byte[] messageHash = HashUtil.sha3(bodyBuild.build().toByteArray());
            byte chainId = 1;
            baseBuild.setSignature(new String(WalletUtil.sign(messageHash, chainId, EC_KEY)));
            baseBuild.setFrom(ADDRESS);
            baseBuild.setBody(body);

            return baseBuild.build();
        }

        public void runTest() throws Exception {

            System.out.println(EC_KEY.getPrivKey());
            // fG58UzwfKzAVXwYNaNUvkLSEPqYSkR25o
            System.out.println(Numeric.toHexString(EC_KEY.getAddress()));
            System.out.println(Arrays.toString(EC_KEY.getAddress()));
            // - atpGetBalance
            String addr = ADDRESS;
              //byte[] addrAsBytes = Numeric.hexStringToByteArray(addr);
            //BigInteger balance01 = platon.getBalance(addrAsBytes);
            //StringResponse balance = protoRpc.atpGetBalance(addr, "latest");

            //System.out.println("balance:" + balance.getData());
            //Assert.assertEquals(RETURN_BALANCE.intValue(), Numeric.toBigInt(balance.getData()).intValue());

            // - atpBlockNumber
            StringResponse blockNumber = protoRpc.atpBlockNumber();
            //Assert.assertEquals(RETURN_BLOCK_NUM, Numeric.toBigInt(blockNumber.getData()));

            // - atpGetBlockTransactionCountByNumber
            StringResponse txCount = protoRpc.atpGetBlockTransactionCountByNumber(Numeric.toHexStringWithPrefix(BigInteger.valueOf(RETURN_BLOCK_NUM)));
            //Assert.assertEquals(Numeric.toHexStringWithPrefix(BigInteger.valueOf(1)), txCount.getData());

            // - atpSendTransaction
            //protoRpc.personalUnlockAccount(ADDRESS, "11112", Numeric.toHexStringWithPrefix(BigInteger.valueOf(Integer.MAX_VALUE)));
            //StringResponse txHash = protoRpc.atpSendTransaction(createTxRequest());
            //System.out.println("atpSendTransaction -> txHash : " + txHash.getData());

            // - atpSendFillTransaction
            StringResponse txHash = protoRpc.atpSendFillTransaction(createTxRequest());
            System.out.println("atpSendFillTransaction -> txHash : " + txHash.getData());

            byte[] testHash = Hex.decode("d5934f3d1a82934e2056515626efa596c79628f1199a247e0d88c91e058dba46");

            // - atpGetTransactionReceipt
            TransactionReceiptResponse receipt = protoRpc.atpGetTransactionReceipt("d5934f3d1a82934e2056515626efa596c79628f1199a247e0d88c91e058dba46");
//            System.out.println(receipt);
//            while(receipt == null){
//                Thread.sleep(1000);
//                receipt = protoRpc.atpGetTransactionReceipt(txHash.getData());
//                System.out.println("////////////// 查询了没找到\\\\\\\\\\\\\\\\\\");
//            }
            System.out.println("...");

            // - atpGetBlockByNumber
            BlockResponse blockResponse = protoRpc.atpGetBlockByNumber(Numeric.toHexStringWithPrefix(BigInteger.valueOf(RETURN_BLOCK_NUM)), true);
            //Assert.assertNotNull(blockResponse);

            // - atpGetBlockByHash
            //BlockResponse blockResponse = protoRpc.atpGetBlockByHash(RETURN_BLOCK_HASH, true);
            //Assert.assertNotNull(blockResponse);

            // - atpGetTransactionByHash
            //TransactionResponse txResponse = protoRpc.atpGetTransactionByHash(RETURN_TX_HASH);
            //Assert.assertNotNull(txResponse);
            new Scanner(System.in).next();
        }

    }

}