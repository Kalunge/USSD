package com.ussd.usddapp.controler;


import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ApiTestController {

    private final AccountValidationApi accountValidationApi;
    private final DepositApi depositApi;



    @Value("${api.key}")
    private String apiKey;

    @PostMapping("/deposit")
    public ResponseEntity<?> testDeposit(@Validated @RequestBody DepositRequest request) {
        try {
            request.setApiKey(apiKey);
            log.info("Received deposit request: {}", request);
            log.info("Received deposit request: account={}, amount={}, apiKey={}", request.getAccount1(), request.getAmount(), request.getApiKey());

            // Validate required fields
            if (request.getAccount1() == null || request.getAccount1().trim().isEmpty()) {
                log.warn("Invalid deposit request: account1 is empty");
                return ResponseEntity.badRequest().body("Account number (account1) is required");
            }
            if (request.getAmount() <= 0) {
                log.warn("Invalid deposit request: amount must be greater than 0");
                return ResponseEntity.badRequest().body("Amount must be greater than 0");
            }

            DepositResponse response = depositApi.performDeposit(request);
            log.info("Deposit successful: tnxCode={}", response.getTnxCode());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing deposit request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process deposit: " + e.getMessage());
        }
    }


    @PostMapping("/validate-account")
    public ResponseEntity<?> testValidateAccount(@Validated @RequestBody AccountValidationRequest request) {
        request.setApiKey(apiKey);
        log.info("Received validate-account request: {}", request);
        try {
            AccountValidationResponse response = accountValidationApi.validateAccount(request);
            log.info("Validate-account successful: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in validate-account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to validate account: " + e.getMessage());
        }
    }
}