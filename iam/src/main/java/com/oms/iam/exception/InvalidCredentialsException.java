package com.oms.iam.exception;

/**
 * Thrown when login fails due to incorrect email or password.
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * @param message the detail message describing the credential failure
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
