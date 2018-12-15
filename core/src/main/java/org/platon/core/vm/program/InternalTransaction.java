/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.platon.core.vm.program;

import org.bouncycastle.util.encoders.Hex;
import org.platon.common.utils.ByteUtil;
import org.platon.common.wrapper.DataWord;
import org.platon.core.transaction.Transaction;
import org.platon.core.transaction.proto.TransactionType;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

public class InternalTransaction extends Transaction {

    private byte[] parentHash;
    private int deep;
    private int index;
    private boolean rejected = false;
    private String note;

    public InternalTransaction(byte[] rawData) {
        super(rawData);
    }

    public InternalTransaction(byte[] parentHash, int deep, int index,byte[] sendAddress,String note,TransactionType type, byte[] value, byte[] receiveAddress,
                               long referenceBlockNum, byte[] referenceBlockHash, byte[] energonPrice,
                               byte[] energonLimit, byte[] data) {

        super(type,new BigInteger(value),receiveAddress,referenceBlockNum,referenceBlockHash,new BigInteger(energonPrice),new BigInteger(energonLimit),data);

        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.sendAddress = nullToEmpty(sendAddress);
        this.note = note;
        this.parsed = true;
    }

    private static byte[] getData(DataWord gasPrice) {
        return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
    }

    public void reject() {
        this.rejected = true;
    }


    public int getDeep() {
        rlpParse();
        return deep;
    }

    public int getIndex() {
        rlpParse();
        return index;
    }

    public boolean isRejected() {
        rlpParse();
        return rejected;
    }

    public String getNote() {
        rlpParse();
        return note;
    }

    @Override
    public byte[] getSender() {
        rlpParse();
        return sendAddress;
    }

    public byte[] getParentHash() {
        rlpParse();
        return parentHash;
    }

    @Override
    //TODO 需要调整成pb
    public byte[] getEncoded() {
        return null;
//        if (rlpEncoded == null) {
//
//            byte[] nonce = getNonce();
//            boolean isEmptyNonce = isEmpty(nonce) || (getLength(nonce) == 1 && nonce[0] == 0);
//
//            this.rlpEncoded = RLP.encodeList(
//                    RLP.encodeElement(isEmptyNonce ? null : nonce),
//                    RLP.encodeElement(this.parentHash),
//                    RLP.encodeElement(getSender()),
//                    RLP.encodeElement(getReceiveAddress()),
//                    RLP.encodeElement(getValue()),
//                    RLP.encodeElement(getGasPrice()),
//                    RLP.encodeElement(getGasLimit()),
//                    RLP.encodeElement(getData()),
//                    RLP.encodeString(this.note),
//                    encodeInt(this.deep),
//                    encodeInt(this.index),
//                    encodeInt(this.rejected ? 1 : 0)
//            );
//        }
//
//        return rlpEncoded;
    }

    @Override
    public byte[] getEncodedRaw() {
        return getEncoded();
    }

    //TODO 需要调整成pb
    public synchronized void rlpParse() {
//        if (parsed) return;
//        RLPList decodedTxList = RLP.decode2(rlpEncoded);
//        RLPList transaction = (RLPList) decodedTxList.get(0);
//
//        setNonce(transaction.get(0).getRLPData());
//        this.parentHash = transaction.get(1).getRLPData();
//        this.sendAddress = transaction.get(2).getRLPData();
//        setReceiveAddress(transaction.get(3).getRLPData());
//        setValue(transaction.get(4).getRLPData());
//        setGasPrice(transaction.get(5).getRLPData());
//        setGasLimit(transaction.get(6).getRLPData());
//        setData(transaction.get(7).getRLPData());
//        this.note = new String(transaction.get(8).getRLPData());
//        this.deep = decodeInt(transaction.get(9).getRLPData());
//        this.index = decodeInt(transaction.get(10).getRLPData());
//        this.rejected = decodeInt(transaction.get(11).getRLPData()) == 1;
//
//        this.parsed = true;
    }


    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    private static int bytesToInt(byte[] bytes) {
        return isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static int decodeInt(byte[] encoded) {
        return bytesToInt(encoded);
    }

    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + Hex.toHexString(getParentHash()) +
                ", hash=" + Hex.toHexString(getHash()) +
                ", gasPrice=" + getEnergonPrice().toString() +
                ", gas=" + getEnergonLimit().toString() +
                ", sendAddress=" + Hex.toHexString(getSender()) +
                ", receiveAddress=" + Hex.toHexString(getReceiveAddress()) +
                ", value=" + getValue().toString() +
                ", data=" + Hex.toHexString(getData()) +
                ", note=" + getNote() +
                ", deep=" + getDeep() +
                ", index=" + getIndex() +
                ", rejected=" + isRejected() +
                "]";
    }
}
