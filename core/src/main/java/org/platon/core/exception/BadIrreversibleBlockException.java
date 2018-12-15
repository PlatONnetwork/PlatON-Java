package org.platon.core.exception;

/**
 * bad irreversible block exception
 *
 * @author alliswell
 * @since 2018/08/13
 */
public class BadIrreversibleBlockException extends RuntimeException {
    public BadIrreversibleBlockException(String message){
        super(message);
    }
}
