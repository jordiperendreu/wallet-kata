package com.playtomic.tests.wallet.stripeclient.fake;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.stripeclient.service.StripeService;
import java.math.BigDecimal;
import java.net.URI;
import lombok.NonNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("test")
public class FakeStripeService extends StripeService {

    public FakeStripeService(@NonNull RestTemplateBuilder restTemplateBuilder) {
        super(URI.create("http://localhost"), URI.create("http://localhost"), restTemplateBuilder);
    }

    @Override
    public Payment charge(@NonNull String creditCardNumber, @NonNull BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(5)) <= 0) {
            throw new StripeAmountTooSmallException();
        }
        return new Payment("1234");
    }
}
