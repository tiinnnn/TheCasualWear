package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
