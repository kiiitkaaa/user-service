package com.deshko.userservice.integration;

import com.deshko.userservice.config.CacheConfig;
import com.deshko.userservice.dto.UserRequestDto;
import com.deshko.userservice.entity.PaymentCard;
import org.junit.jupiter.api.Test;
import com.deshko.userservice.entity.User;
import org.springframework.cache.Cache;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import org.springframework.http.MediaType;

public class UserCacheIntegrationTest extends AbstractIntegrationTest {
    private Cache usersCache() {
        return cacheManager.getCache(CacheConfig.USERS_CACHE);
    }

    @Test
    void getUser_isCachedInRedis_andServedFromCache() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        assertThat(usersCache().get(user.getId())).isNull();

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isOk());
        assertThat(usersCache().get(user.getId())).isNotNull();

        User stored = userRepository.findById(user.getId()).orElseThrow();
        stored.setName("Changed");
        userRepository.save(stored);

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.name").value("Ivan"));
    }

    @Test
    void updateUser_refreshesCache() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        mockMvc.perform(get("/api/v1/users/{id}", user.getId())).andExpect(status().isOk());

        UserRequestDto body = new UserRequestDto("Petr", "Ivanov",
                LocalDate.of(1990, 2, 2), "petr@mail.com", null);
        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.email").value("petr@mail.com"));
    }

    @Test
    void deactivateUser_refreshesCache() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        mockMvc.perform(get("/api/v1/users/{id}", user.getId())).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/users/{id}/status", user.getId())
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteUser_evictsCache() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        mockMvc.perform(get("/api/v1/users/{id}", user.getId())).andExpect(status().isOk());
        assertThat(usersCache().get(user.getId())).isNotNull();

        mockMvc.perform(delete("/api/v1/users/{id}",
                user.getId())).andExpect(status().isNoContent());

        assertThat(usersCache().get(user.getId())).isNull();
        mockMvc.perform(get("/api/v1/users/{id}", user.getId())).andExpect(status().isNotFound());
    }

    @Test
    void createCard_evictsUserCache_soNewCardIsVisible() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.cards", hasSize(0)));
        assertThat(usersCache().get(user.getId())).isNotNull();

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("1111111111111111"))))
                .andExpect(status().isCreated());

        assertThat(usersCache().get(user.getId())).isNull();
        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.cards", hasSize(1)));
    }

    @Test
    void updateAndDeleteCard_evictUserCache() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        PaymentCard card = saveCard(user, "1111111111111111");

        mockMvc.perform(get("/api/v1/users/{id}", user.getId())).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/cards/{id}/status", card.getId())
                        .param("active", "false"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.cards[0].active").value(false));

        mockMvc.perform(delete("/api/v1/cards/{id}",
                card.getId())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(jsonPath("$.cards", hasSize(0)));
    }
}
