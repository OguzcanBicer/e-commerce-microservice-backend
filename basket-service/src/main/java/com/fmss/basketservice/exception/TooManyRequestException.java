package com.fmss.basketservice.exception;

public class TooManyRequestException extends RuntimeException{
    public TooManyRequestException(String message) {
        super(message);
    }
}
