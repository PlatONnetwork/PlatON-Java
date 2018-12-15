package org.platon.core.block;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.platon.common.utils.Numeric;
import org.platon.core.block.proto.BlockProto;
import org.platon.core.transaction.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by alliswell on 2018/7/25.
 */
public class Block {

    private BlockHeader header;
    private byte[] protoData;
    private List<Transaction> transactions = new CopyOnWriteArrayList<>();

    private boolean parsed = false;

    public Block(long timestamp, byte[] author, byte[] coinbase, byte[] parentHash, byte[] bloomLog, long number,
                 BigInteger energonUsed, BigInteger energonCeiling, BigInteger difficulty, byte[] extraData) {
        this.header = new BlockHeader(timestamp, author,coinbase, parentHash, bloomLog,
                number, energonUsed, energonCeiling, difficulty, extraData);

    }

    private Block() {
    }

    public Block(byte[] protoBytes) {
        if (!parsed) {
            parse(protoBytes);
        }
    }

    public BigInteger getTotalDifficulty() {
        return this.header.getDifficulty();
    }

    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.header.setDifficulty(totalDifficulty);
    }

    public BlockHeader getBlockHeader() {
        return header;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setAuthor(byte[] author) {
        header.setAuthor(author);
    }

    public int getBlockSize() {
        if (null == protoData) {
            return 0;
        }
        return protoData.length;
    }

    public byte[] encode() {
        if (parsed || null != protoData) {
            return protoData;
        }
        BlockProto.Block.Builder bodyBuilder = BlockProto.Block.newBuilder();
        bodyBuilder.setHeaderbytes(ByteString.copyFrom(header.encode()));
        for (int i = 0; i < this.transactions.size(); ++i) {
            bodyBuilder.addTransactionbytes(ByteString.copyFrom(transactions.get(i).getEncoded()));
        }
        protoData = bodyBuilder.build().toByteArray();
        return protoData;
    }

    private void parse(byte[] bytes) {
        if (parsed)
            return;
        try {
            BlockProto.Block block = BlockProto.Block.parseFrom(bytes);
            this.header = new BlockHeader(block.getHeaderbytes().toByteArray());
            for (int i = 0; i < block.getTransactionbytesCount(); ++i) {
                this.transactions.add(new Transaction(block.getTransactionbytes(i).toByteArray()));
            }
            for (int i = 0; i < block.getUnclesbytesCount(); ++i) {
                //TODO: write uncles
            }
            for (int i = 0; i < block.getSignaturesCount(); ++i) {
                //TODO: write signatures
            }
            this.parsed = true;
            protoData = bytes;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public boolean isEqual(Block block) {
        return Arrays.areEqual(this.getBlockHeader().getHash(), block.getBlockHeader().getHash());
    }

    public boolean isGenesis() {
        return this.header.getNumber() == 0;
    }

    public boolean isParentOf(Block block) {
        return Arrays.areEqual(this.header.getHash(), block.header.getParentHash());
    }

    public String getShortHash() {
        String hexHash = Numeric.toHexString(this.header.getHash());
        return hexHash.substring(0, 6);
    }

    public String getShortDescr() {
        return "#" + this.header.getNumber() + " (" + Hex.toHexString(this.header.getHash()).substring(0, 6) + " <~ "
                + Hex.toHexString(this.header.getParentHash()).substring(0, 6) + ") Txs:" + getTransactions().size() + ".";
    }
}
