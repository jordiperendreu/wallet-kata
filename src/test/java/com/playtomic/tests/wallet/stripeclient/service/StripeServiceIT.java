package com.playtomic.tests.wallet.stripeclient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.stripeclient.exception.StripeServiceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("stripe-test")
public class StripeServiceIT {

    @Autowired
    private StripeService stripeService;

    @Test
    public void whenChargeAndStripeReturnsUnprocessableEntity_thenThrowStripeAmountTooSmallException() {
        assertThrows(StripeAmountTooSmallException.class, () -> {
            stripeService.charge("4242 4242 4242 4242", new BigDecimal(5));
        });
    }

    @Test
    public void whenChargeAndStripeReturnsSuccessfull_thenTheStripePaymentIdIsReturned()
        throws StripeServiceException {
        Payment actual = stripeService.charge("4242 4242 4242 4242", new BigDecimal(15));

        assertEquals("1234", actual.getId());
    }
}
