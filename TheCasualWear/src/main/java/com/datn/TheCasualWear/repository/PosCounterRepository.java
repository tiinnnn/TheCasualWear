package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.PosCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosCounterRepository extends JpaRepository<PosCounter, Integer> {

    // Dùng cho dropdown chọn quầy lúc cashier vào bán — chỉ hiện quầy đang bật
    List<PosCounter> findByIsActiveTrueOrderByCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Integer id);
}