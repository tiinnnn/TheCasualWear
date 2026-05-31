package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.DeliveryProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryProfileRepository extends JpaRepository<DeliveryProfile, Integer> {

    Optional<DeliveryProfile> findByUserId(Integer userId);

    List<DeliveryProfile> findByIsAvailableTrue();
}