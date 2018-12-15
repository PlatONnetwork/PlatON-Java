package org.platon.p2p;

import com.google.protobuf.Any;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.platon.Header;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-26 18:17
 */
public class ForwardMessageHook {

    private static Map<String, ForwardMessageCallback> hooks = new ConcurrentHashMap();

    public static void add(String serviceName, ForwardMessageHook.ForwardMessageCallback hook){
        hooks.put(serviceName, hook);
    }

    public static void remove(String serviceName) {
        hooks.remove(serviceName);
    }


    
    public static List<NodeID> nextHops(Header header, Any any) {
        ForwardMessageHook.ForwardMessageCallback callback = hooks.get(any.getTypeUrl());

        if (callback != null) {
            return callback.nextHops(header, any);
        }
        return null;
    }

    public static ForwardMessageHook.ForwardMessageCallback get(String serviceName) {
        return hooks.get(serviceName);
    }

    public interface ForwardMessageCallback {
        List<NodeID> nextHops(Header header, Any any);
    }
}
