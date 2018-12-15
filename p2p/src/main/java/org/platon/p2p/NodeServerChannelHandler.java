package org.platon.p2p;

import io.netty.channel.ChannelHandlerContext;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.proto.session.CreateSession;
import org.platon.p2p.session.CreateSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/8/20, lvxiaoyi, Initial Version.
 */
public class NodeServerChannelHandler extends P2pChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(NodeServerChannelHandler.class);

    public NodeServerChannelHandler() {
        super();
    }

    protected void handleCreateSessionRequest(ChannelHandlerContext ctx, CreateSession createSession){

        CreateSessionHandler createSessionHandler = SpringContextUtil.getBean("createSessionHandler");
        createSessionHandler.handleCreateSessionRequest(createSession, ctx);
    }
}
