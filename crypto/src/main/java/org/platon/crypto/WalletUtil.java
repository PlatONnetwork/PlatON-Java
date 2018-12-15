package org.platon.crypto;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.platon.crypto.domain.WalletJson;
import org.platon.crypto.jce.ECKeyFactory;
import org.platon.crypto.jce.NewBouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.*;
import java.security.SignatureException;
import java.util.Arrays;

import static org.platon.crypto.ECKey.CURVE;

public class WalletUtil {

    private static final Logger logger = LoggerFactory.getLogger(WalletUtil.class);

    /**
     * generateECKey
     * @return
     */
    public static ECKey generateECKey(){
       return new ECKey();
    }

    /**
     * Temporary generateWallet
     * @param walletPath
     * @return
     */
    public static ECKey generateWallet(String walletPath){
        return generateWallet(walletPath,null);
    }

    /**
     * Temporary generateWallet
     * @param walletPath
     * @param walletName if walletName is null,walletName is address
     * @return
     * TODO : Wallet must rewrite
     */
    public static ECKey generateWallet(String walletPath,String walletName){
        try {
            //generate eckey
            ECKey eckey = generateECKey();
            //generate walletJsonStr
            WalletJson walletJson = new WalletJson();
            walletJson.setAddress(Hex.toHexString(eckey.getAddress()));
            walletJson.setPrivateKey(Hex.toHexString(eckey.getPrivKeyBytes()));
            walletJson.setPublicKey(Hex.toHexString(eckey.getPubKey()));
            String walletJsonStr = JSON.toJSONString(walletJson);
            //generate walletFile
            String walletUrl;
            if(StringUtils.isNotBlank(walletName)){
                walletUrl = walletPath+File.separator+walletName;
            }else{
                walletUrl = walletPath+File.separator+walletJson.getAddress();
            }
            File file = new File(walletUrl);
            if (!file.exists()) {
                file.createNewFile();
            }
            //write walletFile
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(walletJsonStr);
            bufferedWriter.flush();
            bufferedWriter.close();
            return eckey;
        } catch (Exception e) {
            logger.error("generate Wallet error:",e);
            return null;
        }
    }

    /**
     * Temporary loadWallet
     * @param walletUrl
     * @return
     * TODO : Wallet must rewrite
     */
    public static ECKey loadWallet(String walletUrl){
        //get walletFile
        File file = new File(walletUrl);
        String walletJsonStr;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuffer stringBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                stringBuffer.append(line);
            }
            bufferedReader.close();
            //analyze walletJson
            walletJsonStr = stringBuffer.toString();
            WalletJson walletJson = JSON.parseObject(walletJsonStr,WalletJson.class);
            byte[] priv = Hex.decode(walletJson.getPrivateKey());
            byte[] pub = Hex.decode(walletJson.getPublicKey());
            //generate ECKey
            return ECKey.fromPrivateAndPrecalculatedPublic(priv,pub);
        } catch (Exception e) {
            logger.error("loadWallet error:",e);
            return null;
        }

    }

    /**
     * signature message
     * @param messageHash
     * @param ecKey
     * @return base64 sign byte[]
     */
    public static byte[] sign(byte[] messageHash,ECKey ecKey){
        return Base64.encode(ecKey.sign(messageHash).toByteArray());
    }

    public static byte[] sign(byte[] messageHash,byte chainId, ECKey ecKey){
        ECKey.ECDSASignature signature = ecKey.sign(messageHash);
        byte tv = signature.v;
        byte v = (byte) (tv + (chainId << 1) + 8);
        signature.v = v;
        return Base64.encode(signature.toByteArray());
    }

    /**
     * signature message
     * @param messageHash
     * @param ecKey
     * @return ECDSASignature
     */
    public static ECKey.ECDSASignature signature(byte[] messageHash, ECKey ecKey){
        return ecKey.sign(messageHash);
    }

    /**
     * signature verify
     * @param messageHash
     * @param signature
     * @param address
     * @return -true is verify passed,false is verify failed
     */
    public static boolean verify(byte[] messageHash, byte[] signature, byte[] address) {
        try {
            byte[] signAddress = WalletUtil.signatureToAddress(messageHash,signature);
            return Arrays.equals(signAddress,address);
        } catch (SignatureException e) {
            logger.error("verify error:",e);
            return false;
        }
    }

    /**
     * Compute the address of the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param signatureBase64 Base-64 encoded signature you can use new String(signature) take a signatureBase64
     * @return 20-byte address
     */
    public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64) throws SignatureException {
        return computeAddress(signatureToPubKeyBytes(messageHash, signatureBase64.getBytes()));
    }

    /**
     * Compute the address of the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param signature Base-64 encoded signature
     * @return 20-byte address
     */
    public static byte[] signatureToAddress(byte[] messageHash, byte[] signature) throws SignatureException {
        return computeAddress(signatureToPubKeyBytes(messageHash, signature));
    }

    /**
     * Compute the address of the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param signature Base-64 encoded signature
     * @return 20-byte address
     */
    public static byte[] signatureToAddress(byte[] messageHash, ECKey.ECDSASignature signature) throws SignatureException {
        return computeAddress(signatureToPubKeyBytes(messageHash, signature));
    }

    /**
     * signatureToPubKeyBytes
     * @param messageHash
     * @param signatureEncoded
     * @return
     * @throws SignatureException
     */
    public static byte[] signatureToPubKeyBytes(byte[] messageHash, byte[] signatureEncoded) throws SignatureException {
        try {
            signatureEncoded = Base64.decode(signatureEncoded);
        } catch (RuntimeException e) {
            throw new SignatureException("Could not decode base64", e);
        }
        if (signatureEncoded.length < 65)
            throw new SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);

        return signatureToPubKeyBytes(
                messageHash,
                ECKey.ECDSASignature.fromComponents(
                        Arrays.copyOfRange(signatureEncoded, 1, 33),
                        Arrays.copyOfRange(signatureEncoded, 33, 65),
                        (byte) (signatureEncoded[0] & 0xFF)));
    }

    /**
     * get publicKey from signature
     * @param messageHash
     * @param sig
     * @return
     * @throws SignatureException
     */
    private static byte[] signatureToPubKeyBytes(byte[] messageHash, ECKey.ECDSASignature sig) throws SignatureException {
        int header = sig.v;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        //                  0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34)
            throw new SignatureException("Header byte out of range: " + header);
        if (header >= 31) {
            header -= 4;
        }
        int recId = header - 27;
        byte[] key = ECKey.recoverPubBytesFromSignature(recId, sig, messageHash);
        if (key == null)
            throw new SignatureException("Could not recover public key from signature");
        return key;
    }

    /**
     * Compute an address from an encoded public key.
     * @param pubBytes
     * @return 20-byte address
     */
    public static byte[] computeAddress(byte[] pubBytes) {
        return HashUtil.sha3Hash160(pubBytes);
    }

    /**
     * ECDSASignature sign cover
     * @param signature
     * @return
     */
    public static byte[] signToByteArray(ECKey.ECDSASignature signature){
        return  Base64.encode(signature.toByteArray());
    }

    /**
     * byte[] sign cover ECDSASignature
     * @param signatureByteArray
     * @return
     */
    public static ECKey.ECDSASignature signToECDSASignature(byte[] signatureByteArray){
        return ECKey.ECDSASignature.toSignature(Base64.decode(signatureByteArray));
    }

    public static byte[] encrypt(byte[] message,byte[] publicKey){
        return ECIESCoder.encrypt(ECKey.CURVE.getCurve().decodePoint(publicKey), message);
    }

    public static byte[] encrypt(byte[] message,ECKey ecKey){
        return ECIESCoder.encrypt(ecKey.pub, message);
    }

    public static byte[] decrypt(byte[] encryptMessage,ECKey ecKey){
        try{
            return ECIESCoder.decrypt(ecKey.getPrivKey(),encryptMessage);
        }catch (Exception e){
            logger.error("decrypt error",e);
            return null;
        }
    }



}
