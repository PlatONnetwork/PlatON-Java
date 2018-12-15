package org.platon.core.exception;

/**
 * Created by alliswell on 2018/8/2.
 */
public class AlreadySealedException extends RuntimeException {
	public AlreadySealedException(String message){
			super(message);
		}
}
