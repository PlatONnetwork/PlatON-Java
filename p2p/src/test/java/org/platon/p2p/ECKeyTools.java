package org.platon.p2p;

import org.platon.common.utils.Numeric;
import org.platon.crypto.ECKey;
import org.platon.crypto.WalletUtil;

import java.nio.charset.StandardCharsets;

public class ECKeyTools {
    public static void main(String[] args){
        ECKey ecKey = new ECKey();

        byte[] priKey = ecKey.getPrivKeyBytes();
        byte[] pubKey = ecKey.getPubKey();

        System.out.println(Numeric.toHexString(ecKey.getPrivKeyBytes()));
        System.out.println(Numeric.toHexString(ecKey.getPubKey()));

        byte[] buf = "this is a test message.".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedBytes = WalletUtil.encrypt(buf, NodeContext.ecKey);

    }
}
