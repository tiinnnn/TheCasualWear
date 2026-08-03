package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.StockMovementLog;
import com.datn.TheCasualWear.enums.StockRefType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementLogRepository extends JpaRepository<StockMovementLog, Integer> {

    List<StockMovementLog> findByVariantIdOrderByCreatedAtDesc(Integer variantId);

    List<StockMovementLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    // Tra cứu ngược: phiếu nhập / đơn hàng này đã tạo ra những dòng log nào
    List<StockMovementLog> findByRefTypeAndRefId(StockRefType refType, Integer refId);
}
