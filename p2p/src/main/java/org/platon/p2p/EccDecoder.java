package org.platon.p2p;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.common.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EccDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger(EccDecoder.class);

    private ByteString remoteNodeId;

    public ByteString getRemoteNodeId() {
        return remoteNodeId;
    }

    public void setRemoteNodeId(ByteString remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    public EccDecoder(int maxFrameLength,
                      int lengthFieldOffset, int lengthFieldLength,
                      int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        logger.debug("to decode message from remoteNodeId:={}, channel:{}", CodecUtils.toHexString(remoteNodeId), ctx.channel());

        int length = in.readInt();
        if(in.readableBytes() < length){
            logger.error("cannot decode the message, message length is error");
            throw new Exception("cannot decode the message, message length is error");
        }

        ByteBuf buf = in.readBytes(length);
        byte[] encrypted = new byte[buf.readableBytes()];
        buf.readBytes(encrypted);
        buf.release();

        if(NodeContext.ecKey==null){
            logger.error("cannot decode the message because the local ECKey is missed.");
            throw new Exception("cannot decode the message because the local ECKey is missed.");
        }
        byte[] decrypted = WalletUtil.decrypt(encrypted, NodeContext.ecKey);

        return Unpooled.wrappedBuffer(decrypted);
    }
}
