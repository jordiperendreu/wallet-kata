package com.playtomic.tests.wallet.wallet.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.wallet.dto.CreateWalletRequest;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class WalletControllerTest {

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() {
        UUID userId = UUID.randomUUID();
        WalletService walletService = mock(WalletService.class);
        WalletResponse wallet = new WalletResponse(UUID.randomUUID(), userId, new BigDecimal(0));
        when(walletService.create(userId)).thenReturn(wallet);
        WalletController walletController = new WalletController(walletService);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);
        ResponseEntity<WalletResponse> response = walletController.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        WalletResponse actual = response.getBody();
        assertEquals(wallet.getId(), actual.getId());
        assertEquals(wallet.getUserId(), actual.getUserId());
        assertEquals(wallet.getAmount(), actual.getAmount());
    }
}
