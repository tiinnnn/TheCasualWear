package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderVoucherRepository  extends JpaRepository<OrderDetail, Integer> {

}
