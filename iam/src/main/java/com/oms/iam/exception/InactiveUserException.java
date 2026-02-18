package com.oms.iam.exception;

/**
 * Thrown when a login attempt is made for a user whose account is not active.
 */
public class InactiveUserException extends RuntimeException {

    /**
     * @param message the detail message describing the inactive account
     */
    public InactiveUserException(String message) {
        super(message);
    }
}
