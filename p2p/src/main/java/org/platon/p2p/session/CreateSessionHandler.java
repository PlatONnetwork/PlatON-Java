package org.platon.p2p.session;


import io.netty.channel.ChannelHandlerContext;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.proto.session.CreateSession;
import org.platon.p2p.proto.session.SayHello;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lvxy
 * @version 0.0.1
 * @date 2018/8/27 11:17
 */
@Component("createSessionHandler")
public class CreateSessionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CreateSessionHandler.class);

    @Autowired
    private SessionManager sessionManager;
    
    public void handleCreateSessionRequest(CreateSession createSession, ChannelHandlerContext ctx) {
        sessionManager.handleCreateSessionRequest(createSession.getClientNodeId(), createSession.getEndpoint(), createSession.getMessageHash(), createSession.getSignature(), ctx.channel());
    }


    @PlatonMessageType("SayHello")
    public void sayHello(SayHello sayHello, HeaderHelper header){
        sessionManager.sayHello(sayHello.getNodeId(), sayHello.getHello(), sayHello.getFeedback());
    }


}
