package org.platon.common.exceptions;


public class DataEncodingException extends RuntimeException {

    public DataEncodingException(String message) {
        super(message);
    }

    public DataEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
