package com.jeep.identity.exception;

public class InvalidCredentialsException extends IdentityException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
