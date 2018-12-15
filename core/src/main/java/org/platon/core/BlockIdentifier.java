package org.platon.core;

import com.google.protobuf.ByteString;
import org.platon.common.utils.Numeric;
import org.platon.core.proto.BlockIdentifierMessage;

public class BlockIdentifier {

    private byte[] hash;

    private long number;

    public BlockIdentifier(BlockIdentifierMessage pbMessage) {
        this.hash = pbMessage.getHash().toByteArray();
        this.number = pbMessage.getNumber();
    }

    public BlockIdentifier(byte[] hash, long number) {
        this.hash = hash;
        this.number = number;
    }

    public byte[] getHash() {
        return hash;
    }

    public long getNumber() {
        return number;
    }

    public byte[] getEncoded() {
        BlockIdentifierMessage.Builder builder = BlockIdentifierMessage.newBuilder();
        builder.setHash(ByteString.copyFrom(hash));
        builder.setNumber(number);
        return builder.build().toByteArray();
    }

    @Override
    public String toString() {
        return "BlockIdentifier {" +
                "hash=" + Numeric.toHexString(hash) +
                ", number = " + number +
                '}';
    }
}
