package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN u.roles r WHERE " +
            "(:keyword IS NULL OR " +
            " LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " u.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:roleName IS NULL OR r.name = :roleName)")
    Page<AppUser> searchUsers(@Param("keyword") String keyword,
                              @Param("roleName") String roleName,
                              Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.username = :value " +
            "OR u.email = :value OR u.phone = :value")
    Optional<AppUser> findByUsernameOrEmailOrPhone(@Param("value") String value);
}