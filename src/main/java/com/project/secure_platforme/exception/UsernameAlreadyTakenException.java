package com.project.secure_platforme.exception;

public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException() {
        super("Username is already taken");
    }
}
