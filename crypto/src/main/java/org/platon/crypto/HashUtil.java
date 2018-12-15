package org.platon.crypto;

import org.platon.common.utils.ByteUtil;
import org.platon.crypto.jce.NewBouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Random;

public class HashUtil {

    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static final Provider CRYPTO_PROVIDER;

    private static final String HASH_256_ALGORITHM_NAME;

    public static final byte[] EMPTY_DATA_HASH;

    public static final byte[] EMPTY_HASH;

    static {
        Security.addProvider(NewBouncyCastleProvider.getInstance());
        CRYPTO_PROVIDER = Security.getProvider("BC");
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256";
        EMPTY_HASH = sha3(ByteUtil.EMPTY_BYTE_ARRAY);
        EMPTY_DATA_HASH = sha3(EMPTY_BYTE_ARRAY);
    }
    /**
     * sha3
     *
     * @param data  data
     * @return hash of the data
     */
    public static byte[] sha3(byte[] data) {
        if(null == data) {
            return EMPTY_HASH;
        }
        return sha3(data,0,data.length);
    }

    /**
     * sha3 twice
     * @param data  data
     * @return hash of the data
     */
    public static byte[] sha3Twice(byte[] data) {
        return sha3Twice(data,0,data.length);
    }

    /**
     * sha3
     *
     * @param data1 data1
     * @param data2 data2
     * @return hash of the data
     */
    public static byte[] sha3(byte[] data1, byte[] data2) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(data1, 0, data1.length);
            digest.update(data2, 0, data2.length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Can not find algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * sha3
     *
     * @param data data
     * @param start start of hashing chunk
     * @param length length of hashing chunk
     * @return keccak hash of the chunk
     */
    public static byte[] sha3(byte[] data, int start, int length) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(data, start, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Can not find algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * sha3 twice
     *
     * @param data data
     * @param start start of hashing chunk
     * @param length length of hashing chunk
     * @return keccak hash of the chunk
     */
    public static byte[] sha3Twice(byte[] data, int start, int length) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(data, start, length);
            return digest.digest(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Can not find algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * ripemd160
     *
     * @param data data
     * @return hash of data
     */
    public static byte[] ripemd160(byte[] data) {
        Digest digest = new RIPEMD160Digest();
        if (data != null) {
            byte[] resBuf = new byte[digest.getDigestSize()];
            digest.update(data, 0, data.length);
            digest.doFinal(resBuf, 0);
            return resBuf;
        }
        throw new NullPointerException("Can't hash a NULL value");
    }

    /**
     * RIPEMD160(sha3(input))
     *
     * @param data data
     * @return hash of data
     */
    public static byte[] sha3Hash160(byte[] data) {
        byte[] sha3 = sha3(data);
        Digest digest = new RIPEMD160Digest();
        if (data != null) {
            byte[] resBuf = new byte[digest.getDigestSize()];
            digest.update(sha3, 0, sha3.length);
            digest.doFinal(resBuf, 0);
            return resBuf;
        }
        throw new NullPointerException("Can't hash a NULL value");
    }

    /**
     * random 32 byte hash
     *
     * @return
     */
    public static byte[] randomHash32() {
        byte[] randomHash = new byte[32];
        Random random = new Random();
        random.nextBytes(randomHash);
        return randomHash;
    }
}
