package org.platon.core.exception;

public class OperationException extends Exception {

    private int errorCode;

    public OperationException(String message, int errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
