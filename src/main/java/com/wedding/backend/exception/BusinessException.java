package com.wedding.backend.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
