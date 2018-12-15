package org.platon.p2p.common;

import com.google.protobuf.ByteString;
import org.platon.common.utils.Numeric;

public class CodecUtils {
    public static String toHexString(ByteString byteString){
        if(byteString==null || byteString.isEmpty()){
            return null;
        }
        return Numeric.toHexString(byteString.toByteArray());
    }
}
