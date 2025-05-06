package com.ussd.usddapp.dto;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Double balance = 100.0;

    @Column(name = "password", length = 4)
    private String password;

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public boolean isPasswordValid(String inputPin) {
        return password != null && password.equals(inputPin);
    }
}
