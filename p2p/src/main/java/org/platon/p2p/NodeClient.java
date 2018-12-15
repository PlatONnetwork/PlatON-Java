package org.platon.p2p;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.common.PeerConfig;
import org.platon.p2p.common.PlatonMessageHelper;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/8/21, lvxiaoyi, Initial Version.
 */
public class NodeClient {

    private static Logger logger = LoggerFactory.getLogger(NodeClient.class);

    private static EventLoopGroup workerGroup;

    public NodeClient() {
        workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
            AtomicInteger cnt = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NodeClientWorker-" + cnt.getAndIncrement());
            }
        });
    }

    
    public boolean connect(ByteString remoteId, String host, int port, ByteString remoteNodePubKey){
        ChannelFuture channelFuture = connectAsync(remoteId, host, port, remoteNodePubKey).syncUninterruptibly();
        if(channelFuture.isDone() && channelFuture.isSuccess()){
            logger.debug("success to connect remote node (nodeId=:{}, address:={}:{}", CodecUtils.toHexString(remoteId), host, port);

            SessionManager sessionManager = SpringContextUtil.getBean("sessionManager");
            sessionManager.createSession(remoteId, remoteNodePubKey, channelFuture.channel());

            PlatonMessage platonMessage = PlatonMessageHelper.createCreateSession();


            channelFuture.channel().writeAndFlush(platonMessage);

            return true;
        }else{
            logger.error("cannot connect to remote node (nodeId=:{}, address:={}:{})", CodecUtils.toHexString(remoteId), host, port);
            return false;
        }
    }

    public ChannelFuture connectAsync(ByteString remoteId,String host, int port, ByteString remoteNodePubKey) {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, PeerConfig.getInstance().getPeerConnectTimeout()*1000);

        b.remoteAddress(host, port);
        b.handler(new NodeClientChannelInitializer(remoteId, remoteNodePubKey));
        return b.connect();
    }

    public void shutdown() {
        logger.info("Shutdown NodeClient");
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }
}
