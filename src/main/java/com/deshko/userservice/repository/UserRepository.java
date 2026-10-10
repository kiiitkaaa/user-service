package com.deshko.userservice.repository;

import com.deshko.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Query("select u from User u left join fetch u.cards where u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u
               set u.name = :name,
                   u.surname = :surname,
                   u.birthDate = :birthDate,
                   u.email = :email,
                   u.updatedAt = CURRENT_TIMESTAMP
             where u.id = :id
            """)
    int updateById(@Param("id") Long id,
                   @Param("name") String name,
                   @Param("surname") String surname,
                   @Param("birthDate") LocalDate birthDate,
                   @Param("email") String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE users SET active = :active, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
