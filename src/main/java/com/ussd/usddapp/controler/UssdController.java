package com.ussd.usddapp.controler;


import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.repository.*;
import com.ussd.usddapp.service.*;
import com.ussd.usddapp.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UssdController {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SessionService sessionService;
    private final MenuService menuService;

    @PostMapping(value = "/ussd", consumes = "application/x-www-form-urlencoded")
    public String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        log.debug("Request: sessionId={}, phoneNumber={}, text={}", sessionId, phoneNumber, text);

        // Initialize user if not exists
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null) {
            user = new User();
            user.setPhoneNumber(phoneNumber);
            user.setBalance(100.0);
            userRepository.save(user);
            log.info("New user created: phoneNumber={}", phoneNumber);
        }

        // Split the text input by '*' to handle concatenated inputs
        String[] inputs = text.isEmpty() ? new String[0] : text.split("\\*");
        String latestInput = inputs.length > 0 ? inputs[inputs.length - 1] : "";
        int inputLevel = inputs.length;

        // Retrieve session state
        String state = sessionService.getState(sessionId);
        log.debug("Current state: {}, Input level: {}, Latest input: {}", state, inputLevel, latestInput);

        // Handle the initial request (text is empty)
        if (text.isEmpty()) {
            sessionService.setState(sessionId, "MENU");
            log.debug("Transition to MENU");
            return menuService.getMainMenu();
        }

        // Process based on state and input level
        try {
            if (state.equals("MENU")) {
                switch (latestInput) {
                    case "1":
                        sessionService.setState(sessionId, "CHECK_BALANCE");
                        log.debug("Transition to CHECK_BALANCE");
                        return menuService.getBalanceMessage(user.getBalance());
                    case "2":
                        sessionService.setState(sessionId, "TRANSFER_AMOUNT");
                        log.debug("Transition to TRANSFER_AMOUNT");
                        return menuService.getTransferAmountPrompt();
                    case "0":
                        sessionService.clearSession(sessionId);
                        log.debug("Session ended");
                        return menuService.getExitMessage();
                    default:
                        return menuService.getInvalidOptionMessage();
                }
            } else if (state.equals("CHECK_BALANCE")) {
                sessionService.setState(sessionId, "MENU");
                log.debug("Transition back to MENU after CHECK_BALANCE");
                switch (latestInput) {
                    case "1":
                        sessionService.setState(sessionId, "CHECK_BALANCE");
                        log.debug("Transition to CHECK_BALANCE");
                        return menuService.getBalanceMessage(user.getBalance());
                    case "2":
                        sessionService.setState(sessionId, "TRANSFER_AMOUNT");
                        log.debug("Transition to TRANSFER_AMOUNT");
                        return menuService.getTransferAmountPrompt();
                    case "0":
                        sessionService.clearSession(sessionId);
                        log.debug("Session ended");
                        return menuService.getExitMessage();
                    default:
                        return menuService.getInvalidOptionMessage();
                }
            } else if (state.equals("TRANSFER_AMOUNT")) {
                try {
                    double amount = Double.parseDouble(latestInput);
                    sessionService.setState(sessionId, "TRANSFER_PHONE");
                    sessionService.setData(sessionId, "amount", String.valueOf(amount));
                    log.debug("Amount: {}, Transition to TRANSFER_PHONE", amount);
                    return menuService.getTransferPhonePrompt();
                } catch (NumberFormatException e) {
                    sessionService.clearSession(sessionId);
                    log.warn("Invalid amount entered: {}", latestInput);
                    return menuService.getInvalidAmountMessage();
                }
            } else if (state.equals("TRANSFER_PHONE")) {
                double amount = Double.parseDouble(sessionService.getData(sessionId, "amount", "0"));
                double balance = user.getBalance();
                if (amount <= balance) {
                    user.setBalance(balance - amount);
                    userRepository.save(user);

                    Transaction transaction = new Transaction();
                    transaction.setUser(user);
                    transaction.setAmount(amount);
                    transaction.setRecipientPhone(latestInput);
                    transactionRepository.save(transaction);

                    sessionService.clearSession(sessionId);
                    log.info("Transfer successful: amount={}, recipient={}, new balance={}", amount, latestInput, user.getBalance());
                    return menuService.getTransferSuccessMessage(amount, latestInput);
                } else {
                    sessionService.clearSession(sessionId);
                    log.warn("Insufficient funds: amount={}, balance={}", amount, balance);
                    return menuService.getInsufficientFundsMessage();
                }
            } else {
                log.error("Invalid state: {}", state);
                return menuService.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("Unexpected error processing USSD request: {}", e.getMessage(), e);
            sessionService.clearSession(sessionId);
            return menuService.getErrorMessage();
        }
    }
}
