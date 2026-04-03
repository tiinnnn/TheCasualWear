package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(AppUser user);
}
