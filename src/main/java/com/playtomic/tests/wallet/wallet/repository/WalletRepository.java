package com.playtomic.tests.wallet.wallet.repository;

import com.playtomic.tests.wallet.wallet.model.Wallet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    <S extends Wallet> S save(S wallet);

}
