package org.platon.core.exception;

/**
 * Created by alliswell on 2018/7/31.
 */
public class AlreadyKnownException extends RuntimeException {
	public AlreadyKnownException(String message){
		super(message);
	}
}
