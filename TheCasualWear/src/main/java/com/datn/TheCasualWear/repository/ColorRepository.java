package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Integer> {
    Optional<Color> findByName(String name);
}
