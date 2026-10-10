package com.deshko.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PaymentCardRequestDto(
        @NotBlank
        @Pattern(regexp = "\\d{16}", message = "Card number must contain 16 digits")
        String number,

        @NotBlank
        @Size(max = 200)
        String holder,

        @NotNull
        @FutureOrPresent
        LocalDate expirationDate
) {}