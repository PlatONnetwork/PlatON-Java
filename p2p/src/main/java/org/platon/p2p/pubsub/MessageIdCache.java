package org.platon.p2p.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yangzhou
 * @create 2018-08-01 16:08
 */
public class MessageIdCache {

    private final Map<String, Set<String>> msgCache = new HashMap<>();

    MessageIdCache() {

    }


    void add(String topic, String msgId) {
        synchronized (msgCache) {

            Set<String> msgSet = msgCache.get(topic);
            if (msgSet == null) {
                msgSet = new HashSet<>();
            }

            msgSet.add(msgId);
            msgCache.put(topic, msgSet);
        }
    }

    Set<String> get(String topic) {
        synchronized (msgCache) {
            return msgCache.get(topic);
        }
    }

    void sweep(){
        synchronized (msgCache) {
            msgCache.clear();
        }
    }

    String dump() {
        StringBuilder stringBuilder = new StringBuilder();
        synchronized (msgCache) {
            for (Map.Entry<String, Set<String>> entry : msgCache.entrySet()) {
                stringBuilder.append(String.format("topic:%s size:%d,", entry.getKey(), entry.getValue().size()));
            }
        }
        return stringBuilder.toString();
    }
}
