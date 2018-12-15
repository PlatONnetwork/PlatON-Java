package org.platon.p2p.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;

public class TimerClientHandler extends SimpleChannelInboundHandler<String> {

    private int counter;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        System.out.println("Now is : " + msg + "; the counter is:" + ++counter + " timestamp:" + (new Date()));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message = Unpooled.buffer(TimerClient.req.length);
        message.writeBytes(TimerClient.req);
        ctx.writeAndFlush(message);
    }

    /*public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message = null
        for(int i=0; i<10; i++){
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);

            TimeUnit.SECONDS.sleep(1);
        }
    }*/
}
