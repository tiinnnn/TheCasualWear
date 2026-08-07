package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.StockMovementSummaryDTO;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.entity.StockMovementLog;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.repository.StockMovementLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// ĐIỂM DUY NHẤT nên dùng để thay đổi ProductVariant.stock trong toàn hệ thống
// (thay cho variant.setStock(...) rải rác nhiều nơi), để đảm bảo mọi thay đổi
// tồn kho đều có audit trail trong stock_movement_log.
@Service
@RequiredArgsConstructor
public class StockMovementLogService {

    private final StockMovementLogRepository logRepository;
    private final ProductVariantRepository   variantRepository;

    /**
     * @param variant    variant đã được load sẵn (tránh query lại lần 2)
     * @param changeType loại biến động (IMPORT/SALE/CANCEL/RETURN/ADJUST)
     * @param changeQty  dương = tăng tồn, âm = giảm tồn
     * @param refType    loại bản ghi gốc gây ra biến động (nullable với ADJUST)
     * @param refId      id bản ghi gốc — goods_receipt.id hoặc app_order.id (nullable)
     * @param note       ghi chú tùy chọn, hiển thị ở trang lịch sử
     * @param createdBy  người thực hiện (nullable — vd job tự động chạy nền)
     */
    @Transactional
    public StockMovementLog logMovement(ProductVariant variant,
                                        StockMovementType changeType,
                                        int changeQty,
                                        StockRefType refType,
                                        Integer refId,
                                        String note,
                                        AppUser createdBy) {

        int newBalance = variant.getStock() + changeQty;
        if (newBalance < 0) {
            throw new IllegalStateException(
                    "Tồn kho không thể âm! Variant #" + variant.getId()
                            + " hiện có " + variant.getStock()
                            + ", không thể trừ " + Math.abs(changeQty));
        }

        variant.setStock(newBalance);
        variantRepository.save(variant);

        StockMovementLog log = new StockMovementLog();
        log.setVariant(variant);
        log.setChangeType(changeType);
        log.setChangeQty(changeQty);
        log.setBalanceAfter(newBalance);
        log.setRefType(refType);
        log.setRefId(refId);
        log.setNote(note);
        log.setCreatedBy(createdBy);
        log.setCreatedAt(LocalDateTime.now());

        return logRepository.save(log);
    }

    // Overload tiện dùng khi chỉ có variantId, chưa load entity ProductVariant
    @Transactional
    public StockMovementLog logMovement(Integer variantId,
                                        StockMovementType changeType,
                                        int changeQty,
                                        StockRefType refType,
                                        Integer refId,
                                        String note,
                                        AppUser createdBy) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy variant với id: " + variantId));
        return logMovement(variant, changeType, changeQty, refType, refId, note, createdBy);
    }

    // ─────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────

    // Tổng khối lượng biến động kho theo loại (IMPORT/SALE/CANCEL/RETURN/ADJUST)
    // trong khoảng thời gian — dùng vẽ biểu đồ nhập/xuất kho. totalQty luôn
    // dương (lấy trị tuyệt đối changeQty), vì dấu +/- chỉ để tính balance,
    // không cần thiết khi hiển thị "khối lượng đã xử lý".
    // Lưu ý: đang group bằng Java stream (dựa trên getLogsBetween() có sẵn) —
    // nếu khối lượng log lớn (hàng chục nghìn dòng/kỳ báo cáo), nên đổi sang
    // JPQL "GROUP BY changeType" ở StockMovementLogRepository để đỡ tải RAM.
    public List<StockMovementSummaryDTO> getMovementSummary(LocalDateTime from, LocalDateTime to) {
        return getLogsBetween(from, to).stream()
                .collect(Collectors.groupingBy(
                        StockMovementLog::getChangeType,
                        Collectors.summingLong(l -> Math.abs(l.getChangeQty()))))
                .entrySet().stream()
                .map(e -> new StockMovementSummaryDTO(e.getKey(), e.getValue()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // TRUY VẤN LỊCH SỬ (dùng cho trang xem lịch sử biến động kho)
    // ─────────────────────────────────────────────────────────────

    public List<StockMovementLog> getLogsByVariant(Integer variantId) {
        return logRepository.findByVariantIdOrderByCreatedAtDesc(variantId);
    }

    public List<StockMovementLog> getLogsBetween(LocalDateTime from, LocalDateTime to) {
        return logRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
    }

    public List<StockMovementLog> getLogsByRef(StockRefType refType, Integer refId) {
        return logRepository.findByRefTypeAndRefId(refType, refId);
    }

    // Tìm kiếm nâng cao: theo tên sản phẩm, mã SKU, loại biến động, khoảng ngày.
    // Chuỗi rỗng/blank được coi như không lọc theo tiêu chí đó.
    public List<StockMovementLog> searchLogs(String productName,
                                             String sku,
                                             StockMovementType changeType,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        return logRepository.search(
                blankToNull(productName),
                blankToNull(sku),
                changeType,
                from,
                to);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}