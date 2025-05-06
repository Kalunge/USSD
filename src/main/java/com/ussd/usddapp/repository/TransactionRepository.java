package com.ussd.usddapp.repository;

import com.ussd.usddapp.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
