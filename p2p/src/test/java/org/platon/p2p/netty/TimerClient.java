package org.platon.p2p.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerClient {
    private static ExecutorService executor =  Executors.newCachedThreadPool();


    public static byte[] req = ("QUERY TIME ORDER"+System.getProperty("line.separator")).getBytes();


    private static EventLoopGroup workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ClientWorker-" + cnt.getAndIncrement());
        }
    });

    private static Map<String, Channel> channelMap = new HashMap<>();

    public void connect(String host, int port) {
        try {
            ChannelFuture channelFuture = connectAsync(host, port);

            channelFuture.sync();

            channelMap.put("" + port, channelFuture.channel());

            //channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChannelFuture connectAsync(String host, int port) {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        b.remoteAddress(host, port);

        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                System.out.println("init channel for remote: " + ch.remoteAddress());
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024))
                        .addLast(new StringDecoder())
                        .addLast(new TimerClientHandler());
            }
        });
        return b.connect();
    }

    public static void main(String[] args) throws Exception {
        TimerClient timerClient = new TimerClient();
        timerClient.test();

        TimeUnit.SECONDS.sleep(5);

        channelMap.entrySet().forEach(entry->{
            ByteBuf message = Unpooled.buffer(TimerClient.req.length);
            message.writeBytes(TimerClient.req);
            entry.getValue().writeAndFlush(message);
        });

        TimeUnit.SECONDS.sleep(5);

    }


    private void test(){
        executor.execute(()->{
            this.connect("127.0.0.1", 65534);
        });
        executor.execute(()->{
            this.connect("127.0.0.1", 65535);
        });
    }
}
