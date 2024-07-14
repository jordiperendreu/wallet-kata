package com.playtomic.tests.wallet.wallet.repository;

import com.playtomic.tests.wallet.wallet.model.Transaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    <S extends Transaction> S save(S transaction);

}
