package com.deshko.userservice.repository;

import com.deshko.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentCardRepository extends
        JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    @Query("select c from PaymentCard c join fetch c.user where c.id = :id")
    Optional<PaymentCard> findByIdWithUser(@Param("id") Long id);

    List<PaymentCard> findAllByUserId(Long userId);
    Page<PaymentCard> findAllByUserId(Long userId, Pageable pageable);
    boolean existsByNumber(String number);

    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId", nativeQuery = true)
    long countByUserIdNative(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentCard c
               set c.number = :number,
                   c.holder = :holder,
                   c.expirationDate = :expirationDate,
                   c.updatedAt = CURRENT_TIMESTAMP
             where c.id = :id
            """)
    int updateById(@Param("id") Long id,
                   @Param("number") String number,
                   @Param("holder") String holder,
                   @Param("expirationDate") LocalDate expirationDate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE payment_cards SET active = :active, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
