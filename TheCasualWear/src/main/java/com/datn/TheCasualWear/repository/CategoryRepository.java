package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}
