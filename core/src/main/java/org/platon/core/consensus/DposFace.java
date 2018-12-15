package org.platon.core.consensus;

import org.platon.common.utils.ByteUtil;

import java.util.Vector;

/**
 * dpos related
 *
 * @author alliswell
 * @version 0.0.1
 * @date 2018/8/28 13:42
 */
public class DposFace {

    /**
     * primary node's address list
     */
    private Vector<byte[]> primaryList;
    /**
     * block number of last consensus cycle
     */
    private long lastCycleBlockNum = 0L;

    public DposFace() {
        this.primaryList = new Vector<>();
    }

    public long getLastCycleBlockNum() {
        return lastCycleBlockNum;
    }

    public void setLastCycleBlockNum(long lastCycleBlockNum) {
        this.lastCycleBlockNum = lastCycleBlockNum;
    }

    /**
     * TODO get the current primary node's address list
     *
     * @return
     */
    public Vector<byte[]> getCurrentPrimary() {
        return this.primaryList;
    }

    public boolean isPrimary(byte[] address) {
        return ByteUtil.contains(primaryList, address);
    }
}
