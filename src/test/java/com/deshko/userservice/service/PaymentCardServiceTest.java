package com.deshko.userservice.service;

import com.deshko.userservice.config.CacheConfig;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {
    @Mock PaymentCardRepository cardRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentCardMapper cardMapper;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;
    @InjectMocks PaymentCardService cardService;

    private static User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("Ivan");
        u.setSurname("Petrov");
        return u;
    }

    private static PaymentCard card(Long id, User owner, String number) {
        PaymentCard c = new PaymentCard();
        c.setId(id);
        c.setUser(owner);
        c.setNumber(number);
        c.setHolder("IVAN PETROV");
        c.setExpirationDate(LocalDate.now().plusYears(3));
        return c;
    }

    private static PaymentCardRequestDto dto(String number) {
        return new PaymentCardRequestDto(number, "IVAN PETROV", LocalDate.now().plusYears(3));
    }

    private static PaymentCardResponseDto response(Long id, Long userId, String number) {
        return new PaymentCardResponseDto(id, userId, number, "IVAN PETROV",
                LocalDate.now().plusYears(3), true, null, null);
    }

    @Test
    void create_belowLimit_attachesCardToUserAndSaves() {
        User owner = user(1L);
        PaymentCardRequestDto dto = dto("1111111111111111");
        PaymentCard entity = new PaymentCard();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(cardRepository.countByUserIdNative(1L)).thenReturn(4L);
        when(cardRepository.existsByNumber(dto.number())).thenReturn(false);
        when(cardMapper.toEntity(dto)).thenReturn(entity);
        when(cardRepository.save(entity)).thenReturn(entity);
        when(cardMapper.toDto(entity)).thenReturn(response(10L, 1L, dto.number()));

        PaymentCardResponseDto result = cardService.create(1L, dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(entity.getUser()).isSameAs(owner);
        assertThat(owner.getCards()).contains(entity);
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(1L, dto("1111111111111111")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void create_limitReached_throwsCardLimitExceeded() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cardRepository.countByUserIdNative(1L)).thenReturn(5L);

        assertThatThrownBy(() -> cardService.create(1L, dto("1111111111111111")))
                .isInstanceOf(CardLimitExceededException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void create_duplicateNumber_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cardRepository.countByUserIdNative(1L)).thenReturn(0L);
        when(cardRepository.existsByNumber("1111111111111111")).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(1L, dto("1111111111111111")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsDto() {
        PaymentCard entity = card(10L, user(1L), "1111111111111111");
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(entity));
        when(cardMapper.toDto(entity)).thenReturn(response(10L, 1L, entity.getNumber()));

        assertThat(cardService.getById(10L).id()).isEqualTo(10L);
    }

    @Test
    void getById_notFound_throws() {
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentCard entity = card(10L, user(1L), "1111111111111111");
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(cardMapper.toDto(entity)).thenReturn(response(10L, 1L, entity.getNumber()));

        Page<PaymentCardResponseDto> page = cardService.getAll("iv", null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getAllByUserId_success_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentCard entity = card(10L, user(1L), "1111111111111111");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(cardRepository.findAllByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(cardMapper.toDto(entity)).thenReturn(response(10L, 1L, entity.getNumber()));

        assertThat(cardService.getAllByUserId(1L, pageable).getContent()).hasSize(1);
    }

    @Test
    void getAllByUserId_userNotFound_throws() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> cardService.getAllByUserId(1L,
                PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).findAllByUserId(anyLong(), any(Pageable.class));
    }

    @Test
    void update_success_callsRepositoryUpdate() {
        PaymentCard existing = card(10L, user(1L), "1111111111111111");
        PaymentCardRequestDto dto = dto("2222222222222222");
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(existing));
        when(cardRepository.existsByNumber(dto.number())).thenReturn(false);
        when(cardMapper.toDto(existing)).thenReturn(response(10L, 1L, dto.number()));

        PaymentCardResponseDto result = cardService.update(10L, dto);

        verify(cardRepository).updateById(10L, dto.number(), dto.holder(), dto.expirationDate());
        assertThat(result.number()).isEqualTo(dto.number());
    }

    @Test
    void update_sameNumber_skipsDuplicateCheck() {
        PaymentCard existing = card(10L, user(1L), "1111111111111111");
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(existing));
        when(cardMapper.toDto(existing)).thenReturn(response(10L, 1L, existing.getNumber()));

        cardService.update(10L, dto("1111111111111111"));

        verify(cardRepository, never()).existsByNumber(anyString());
    }

    @Test
    void update_numberTakenByAnother_throws() {
        PaymentCard existing = card(10L, user(1L), "1111111111111111");
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(existing));
        when(cardRepository.existsByNumber("2222222222222222")).thenReturn(true);

        assertThatThrownBy(() -> cardService.update(10L, dto("2222222222222222")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(cardRepository, never()).updateById(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void update_notFound_throws() {
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.update(10L, dto("1111111111111111")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setActive_success_updatesFlag(boolean active) {
        PaymentCard entity = card(10L, user(1L), "1111111111111111");
        when(cardRepository.updateActiveById(10L, active)).thenReturn(1);
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(entity));
        when(cardMapper.toDto(entity)).thenReturn(response(10L, 1L, entity.getNumber()));

        assertThat(cardService.setActive(10L, active)).isNotNull();
        verify(cardRepository).updateActiveById(10L, active);
    }

    @Test
    void setActive_notFound_throws() {
        when(cardRepository.updateActiveById(10L, true)).thenReturn(0);

        assertThatThrownBy(() -> cardService.setActive(10L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_success_deletesCardAndEvictsUserCache() {
        PaymentCard existing = card(10L, user(1L), "1111111111111111");
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.of(existing));
        when(cacheManager.getCache(CacheConfig.USERS_CACHE)).thenReturn(cache);

        cardService.delete(10L);

        verify(cardRepository).delete(existing);
        verify(cache).evict(1L);
    }

    @Test
    void delete_notFound_throwsAndDoesNotDelete() {
        when(cardRepository.findByIdWithUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.delete(10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }
}