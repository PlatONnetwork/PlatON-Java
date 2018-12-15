package org.platon.common;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class BasicPbCodecTest {

	private final byte[] bytes0 = "test0".getBytes();
	private final byte[] bytes1 = "test1".getBytes();
	private final byte[] bytes2 = "test2".getBytes();
	private final int intData = RandomUtils.randomInt(100);
	private final long longData = RandomUtils.randomInt(1000000);
	private final String stringData = "platon test";

	@Test
	public void encodeBytesList() throws InvalidProtocolBufferException {
		byte[] result = BasicPbCodec.encodeBytesList(bytes0,bytes1,bytes2);
		List<ByteArrayWrapper> bytesList = new ArrayList<>();
		bytesList.add(new ByteArrayWrapper(bytes0));
		bytesList.add(new ByteArrayWrapper(bytes1));
		bytesList.add(new ByteArrayWrapper(bytes2));
		List<ByteArrayWrapper> decodeList = BasicPbCodec.decodeBytesList(result);
		for(int i=0;i<decodeList.size();i++){
			Assert.assertEquals(bytesList.get(i),bytesList.get(i));
		}
		byte[] result1 = BasicPbCodec.encodeBytesList(bytesList);
		Assert.assertArrayEquals(result,result1);
	}

	@Test
	public void encodeInt() throws InvalidProtocolBufferException {
		byte[] result = BasicPbCodec.encodeInt(intData);
		int intResult = BasicPbCodec.decodeInt(result);
		Assert.assertEquals(intData,intResult);
	}

	@Test
	public void encodeLong() throws InvalidProtocolBufferException {
		byte[] result = BasicPbCodec.encodeLong(longData);
		long longResult = BasicPbCodec.decodeLong(result);
		Assert.assertEquals(longData,longResult);
	}

	@Test
	public void encodeString() throws InvalidProtocolBufferException {
		byte[] result = BasicPbCodec.encodeString(stringData);
		String stringResult = BasicPbCodec.decodeString(result);
		Assert.assertEquals(stringData,stringResult);
	}

}