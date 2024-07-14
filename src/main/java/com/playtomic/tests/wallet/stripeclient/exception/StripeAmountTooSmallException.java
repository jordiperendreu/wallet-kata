package com.playtomic.tests.wallet.stripeclient.exception;

public class StripeAmountTooSmallException extends StripeServiceException {

    public StripeAmountTooSmallException() {
        super("Amount too small");
    }

}
