package org.platon.core.keystore;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manages real accounts
 *
 * @author alliswell
 * @version 0.0.1
 * @date 2018/8/30 18:01
 */
public class AccountHolder {

    @Autowired
    private Keystore keystore;

    /**
     * beneficiary of mining
     */
    private byte[] miner;

    /**
     * node's address
     */
    private byte[] nodeAddress;

    public String[] accounts() {
        return keystore.listStoredKeys();
    }

    public byte[] getMiner() {
        if (null == miner || 0 == miner.length) {
            miner = accounts()[0].getBytes();
        }
        return miner;
    }

    public void setMiner(byte[] miner) {
        this.miner = miner;
    }

    public byte[] getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(byte[] nodeAddress) {
        this.nodeAddress = nodeAddress;
    }
}
