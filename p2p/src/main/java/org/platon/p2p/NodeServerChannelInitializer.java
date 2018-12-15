package org.platon.p2p;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/8/20, lvxiaoyi, Initial Version.
 */
public class NodeServerChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(NodeServerChannelInitializer.class);


    private final static int MAX_FRAME_LENGTH = 1024*1024*1024;
    private final static int LENGTH_FIELD = 4;
    private final static int LENGTH_FIELD_OFFSET = 0;
    private final static int LENGTH_ADJUSTMENT  = 0;
    private final static int INITIAL_BYTES_TO_STRIP = 4;

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {


        ChannelPipeline p = channel.pipeline();

        p.addLast(new LoggingHandler(LogLevel.INFO));

        p.addLast("eccDecoder", new EccDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));

        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(PlatonMessage.getDefaultInstance()));


        p.addLast("eccEncoder", new EccEncoder(LENGTH_FIELD));

        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());

        p.addLast("nodeServerChannelHandler", new NodeServerChannelHandler());
    }
}
