package com.example.saas.customer.dto;

import jakarta.validation.constraints.Email;

public record UpdateCustomerRequest(
        String name,
        String phone,
        @Email String email,
        String memo
) {
}
