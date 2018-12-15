package org.platon.p2p;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.platon.common.cache.DelayCache;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.PlatonMessageHelper;
import org.platon.p2p.handler.PlatonMessageHandlerContext;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.proto.session.CreateSession;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;


public abstract class P2pChannelHandler extends SimpleChannelInboundHandler<PlatonMessage> {

    private static final Logger logger = LoggerFactory.getLogger(P2pChannelHandler.class);
    private static DelayCache<String, Boolean> txIdCache = new DelayCache<>();

    protected ByteString remoteNodeId;

    public ByteString getRemoteNodeId() {
        return remoteNodeId;
    }

    public void setRemoteNodeId(ByteString remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    protected abstract void handleCreateSessionRequest(ChannelHandlerContext ctx, CreateSession createSession);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("");
    }


    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);


        if(remoteNodeId!=null) {
            SessionManager sessionManager = SpringContextUtil.getBean("sessionManager");
            sessionManager.closeSession(remoteNodeId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PlatonMessage msg) throws Exception {
        logger.debug("Received PlatonMessage:{}", msg);

        Header header = msg.getHeader();
        Any any =  msg.getBody().getData();


        String messageName = StringUtils.substringAfterLast(any.getTypeUrl(), "/");


        String messageSimpleName = StringUtils.substringAfterLast(any.getTypeUrl(), ".");


        String tx_id = header.getTxId();





        List<RoutableID> destIdList = header.getDestList();

        if(PlatonMessageHelper.arrived(destIdList, NodeContext.localNodeId)){

            String uniqueTxId = StringUtils.trimToEmpty(tx_id) + "_" + messageSimpleName;
            if (txIdCache.get(uniqueTxId) == null) {
                txIdCache.put(uniqueTxId, true, NodeContext.timeIntervalForDuplicatedMessage, TimeUnit.SECONDS);
            } else {
                logger.warn("Discarding the duplicated request, tthe unique msg nodeId:={}", uniqueTxId);
                return;
            }


            if(StringUtils.equalsIgnoreCase(messageSimpleName, "CreateSession")){
                handleCreateSessionRequest(ctx, any.unpack(CreateSession.class));
            }else{

                Class messageClz = Class.forName(messageName);
                Object message = any.unpack(messageClz);

                Object handler = PlatonMessageHandlerContext.getInstance().getHandler(messageSimpleName);
                if (handler != null) {
                    Method method = PlatonMessageHandlerContext.getInstance().getMethod(messageSimpleName);

                    method.invoke(handler, message, HeaderHelper.build(header));
                }
            }

            if (CollectionUtils.isNotEmpty(destIdList) && destIdList.size() > 1) {
                forwardPlatonMessage(header, any, NodeContext.localNodeId);
            }
        }else if (header.getTtl() > 0) {



            String uniqueTxId = StringUtils.trimToEmpty(tx_id) + "_" + messageName + "_" + destIdList.get(0).getId().toStringUtf8();
            if (txIdCache.get(uniqueTxId) == null) {
                txIdCache.put(uniqueTxId, true, NodeContext.timeIntervalForDuplicatedMessage, TimeUnit.SECONDS);

                logger.debug("Just forward request to next node ...");

                forwardPlatonMessage(header, any, NodeContext.localNodeId);
            } else {
                logger.warn("Discarding the duplicated passing request, the unique msg nodeId:={}", uniqueTxId);
            }
        }else{
            logger.debug("Do not forward request to next node, because TTL runs out ...");
        }
    }


    private void forwardPlatonMessage(Header header, Any any, ByteString localNodeId){
        MessageRouter messageRouter = SpringContextUtil.getBean("messageRouter");
        messageRouter.forwardPlatonMessage(header, any, localNodeId);
    }
}
