package org.platon.p2p.session;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import org.platon.p2p.common.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/4/28, lvxiaoyi, Initial Version.
 */
public class Session {

    private static final Logger logger = LoggerFactory.getLogger(Session.class);



    private ByteString remoteNodeId;

    private Channel connection;

    private Date timestamp;

    public ByteString getRemoteNodeId() {
        return remoteNodeId;
    }

    public void setRemoteNodeId(ByteString remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    public Channel getConnection() {
        return connection;
    }

    public void setConnection(Channel connection) {
        this.connection = connection;
     }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(remoteNodeId, session.remoteNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteNodeId);
    }

    public void refresh(){
        timestamp = new Date();
    }

    public void destroy(){
        connection.close();
    }


    @Override
    public String toString() {
        return "Session{" +
                "remoteNodeId='" + CodecUtils.toHexString(remoteNodeId) + '\'' +
                ", connection=" + connection +
                '}';
    }
}
