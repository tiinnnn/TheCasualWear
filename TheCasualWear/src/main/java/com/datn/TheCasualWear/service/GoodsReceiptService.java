package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.GoodsReceiptItemDTO;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.GoodsReceipt;
import com.datn.TheCasualWear.entity.GoodsReceiptItem;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import com.datn.TheCasualWear.repository.GoodsReceiptRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository   receiptRepository;
    private final ProductVariantRepository variantRepository;
    private final StockMovementLogService  stockMovementLogService;

    private static final DateTimeFormatter CODE_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public GoodsReceipt getReceiptById(Integer id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiếu nhập kho với id: " + id));
    }

    public List<GoodsReceipt> getAllReceipts() {
        return receiptRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Tạo phiếu nhập kho: lưu header + từng dòng item, cộng stock cho variant
     * tương ứng và ghi log biến động (IMPORT) — tất cả trong 1 transaction
     * để đảm bảo không có trường hợp lưu phiếu xong nhưng thiếu log hoặc
     * cộng thiếu stock.
     */
    @Transactional
    public GoodsReceipt createReceipt(String supplierName, String note,
                                      List<GoodsReceiptItemDTO> items,
                                      AppUser createdBy) {

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Phiếu nhập kho phải có ít nhất 1 sản phẩm!");
        }
        if (supplierName == null || supplierName.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên nhà cung cấp!");
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setCode(generateCode());
        receipt.setSupplierName(supplierName.trim());
        receipt.setNote(note);
        receipt.setCreatedBy(createdBy);
        receiptRepository.save(receipt); // lưu trước để có id dùng làm refId

        BigDecimal total = BigDecimal.ZERO;

        for (GoodsReceiptItemDTO dto : items) {
            if (dto.quantity() == null || dto.quantity() <= 0) {
                throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0!");
            }
            BigDecimal unitCost = dto.unitCostPrice() != null
                    ? dto.unitCostPrice() : BigDecimal.ZERO;

            ProductVariant variant = variantRepository.findById(dto.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm với id: " + dto.variantId()));

            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setGoodsReceipt(receipt);
            item.setVariant(variant);
            item.setQuantity(dto.quantity());
            item.setUnitCostPrice(unitCost);
            receipt.getItems().add(item);

            total = total.add(unitCost.multiply(BigDecimal.valueOf(dto.quantity())));

            // Giá vốn bình quân gia quyền (Weighted Average Cost) — tính TRƯỚC
            // khi logMovement() cập nhật stock, vì công thức cần tồn kho CŨ:
            //
            //   giá vốn mới = (tồn cũ × giá vốn cũ + số lượng nhập × giá nhập)
            //                 ────────────────────────────────────────────────
            //                          (tồn cũ + số lượng nhập)
            //
            // Khi variant chưa từng có tồn kho (tồn cũ = 0), công thức tự động
            // rút gọn về đúng giá nhập lần này — không cần xử lý riêng.
            int oldStock = variant.getStock();
            BigDecimal oldCost = variant.getCostPrice() != null
                    ? variant.getCostPrice() : BigDecimal.ZERO;
            int newStockTotal = oldStock + dto.quantity();

            BigDecimal newAvgCost = BigDecimal.valueOf(oldStock).multiply(oldCost)
                    .add(BigDecimal.valueOf(dto.quantity()).multiply(unitCost))
                    .divide(BigDecimal.valueOf(newStockTotal), 2, java.math.RoundingMode.HALF_UP);
            variant.setCostPrice(newAvgCost);

            // Cộng stock + ghi log biến động, gắn ref về phiếu nhập này.
            // logMovement() sẽ save() variant, nên costPrice vừa set ở trên
            // được lưu cùng luôn trong 1 lần save, không cần gọi save() riêng.
            stockMovementLogService.logMovement(
                    variant,
                    StockMovementType.IMPORT,
                    dto.quantity(),
                    StockRefType.GOODS_RECEIPT,
                    receipt.getId(),
                    "Nhập kho từ phiếu " + receipt.getCode(),
                    createdBy
            );
        }

        receipt.setTotalAmount(total);
        return receiptRepository.save(receipt);
    }

    // Sinh mã dạng PN-yyyyMMdd-xxx, xxx = số thứ tự phiếu trong ngày
    private String generateCode() {
        String datePart = LocalDate.now().format(CODE_DATE_FMT);
        String prefix = "PN-" + datePart;
        long countToday = receiptRepository.countByCodeStartingWith(prefix);
        return String.format("%s-%03d", prefix, countToday + 1);
    }
}