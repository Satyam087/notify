package com.npaas.notify.common.security;

public class InvalidRequestCharsetException extends RuntimeException {

    public InvalidRequestCharsetException(String charset, Throwable cause) {
        super("Unsupported request charset: " + charset, cause);
    }
}
