package org.platon.p2p.netty.platon;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.platon.p2p.EccDecoder;
import org.platon.p2p.EccEncoder;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatonClientChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(PlatonClientChannelInitializer.class);


    private final static int MAX_FRAME_LENGTH = 1024*1024*1024;
    private final static int LENGTH_FIELD = 4;
    private final static int LENGTH_FIELD_OFFSET = 0;
    private final static int LENGTH_ADJUSTMENT  = 0;
    private final static int INITIAL_BYTES_TO_STRIP = 4;


    private ByteString remoteNodeId;
    private ByteString remoteNodePubKey;

    public PlatonClientChannelInitializer(ByteString remoteNodeId, ByteString remoteNodePubKey) {
        this.remoteNodeId = remoteNodeId;
        this.remoteNodePubKey = remoteNodePubKey;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        logger.debug("init client channel ......");


        ChannelPipeline p = channel.pipeline();


        p.addLast(new EccDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));

        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(PlatonMessage.getDefaultInstance()));


        p.addLast(new EccEncoder(remoteNodeId, remoteNodePubKey, LENGTH_FIELD));

        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());

        p.addLast(new PlaonClientHandler(remoteNodeId, remoteNodePubKey));
    }
}
