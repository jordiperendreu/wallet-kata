package com.playtomic.tests.wallet.wallet.api;

import com.playtomic.tests.wallet.wallet.dto.CreateWalletRequest;
import com.playtomic.tests.wallet.wallet.dto.TopUpRequest;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class WalletController {

    private final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;

    @PostMapping("/v1/wallet/")
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody CreateWalletRequest request) {
        log.info("Creating wallet for user {}", request.getUserId());

        WalletResponse walletResponse = walletService.create(request.getUserId());

        return new ResponseEntity<>(walletResponse, HttpStatus.CREATED);
    }

    @PostMapping("/v1/wallet/{walletId}/actions/topup/")
    public ResponseEntity<WalletResponse> topUp(@PathVariable UUID walletId,
        @Valid @RequestBody TopUpRequest topUpRequest) {
        log.info("Top-up wallet {} with card number {} and amount {}", walletId,
            topUpRequest.getCardNumber(), topUpRequest.getAmount());

        WalletResponse walletResponse = walletService.topUp(walletId, topUpRequest.getCardNumber(),
            topUpRequest.getAmount());

        return new ResponseEntity<>(walletResponse, HttpStatus.ACCEPTED);
    }

    @GetMapping("/v1/wallet/{walletId}")
    public ResponseEntity<WalletResponse> get(@PathVariable UUID walletId) {
        return new ResponseEntity<>(walletService.get(walletId), HttpStatus.OK);
    }

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
}
