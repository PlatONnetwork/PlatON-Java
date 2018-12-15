package org.platon.core.transaction.util;

import org.bouncycastle.jcajce.provider.digest.SHA3;

public class HashUtil {

    public static byte[] EMPTY_ROOT = bcSHA3Digest256(new byte[0]);
    // bouncycastle SHA3
    public static byte[] bcSHA3Digest256(byte[] value) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
        return digestSHA3.digest(value);
    }
}