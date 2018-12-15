package org.platon.crypto;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

public class WalletUtilTest {

	private static final Logger logger = LoggerFactory.getLogger(WalletUtilTest.class);

	private static final String message = "welcome to platon";

	private static final String defWalletFileName = "5f2ae1c60a3038956cf3355960cb211de78bab50";

	private static final String defAddress = "5f2ae1c60a3038956cf3355960cb211de78bab50";

	private static final String walletFilePath = Thread.currentThread().getContextClassLoader().getResource("keys").getPath();

	@Test
	public void generateECKey() {
		ECKey ecKey = WalletUtil.generateECKey();
		logECKey(ecKey);
		Assert.assertNotNull(ecKey);
	}

	@Test
	public void signAndVerify() {
		try {
			byte[] messageHash = HashUtil.sha3(message.getBytes());
			logger.debug("messageHash:{}", Hex.toHexString(messageHash));
			ECKey ecKey = WalletUtil.loadWallet(walletFilePath + File.separator + defWalletFileName);
			logECKey(ecKey);
			byte[] sign = WalletUtil.sign(messageHash, ecKey);
			logger.debug("sign:{}", new String(sign));
			Assert.assertTrue(WalletUtil.verify(messageHash, sign, Hex.decode(defAddress)));
		} catch (Exception e) {
			logger.error("signAndVerify error:", e);
		}
	}

	/**
	 * get address from signature and message
	 *
	 * @throws SignatureException
	 */
	@Test
	public void signatureToAddress() throws SignatureException {
		ECKey ecKey = WalletUtil.loadWallet(walletFilePath + File.separator + defWalletFileName);
		logECKey(ecKey);
		byte[] messageHash = HashUtil.sha3(message.getBytes());
		byte[] sign = WalletUtil.sign(messageHash, ecKey);
		logger.debug("sign:{}", new String(sign));
		byte[] address = WalletUtil.signatureToAddress(messageHash, sign);
		Assert.assertArrayEquals(ecKey.getAddress(), address);
	}

	@Test
	public void signatureStringToAddress() throws SignatureException {
		ECKey ecKey = WalletUtil.loadWallet(walletFilePath + File.separator + defWalletFileName);
		logECKey(ecKey);
		byte[] messageHash = HashUtil.sha3(message.getBytes());
		byte[] sign = WalletUtil.sign(messageHash, ecKey);
		logger.debug("sign:{}", new String(sign));
		byte[] address = WalletUtil.signatureToAddress(messageHash, new String(sign));
		Assert.assertArrayEquals(ecKey.getAddress(), address);
	}

	@Test
	public void signatureCover() throws SignatureException {
		ECKey ecKey = WalletUtil.loadWallet(walletFilePath + File.separator + defWalletFileName);
		logECKey(ecKey);
		byte[] messageHash = HashUtil.sha3(message.getBytes());
		byte[] sign = WalletUtil.sign(messageHash, ecKey);
		logger.debug("sign:{}", new String(sign));
		ECKey.ECDSASignature signature = WalletUtil.signToECDSASignature(sign);
		byte[] sign2 = WalletUtil.signToByteArray(signature);
		Assert.assertArrayEquals(sign, sign2);
	}

	/**
	 * Open when you need to test, will generate a test wallet in the "test/resources/"
	 */
	@Test
	@Ignore
	public void generateWallet() {
		logger.info("walletFilePath:{}", walletFilePath);
		ECKey ecKey = WalletUtil.generateWallet(walletFilePath);
		logECKey(ecKey);
		Assert.assertNotNull(ecKey);
	}

	private void logECKey(ECKey ecKey) {
		logger.debug("address is:{}", Hex.toHexString(ecKey.getAddress()));
		logger.debug("privateKey is:{}", ecKey.getPrivKey());
		logger.debug("publicKey is:{}", ecKey.getPubKeyPoint().toString());
	}

	@Test
	public void encrypt(){
		ECKey ecKey = WalletUtil.loadWallet(walletFilePath + File.separator + defWalletFileName);
		byte[] initMessage = message.getBytes(StandardCharsets.UTF_8);
		byte[] encryptMessage = WalletUtil.encrypt(initMessage,ecKey);
		byte[] encryptMessageFromBytes = WalletUtil.encrypt(initMessage,ecKey.getPubKey());
		byte[] decryptMessage = WalletUtil.decrypt(encryptMessage,ecKey);
		Assert.assertArrayEquals(initMessage,decryptMessage);
		byte[] decryptMessageFromBytes = WalletUtil.decrypt(encryptMessageFromBytes,ecKey);
		Assert.assertArrayEquals(initMessage,decryptMessageFromBytes);
	}

}