package com.playtomic.tests.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.exception.CreateWalletError;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WalletServiceIT {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @AfterEach
    public void tearDown() {
        walletRepository.deleteAll();
    }

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() {
        UUID userID = UUID.randomUUID();

        WalletResponse actual = walletService.create(userID);

        Wallet saved = walletRepository.findById(actual.getId()).orElseThrow();
        assertNotNull(actual.getId());
        assertEquals(userID, actual.getUserId());
        assertEquals(saved.getUserId(), actual.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(actual.getAmount()));
        assertEquals(0, saved.getAmount().compareTo(actual.getAmount()));
    }

    @Test
    public void whenCreatingWallerOfAUserWithAWalletCreated_thenReturnsInvalidParameter() {
        UUID userID = UUID.randomUUID();
        walletRepository.save(aNewWalletWithUserIdAndAmount(userID, new BigDecimal(0)));

        CreateWalletError exception = assertThrows(
            CreateWalletError.class,
            () -> walletService.create(userID)
        );

        assertEquals("Failed to create wallet, maybe the user has already a Wallet",
            exception.getMessage());
    }

    private static Wallet aNewWalletWithUserIdAndAmount(UUID userId, BigDecimal amount) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAmount(amount);
        return wallet;
    }
}
