package org.platon.core.transaction;

import java.math.BigInteger;

/** transaction execute result
 * Created by alliswell on 2018/8/2.
 */
public class ExecuteResult {
	private BigInteger energonUsed;
	byte[] contractAddress;
	byte[] output;
	BigInteger energonRefunded;
	int depositSize;
	BigInteger energonForDeposit;

	public ExecuteResult() {
	}

	public void setEnergonUsed(BigInteger energonUsed) {
		this.energonUsed = energonUsed;
	}

	public void setContractAddress(byte[] contractAddress) {
		this.contractAddress = contractAddress;
	}

	public void setOutput(byte[] output) {
		this.output = output;
	}

	public void setEnergonRefunded(BigInteger energonRefunded) {
		this.energonRefunded = energonRefunded;
	}

	public void setDepositSize(int depositSize) {
		this.depositSize = depositSize;
	}

	public void setEnergonForDeposit(BigInteger energonForDeposit) {
		this.energonForDeposit = energonForDeposit;
	}

	public BigInteger getEnergonUsed() {
		return energonUsed;
	}

	public byte[] getContractAddress() {
		return contractAddress;
	}

	public byte[] getOutput() {
		return output;
	}

	public BigInteger getEnergonRefunded() {
		return energonRefunded;
	}

	public int getDepositSize() {
		return depositSize;
	}

	public BigInteger getEnergonForDeposit() {
		return energonForDeposit;
	}
}
