package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository  extends JpaRepository<Address, Integer> {
    List<Address> findByUserId(Integer userId);

    // Lấy địa chỉ mặc định
    Optional<Address> findByUserIdAndIsDefaultTrue(Integer userId);
}
