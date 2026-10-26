package com.deshko.userservice.util;

import com.deshko.userservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("cardAccess")
@RequiredArgsConstructor
public class CardAccess {
    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long cardId, Authentication authentication) {
        return paymentCardRepository.existsByIdAndUserId(cardId, Long.valueOf(authentication.getName()));
    }
}