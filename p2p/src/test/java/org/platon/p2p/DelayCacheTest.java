package org.platon.p2p;

import com.google.protobuf.ByteString;
import org.platon.common.cache.DelayCache;
import org.platon.common.utils.Numeric;
import org.platon.p2p.common.CodecUtils;

import java.util.concurrent.TimeUnit;

public class DelayCacheTest {
    public static void main(String[] args) throws InterruptedException {
        DelayCache<ByteString, Integer> testCache = new DelayCache<>();
        testCache.setTimeoutCallback((key, value) ->{
            System.out.println("key:" + CodecUtils.toHexString((ByteString)key));
            System.out.println("value:" + value);
        });

        byte[] bytes = Numeric.hexStringToByteArray("0x9f6d816d91405c36d3c408f2bcdae97f5e5df182");

        testCache.put(ByteString.copyFrom(bytes), 1, 3, TimeUnit.SECONDS);

        testCache.remove(ByteString.copyFrom(bytes));

        TimeUnit.SECONDS.sleep(100);


    }
}
