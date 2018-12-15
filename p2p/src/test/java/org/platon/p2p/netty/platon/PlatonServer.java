package org.platon.p2p.netty.platon;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.platon.common.config.ConfigProperties;
import org.platon.common.config.NodeConfig;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.crypto.ECKey;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.NodeContext;
import org.platon.p2p.NodeServer;
import org.platon.p2p.NodeServerChannelInitializer;
import org.platon.p2p.common.PeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PlatonServer {
    private static final Logger logger = LoggerFactory.getLogger(NodeServer.class);



    private EventLoopGroup bossGroup = new NioEventLoopGroup();


    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private void initContext(){
        NodeContext.host = NodeConfig.getInstance().getHost();
        NodeContext.port = PeerConfig.getInstance().getPort();
        NodeContext.privateKey = Numeric.hexStringToByteArray(NodeConfig.getInstance().getPrivateKey());
        NodeContext.publicKey = Numeric.hexStringToByteArray(NodeConfig.getInstance().getPublicKey());

        NodeContext.ecKey = ECKey.fromPrivate(NodeContext.privateKey );
        NodeContext.localNodeId = ByteString.copyFrom(WalletUtil.computeAddress(NodeContext.publicKey));

        NodeContext.timeIntervalForDuplicatedMessage = PeerConfig.getInstance().getTimeIntervalForDuplicatedMessage();

        ApplicationContext context = new AnnotationConfigApplicationContext(SpringContextUtil.class);

    }

    public void startup() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                shutdown();
            }
        });


        logger.info("starting Node server ...");

        initContext();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NodeServerChannelInitializer());

        ChannelFuture channelFuture = bootstrap.bind(NodeContext.host, NodeContext.port).syncUninterruptibly();

        channelFuture.channel().closeFuture().addListener( new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                shutdown();
            }
        });
    }

    private void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }


    public static void main(String[] args) throws Exception {
        ConfigProperties.setConfigPath("D:\\workspace\\Juzix-Platon\\platon\\p2p\\src\\test\\resources\\config\\node1");
        PlatonServer server1 = new PlatonServer();
        server1.startup();
    }
}
