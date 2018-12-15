package org.platon.core.exception;


public class PlatonException extends RuntimeException {

    private int errorCode;

    public PlatonException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PlatonException(String message) {
        super(message);
    }

    public PlatonException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
