package com.playtomic.tests.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.stripeclient.service.StripeService;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.exception.CreateWalletError;
import com.playtomic.tests.wallet.wallet.exception.ProcessingChargeError;
import com.playtomic.tests.wallet.wallet.model.Transaction;
import com.playtomic.tests.wallet.wallet.model.TransactionStatus;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WalletServiceIT {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    @MockBean
    private StripeService stripeService;

    @AfterEach
    public void tearDown() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() {
        UUID userID = UUID.randomUUID();

        WalletResponse actual = walletService.create(userID);

        Wallet saved = walletRepository.findById(actual.getId()).orElseThrow();
        assertNotNull(actual.getId());
        assertEquals(userID, actual.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(actual.getAmount()));
        assertEqualWallet(saved, actual);
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

    @Test
    public void whenTopUp_thenTheAmountIsAddedToTheWallet() {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        Payment payment = new Payment("paymentId");
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenReturn(payment);

        WalletResponse actual = walletService.topUp(wallet.getId(), "cardNumber",
            new BigDecimal(10));

        BigDecimal newAmount = wallet.getAmount().add(new BigDecimal(10));
        Wallet savedWallet = walletRepository.findById(actual.getId()).orElseThrow();
        assertEquals(wallet.getId(), savedWallet.getId());
        assertEquals(wallet.getUserId(), savedWallet.getUserId());
        assertEquals(0, newAmount.compareTo(savedWallet.getAmount()));
        assertEqualWallet(savedWallet, actual);
    }

    @Test
    public void whenTopUp_thenTheTransactionIsAndUpdatedToSUCCESS() {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        Payment payment = new Payment("paymentId");
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenReturn(payment);

        walletService.topUp(wallet.getId(), "cardNumber", new BigDecimal(10));

        Transaction transaction = transactionRepository.findAll().get(0);
        assertNotNull(transaction.getId());
        assertEquals(wallet.getId(), transaction.getWallet().getId());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals(0, new BigDecimal(10).compareTo(transaction.getAmount()));
        assertEquals(payment.getId(), transaction.getPaymentId());
    }

    @Test
    public void whenTopUpAndChargeFails_thenTheTransactionIsAndUpdatedToFAILED() {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenThrow(
            new ProcessingChargeError("Service is down"));

        ProcessingChargeError exception = assertThrows(
            ProcessingChargeError.class,
            () -> walletService.topUp(wallet.getId(), "cardNumber", new BigDecimal(10))
        );

        assertEquals("Failed to charge card", exception.getMessage());
        Transaction savedTransaction = transactionRepository.findAll().get(0);
        assertEquals(TransactionStatus.FAILED, savedTransaction.getStatus());
    }

    @Test
    public void whenTopUpAndAmountIsTooSmall_thenTheTransactionIsAndUpdatedToFAILED() {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenThrow(
            new StripeAmountTooSmallException());

        ProcessingChargeError exception = assertThrows(
            ProcessingChargeError.class,
            () -> walletService.topUp(wallet.getId(), "cardNumber", new BigDecimal(10))
        );

        assertEquals("Amount too small", exception.getMessage());
        Transaction savedTransaction = transactionRepository.findAll().get(0);
        assertEquals(TransactionStatus.FAILED, savedTransaction.getStatus());
    }

    @Test
    public void whenTwoConcurrentTopUp_thenTheWalletAmountIsUpdatedCorrectly()
        throws InterruptedException {
        Wallet wallet = walletRepository.save(aNewWalletWithUserIdAndAmount(UUID.randomUUID(),
            new BigDecimal(0)));
        Payment payment = new Payment("paymentId");
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenAnswer(invocation -> {
            Thread.sleep(50);
            return payment;
        });
        Runnable topUpOperation = () -> {
            walletService.topUp(wallet.getId(), "cardNumber", new BigDecimal(10));
        };

        int numberOfThreads = 10;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(topUpOperation);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        List<Transaction> transactions = transactionRepository.findAllByStatus(
            TransactionStatus.SUCCESS);
        BigDecimal expectedWalletAmount = new BigDecimal(10).multiply(
            BigDecimal.valueOf(transactions.size()));
        assertEquals(0, expectedWalletAmount.compareTo(updatedWallet.getAmount()));
    }

    private static Wallet aNewWalletWithUserIdAndAmount(UUID userId, BigDecimal amount) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAmount(amount);
        return wallet;
    }

    private void assertEqualWallet(Wallet expected, WalletResponse actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(0, expected.getAmount().compareTo(actual.getAmount()));
    }
}
