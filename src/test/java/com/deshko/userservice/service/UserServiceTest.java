package com.deshko.userservice.service;

import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.UserRequestDto;
import com.deshko.userservice.dto.UserResponseDto;
import com.deshko.userservice.entity.PaymentCard;
import com.deshko.userservice.entity.User;
import com.deshko.userservice.exception.CardLimitExceededException;
import com.deshko.userservice.exception.DuplicateResourceException;
import com.deshko.userservice.exception.ResourceNotFoundException;
import com.deshko.userservice.mapper.PaymentCardMapper;
import com.deshko.userservice.mapper.UserMapper;
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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock PaymentCardRepository cardRepository;
    @Mock UserMapper userMapper;
    @Mock PaymentCardMapper cardMapper;
    @InjectMocks UserService userService;

    private static final LocalDate BIRTH = LocalDate.of(1995, 5, 20);

    private static UserRequestDto userDto(List<PaymentCardRequestDto> cards) {
        return new UserRequestDto("Ivan", "Petrov", BIRTH, "ivan@mail.com", cards);
    }

    private static PaymentCardRequestDto cardDto(String number) {
        return new PaymentCardRequestDto(number, "IVAN PETROV", LocalDate.now().plusYears(3));
    }

    private static UserResponseDto response(Long id) {
        return new UserResponseDto(id, "Ivan", "Petrov", BIRTH,
                "ivan@mail.com", true, null, null, List.of());
    }

    private static User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setName("Ivan");
        u.setSurname("Petrov");
        u.setBirthDate(BIRTH);
        u.setEmail(email);
        return u;
    }

    @Test
    void create_withoutCards_savesUser() {
        UserRequestDto dto = userDto(null);
        User entity = user(null, dto.email());
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(response(1L));

        UserResponseDto result = userService.create(dto);

        assertThat(result.id()).isEqualTo(1L);
        verify(userRepository).save(entity);
    }

    @Test
    void create_withCards_attachesCardsToUser() {
        PaymentCardRequestDto c1 = cardDto("1111111111111111");
        PaymentCardRequestDto c2 = cardDto("2222222222222222");
        UserRequestDto dto = userDto(List.of(c1, c2));
        User entity = user(null, dto.email());
        PaymentCard card1 = new PaymentCard();
        PaymentCard card2 = new PaymentCard();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(cardRepository.existsByNumber(anyString())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(entity);
        when(cardMapper.toEntity(c1)).thenReturn(card1);
        when(cardMapper.toEntity(c2)).thenReturn(card2);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(response(1L));

        userService.create(dto);

        assertThat(entity.getCards()).containsExactly(card1, card2);
        assertThat(card1.getUser()).isSameAs(entity);
        assertThat(card2.getUser()).isSameAs(entity);
    }

    @Test
    void create_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("ivan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(userDto(null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_moreThanFiveCards_throwsCardLimitExceeded() {
        List<PaymentCardRequestDto> cards = IntStream.range(0, 6)
                .mapToObj(i -> cardDto("100000000000000" + i))
                .toList();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.create(userDto(cards)))
                .isInstanceOf(CardLimitExceededException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateCardNumber_throws() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(cardRepository.existsByNumber("1111111111111111")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(userDto(List.of(cardDto("1111111111111111")))))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsDto() {
        User entity = user(1L, "ivan@mail.com");
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(response(1L));

        assertThat(userService.getById(1L).id()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throws() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User entity = user(1L, "ivan@mail.com");
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(userMapper.toDto(entity)).thenReturn(response(1L));

        Page<UserResponseDto> page = userService.getAll("iv", null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_success_callsRepositoryUpdate() {
        User existing = user(1L, "old@mail.com");
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("ivan@mail.com")).thenReturn(false);
        when(userMapper.toDto(existing)).thenReturn(response(1L));

        UserResponseDto result = userService.update(1L, userDto(null));

        verify(userRepository).updateById(1L, "Ivan",
                "Petrov", BIRTH, "ivan@mail.com");
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void update_sameEmail_skipsDuplicateCheck() {
        User existing = user(1L, "IVAN@mail.com");
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(existing));
        when(userMapper.toDto(existing)).thenReturn(response(1L));

        userService.update(1L, userDto(null));

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).updateById(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void update_emailTakenByAnother_throws() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(user(1L, "old@mail.com")));
        when(userRepository.existsByEmail("ivan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, userDto(null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).updateById(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void update_notFound_throws() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, userDto(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setActive_success_updatesFlag(boolean active) {
        User entity = user(1L, "ivan@mail.com");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.updateActiveById(1L, active)).thenReturn(1);
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(response(1L));

        UserResponseDto result = userService.setActive(1L, active);

        assertThat(result).isNotNull();
        verify(userRepository).updateActiveById(1L, active);
    }

    @Test
    void setActive_notFound_throws() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> userService.setActive(1L, true))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).updateActiveById(anyLong(), anyBoolean());
    }

    @Test
    void delete_success_deletesEntity() {
        User existing = user(1L, "ivan@mail.com");
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(existing));

        userService.delete(1L);

        verify(userRepository).delete(existing);
    }

    @Test
    void delete_notFound_throwsAndDoesNotDelete() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}
