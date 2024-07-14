package com.playtomic.tests.wallet.wallet.api;

import com.playtomic.tests.wallet.wallet.dto.CreateWalletRequest;
import com.playtomic.tests.wallet.wallet.dto.WalletResponse;
import com.playtomic.tests.wallet.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
}
