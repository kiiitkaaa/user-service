package com.deshko.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(String message) {
        super(message);
    }

    public CardLimitExceededException(int max) {
        super("User cannot have more than " + max + " cards");
    }
}
