package com.smartbi.common.exception;

/**
 * Domain-level runtime exception for controllable business errors.
 * Returned to client as 400 by GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
