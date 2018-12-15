package org.platon.crypto;

import org.junit.Assert;
import org.junit.Test;
import org.bouncycastle.util.encoders.Hex;

public class HashUtilTest {

    private static final String message = "welcome to platon";

    private static final String messageSha3Str = "86139924b67294e70849375a7b59531c0819e790471a607578b162211ca44871";

    private static final String messageR160Str = "89af04763c4b9c3cb447af31b5e0f5756bddc76d";

    @Test
    public void sha3() {
        byte[] messageSha3= HashUtil.sha3(message.getBytes());
        Assert.assertEquals(Hex.toHexString(messageSha3),messageSha3Str);
    }

    @Test
    public void ripemd160() {
        byte[] messageR160= HashUtil.ripemd160(message.getBytes());
        Assert.assertEquals(Hex.toHexString(messageR160),messageR160Str);
    }

}