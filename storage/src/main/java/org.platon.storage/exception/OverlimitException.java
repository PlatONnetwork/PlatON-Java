package org.platon.storage.exception;


public class OverlimitException extends RuntimeException{

	public OverlimitException(String message){
		super(message);
	}

	public OverlimitException(String message, Throwable cause) {
		super(message, cause);
	}

	public OverlimitException(Throwable cause) {
		super(cause);
	}
}
