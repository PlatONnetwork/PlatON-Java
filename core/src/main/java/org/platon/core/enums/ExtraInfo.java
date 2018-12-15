package org.platon.core.enums;

/**
 * Created by alliswell on 2018/8/8.
 */
public enum ExtraInfo {
	BLOCKINFO((byte)1),
	LOGBLOOM((byte)2),
	RCECIPTS((byte)3),
	TXPOS((byte)4),
	BLOCKHASH((byte)5);
	private byte value;

	ExtraInfo(byte value) {
		this.value = value;
	}
	public byte getValue() {
		return value;
	}
}
