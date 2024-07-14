package com.playtomic.tests.wallet.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopUpRequest {

    @JsonProperty("cardNumber")
    @NotBlank(message = "cardNumber must not be blank")
    public String cardNumber;
    @JsonProperty("amount")
    @Positive(message = "amount must be a positive number")
    public BigDecimal amount;

    public TopUpRequest() {
    }
}
