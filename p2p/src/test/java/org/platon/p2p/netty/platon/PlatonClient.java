package org.platon.p2p.netty.platon;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.platon.common.config.ConfigProperties;
import org.platon.common.config.NodeConfig;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.crypto.ECKey;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.NodeClient;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.common.PeerConfig;
import org.platon.p2p.common.PlatonMessageHelper;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.proto.session.SayHello;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PlatonClient {
    private static Logger logger = LoggerFactory.getLogger(NodeClient.class);

    private static EventLoopGroup workerGroup;

    public PlatonClient() {
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
        b.handler(new PlatonClientChannelInitializer(remoteId, remoteNodePubKey));
        return b.connect();
    }

    public void shutdown() {
        logger.info("Shutdown NodeClient");
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }

    public static void main(String[] args) throws Exception {
        ConfigProperties.setConfigPath("D:\\workspace\\Juzix-Platon\\platon\\p2p\\src\\test\\resources\\config\\node2");
        NodeContext.host = NodeConfig.getInstance().getHost();
        NodeContext.port = PeerConfig.getInstance().getPort();
        NodeContext.privateKey = Numeric.hexStringToByteArray(NodeConfig.getInstance().getPrivateKey());
        NodeContext.publicKey = Numeric.hexStringToByteArray(NodeConfig.getInstance().getPublicKey());

        NodeContext.ecKey = ECKey.fromPrivate(NodeContext.privateKey );
        NodeContext.localNodeId = ByteString.copyFrom(WalletUtil.computeAddress(NodeContext.publicKey));

        NodeContext.timeIntervalForDuplicatedMessage = PeerConfig.getInstance().getTimeIntervalForDuplicatedMessage();
        ApplicationContext context = new AnnotationConfigApplicationContext(SpringContextUtil.class);

        PlatonClient client = new PlatonClient();
        byte[] serverPubKey = Numeric.hexStringToByteArray("0x044c73f76a65ada74e582fecfe576043657b61a71bdff3f21cf00e3a4f3a4a19212a7510efbe32c7d1f4d041b5cea3b1d0a5f3238af89f4f42b256b3d95702ec97");

        ByteString serverId = ByteString.copyFrom(WalletUtil.computeAddress(serverPubKey));
        ByteString pubKey = ByteString.copyFrom(serverPubKey);

        client.connect(serverId, "127.0.0.1", 11001, pubKey);

        sayHello(serverId, "abc");
        TimeUnit.SECONDS.sleep(10);
    }

    private static void sayHello(ByteString remoteNodeId, String hello) {

        NodeID nodeID = NodeID.newBuilder().setId(remoteNodeId).build();

        SayHello sayHello = SayHello.newBuilder().setNodeId(NodeContext.localNodeId).setHello(hello).setFeedback(true).build();

        MessageRouter messageRouter = SpringContextUtil.getBean("messageRouter");


        messageRouter.sendRequest(sayHello, nodeID, MessageRouter.ForwardingOptionType.DIRECT_CONNECTION, false);
    }

}
