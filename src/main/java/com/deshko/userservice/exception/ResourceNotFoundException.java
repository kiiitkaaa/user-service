package com.deshko.userservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " with id " + id + " not found");
    }

    public ResourceNotFoundException(String entity, String email) {
        super(entity + " with email " + email + " not found");
    }
}
