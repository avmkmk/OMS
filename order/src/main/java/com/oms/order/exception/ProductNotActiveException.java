package com.oms.order.exception;

/**
 * Thrown when an order references a product that is not in ACTIVE status.
 */
public class ProductNotActiveException extends RuntimeException {

    /**
     * @param message the detail message describing the inactive product
     */
    public ProductNotActiveException(String message) {
        super(message);
    }
}
