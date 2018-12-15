package org.platon.p2p.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * @author yangzhou
 * @create 2018-08-22 14:42
 */
public class ProtoBufHelper {
    public static String getTypeNameFromTypeUrl(
            java.lang.String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        return pos == -1 ? "" : typeUrl.substring(pos + 1);
    }

    public static  <T extends com.google.protobuf.Message> String getFullName(Class<T> clazz) {
        return (Internal.getDefaultInstance(clazz)).getDescriptorForType().getFullName();
    }



    public static ByteString encodeIceData(Message v) {
        return v.toByteString();
    }



    public static <T extends Message> T decodeIceData(ByteString buf, com.google.protobuf.Parser<T> parser) {
        T data = null;
        try {
            data = parser.parseFrom(buf);
        } catch (InvalidProtocolBufferException e) {
            data = null;
        }
        return data;
    }

    public static <T extends Message> T decodeIceData(byte[] buf, com.google.protobuf.Parser<T> parser) {
        T data = null;
        try {
            data = parser.parseFrom(buf);
        } catch (InvalidProtocolBufferException e) {
            data = null;
        }
        return data;
    }
}
