package com.deshko.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    public static final int MAX_CARDS = 5;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<PaymentCard> cards = new ArrayList<>();

    public List<PaymentCard> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public void addCard(PaymentCard card) {
        if (cards.size() >= MAX_CARDS) {
            throw new IllegalStateException("User cannot have more than 5 cards");
        }

        cards.add(card);
        card.setUser(this);
    }

    public void removeCard(PaymentCard card) {
        cards.remove(card);
        card.setUser(null);
    }

    @PrePersist
    private void checkCardsLimitOnCreate() {
        if (cards.size() > MAX_CARDS) {
            throw new IllegalStateException("User cannot have more than 5 cards");
        }
    }
}
