package org.platon.p2p;

import org.platon.p2p.proto.platon.PlatonMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-23 13:59
 */
public class MessageHook {

    
    private static Map<String, MessageCallback> hooks = new ConcurrentHashMap();

    public static void add(String serviceName, MessageCallback hook){
        hooks.put(serviceName, hook);
    }

    public static void remove(String serviceName) {
        hooks.remove(serviceName);
    }


    
    public static boolean isNeedProcess(PlatonMessage request) {

        MessageCallback callback = hooks.get(request.getBody().getData().getTypeUrl());

        if (callback != null) {
            return callback.isNeedProcess(request);
        }
        return false;
    }

    public static MessageCallback get(String serviceName) {
        return hooks.get(serviceName);
    }

    public interface MessageCallback {
        boolean isNeedProcess(PlatonMessage request);
    }
}
