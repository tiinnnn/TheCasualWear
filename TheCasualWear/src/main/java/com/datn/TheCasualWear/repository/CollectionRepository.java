package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Integer> {

    // Chi lay collection dang active va con han
    @Query("SELECT c FROM Collection c WHERE c.isActive = true " +
           "AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE) " +
           "ORDER BY c.createdAt DESC")
    List<Collection> findActiveCollections();

    List<Collection> findAllByOrderByCreatedAtDesc();
}
