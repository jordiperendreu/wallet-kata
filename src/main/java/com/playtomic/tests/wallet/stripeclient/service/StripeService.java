package com.playtomic.tests.wallet.stripeclient.service;

import com.playtomic.tests.wallet.stripeclient.dto.ChargeRequest;
import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.exception.StripeServiceException;
import com.playtomic.tests.wallet.stripeclient.infrastructure.StripeRestTemplateResponseErrorHandler;
import java.math.BigDecimal;
import java.net.URI;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


/**
 * Handles the communication with Stripe.
 *
 * A real implementation would call to String using their API/SDK.
 * This dummy implementation throws an error when trying to charge less than 10€.
 */
@Service
public class StripeService {

    @NonNull
    private URI chargesUri;

    @NonNull
    private URI refundsUri;

    @NonNull
    private RestTemplate restTemplate;

    public StripeService(@Value("${stripe.simulator.charges-uri}") @NonNull URI chargesUri,
                         @Value("${stripe.simulator.refunds-uri}") @NonNull URI refundsUri,
                         @NonNull RestTemplateBuilder restTemplateBuilder) {
        this.chargesUri = chargesUri;
        this.refundsUri = refundsUri;
        this.restTemplate =
                restTemplateBuilder
                .errorHandler(new StripeRestTemplateResponseErrorHandler())
                .build();
    }

    /**
     * Charges money in the credit card.
     *
     * Ignore the fact that no CVC or expiration date are provided.
     *
     * @param creditCardNumber The number of the credit card
     * @param amount The amount that will be charged.
     *
     * @throws StripeServiceException
     */
    public Payment charge(@NonNull String creditCardNumber, @NonNull BigDecimal amount) throws StripeServiceException {
        ChargeRequest body = new ChargeRequest(creditCardNumber, amount);
        return restTemplate.postForObject(chargesUri, body, Payment.class);
    }

    /**
     * Refunds the specified payment.
     */
    public void refund(@NonNull String paymentId) throws StripeServiceException {
        // Object.class because we don't read the body here.
        restTemplate.postForEntity(chargesUri.toString(), null, Object.class, paymentId);
    }
}
