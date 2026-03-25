package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
