package com.playtomic.tests.wallet.wallet.service;

import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.exception.CreateWalletError;
import com.playtomic.tests.wallet.wallet.model.Wallet;
import com.playtomic.tests.wallet.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final Logger log = LoggerFactory.getLogger(WalletService.class);

    public WalletRepository walletRepository;

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

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }
}
