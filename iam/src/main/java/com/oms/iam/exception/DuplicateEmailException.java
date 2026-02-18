package com.oms.iam.exception;

/**
 * Thrown when a registration attempt uses an email that already exists.
 */
public class DuplicateEmailException extends RuntimeException {

    /**
     * @param message the detail message describing the duplicate email
     */
    public DuplicateEmailException(String message) {
        super(message);
    }
}
