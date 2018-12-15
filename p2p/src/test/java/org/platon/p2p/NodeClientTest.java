package org.platon.p2p;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NodeClientTest {

    public void connect(String host, int port) {
        try {
            ChannelFuture channelFuture = connectAsync(host, port);

            channelFuture.sync();

            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChannelFuture connectAsync(String host, int port) {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup());
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        b.remoteAddress(host, port);


        return b.connect();
    }

    public static void main(String[] args) throws Exception {
        NodeClientTest timerClient = new NodeClientTest();
        timerClient.test();

    }

    private void test(){
        connect("192.168.7.113", 11001);
    }


}
