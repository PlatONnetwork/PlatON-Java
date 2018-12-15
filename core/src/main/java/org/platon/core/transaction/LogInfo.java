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
package org.platon.core.transaction;


//import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.wrapper.DataWord;
import org.platon.core.transaction.proto.TransactionReceiptProto;
import org.platon.core.transaction.util.HashUtil;

import java.util.ArrayList;
import java.util.List;

import static org.platon.core.transaction.util.ByteUtil.toHexString;

/**
 * @author Roman Mandeleil
 * @since 19.11.2014
 */
public class LogInfo {

    byte[] address = new byte[]{};
    //todo need to define the correct type for list, ethereum use DataWord, what we really need? list<byte[]> ok?
    List<DataWord> topics = new ArrayList<>();
    byte[] data = new byte[]{};

    public LogInfo(byte[] protoEncode) {
        try {
            TransactionReceiptProto.LogEntry logEntry = TransactionReceiptProto.LogEntry.parseFrom(protoEncode);

            if (logEntry.getAddress() != null) {
                address = logEntry.getAddress().toByteArray();
            }

            if(logEntry.getTopicCount() > 0) {
                for(int i = 0; i < logEntry.getTopicCount(); i++) {
                    byte[] logTopic = logEntry.getTopic(i).toByteArray();
                    topics.add(DataWord.of(logTopic));
                }
            }

            if(logEntry.getData() != null) {
                data = logEntry.getData().toByteArray();
            }
        }  catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Proto buff decode log entry fail!");
        }
    }

    public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
        this.address = (address != null) ? address : new byte[]{};
        //todo what data type is needed for receipt log? List<byte[]> is ok?
        this.topics = (topics != null) ? topics : new ArrayList<>();
        this.data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return address;
    }

    public List<DataWord> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    /*  [address, [topic, topic ...] data] */
    public byte[] getEncoded() {
        TransactionReceiptProto.LogEntry.Builder logEntry = TransactionReceiptProto.LogEntry.newBuilder();

        if (address != null && address.length > 0) {
            logEntry.setAddress(ByteString.copyFrom(address));
        }

        if (topics !=  null && topics.size() > 0) {
            for (DataWord logTopic : topics) {
                logEntry.addTopic(ByteString.copyFrom(logTopic.getData()));
            }
        }

        if(data !=  null && data.length > 0) {
            logEntry.setData(ByteString.copyFrom(data));
        }

        return logEntry.build().toByteArray();
    }

    public TransactionReceiptProto.LogEntry getEncodedBuilder() {
        TransactionReceiptProto.LogEntry.Builder logEntry = TransactionReceiptProto.LogEntry.newBuilder();

        if (address != null && address.length > 0) {
            logEntry.setAddress(ByteString.copyFrom(address));
        }

        if (topics !=  null && topics.size() > 0) {
            for (DataWord logTopic : topics) {
                logEntry.addTopic(ByteString.copyFrom(logTopic.getData()));
            }
        }

        if(data !=  null && data.length > 0) {
            logEntry.setData(ByteString.copyFrom(data));
        }

        return logEntry.build();
    }

    public Bloom getBloom() {
        Bloom ret = Bloom.create(HashUtil.bcSHA3Digest256(address));
        for (DataWord topic : topics) {
            ret.or(Bloom.create(HashUtil.bcSHA3Digest256(topic.getData())));
        }
        return ret;
    }

    @Override
    public String toString() {

        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (DataWord topic : topics) {
            String topicStr = toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");

        return "LogInfo{" +
                "address=" + toHexString(address) +
                ", topics=" + topicsStr +
                ", data=" + toHexString(data) +
                '}';
    }

//    public static final MemSizeEstimator<LogInfo> MemEstimator = log ->
//            ByteArrayEstimator.estimateSize(log.address) +
//            ByteArrayEstimator.estimateSize(log.data) +
//            log.topics.size() * DataWord.MEM_SIZE + 16;
}
