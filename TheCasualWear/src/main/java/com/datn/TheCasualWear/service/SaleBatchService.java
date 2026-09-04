package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.entity.SaleBatch;
import com.datn.TheCasualWear.repository.ProductSaleRepository;
import com.datn.TheCasualWear.repository.SaleBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale theo đợt: admin chọn nhiều sản phẩm cùng lúc, set chung 1 mức %
 * và 1 khoảng thời gian. Mỗi sản phẩm trong đợt vẫn tạo ra đúng 1 dòng
 * ProductSale như cách tạo thủ công trước đây (dùng chung 1 nguồn dữ
 * liệu, chỉ khác cách tạo) — chỉ khác là có thêm saleBatchId để nhóm lại.
 *
 * Trùng lịch: tái sử dụng đúng rule chặn cứng đã có ở
 * ProductSaleService (existsOverlapping) — sản phẩm nào trùng thì bị
 * bỏ qua (skip), không rollback cả batch chỉ vì 1 sản phẩm lỗi.
 */
@Service
@RequiredArgsConstructor
public class SaleBatchService {

    private final SaleBatchRepository saleBatchRepository;
    private final ProductSaleRepository saleRepository;

    public record BulkSaleResult(List<ProductSale> created, List<String> skipped) {
    }

    @Transactional
    public BulkSaleResult createSaleBatch(String name, List<Product> products,
                                          BigDecimal discountPercent,
                                          LocalDateTime start, LocalDateTime end) {
        validateSaleInput(name, products, discountPercent, start, end);

        SaleBatch batch = new SaleBatch();
        batch.setName(name);
        batch.setDiscountPercent(discountPercent);
        batch.setStartDate(start);
        batch.setEndDate(end);
        batch = saleBatchRepository.save(batch);

        List<ProductSale> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Product p : products) {
            if (saleRepository.existsOverlapping(p.getId(), start, end, -1)) {
                skipped.add(p.getName() + " (đã có sale khác trùng thời gian)");
                continue;
            }
            ProductSale sale = new ProductSale();
            sale.setProduct(p);
            sale.setDiscountPercent(discountPercent);
            sale.setStartDate(start);
            sale.setEndDate(end);
            sale.setSaleBatchId(batch.getId());
            created.add(saleRepository.save(sale));
        }
        return new BulkSaleResult(created, skipped);
    }

    private void validateSaleInput(String name, List<Product> products,
                                   BigDecimal discountPercent, LocalDateTime start, LocalDateTime end) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên đợt sale không được để trống!");
        }
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất 1 sản phẩm!");
        }
        if (discountPercent == null
                || discountPercent.compareTo(BigDecimal.ZERO) <= 0
                || discountPercent.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm giá phải trong khoảng 0-90%!");
        }
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu!");
        }
    }

    // ── ADMIN LIST / DETAIL ──────────────────────────────────────────────

    public List<SaleBatch> getAllBatches() {
        return saleBatchRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public SaleBatch getBatchById(Integer id) {
        return saleBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt sale!"));
    }

    // Số sản phẩm thực tế đã tạo được trong đợt (không tính sản phẩm bị
    // skip do trùng lịch lúc tạo)
    public long countProductsInBatch(Integer batchId) {
        return saleRepository.countBySaleBatchId(batchId);
    }

    // Batch có còn dòng product_sale nào active không — false nghĩa là đã
    // bị huỷ sớm (deactivateBatch), kể cả khi endDate của batch vẫn còn ở
    // tương lai. Trang list dùng cái này để hiện đúng "Đã huỷ sớm" thay vì
    // suy trạng thái chỉ từ startDate/endDate của chính sale_batch.
    public boolean isBatchStillActive(Integer batchId) {
        return saleRepository.existsBySaleBatchIdAndIsActiveTrue(batchId);
    }

    public enum BatchStatus { UPCOMING, RUNNING, EXPIRED, CANCELLED_EARLY }

    // Trạng thái hiển thị cho trang admin/product/sales.html — gộp luôn cả
    // 2 nguồn: cờ isActive (do SaleScheduler dọn mỗi 5 phút, hoặc do
    // deactivateBatch() set thủ công) và mốc thời gian start/end của batch.
    //
    // Lý do không dùng thẳng isBatchStillActive(): nó chỉ trả true/false,
    // không tự phân biệt được "hết hạn tự nhiên" và "huỷ sớm" — phải so
    // thêm với endDate ở tầng gọi, dễ lặp/lệch logic giữa các view. Đưa hết
    // vào 1 chỗ duy nhất ở đây.
    //
    // Lưu ý: vì SaleScheduler chạy mỗi 5 phút (không realtime), có khoảng
    // hở tối đa 5 phút giữa lúc sale thực sự hết hạn (theo endDate) và lúc
    // isActive được set false. Case now.isAfter(endDate) bù cho khoảng hở
    // đó để trạng thái EXPIRED hiện đúng ngay cả khi job dọn chưa kịp chạy.
    public BatchStatus getBatchStatus(SaleBatch batch) {
        LocalDateTime now = LocalDateTime.now();
        boolean stillActive = isBatchStillActive(batch.getId());

        if (!stillActive) {
            // Không còn ProductSale nào active: nếu endDate vẫn ở tương lai
            // thì chắc chắn là bị huỷ sớm thủ công, chưa thể do scheduler.
            return now.isBefore(batch.getEndDate())
                    ? BatchStatus.CANCELLED_EARLY
                    : BatchStatus.EXPIRED;
        }
        if (now.isAfter(batch.getEndDate())) {
            return BatchStatus.EXPIRED; // hết hạn nhưng scheduler chưa kịp dọn isActive
        }
        if (now.isBefore(batch.getStartDate())) {
            return BatchStatus.UPCOMING;
        }
        return BatchStatus.RUNNING;
    }

    // Danh sách product_sale thuộc batch, kèm Product (fetch qua @ManyToOne
    // mặc định) — dùng cho trang xem chi tiết đợt sale.
    public List<ProductSale> getSalesInBatch(Integer batchId) {
        return saleRepository.findBySaleBatchId(batchId);
    }

    @Transactional
    public void deactivateBatch(Integer batchId) {
        List<ProductSale> sales = saleRepository.findBySaleBatchId(batchId);
        sales.forEach(s -> s.setIsActive(false));
        saleRepository.saveAll(sales);
    }
}