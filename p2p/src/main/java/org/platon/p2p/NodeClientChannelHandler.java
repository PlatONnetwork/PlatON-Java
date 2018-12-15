package org.platon.p2p;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.proto.session.CreateSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeClientChannelHandler extends P2pChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(NodeClientChannelHandler.class);

    private boolean isHandshakeDone = false;


    private ByteString remoteNodePubKey;

    public NodeClientChannelHandler(ByteString remoteNodeId, ByteString remoteNodePubKey) {
        super();
        this.remoteNodeId = remoteNodeId;
        this.remoteNodePubKey = remoteNodePubKey;
    }


    @Override
    protected void handleCreateSessionRequest(ChannelHandlerContext ctx, CreateSession createSession) {

    }

    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channel is active for:" + CodecUtils.toHexString(remoteNodeId));
    }
}
