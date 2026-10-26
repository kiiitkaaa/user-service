package com.deshko.userservice.integration;

import com.deshko.userservice.config.CacheConfig;
import com.deshko.userservice.config.TestContainersConfig;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.UserRequestDto;
import com.deshko.userservice.entity.PaymentCard;
import com.deshko.userservice.entity.User;
import com.deshko.userservice.repository.PaymentCardRepository;
import com.deshko.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
abstract class AbstractIntegrationTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PaymentCardRepository cardRepository;
    @Autowired protected CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
        Cache cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    protected String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    protected User saveUser(String name, String surname, String email) {
        User u = new User();
        u.setName(name);
        u.setSurname(surname);
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setEmail(email);
        return userRepository.save(u);
    }

    protected PaymentCard saveCard(User owner, String number) {
        PaymentCard c = new PaymentCard();
        c.setUser(owner);
        c.setNumber(number);
        c.setHolder(owner.getName().toUpperCase());
        c.setExpirationDate(LocalDate.now().plusYears(3));
        return cardRepository.save(c);
    }

    protected UserRequestDto userRequest(String email, List<PaymentCardRequestDto> cards) {
        return new UserRequestDto("Ivan", "Petrov",
                LocalDate.of(1995, 5, 20), email, cards);
    }

    protected PaymentCardRequestDto cardRequest(String number) {
        return new PaymentCardRequestDto(number, "IVAN PETROV", LocalDate.now().plusYears(3));
    }

    protected static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt()
                .jwt(j -> j.subject("0").claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    protected static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user(Long userId) {
        return jwt()
                .jwt(j -> j.subject(userId.toString()).claim("role", "USER"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
