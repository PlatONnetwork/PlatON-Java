package org.platon.p2p.session;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author yangzhou
 * @create 2018-07-23 17:51
 */
public class SessionNotify {
    private static final Logger logger = LoggerFactory.getLogger(SessionNotify.class);

    private final static Set<SessionNotifyCallback> sessionListeners = new CopyOnWriteArraySet<>();


    public static void addListener(SessionNotifyCallback listener){
        logger.debug("addSessionFuture session notify serviceName:{}", listener);
        sessionListeners.add(listener);
    }

    public static  void removeListener(SessionNotifyCallback listener){
        sessionListeners.remove(listener);
    }

    public static void createNotify(ByteString remoteNodeId) {
        for (SessionNotifyCallback callback : sessionListeners) {
            callback.create(remoteNodeId);
        }
    }

    public static void closeNotify(ByteString remoteNodeId) {
        for (SessionNotifyCallback callback : sessionListeners) {
            callback.close(remoteNodeId);
        }
    }

    public interface SessionNotifyCallback{
        void create(ByteString remoteNodeId);
        void close(ByteString remoteNodeId);
    }
}
