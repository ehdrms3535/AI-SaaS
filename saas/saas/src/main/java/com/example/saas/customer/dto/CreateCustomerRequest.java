package com.example.saas.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String name,
        String phone,
        @Email String email,
        String memo
) {
}
