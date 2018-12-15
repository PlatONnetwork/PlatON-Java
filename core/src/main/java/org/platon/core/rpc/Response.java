package org.platon.core.rpc;


import com.google.protobuf.Any;
import org.platon.slice.message.ResultCodeMessage;
import org.platon.slice.message.response.BaseResponse;

public class Response {

    private static final Response DEFAULT_INSTANCE;
    private static BaseResponse.Builder response = null;

    public enum ErrorCode {

        Unsupported(95271, "Not Supported."),
        ;

        private int code ;
        private String reason;

        public int getCode(){
            return this.code;
        }

        public String getReason(){
            return this.reason;
        }

        ErrorCode(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }
    }

    static {
        DEFAULT_INSTANCE = new Response();
    }

    public static Response newResponse() {
        return DEFAULT_INSTANCE;
    }

    public static Response newResponse(ResultCodeMessage.ResultCode code, String msg, Any data) {
        response = BaseResponse.newBuilder();
        DEFAULT_INSTANCE.withData(data);
        DEFAULT_INSTANCE.withMsg(msg);
        DEFAULT_INSTANCE.withResultCode(code);
        return DEFAULT_INSTANCE;
    }

    public static Response newResponse(ResultCodeMessage.ResultCode code, String msg) {
        response = BaseResponse.newBuilder();
        DEFAULT_INSTANCE.withMsg(msg);
        DEFAULT_INSTANCE.withResultCode(code);
        return DEFAULT_INSTANCE;
    }

    public Response withResultCode(ResultCodeMessage.ResultCode code){
        response.setCode(code);
        return this;
    }

    public Response withMsg(String msg){
        response.setMsg(msg);
        return this;
    }

    public Response withData(Any data){
        response.setData(data);
        return this;
    }

    public BaseResponse build(){
        return response.build();
    }

}
