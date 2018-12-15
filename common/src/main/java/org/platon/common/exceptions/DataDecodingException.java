package org.platon.common.exceptions;


public class DataDecodingException extends RuntimeException {

    public DataDecodingException(String message) {
        super(message);
    }

    public DataDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
