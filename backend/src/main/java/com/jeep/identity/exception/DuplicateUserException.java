package com.jeep.identity.exception;

public class DuplicateUserException extends IdentityException {

    public DuplicateUserException(String email) {
        super("User with email '" + email + "' already exists");
    }
}
