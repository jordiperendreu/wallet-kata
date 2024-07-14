package com.playtomic.tests.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class WalletServiceTest {

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() {
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        Wallet savedWalled = new Wallet(UUID.randomUUID(), 0L, userID, new BigDecimal(0));
        when(walletRepository.save(walletCaptor.capture())).thenReturn(savedWalled);
        WalletService walletService = new WalletService(walletRepository);

        WalletResponse actual = walletService.create(userID);

        Wallet toSaveWallet = walletCaptor.getValue();
        assertEquals(userID, toSaveWallet.getUserId());
        assertEquals(new BigDecimal(0), toSaveWallet.getAmount());
        assertEquals(savedWalled.getId(), actual.getId());
        assertEquals(savedWalled.getUserId(), actual.getUserId());
        assertEquals(savedWalled.getAmount(), actual.getAmount());
    }
}
