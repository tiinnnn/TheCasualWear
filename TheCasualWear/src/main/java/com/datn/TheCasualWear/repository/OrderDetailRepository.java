package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderDetail;
import com.datn.TheCasualWear.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrderId(Integer orderId);

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od WHERE od.variant.product.id = :productId " +
            "AND od.order.status != :status")
    boolean existsByProductIdAndOrderStatusNot(@Param("productId") Integer productId,
                                               @Param("status")    OrderStatus status);

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od WHERE od.variant.id = :variantId " +
            "AND od.order.status != :status")
    boolean existsByVariantIdAndOrderStatusNot(@Param("variantId") Integer variantId,
                                               @Param("status")    OrderStatus status);

    @Modifying @Transactional
    @Query("DELETE FROM OrderDetail od WHERE od.variant.product.id = :productId " +
            "AND od.order.status = com.datn.TheCasualWear.enums.OrderStatus.CANCELLED")
    void deleteByProductId(@Param("productId") Integer productId);

    // MỚI: dùng bởi ProductVariantService.deleteVariant() — xóa order_detail
    // của variant này nhưng CHỈ thuộc đơn CANCELLED, cùng pattern với
    // deleteByProductId() ở trên. Không dùng derived delete (deleteByVariantId
    // mặc định sẽ xóa TẤT CẢ order_detail bất kể trạng thái đơn) vì có thể
    // vô tình xóa order_detail của đơn đang hoạt động nếu method này lỡ được
    // gọi mà không qua check hasActiveOrder trước đó — lọc CANCELLED tường
    // minh ở đây giúp an toàn ngay cả khi thứ tự gọi ở service bị thay đổi
    // sau này.
    @Modifying @Transactional
    @Query("DELETE FROM OrderDetail od WHERE od.variant.id = :variantId " +
            "AND od.order.status = com.datn.TheCasualWear.enums.OrderStatus.CANCELLED")
    void deleteByVariantId(@Param("variantId") Integer variantId);

    // ── DASHBOARD: hiệu quả sale — tổng SL bán, doanh thu, tổng tiền giảm
    // cho 1 sản phẩm trong 1 khoảng thời gian (dùng cho ProductSaleService
    // .getSaleEffectiveness(), truyền vào [sale.startDate, sale.endDate]).
    // Chỉ tính đơn COMPLETED. Luôn trả về ĐÚNG 1 dòng (COALESCE xử lý khi
    // không có đơn nào khớp).
    //
    // ⚠️ QUAN TRỌNG: khai báo List<Object[]> chứ KHÔNG phải Object[] trực
    // tiếp — Spring Data JPA xử lý kiểu trả về mảng như 1 dạng collection
    // và bọc thêm 1 lớp mảng nữa quanh kết quả thật (Object[1] chứa
    // Object[3] bên trong), gây ClassCastException khi cast phần tử ra
    // Long/BigDecimal ở tầng service. List<Object[]> mới là kiểu "collection
    // execution" đúng chuẩn của Spring Data.
    @Query("""
        SELECT COALESCE(SUM(od.quantity), 0),
               COALESCE(SUM(od.quantity * od.price), 0),
               COALESCE(SUM(od.quantity * (od.originalPrice - od.price)), 0)
        FROM OrderDetail od
        WHERE od.variant.product.id = :productId
          AND od.order.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED
          AND od.order.orderDate BETWEEN :from AND :to
        """)
    List<Object[]> sumSoldForProductBetween(@Param("productId") Integer productId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}