package org.platon.core.block;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.core.block.proto.BlockProto;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * block detail info
 * Created by alliswell on 2018/8/3.
 */

public class BlockInfo {
    /**
     * block height
     **/
    private long number;

    /**
     * only inuse when consensus is POW
     **/
    private BigInteger totalDifficulty;

    /**
     * parent hash
     **/
    private byte[] parentHash;

    /**
     * children block in bytes
     **/
    private ArrayList<byte[]> children;

    private ArrayList<byte[]> bloomLogs;

    private ArrayList<byte[]> txReceipts;

    private boolean parsed = false;

    public BlockInfo(byte[] bytes) {
        init();
        if (!parsed) {
            parse(bytes);
        }
    }

    public BlockInfo() {
        init();
    }

    private void init() {
        this.children = new ArrayList<>();
        this.bloomLogs = new ArrayList<>();
        this.txReceipts = new ArrayList<>();
        this.totalDifficulty = BigInteger.ZERO;
    }

    public boolean isNull() {
        return number == -1;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public void setParentHash(byte[] parentHash) {
        this.parentHash = parentHash;
    }

    public void addChild(byte[] child) {
        this.children.add(child);
    }

    public ArrayList<byte[]> getBloomLogs() {
        return bloomLogs;
    }

    public void addBloomLogs(byte[] bloomlog) {
        this.bloomLogs.add(bloomlog);

    }

    public long getNumber() {
        return number;
    }

    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public ArrayList<byte[]> getChildren() {
        return children;
    }

    public byte[] encode() {
        BlockProto.BlockInfo.Builder infoBuilder = BlockProto.BlockInfo.newBuilder();

        infoBuilder.setNumber(this.number);
        if (null != this.parentHash) {
            infoBuilder.setParentHash(ByteString.copyFrom(this.parentHash));
        }
        for (int i = 0; i < children.size(); ++i) {
            infoBuilder.addChildren(ByteString.copyFrom(children.get(i)));
        }
        for (int i = 0; i < bloomLogs.size(); ++i) {
            infoBuilder.addBloomlog(ByteString.copyFrom(bloomLogs.get(i)));
        }
//		for(int i=0; i<txReceipts.size(); ++i) {
//			infoBuilder.addTxreceipts(ByteString.copyFrom(txReceipts.get(i)));
//		}

        return infoBuilder.build().toByteArray();
    }

    private void parse(byte[] bytes) {
        try {
            BlockProto.BlockInfo blockInfo = BlockProto.BlockInfo.parseFrom(bytes);

            this.number = blockInfo.getNumber();

            byte[] byteDifficulty = blockInfo.getTotalDifficulty().toByteArray();
            if (byteDifficulty != null && byteDifficulty.length > 0) {
                this.totalDifficulty = new BigInteger(blockInfo.getTotalDifficulty().toByteArray());
            }

            if (blockInfo.getParentHash() != null) {
                this.parentHash = blockInfo.getParentHash().toByteArray();
            }
            for (int i = 0; i < blockInfo.getChildrenCount(); ++i) {
                this.children.add(blockInfo.getChildren(i).toByteArray());
            }
            for (int i = 0; i < blockInfo.getBloomlogCount(); ++i) {
                this.bloomLogs.add(blockInfo.getBloomlog(i).toByteArray());
            }
//			for(int i=0; i<blockInfo.getTxreceiptsCount(); ++i) {
//				this.txReceipts.add(blockInfo.getTxreceipts(i).toByteArray());
//			}
            parsed = true;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}

