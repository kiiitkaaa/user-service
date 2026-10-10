package com.deshko.userservice.dto;

import com.deshko.userservice.config.BusinessRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record UserRequestDto(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 100)
        String surname,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Valid
        @Size(max = BusinessRules.MAX_CARDS,
                message = "User cannot have more than " + BusinessRules.MAX_CARDS + "cards")
        List<PaymentCardRequestDto> cards
) {}