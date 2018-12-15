package org.platon.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.proto.BaseProto;
import org.platon.common.utils.ByteArrayWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * base encode and decode by protoBuf
 * @author yanze
 */
public class BasicPbCodec {

	public static byte[] encodeBytesList(byte[]... elements){
		if(elements == null){
			return null;
		}
		BaseProto.BaseBytesList.Builder builder = BaseProto.BaseBytesList.newBuilder();
		for(byte[] element : elements){
			builder.addBytesListData(ByteString.copyFrom(element));
		}
		return builder.build().toByteArray();
	}

	public static byte[] encodeBytesList(List<ByteArrayWrapper> bytesList){
		if(bytesList == null){
			return null;
		}
		BaseProto.BaseBytesList.Builder builder = BaseProto.BaseBytesList.newBuilder();
		for(int i = 0;i<bytesList.size();i++){
			builder.addBytesListData(ByteString.copyFrom(bytesList.get(i).getData()));
		}
		return builder.build().toByteArray();
	}

	public static List<ByteArrayWrapper> decodeBytesList(byte[] protoBuf) throws InvalidProtocolBufferException {
		BaseProto.BaseBytesList baseBytesList = BaseProto.BaseBytesList.parseFrom(protoBuf);
		List<ByteArrayWrapper> list = new ArrayList<>();
		for(int i = 0;i<baseBytesList.getBytesListDataCount();i++){
			list.add(new ByteArrayWrapper(baseBytesList.getBytesListData(i).toByteArray()));
		}
		return list;
	}

	public static byte[] encodeInt(int data){
		BaseProto.BaseInt.Builder builder = BaseProto.BaseInt.newBuilder();
		builder.setIntData(data);
		return builder.build().toByteArray();
	}

	public static int decodeInt(byte[] protoBuf) throws InvalidProtocolBufferException{
		BaseProto.BaseInt baseInt = BaseProto.BaseInt.parseFrom(protoBuf);
		return baseInt.getIntData();
	}

	public static byte[] encodeLong(long data){
		BaseProto.BaseLong.Builder builder = BaseProto.BaseLong.newBuilder();
		builder.setLongData(data);
		return builder.build().toByteArray();
	}

	public static long decodeLong(byte[] protoBuf) throws InvalidProtocolBufferException{
		BaseProto.BaseLong baseLong = BaseProto.BaseLong.parseFrom(protoBuf);
		return baseLong.getLongData();
	}

	public static byte[] encodeString(String data){
		if(data == null){
			return null;
		}
		BaseProto.BaseString.Builder builder = BaseProto.BaseString.newBuilder();
		builder.setStringData(data);
		return builder.build().toByteArray();
	}

	public static String decodeString(byte[] protoBuf) throws InvalidProtocolBufferException{
		BaseProto.BaseString baseString = BaseProto.BaseString.parseFrom(protoBuf);
		return baseString.getStringData();
	}

}
