package com.playtomic.tests.wallet.wallet.service;

import com.playtomic.tests.wallet.stripeclient.dto.Payment;
import com.playtomic.tests.wallet.stripeclient.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.stripeclient.service.StripeService;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.exception.CreateWalletError;
import com.playtomic.tests.wallet.wallet.exception.GetWalletError;
import com.playtomic.tests.wallet.wallet.exception.ProcessingChargeError;
import com.playtomic.tests.wallet.wallet.model.Transaction;
import com.playtomic.tests.wallet.wallet.model.TransactionStatus;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WalletService {

    public static final int MAX_WALLET_RETRIES = 3;
    private final Logger log = LoggerFactory.getLogger(WalletService.class);

    public WalletRepository walletRepository;
    public TransactionRepository transactionRepository;
    public StripeService stripeService;
    private PlatformTransactionManager transactionManager;

    public WalletResponse create(UUID userID) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userID);
        wallet.setAmount(BigDecimal.ZERO);

        try {
            wallet = walletRepository.save(wallet);
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to create wallet for user {}", userID, e);
            throw new CreateWalletError(
                "Failed to create wallet, maybe the user has already a Wallet");
        }

        return WalletResponse.from(wallet);
    }

    public WalletResponse topUp(UUID walletId, String cardNumber, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);

        Transaction transaction = createTransaction(amount, wallet);

        transaction = charge(transaction, wallet, cardNumber, amount);

        wallet = addAmountToWalletAndConfirmTransaction(transaction, wallet, amount);

        return WalletResponse.from(wallet);
    }

    public WalletResponse get(UUID walletId) {
        return WalletResponse.from(getWalletById(walletId));
    }

    private Wallet getWalletById(UUID walletId) {
        Wallet waller;
        try {
            waller = walletRepository.findById(walletId).orElseThrow();
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("Wallet not found");
        } catch (Exception e) {
            log.error("Failed to get wallet {}", walletId, e);
            throw new GetWalletError("Failed to get wallet");
        }
        return waller;
    }

    private Payment getStripePaymentId(Transaction transaction, Wallet wallet, String cardNumber,
        BigDecimal amount) {
        Payment payment;
        try {
            payment = stripeService.charge(cardNumber, amount);
        } catch (StripeAmountTooSmallException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            saveTransaction(transaction);
            throw new ProcessingChargeError("Amount too small");
        } catch (Exception e) {
            log.error("Failed to charge card {} with amount {} for wallet {}", cardNumber, amount,
                wallet, e);
            transaction.setStatus(TransactionStatus.FAILED);
            saveTransaction(transaction);
            throw new ProcessingChargeError("Failed to charge card");
        }
        return payment;
    }

    private Transaction createTransaction(BigDecimal amount, Wallet savedWallet) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setWallet(savedWallet);
        transaction.setStatus(TransactionStatus.INITIATED);

        return saveTransaction(transaction);
    }

    private Transaction charge(Transaction transaction, Wallet wallet,
        String cardNumber, BigDecimal amount) {
        Payment payment = getStripePaymentId(transaction, wallet, cardNumber, amount);

        transaction.setPaymentId(payment.getId());
        transaction.setStatus(TransactionStatus.PROCESSED);
        return saveTransaction(transaction);
    }

    private Wallet addAmountToWalletAndConfirmTransaction(Transaction transaction, Wallet wallet,
        BigDecimal amount) {
        int retries = 1;
        boolean updated = false;
        while (retries <= MAX_WALLET_RETRIES && !updated) {
            try {
                wallet.setAmount(wallet.getAmount().add(amount));
                transaction.setStatus(TransactionStatus.SUCCESS);
                updateWalletAndTransactionTransactionally(wallet, transaction);
                updated = true;
            } catch (OptimisticLockingFailureException e) {
                wallet = getWalletById(wallet.getId());
            }
            retries++;
        }

        if (!updated && (retries > MAX_WALLET_RETRIES)) {
            transaction.setStatus(TransactionStatus.FAILED);
            saveTransaction(transaction);
            throw new ProcessingChargeError("Error updating the Wallet");
        }

        return wallet;
    }

    protected void updateWalletAndTransactionTransactionally(Wallet wallet,
        Transaction transaction) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(
                org.springframework.transaction.TransactionStatus springTransactionStatus) {
                saveWalletAllowingOptimisticLocking(wallet);
                saveTransaction(transaction);
            }
        });
    }

    private Wallet saveWalletAllowingOptimisticLocking(Wallet wallet) {
        try {
            wallet = walletRepository.save(wallet);
        } catch (OptimisticLockingFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to save wallet {}", wallet, e);
            throw new ProcessingChargeError("Failed to save wallet");
        }
        return wallet;
    }

    private Transaction saveTransaction(Transaction transaction) {
        try {
            transaction = transactionRepository.save(transaction);
        } catch (Exception e) {
            log.error("Failed to update transaction {}", transaction, e);
            throw new ProcessingChargeError("Failed to update transaction");
        }
        return transaction;
    }

    public WalletService(WalletRepository walletRepository,
        TransactionRepository transactionRepository, StripeService stripeService,
        PlatformTransactionManager transactionManager) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.stripeService = stripeService;
        this.transactionManager = transactionManager;
    }
}
