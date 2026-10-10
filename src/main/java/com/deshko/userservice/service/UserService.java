package com.deshko.userservice.service;

import com.deshko.userservice.config.BusinessRules;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.UserRequestDto;
import com.deshko.userservice.dto.UserResponseDto;
import com.deshko.userservice.entity.User;
import com.deshko.userservice.exception.CardLimitExceededException;
import com.deshko.userservice.exception.DuplicateResourceException;
import com.deshko.userservice.exception.ResourceNotFoundException;
import com.deshko.userservice.mapper.PaymentCardMapper;
import com.deshko.userservice.mapper.UserMapper;
import com.deshko.userservice.repository.PaymentCardRepository;
import com.deshko.userservice.repository.UserRepository;
import com.deshko.userservice.specification.UserSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("User with email " + dto.email() + " already exists");
        }

        List<PaymentCardRequestDto> cardDtos = dto.cards() == null ? List.of() : dto.cards();
        if (cardDtos.size() > BusinessRules.MAX_CARDS) {
            throw new CardLimitExceededException(BusinessRules.MAX_CARDS);
        }
        cardDtos.forEach(c -> {
            if (paymentCardRepository.existsByNumber(c.number())) {
                throw new DuplicateResourceException("Card with number " + c.number() + " already exists");
            }
        });

        User user = userMapper.toEntity(dto);
        cardDtos.stream()
                .map(paymentCardMapper::toEntity)
                .forEach(user::addCard);

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        return userMapper.toDto(findUserWithCards(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAll(String name, String surname, Pageable pageable) {
        return userRepository
                .findAll(UserSpecifications.byNameAndSurname(name, surname), pageable)
                .map(userMapper::toDto);
    }

    @Transactional
    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = findUserWithCards(id);
        if (!user.getEmail().equalsIgnoreCase(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("User with email " + dto.email() + " already exists");
        }
        userRepository.updateById(id, dto.name(), dto.surname(), dto.birthDate(), dto.email());
        return getById(id);
    }

    @Transactional
    public UserResponseDto setActive(Long id, boolean active) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.updateActiveById(id, active);
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findUserWithCards(id));
    }

    private User findUserWithCards(Long id) {
        return userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
