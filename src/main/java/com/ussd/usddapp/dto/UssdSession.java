package com.ussd.usddapp.dto;

import lombok.*;

@Data
public class UssdSession {
    public enum State {INIT, SELECT_BANK, MENU, ENTER_ACCOUNT, ENTER_AMOUNT, CONFIRM_ACCOUNT, ENTER_PIN}

    private State state = State.INIT;
    private String bank;
    private String accountNumber;
    private double amount;
    private String pin;
    private AccountValidationResponse accountValidationResponse;
}