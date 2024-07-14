package com.playtomic.tests.wallet.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {

    @JsonProperty("userId")
    @NotNull(message = "userID should be a valid UUID")
    public UUID userId;

    public CreateWalletRequest() {
    }

}
