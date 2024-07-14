package com.playtomic.tests.wallet.service.impl;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.service.Payment;
import com.playtomic.tests.wallet.service.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.service.StripeServiceException;
import com.playtomic.tests.wallet.service.StripeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;
import java.net.URI;
import org.springframework.web.client.RestTemplate;

public class StripeServiceTest {

    private RestTemplate restTemplateMock;
    private StripeService stripeService;
    private final URI chargesUri = URI.create("http://mock-url/stripe/charges");
    private final URI refundsUri = URI.create("http://mock-url/stripe/refunds");

    @BeforeEach
    public void setUp() {
        restTemplateMock = mock(RestTemplate.class);
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.errorHandler(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplateMock);
        stripeService = new StripeService(chargesUri, refundsUri, restTemplateBuilder);
    }

    @Test
    public void whenChargeThrowsException_thenTheExceptionIsPropagated() {
        when(restTemplateMock.postForObject(eq(chargesUri), any(), eq(Payment.class))).thenThrow(
            new StripeAmountTooSmallException());

        assertThrows(StripeAmountTooSmallException.class, () -> {
            stripeService.charge("4242 4242 4242 4242", new BigDecimal(5));
        });
    }

    @Test
    public void whenCharge_thenThePaymentIdFromStripeIsReturned() throws StripeServiceException {
        when(restTemplateMock.postForObject(any(), any(), any())).thenReturn(new Payment("1234"));

        Payment actual = stripeService.charge("4242 4242 4242 4242", new BigDecimal(15));

        assertEquals("1234", actual.getId());
    }
}
