package com.deshko.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardResponseDto(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}