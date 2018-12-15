package org.platon.p2p;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.platon.crypto.WalletUtil;
import org.platon.p2p.common.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EccEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(EccEncoder.class);

    private ByteString remoteNodeId;
    private ByteString remoteNodePubKey;

    private final int lengthFieldLength;

    public EccEncoder(int lengthFieldLength) {
        this.lengthFieldLength = lengthFieldLength;
    }


    public EccEncoder(ByteString remoteNodeId, ByteString remoteNodePubKey, int lengthFieldLength) {
        this.remoteNodeId = remoteNodeId;
        this.remoteNodePubKey = remoteNodePubKey;
        this.lengthFieldLength = lengthFieldLength;
    }

    public ByteString getRemoteNodeId() {
        return remoteNodeId;
    }

    public void setRemoteNodeId(ByteString remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    public ByteString getRemoteNodePubKey() {
        return remoteNodePubKey;
    }

    public void setRemoteNodePubKey(ByteString remoteNodePubKey) {
        this.remoteNodePubKey = remoteNodePubKey;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception{
        logger.debug("to encode message to remoteNodeId:{}, channel:{}", CodecUtils.toHexString(remoteNodeId), ctx.channel() );
        byte[] dataBytes = ByteBufUtil.getBytes(in);
        if(remoteNodePubKey!=null && !remoteNodePubKey.isEmpty()){
            byte[] encryptedBytes = WalletUtil.encrypt(dataBytes, remoteNodePubKey.toByteArray());
            if(lengthFieldLength==4){
                out.writeInt(encryptedBytes.length);
                out.writeBytes(encryptedBytes);
            }else{
                throw new IllegalArgumentException("length field only can be an integer");
            }
        }else{
            logger.error("cannot find remote node's public key");
            throw new Exception("cannot find remote node's public key");
        }
    }
}
