package org.platon.core.transaction;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

public class TransactionPoolTest {

    private static byte[] tx1Bytes = Hex.decode("0a791201011a2166473538557a77664b7a41565877594e614e55766b4c5345507159536b5232356f30013a05ef1a1a1a1a42022af84a024e2052400a2d747970652e676f6f676c65617069732e636f6d2f706c61746f6e2e74782e436f6e747261637452657175657374120f0a0463616c6c12074d7948656c6c6f12584a646a64594d6e3641585253507434736d30573161303830766b44524658506d2f6d586c5a75534b585334724e5045384e644b4e6f4c715076346d71755657784c714748714c5961486754476a5977576b504c474643633d");
    private static byte[] tx2Bytes = Hex.decode("0a7912010a1a2166473538557a77664b7a41565877594e614e55766b4c5345507159536b5232356f30013a05ef1a1a1a1a42022af84a024e2052400a2d747970652e676f6f676c65617069732e636f6d2f706c61746f6e2e74782e436f6e747261637452657175657374120f0a0463616c6c12074d7948656c6c6f12584a595331346d2f65617431375669676e62625841485a4a614a4f6d36736134614f464d677770773143526d73572b546b4a44645363774c736e437575617a6e7a796e2f6d65316f5465304c74436b6166684c785a4842593d");

    TransactionPool pool = TransactionPool.getInstance();
    private static  Transaction tx1 = new Transaction(tx1Bytes);
    private static  Transaction tx2 = new Transaction(tx2Bytes);

    @Test
    public void inject() {
        pool.clear();
        pool.inject(tx1);
        pool.inject(tx2);
        Assert.assertTrue(pool.isKnown(tx1.getHash()));
        Assert.assertTrue(pool.isKnown(tx2.getHash()));
    }

    @Test
    public void isKnown() {
        inject();
        Assert.assertTrue(TransactionPool.getInstance().isKnown(tx1.getHash()));
        pool.drop(tx2);
        Assert.assertTrue(!TransactionPool.getInstance().isKnown(tx2.getHash()));
    }

    @Test
    public void consume() {
        inject();
        HashSet<byte[]> avoid = new HashSet<>();
        avoid.add(tx2.getHash());

        ArrayList<Transaction> ret = pool.consume(10, avoid);
        Assert.assertEquals(ret.size(), 1);
    }

    public static Transaction getTx1() {
        return tx1;
    }
    public static Transaction getTx2() {
        return tx2;
    }
}