package com.ussd.usddapp.controler;


import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.dto.Transaction;
import com.ussd.usddapp.repository.*;
import com.ussd.usddapp.service.*;
import com.ussd.usddapp.util.*;
import jakarta.transaction.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.cache.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class UssdController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SessionService sessionService;
    private final MenuService menuService;
    private final UserService userService;

    @PostMapping(value = "/ussd", consumes = "application/x-www-form-urlencoded")
    @Transactional
    public String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        log.debug("Received USSD request: sessionId={}, phoneNumber={}, text={}", sessionId, phoneNumber, text);

        // Check if session has expired and reset if necessary
        String state = sessionService.getState(sessionId);
        if (state == null && !text.isEmpty()) {
            log.debug("Session likely expired for sessionId={}, resetting to START", sessionId);
            sessionService.clearSession(sessionId);
            state = "START";
        }

        // Initialize user if not exists
        log.debug("Fetching user for phoneNumber={}", phoneNumber);
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

        // Log the current state
        log.debug("Current state: {}, Input level: {}, Latest input: {}", state, inputLevel, latestInput);

        // Handle the initial request (text is empty) or if state is START
        if (text.isEmpty() || state.equals("START")) {
            if (user.hasPassword()) {
                sessionService.setState(sessionId, "ENTER_PIN");
                log.debug("User has PIN, transitioning to ENTER_PIN");
                return menuService.getEnterPinPrompt();
            } else {
                sessionService.setState(sessionId, "SET_PIN");
                log.debug("User has no PIN, transitioning to SET_PIN");
                return menuService.getSetPinPrompt();
            }
        }

        // Process based on state and input level
        try {
            if (state.equals("ENTER_PIN")) {
                if (user.isPasswordValid(latestInput)) {
                    sessionService.setState(sessionId, "MENU");
                    log.debug("PIN validated, transitioning to MENU");
                    String response = menuService.getMainMenu();
                    log.debug("Returning menu response: {}", response);
                    return response;
                } else {
                    log.debug("Invalid PIN entered: {}", latestInput);
                    return menuService.getInvalidPinMessage();
                }
            } else if (state.equals("SET_PIN")) {
                if (latestInput.length() != 4 || !latestInput.matches("\\d{4}")) {
                    log.debug("Invalid PIN format: {}", latestInput);
                    return "CON Invalid PIN. Please enter a 4-digit number:";
                }
                sessionService.setData(sessionId, "temp_pin", latestInput);
                sessionService.setState(sessionId, "CONFIRM_PIN");
                log.debug("PIN entered, transitioning to CONFIRM_PIN");
                return menuService.getConfirmPinPrompt();
            } else if (state.equals("CONFIRM_PIN")) {
                String tempPin = sessionService.getData(sessionId, "temp_pin", "");
                if (latestInput.equals(tempPin)) {
                    user.setPassword(latestInput);
                    userRepository.save(user);
                    sessionService.clearSession(sessionId); // Clear temp data
                    sessionService.setState(sessionId, "MENU");
                    log.debug("PIN confirmed and set, transitioning to MENU");
                    String response = menuService.getPinSetSuccessMessage() + "\n" + menuService.getMainMenu();
                    log.debug("Returning PIN set success and menu: {}", response);
                    return response;
                } else {
                    sessionService.clearSession(sessionId); // Clear temp data
                    sessionService.setState(sessionId, "SET_PIN");
                    log.debug("PIN confirmation failed, transitioning back to SET_PIN");
                    return menuService.getPinMismatchMessage();
                }
            } else if (state.equals("MENU")) {
                switch (latestInput) {
                    case "1":
                        sessionService.setState(sessionId, "CHECK_BALANCE");
                        log.debug("Transition to CHECK_BALANCE");
                        double balance = userService.getUserBalance(phoneNumber);
                        log.info("Balance retrieved for display: phoneNumber={}, balance={}", phoneNumber, balance);
                        return menuService.getBalanceMessage(balance);
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
                        double balance = userService.getUserBalance(phoneNumber);
                        log.info("Balance retrieved for display: phoneNumber={}, balance={}", phoneNumber, balance);
                        return menuService.getBalanceMessage(balance);
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
                    log.debug("Processing TRANSFER_AMOUNT: parsing amount={}", latestInput);
                    double amount = Double.parseDouble(latestInput);
                    log.debug("Amount parsed successfully: amount={}", amount);
                    sessionService.setState(sessionId, "TRANSFER_PHONE");
                    log.debug("Setting data for amount: sessionId={}, amount={}", sessionId, amount);
                    sessionService.setData(sessionId, "amount", String.valueOf(amount));
                    log.debug("Amount: {}, Transition to TRANSFER_PHONE", amount);
                    String response = menuService.getTransferPhonePrompt();
                    log.debug("Returning response for TRANSFER_PHONE: {}", response);
                    return response;
                } catch (NumberFormatException e) {
                    log.warn("Invalid amount entered: {}", latestInput);
                    sessionService.clearSession(sessionId);
                    return menuService.getInvalidAmountMessage();
                }
            } else if (state.equals("TRANSFER_PHONE")) {
                log.debug("Processing TRANSFER_PHONE: retrieving amount for sessionId={}", sessionId);
                double amount = Double.parseDouble(sessionService.getData(sessionId, "amount", "0"));
                log.debug("Amount retrieved: amount={}", amount);
                double balance = user.getBalance();
                log.debug("Checking balance: balance={}, amount={}", balance, amount);
                if (amount <= balance) {
                    double newBalance = balance - amount;
                    user.setBalance(newBalance);

                    log.debug("Updating balance: phoneNumber={}, newBalance={}", phoneNumber, newBalance);
                    userService.updateUserBalance(phoneNumber, newBalance);

                    log.debug("Saving transaction: amount={}, recipient={}", amount, latestInput);
                    Transaction transaction = new Transaction();
                    transaction.setUser(user);
                    transaction.setAmount(amount);
                    transaction.setRecipientPhone(latestInput);
                    transactionRepository.save(transaction);

                    log.debug("Clearing session after transfer: sessionId={}", sessionId);
                    sessionService.clearSession(sessionId);
                    log.info("Transfer successful: amount={}, recipient={}, new balance={}", amount, latestInput, user.getBalance());
                    String response = menuService.getTransferSuccessMessage(amount, latestInput);
                    log.debug("Returning transfer success response: {}", response);
                    return response;
                } else {
                    log.warn("Insufficient funds: amount={}, balance={}", amount, balance);
                    sessionService.clearSession(sessionId);
                    return menuService.getInsufficientFundsMessage();
                }
            } else {
                log.error("Invalid state: {}", state);
                sessionService.clearSession(sessionId);
                return menuService.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("Unexpected error processing USSD request: {}", e.getMessage(), e);
            sessionService.clearSession(sessionId);
            return menuService.getErrorMessage();
        }
    }

    @GetMapping("/test-cache/{phoneNumber}")
    public String testCache(@PathVariable String phoneNumber) {
        log.info("Testing cache for phoneNumber={}", phoneNumber);
        double balance = userService.getUserBalance(phoneNumber);
        return "Balance for " + phoneNumber + ": " + balance;
    }
}
