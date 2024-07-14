package com.playtomic.tests.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.service.StripeService;
import com.playtomic.tests.wallet.wallet.exception.ProcessingChargeError;
import com.playtomic.tests.wallet.wallet.model.Transaction;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WalletServiceTransactionabilityIT {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private StripeService stripeService;

    @AfterEach
    public void tearDown() {
        walletRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    public void whenErrorUpdatingTransaction_thenTheWalletIsNotUpdated() {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        Payment payment = new Payment("paymentId");
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenReturn(payment);
        when(transactionRepository.save(any())).thenReturn(new Transaction())
            .thenReturn(new Transaction()).thenThrow(OptimisticLockingFailureException.class);

        assertThrows(ProcessingChargeError.class, () -> walletService
            .topUp(wallet.getId(), "cardNumber", new BigDecimal(10))
        );

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(0, new BigDecimal(0).compareTo(updatedWallet.getAmount()));
    }

    private static Wallet aNewWalletWithUserIdAndAmount(UUID userId, BigDecimal amount) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAmount(amount);
        return wallet;
    }
}
