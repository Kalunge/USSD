package com.ussd.usddapp.dto;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime transactionDate;
    private String depositorName;
    private String depositorId;
    private String transactionType;
    private double amount;
    private String mobileNumber;
    private String transactionNumber;
    private boolean isSigned;

}