package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Notification> findByUserIdAndIsReadFalse(Integer userId);
    int countByUserIdAndIsReadFalse(Integer userId);
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.isRead = true " +
            "AND n.createdAt < :sevenDaysAgo")
    void deleteReadNotificationsOlderThan(@Param("sevenDaysAgo") LocalDateTime ago);
}