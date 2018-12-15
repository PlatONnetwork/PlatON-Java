package org.platon.core.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.wrapper.DataWord;
import org.platon.core.proto.DataWordMessage;

public final class DataWordCodec {

    public static DataWordMessage encode(DataWord dataWord) {
        if (null == dataWord) {
            return null;
        }
        DataWordMessage.Builder dataWordBuilder = DataWordMessage.newBuilder();
        dataWordBuilder.setData(ByteString.copyFrom(dataWord.getData()));
        return dataWordBuilder.build();
    }

    public static DataWord decode(DataWordMessage dataWordMessage) {
        return decode(dataWordMessage.toByteArray());
    }

    public static DataWord decode(byte[] data) {
        try {
            DataWordMessage message = DataWordMessage.parseFrom(data);
            return DataWord.of(message.getData().toByteArray());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

    }
}
