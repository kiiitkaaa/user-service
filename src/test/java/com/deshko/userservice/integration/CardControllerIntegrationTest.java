package com.deshko.userservice.integration;

import com.deshko.userservice.entity.PaymentCard;
import org.junit.jupiter.api.Test;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.entity.User;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.containsString;

class CardControllerIntegrationTest extends AbstractIntegrationTest {
    @Test
    void createCard_returns201_withLocation() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("1111111111111111"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/cards/")))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(cardRepository.findAllByUserId(user.getId())).hasSize(1);
    }

    @Test
    void createCard_sixthCard_returns409_andNothingSaved() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        for (int i = 0; i < 5; i++) {
            saveCard(user, "100000000000000" + i);
        }

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("9999999999999999"))))
                .andExpect(status().isConflict());

        assertThat(cardRepository.countByUserIdNative(user.getId())).isEqualTo(5);
    }

    @Test
    void createCard_fifthCard_isAllowed() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        for (int i = 0; i < 4; i++) {
            saveCard(user, "100000000000000" + i);
        }

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("9999999999999999"))))
                .andExpect(status().isCreated());
    }

    @Test
    void createCard_duplicateNumber_returns409() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        saveCard(user, "1111111111111111");

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("1111111111111111"))))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_userNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/cards", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("1111111111111111"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_invalidNumber_returns400() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");

        mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.number").exists());
    }

    @Test
    void getCardById_returnsCard_or404() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        PaymentCard card = saveCard(user, "1111111111111111");

        mockMvc.perform(get("/api/v1/cards/{id}", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.number").value("1111111111111111"));

        mockMvc.perform(get("/api/v1/cards/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCardsByUserId_returnsOnlyThatUsersCards_withPagination() throws Exception {
        User ivan = saveUser("Ivan", "Petrov", "ivan@mail.com");
        User anna = saveUser("Anna", "Petrova", "anna@mail.com");
        saveCard(ivan, "1111111111111111");
        saveCard(ivan, "2222222222222222");
        saveCard(ivan, "3333333333333333");
        saveCard(anna, "4444444444444444");

        mockMvc.perform(get("/api/v1/users/{userId}/cards",
                        ivan.getId()).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/users/{userId}/cards", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCards_filtersByOwnerNameAndSurname() throws Exception {
        User ivan = saveUser("Ivan", "Petrov", "ivan@mail.com");
        User anna = saveUser("Anna", "Petrova", "anna@mail.com");
        saveCard(ivan, "1111111111111111");
        saveCard(anna, "2222222222222222");

        mockMvc.perform(get("/api/v1/cards").param("name", "ivan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("1111111111111111"));

        mockMvc.perform(get("/api/v1/cards").param("surname", "petrov"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateCard_returns200_andUpdatesDb() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        PaymentCard card = saveCard(user, "1111111111111111");
        PaymentCardRequestDto body = new PaymentCardRequestDto(
                "2222222222222222", "NEW HOLDER", LocalDate.now().plusYears(5));

        mockMvc.perform(put("/api/v1/cards/{id}", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("2222222222222222"))
                .andExpect(jsonPath("$.holder").value("NEW HOLDER"));

        PaymentCard updated = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updated.getNumber()).isEqualTo("2222222222222222");
        assertThat(updated.getHolder()).isEqualTo("NEW HOLDER");
    }

    @Test
    void updateCard_numberTakenByAnother_returns409() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        saveCard(user, "1111111111111111");
        PaymentCard second = saveCard(user, "2222222222222222");

        mockMvc.perform(put("/api/v1/cards/{id}", second.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cardRequest("1111111111111111"))))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivateAndActivateCard_changesFlagInDb() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        PaymentCard card = saveCard(user, "1111111111111111");

        mockMvc.perform(patch("/api/v1/cards/{id}/status", card.getId())
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertThat(cardRepository.findById(card.getId()).orElseThrow().isActive()).isFalse();

        mockMvc.perform(patch("/api/v1/cards/{id}/status", card.getId())
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        assertThat(cardRepository.findById(card.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void deleteCard_returns204_andKeepsUser() throws Exception {
        User user = saveUser("Ivan", "Petrov", "ivan@mail.com");
        PaymentCard card = saveCard(user, "1111111111111111");

        mockMvc.perform(delete("/api/v1/cards/{id}", card.getId()))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.findById(card.getId())).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();

        mockMvc.perform(delete("/api/v1/cards/{id}", card.getId()))
                .andExpect(status().isNotFound());
    }
}
