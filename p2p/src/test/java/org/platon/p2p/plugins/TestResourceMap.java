package org.platon.p2p.plugins;

import com.google.protobuf.ByteString;
import org.apache.http.util.Asserts;
import org.junit.Test;
import org.platon.p2p.proto.common.ResourceID;

/**
 * @author yangzhou
 * @create 2018-07-23 10:49
 */
public class TestResourceMap {
    @Test
    public void TestAdd(){
        ResourceMap resourceMap = new ResourceMap();
        ResourceID resourceID = ResourceID.newBuilder().setId(ByteString.copyFrom("hello".getBytes())).build();
        ResourceID resourceID2 = ResourceID.newBuilder().setId(ByteString.copyFrom("hello1".getBytes())).build();

        resourceMap.add(resourceID);
        resourceMap.add(resourceID2);

        Asserts.check(resourceMap.isExist(resourceID), "is exist fail");
        Asserts.check(resourceMap.remove(resourceID), "is removeSessionFuture fail");
    }
}
