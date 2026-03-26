package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Integer> {
    Optional<Size> findByName(String name);
}
