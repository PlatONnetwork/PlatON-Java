package org.platon.p2p;

import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.platon.common.config.NodeConfig;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.crypto.ECKey;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.common.CodecUtils;
import org.platon.p2p.common.PeerConfig;
import org.platon.p2p.plugins.KadTopologyPlugin;
import org.platon.p2p.proto.common.NodeID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/8/20, lvxiaoyi, Initial Version.
 */
public class NodeServer {
    private static final Logger logger = LoggerFactory.getLogger(NodeServer.class);



    private EventLoopGroup bossGroup = new NioEventLoopGroup();


    private EventLoopGroup workerGroup = new NioEventLoopGroup();



    public static void main(String[] args) throws Exception {
        NodeServer nodeServer = new NodeServer();
        nodeServer.startup();
    }

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

        logger.info("Node server started, NodeId:={} / {}", CodecUtils.toHexString(NodeContext.localNodeId), NodeContext.getNodeID());
        System.out.println(String.format("Node server started, NodeId:%s / %s...", CodecUtils.toHexString(NodeContext.localNodeId), NodeContext.getNodeID()));


        List<Config> configs = PeerConfig.getInstance().getActiveNodeConfig();
        configs.forEach(config -> {
                    String ip = config.getString("host");
                    String port = config.getString("port");
                    String publicKey = config.getString("public-key");

                    logger.info("connecting to active node:{}:{}", ip, port);
                    join(ip, port, publicKey);
                }
        );
    }

    private void join(String ip, String port, String pubKey){

        byte[] remoteNodePubKeyBytes = Numeric.hexStringToByteArray(pubKey);
        ByteString remoteNodeId = ByteString.copyFrom(WalletUtil.computeAddress(remoteNodePubKeyBytes));

        NodeID remote = NodeID.newBuilder().setId(remoteNodeId).setEndpoint(ip+":"+port).setPubKey(ByteString.copyFrom(remoteNodePubKeyBytes)).build();

        KadTopologyPlugin topologyPlugin = SpringContextUtil.getBean("topologyPlugin");

        topologyPlugin.join(remote);
    }

    private void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
