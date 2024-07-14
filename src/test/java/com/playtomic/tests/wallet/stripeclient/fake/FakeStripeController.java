package com.playtomic.tests.wallet.stripeclient.fake;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
class FakeStripeController {
    @PostMapping("/stripe/charges")
    public ResponseEntity<Payment> charge(@RequestBody ChargeRequest chargeRequest) {
        if (chargeRequest.getAmount().equals(BigDecimal.valueOf(5))) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new Payment("1234"));
        } else if (chargeRequest.getAmount().equals(BigDecimal.valueOf(15))) {
            return ResponseEntity.ok(new Payment("1234"));
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Duplicated with the defined in stripeclient.dto but is giving problems building the object
     * in the controller (notnull parameters). For reasons of the exercise I've decided to not
     * modify the original ChangeRequest for now
     */
    @Getter
    @Setter
    public static class ChargeRequest {
        private BigDecimal amount;

        @JsonProperty("credit_card")
        private String creditCard;

        public ChargeRequest() {
        }
    }
}
