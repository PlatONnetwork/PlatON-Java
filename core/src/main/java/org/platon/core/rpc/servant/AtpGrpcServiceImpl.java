package org.platon.core.rpc.servant;

import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import org.platon.common.utils.Numeric;
import org.platon.common.utils.SpringContextUtil;
import org.platon.core.exception.OperationException;
import org.platon.core.rpc.ProtoRpc;
import org.platon.core.rpc.Response;
import org.platon.service.grpc.PlatonServiceGrpc;
import org.platon.slice.message.ResultCodeMessage;
import org.platon.slice.message.request.*;
import org.platon.slice.message.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class AtpGrpcServiceImpl extends PlatonServiceGrpc.PlatonServiceImplBase {

    private Logger logger = LoggerFactory.getLogger(AtpGrpcServiceImpl.class);

    private ProtoRpc protoRpc;

    public AtpGrpcServiceImpl(){
        protoRpc = SpringContextUtil.getBean(ProtoRpc.class);
    }

    @Override
    public void atpCall(TransactionBaseRequest request, StreamObserver<BaseResponse> responseObserver) {
        throw new RuntimeException("Unsupported operation.");
    }

    @Override
    public void atpSendTransaction(TransactionBaseRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpSendTransaction, the txRequest : {}", request);
        }

        try {
            StringResponse resp = this.protoRpc.atpSendTransaction(request);
            if(null == resp){
                throw new OperationException("get null for call atpSendTransaction .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpSendTransaction.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        } catch (Exception e) {
            logger.error("Throw exception.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, "system exception.").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetTransactionReceiptByHash(GetTransactionReceiptByHashRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetTransactionReceiptByHash, the txHash : {}", request.getTxHash());
        }
        String txHash = request.getTxHash();
        try {
            TransactionReceiptResponse resp = this.protoRpc.atpGetTransactionReceipt(txHash);
            int sleepDuration = 200;
            int attempts = 15;
             for (int i = 0; i < attempts; i++) {
                if (null == resp) {
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    resp = this.protoRpc.atpGetTransactionReceipt(txHash);
                } else {
                    break;
                }
            }
            if(null == resp) {
                throw new OperationException("cound not get receipt after (" + (sleepDuration * attempts) + ") seconds.");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetTransactionReceipt.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpSendFillTransaction(TransactionBaseRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpSendFillTransaction, the txRequest : {}", request);
        }

        try {
            if(null == request.getSignature()) throw new OperationException("Signature required");
            if(null == request.getFrom()) throw new OperationException("From required.");

            StringResponse resp = this.protoRpc.atpSendFillTransaction(request);
            if(null == resp){
                throw new OperationException("get null for call atpSendFillTransaction .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (Exception e) {
            logger.error("throw exception for call protoRpc.atpSendFillTransaction.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpEnergonPrice(VoidRequest request, StreamObserver<BaseResponse> responseObserver) {
        responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS,
                "ok", Any.pack(StringResponse.newBuilder().setData("0x11").build()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetTransactionCount(GetTransactionCountRequest request, StreamObserver<BaseResponse> responseObserver) {
        throw new RuntimeException("Unsupported operation.");
    }

    @Override
    public void atpProtocolVersion(VoidRequest request, StreamObserver<BaseResponse> responseObserver) {
        throw new RuntimeException("Unsupported operation.");
    }

    @Override
    public void atpCoinbase(VoidRequest request, StreamObserver<BaseResponse> responseObserver) {
        throw new RuntimeException("Unsupported operation.");
    }

    @Override
    public void atpAccounts(VoidRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpAccounts.");
        }
        try {
            StringArrayResponse resp = this.protoRpc.atpAccounts();
            if(null == resp){
                throw new OperationException("get null for call atpAccounts .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpAccounts.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpBlockNumber(VoidRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpBlockNumber.");
        }
        try {
            StringResponse resp = this.protoRpc.atpBlockNumber();
            if(null == resp){
                throw new OperationException("get null for call atpBlockNumber .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpBlockNumber.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetBalance(GetBalanceRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetBalance. address : {}, blockParams : {}", request.getAddress(), request.getBlockParams());
        }
        try {
            StringResponse resp = this.protoRpc.atpGetBalance(request.getAddress(), request.getBlockParams());
            if(null == resp){
                throw new OperationException("get null for call atpGetBalance .");
            }
            responseObserver.onNext( Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetBalance.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetBlockTransactionCountByHash(GetBlockTransactionCountByHashRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isTraceEnabled()) {
            logger.trace("Proto Request : atpGetBlockTransactionCountByHash(). blockHash:{}", request.getBlockHash());
        }
        try {
            StringResponse resp = this.protoRpc.atpGetBlockTransactionCountByHash(request.getBlockHash());
            if (resp == null) {
                throw new OperationException("Get null for call atpGetBlockTransactionCountByHash.");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp)).build());
        }catch (Exception e){
            logger.error("throw exception for call protoRpc.atpGetBlockTransactionCountByHash.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetBlockTransactionCountByNumber(GetBlockTransactionCountByNumberRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetBlockTransactionCountByNumber. blockNumber : {}.", request.getBlockNumber());
        }
        try {
            StringResponse resp = this.protoRpc.atpGetBlockTransactionCountByNumber(request.getBlockNumber());
            if(null == resp){
                throw new OperationException("get null for call atpGetBlockTransactionCountByNumber .");
            }
            responseObserver.onNext( Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok",Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetBlockTransactionCountByNumber.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetBlockByHash(GetBlockByHashRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetBlockByHash. blockHash : {}.", request.getBlockHash());
        }
        try {
            BlockResponse resp = this.protoRpc.atpGetBlockByHash(request.getBlockHash(),true);
            if(null == resp){
                throw new OperationException("get null for call atpGetBlockByHash .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetBlockByHash.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetBlockByNumber(GetBlockByNumberRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetBlockByNumber. blockNumber : {}.", request.getBlockNumber());
        }
        try {
            BlockResponse resp = this.protoRpc.atpGetBlockByNumber(request.getBlockNumber(),true);
            if(null == resp){
                throw new OperationException("get null for call atpGetBlockByNumber .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetBlockByNumber.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetTransactionByHash(GetTransactionByHashRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetTransactionByHash. txHash : {}.", request.getTxHash());
        }
        try {
            TransactionResponse resp = this.protoRpc.atpGetTransactionByHash(request.getTxHash());
            if(null == resp){
                throw new OperationException("get null for call atpGetTransactionByHash .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetTransactionByHash.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void atpGetTransactionByBlockHashAndIndex(GetTransactionByBlockHashAndIndexRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetTransactionByBlockHashAndIndex. blockHash : {}, index : {}"
                    , request.getBlockHash(), request.getIndex());
        }
        try {
            TransactionResponse resp = this.protoRpc.atpGetTransactionByBlockHashAndIndex(request.getBlockHash(),Numeric.toHexStringWithPrefix(BigInteger.valueOf(request.getIndex())));
            if(null == resp){
                throw new OperationException("get null for call atpGetTransactionByBlockHashAndIndex .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetTransactionByBlockHashAndIndex.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
    }

    @Override
    public void atpGetTransactionByBlockNumberAndIndex(GetTransactionByBlockNumberAndIndexRequest request, StreamObserver<BaseResponse> responseObserver) {
        if (logger.isDebugEnabled()) {
            logger.debug("Proto Request : atpGetTransactionByBlockNumberAndIndex. blockNumber : {}, index : {}"
                    , request.getBlockNumber(), request.getIndex());
        }
        try {
            TransactionResponse resp = this.protoRpc.atpGetTransactionByBlockNumberAndIndex(request.getBlockNumber(),Numeric.toHexStringWithPrefix(BigInteger.valueOf(request.getIndex())));
            if(null == resp){
                throw new OperationException("get null for call atpGetTransactionByBlockNumberAndIndex .");
            }
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.SUCCESS, "ok", Any.pack(resp))
                    .build());
        } catch (OperationException e) {
            logger.error("throw exception for call protoRpc.atpGetTransactionByBlockNumberAndIndex.", e);
            responseObserver.onNext(Response.newResponse(ResultCodeMessage.ResultCode.FAIL, e.getMessage()).build());
        }
        responseObserver.onCompleted();
    }
}
