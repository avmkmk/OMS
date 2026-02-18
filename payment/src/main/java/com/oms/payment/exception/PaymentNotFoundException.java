package com.oms.payment.exception;

/**
 * Thrown when a payment is not found for a given order.
 */
public class PaymentNotFoundException extends RuntimeException {

    /**
     * @param message the detail message describing the missing payment
     */
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
