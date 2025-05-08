package com.ussd.usddapp.dto;

import lombok.*;

@Data
public class UssdSession {
    public enum State {
        INIT, SELECT_BANK, MENU, ENTER_ACCOUNT, ENTER_AMOUNT, CONFIRM_ACCOUNT, ENTER_PIN,
        SELECT_MOBILE_MONEY_OPTION, ENTER_PHONE, ENTER_MOBILE_AMOUNT, CONFIRM_MOBILE, ENTER_MOBILE_PIN,
        ENTER_WITHDRAW_AMOUNT, CONFIRM_WITHDRAW, ENTER_WITHDRAW_PIN, SELECT_TELCO,

        ENTER_RECIPIENT_ACCOUNT, ENTER_ADMISSION_NUMBER, ENTER_DEPOSITED_BY, CONFIRM_LIPA_KARO, SELECT_LIPA_KARO
    }

    private State state = State.INIT;
    private String bank;
    private String accountNumber;
    private double amount;
    private String pin;
    private AccountValidationResponse accountValidationResponse;
    private String mobilePhone;
    private String mobileTransactionType;
    private String accountName;
    private String telco;

    private String transactionType;
    private String recipientAccount;
    private String admissionNumber;
    private String depositedBy;
}