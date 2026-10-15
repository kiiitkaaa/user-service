package com.deshko.userservice.integration;

import com.deshko.userservice.dto.UserRequestDto;
import org.junit.jupiter.api.Test;
import java.util.List;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.entity.User;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import java.util.stream.IntStream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import org.springframework.http.MediaType;

class UserControllerIntegrationTest extends AbstractIntegrationTest {
    @Test
    void createUser_returns201_andPersistsUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRequest("ivan@mail.com", null))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ivan@mail.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(userRepository.existsByEmail("ivan@mail.com")).isTrue();
    }

    @Test
    void createUser_withCards_cascadesCardsToDb() throws Exception {
        UserRequestDto body = userRequest("ivan@mail.com",
                List.of(cardRequest("1111111111111111"),
                        cardRequest("2222222222222222")));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cards", hasSize(2)));

        assertThat(cardRepository.count()).isEqualTo(2);
    }

    @Test
    void createUser_invalidBody_returns400WithDetails() throws Exception {
        UserRequestDto body = new UserRequestDto("", "Petrov",
                LocalDate.of(1995, 5, 20), "not-an-email", null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.email").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        saveUser("Ivan", "Petrov", "ivan@mail.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRequest("ivan@mail.com", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_moreThanFiveCards_returns400() throws Exception {
        List<PaymentCardRequestDto> cards = IntStream.range(0, 6)
                .mapToObj(i -> cardRequest("100000000000000" + i))
                .toList();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRequest("ivan@mail.com", cards))))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void getUserById_returnsUserWithCards() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        saveCard(user, "1111111111111111");

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ivan@mail.com"))
                .andExpect(jsonPath("$.cards", hasSize(1)))
                .andExpect(jsonPath("$.cards[0].number").value("1111111111111111"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAllUsers_filtersByNameAndSurname_withPagination() throws Exception {
        saveUser("Ivan", "Petrov", "ivan.p@mail.com");
        saveUser("Ivan", "Sidorov", "ivan.s@mail.com");
        saveUser("Anna", "Petrova", "anna@mail.com");

        mockMvc.perform(get("/api/v1/users")
                        .param("name", "ivan")
                        .param("size", "1")
                        .param("page", "0")
                        .param("sort", "surname,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].surname").value("Petrov"));

        mockMvc.perform(get("/api/v1/users")
                        .param("name", "ivan")
                        .param("surname", "sid"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("ivan.s@mail.com"));
    }

    @Test
    void getAllUsers_unknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("sort", "nonexistent,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_returns200_andUpdatesDb() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        UserRequestDto body = new UserRequestDto("Petr", "Ivanov",
                LocalDate.of(1990, 2, 2), "petr@mail.com", null);

        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.email").value("petr@mail.com"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Petr");
        assertThat(updated.getSurname()).isEqualTo("Ivanov");
    }

    @Test
    void updateUser_emailTakenByAnother_returns409() throws Exception {
        saveUser("Anna", "Petrova", "anna@mail.com");
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");

        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRequest("anna@mail.com", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRequest("ivan@mail.com", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivateAndActivateUser_changesFlagInDb() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");

        mockMvc.perform(patch("/api/v1/users/{id}/status", user.getId())
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertThat(userRepository.findById(user.getId()).orElseThrow().isActive()).isFalse();

        mockMvc.perform(patch("/api/v1/users/{id}/status", user.getId())
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        assertThat(userRepository.findById(user.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void deleteUser_returns204_andCascadesCards() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        saveCard(user, "1111111111111111");
        saveCard(user, "2222222222222222");

        mockMvc.perform(delete("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(cardRepository.count()).isZero();

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isNotFound());
    }
}
