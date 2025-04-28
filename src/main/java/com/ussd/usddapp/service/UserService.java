package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.repository.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.cache.annotation.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "userBalance", key = "#phoneNumber")
    public double getUserBalance(String phoneNumber) {
        log.info("Fetching balance from database for phoneNumber={}", phoneNumber);
        User user = userRepository.findByPhoneNumber(phoneNumber);
        double balance = user != null ? user.getBalance() : 0.0;
        log.info("Balance retrieved from database: phoneNumber={}, balance={}", phoneNumber, balance);
        return balance;
    }

    @CacheEvict(value = "userBalance", key = "#phoneNumber")
    public void updateUserBalance(String phoneNumber, double newBalance) {
        log.debug("Updating balance for phoneNumber={}, newBalance={}", phoneNumber, newBalance);
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            user.setBalance(newBalance);
            userRepository.save(user);
            log.info("Balance updated: phoneNumber={}, newBalance={}", phoneNumber, newBalance);
        } else {
            log.warn("User not found for balance update: phoneNumber={}", phoneNumber);
        }
    }
}