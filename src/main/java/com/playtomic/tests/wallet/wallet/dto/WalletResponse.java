package com.playtomic.tests.wallet.wallet.dto;

import com.playtomic.tests.wallet.wallet.model.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WalletResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal amount;

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getAmount());
    }
}
