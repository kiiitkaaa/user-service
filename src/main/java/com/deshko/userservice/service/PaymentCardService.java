package com.deshko.userservice.service;

import com.deshko.userservice.config.BusinessRules;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.PaymentCardResponseDto;
import com.deshko.userservice.entity.PaymentCard;
import com.deshko.userservice.entity.User;
import com.deshko.userservice.exception.CardLimitExceededException;
import com.deshko.userservice.exception.DuplicateResourceException;
import com.deshko.userservice.exception.ResourceNotFoundException;
import com.deshko.userservice.mapper.PaymentCardMapper;
import com.deshko.userservice.repository.PaymentCardRepository;
import com.deshko.userservice.repository.UserRepository;
import com.deshko.userservice.specification.CardSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCardService {
    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public PaymentCardResponseDto create(Long userId, PaymentCardRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (paymentCardRepository.countByUserIdNative(userId) >= BusinessRules.MAX_CARDS) {
            throw new CardLimitExceededException(BusinessRules.MAX_CARDS);
        }
        if (paymentCardRepository.existsByNumber(dto.number())) {
            throw new DuplicateResourceException("Card with number " + dto.number() + " already exists");
        }

        PaymentCard card = paymentCardMapper.toEntity(dto);
        user.addCard(card);
        return paymentCardMapper.toDto(paymentCardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public PaymentCardResponseDto getById(Long id) {
        return paymentCardMapper.toDto(findCard(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponseDto> getAll(String ownerName, String ownerSurname, Pageable pageable) {
        return paymentCardRepository
                .findAll(CardSpecifications.byOwnerNameAndSurname(ownerName, ownerSurname), pageable)
                .map(paymentCardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponseDto> getAllByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return paymentCardRepository.findAllByUserId(userId, pageable).map(paymentCardMapper::toDto);
    }

    @Transactional
    public PaymentCardResponseDto update(Long id, PaymentCardRequestDto dto) {
        PaymentCard card = findCard(id);
        if (!card.getNumber().equals(dto.number()) && paymentCardRepository.existsByNumber(dto.number())) {
            throw new DuplicateResourceException("Card with number " + dto.number() + " already exists");
        }

        paymentCardRepository.updateById(id, dto.number(), dto.holder(), dto.expirationDate());
        return getById(id);
    }

    @Transactional
    public PaymentCardResponseDto setActive(Long id, boolean active) {
        if (paymentCardRepository.updateActiveById(id, active) == 0) {
            throw new ResourceNotFoundException("Card", id);
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        paymentCardRepository.delete(findCard(id));
    }

    private PaymentCard findCard(Long id) {
        return paymentCardRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }
}
