package org.platon.p2p.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimerServerHandler extends SimpleChannelInboundHandler<String> {
    private int counter;

    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception{
        System.out.println("Receive order :" + msg + ", the counter is : " + ++counter);
        String currentTime = "QUERY TIME ORDER".equals(msg) ? new Date().toString() : "BAD ORDER";

        currentTime = currentTime + System.getProperty("line.separator");

        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        TimeUnit.SECONDS.sleep(1);

        System.out.println("send response timestamp: " + new Date());
        ctx.writeAndFlush(resp);
    }
}