package com.playtomic.tests.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.service.StripeService;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.exception.ProcessingChargeError;
import com.playtomic.tests.wallet.wallet.model.Transaction;
import com.playtomic.tests.wallet.wallet.model.TransactionStatus;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

public class WalletServiceTest {

    @Test
    public void whenCreatingWalletWithAUserID_thenReturnsTheWallet() {
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        Wallet savedWalled = new Wallet(UUID.randomUUID(), 0L, userID, new BigDecimal(0));
        when(walletRepository.save(walletCaptor.capture())).thenReturn(savedWalled);
        WalletService walletService = new WalletService(walletRepository,
            aTransactionRepositoryMock(), aStripeServiceMock(), aPlatformTransactionManager());

        WalletResponse actual = walletService.create(userID);

        Wallet toSaveWallet = walletCaptor.getValue();
        assertEquals(userID, toSaveWallet.getUserId());
        assertEquals(new BigDecimal(0), toSaveWallet.getAmount());
        assertEqualWallet(savedWalled, actual);
    }

    @Test
    public void whenTopUp_thenTheAmountIsAddedToTheWallet() {
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        UUID walletId = UUID.randomUUID();
        Wallet initialWallet = new Wallet(walletId, 0L, userID, new BigDecimal(5));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(initialWallet));
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        Wallet wallet = new Wallet(walletId, 0L, userID, new BigDecimal(15));
        when(walletRepository.save(walletCaptor.capture())).thenReturn(wallet);
        WalletService walletService = new WalletService(walletRepository,
            aTransactionRepositoryMock(), aStripeServiceMock(), aPlatformTransactionManager());

        WalletResponse actual = walletService.topUp(wallet.getId(), "cardNumber",
            new BigDecimal(10));

        Wallet toSaveWallet = walletCaptor.getValue();
        assertEquals(wallet, toSaveWallet);
        assertEqualWallet(wallet, actual);
    }

    @Test
    public void whenTopUp_thenTheTransactionIsAndUpdatedToSUCCESS() {
        String paymentId = "paymentId";
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 0L, userID, new BigDecimal(5));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        TransactionRepository transactionRepository = aTransactionRepositoryMock();
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(transactionCaptor.capture())).thenReturn(new Transaction())
            .thenReturn(new Transaction());
        WalletService walletService = new WalletService(walletRepository, transactionRepository,
            aStripeServiceMockWithPaymentId(paymentId), aPlatformTransactionManager());

        walletService.topUp(walletId, "cardNumber", new BigDecimal(10));

        //INITIATED
        Transaction transactionInitiated = transactionCaptor.getAllValues().get(0);
        assertEquals(walletId, transactionInitiated.getWallet().getId());
        assertEquals(new BigDecimal(10), transactionInitiated.getAmount());
        assertEquals(TransactionStatus.INITIATED, transactionInitiated.getStatus());
        //PROCESSED
        Transaction transactionProcessed = transactionCaptor.getAllValues().get(1);
        assertEquals(paymentId, transactionProcessed.getPaymentId());
        assertEquals(TransactionStatus.PROCESSED, transactionProcessed.getStatus());
        //SUCCESS
        Transaction transactionSuccess = transactionCaptor.getAllValues().get(2);
        assertEquals(TransactionStatus.SUCCESS, transactionSuccess.getStatus());
    }

    @Test
    public void whenTopUpAndChargeFails_thenTheTransactionIsAndUpdatedToFAILED() {
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 0L, userID, new BigDecimal(5));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        TransactionRepository transactionRepository = aTransactionRepositoryMock();
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(transactionCaptor.capture())).thenReturn(new Transaction())
            .thenReturn(new Transaction());
        WalletService walletService = new WalletService(walletRepository, transactionRepository,
            aStripeServiceReturningAnError(), aPlatformTransactionManager());

        ProcessingChargeError exception = assertThrows(ProcessingChargeError.class,
            () -> walletService.topUp(walletId, "cardNumber", new BigDecimal(10)));

        assertEquals("Failed to charge card", exception.getMessage());
        Transaction transactionProcessed = transactionCaptor.getAllValues().get(1);
        assertEquals(TransactionStatus.FAILED, transactionProcessed.getStatus());
    }

    @Test
    public void whenTopUpAndUpdateWalletFails_thenTheTransactionIsAndUpdatedToFAILED() {
        UUID userID = UUID.randomUUID();
        WalletRepository walletRepository = mock(WalletRepository.class);
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 0L, userID, new BigDecimal(5));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenThrow(
            new OptimisticLockingFailureException("Error updating the Wallet"));
        TransactionRepository transactionRepository = aTransactionRepositoryMock();
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(transactionCaptor.capture())).thenReturn(new Transaction())
            .thenReturn(new Transaction());
        WalletService walletService = new WalletService(walletRepository, transactionRepository,
            aStripeServiceMock(), aPlatformTransactionManager());

        ProcessingChargeError exception = assertThrows(ProcessingChargeError.class,
            () -> walletService.topUp(walletId, "cardNumber", new BigDecimal(10)));

        assertEquals("Error updating the Wallet", exception.getMessage());
        Transaction transactionProcessed = transactionCaptor.getAllValues().get(2);
        assertEquals(TransactionStatus.FAILED, transactionProcessed.getStatus());
    }

    @Test
    public void whenTopUp_thenStripeIsCalled() {
        UUID userId = UUID.randomUUID();
        String cardNumber = "cardNumber";
        WalletRepository walletRepository = mock(WalletRepository.class);
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 0L, userId, new BigDecimal(5));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        TransactionRepository transactionRepository = aTransactionRepositoryMock();
        StripeService stripeService = mock(StripeService.class);
        when(stripeService.charge("cardNumber", new BigDecimal(10))).thenReturn(
            new Payment("chargeId"));
        WalletService walletService = new WalletService(walletRepository, transactionRepository,
            stripeService, aPlatformTransactionManager());

        walletService.topUp(walletId, "cardNumber", new BigDecimal(10));

        verify(stripeService).charge(cardNumber, new BigDecimal(10));
    }

    public static StripeService aStripeServiceMock() {
        return aStripeServiceMockWithPaymentId("paymentId");
    }

    public static StripeService aStripeServiceMockWithPaymentId(String paymentId) {
        StripeService stripeService = mock(StripeService.class);
        when(stripeService.charge(any(), any())).thenReturn(new Payment(paymentId));
        return stripeService;
    }

    public static StripeService aStripeServiceReturningAnError() {
        StripeService stripeService = mock(StripeService.class);
        when(stripeService.charge(any(), any())).thenThrow(
            new ProcessingChargeError("Service is down"));
        return stripeService;
    }

    public static TransactionRepository aTransactionRepositoryMock() {
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
        return transactionRepository;
    }

    public static PlatformTransactionManager aPlatformTransactionManager() {
        PlatformTransactionManager mock = mock(PlatformTransactionManager.class);
        return mock;
    }

    private void assertEqualWallet(Wallet expected, WalletResponse actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(0, expected.getAmount().compareTo(actual.getAmount()));
    }
}
