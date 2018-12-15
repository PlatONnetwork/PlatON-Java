package org.platon.p2p.pubsub;

import org.apache.http.util.Asserts;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author yangzhou
 * @create 2018-07-24 9:58
 */
public class TimeCache {
    private long span;

    private Map<String, Long> seen = new HashMap<>();
    private Queue<String> queue = new LinkedList<>();

    TimeCache(long span) {
        this.span = span;
    }

    public void add(String msg) {
        seen.put(msg, System.currentTimeMillis());
        queue.offer(msg);
        sweep();
    }

    public boolean has(String msg) {
        return seen.containsKey(msg);
    }

    public void sweep() {
        Long current = System.currentTimeMillis();

        while (true) {
            String msg = queue.peek();
            if (msg == null) {
                return;
            }
            Long time = seen.get(msg);
            Asserts.check(time != null, "inconsistent cache state");
            if (current - time > span) {
                queue.poll();
                seen.remove(msg);
            } else {
                return;
            }
        }
    }
//    public String dump() {
//        return JSON.toJSONString(seen, true);
//    }
}
