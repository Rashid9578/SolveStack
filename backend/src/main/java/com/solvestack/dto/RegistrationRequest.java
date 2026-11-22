package com.solvestack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.solvestack.validation.BcryptPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Email(regexp = "[\\x21-\\x7e]+") @Size(max = 254) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(min = 12, message = "Use at least 12 characters.") @BcryptPassword String password) {}
