package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppOrderRepository extends JpaRepository<AppOrder, Integer> {

    List<AppOrder> findByCustomerIdOrderByOrderDateDesc(Integer customerId);

    List<AppOrder> findByStatus(OrderStatus status);

    List<AppOrder> findByCustomerIdAndStatus(Integer customerId, String status);

    @Query("SELECT o FROM AppOrder o ORDER BY " +
            "CASE o.status " +
            "WHEN 'PENDING'   THEN 1 " +
            "WHEN 'CONFIRMED' THEN 2 " +
            "WHEN 'SHIPPING'  THEN 3 " +
            "WHEN 'COMPLETED' THEN 4 " +
            "WHEN 'CANCELLED' THEN 5 " +
            "WHEN 'RETURNED'  THEN 6 " +
            "END ASC, o.orderDate DESC")
    List<AppOrder> findAllOrderedByStatus();

    @Query("SELECT o FROM AppOrder o LEFT JOIN o.customer c WHERE " +
            "(:keyword  IS NULL OR LOWER(c.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status   IS NULL OR o.status     = :status) " +
            "AND (:fromDate IS NULL OR o.orderDate >= :fromDate) " +
            "AND (:toDate   IS NULL OR o.orderDate <= :toDate) " +
            "ORDER BY CASE o.status " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.PENDING   THEN 1 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.CONFIRMED THEN 2 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.SHIPPING  THEN 3 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.COMPLETED THEN 4 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.CANCELLED THEN 5 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.RETURNED  THEN 6 " +
            "END ASC, o.orderDate DESC")
    Page<AppOrder> searchOrders(@Param("keyword")  String        keyword,
                                @Param("status")   OrderStatus   status,
                                @Param("fromDate") LocalDateTime fromDate,
                                @Param("toDate")   LocalDateTime toDate,
                                Pageable pageable);

    @Query("SELECT o FROM AppOrder o WHERE o.orderType = com.datn.TheCasualWear.enums.OrderType.COUNTER " +
            "AND o.cashier.id = :cashierId AND o.orderDate >= :fromDate " +
            "ORDER BY o.orderDate DESC")
    List<AppOrder> findRecentCounterOrdersByCashier(@Param("cashierId") Integer cashierId,
                                                    @Param("fromDate") LocalDateTime fromDate);

    @Query("""
        SELECT o.orderType, COALESCE(SUM(o.totalPrice), 0), COUNT(o)
        FROM AppOrder o
        WHERE o.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED
          AND o.orderDate BETWEEN :from AND :to
        GROUP BY o.orderType
        """)
    List<Object[]> sumRevenueByOrderType(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("""
        SELECT o.cancelReason, COUNT(o)
        FROM AppOrder o
        WHERE o.status IN (com.datn.TheCasualWear.enums.OrderStatus.CANCELLED,
                            com.datn.TheCasualWear.enums.OrderStatus.RETURNED)
          AND o.cancelReason IS NOT NULL
        GROUP BY o.cancelReason
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> countByCancelReason();

    @Query("""
        SELECT o.customer,
               SUM(CASE WHEN o.status = com.datn.TheCasualWear.enums.OrderStatus.CANCELLED THEN 1L ELSE 0L END),
               SUM(CASE WHEN o.status = com.datn.TheCasualWear.enums.OrderStatus.RETURNED THEN 1L ELSE 0L END),
               COUNT(o)
        FROM AppOrder o
        WHERE o.status IN (com.datn.TheCasualWear.enums.OrderStatus.CANCELLED,
                            com.datn.TheCasualWear.enums.OrderStatus.RETURNED)
          AND o.customer IS NOT NULL
        GROUP BY o.customer
        HAVING COUNT(o) >= :minCount
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> findFrequentCancellers(@Param("minCount") long minCount);

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0), COUNT(o)
        FROM AppOrder o
        WHERE o.orderType = com.datn.TheCasualWear.enums.OrderType.COUNTER
          AND o.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED
          AND (:from IS NULL OR o.orderDate >= :from)
          AND (:to   IS NULL OR o.orderDate <= :to)
        """)
    List<Object[]> sumPosRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0), COUNT(o)
        FROM AppOrder o
        WHERE o.orderType = com.datn.TheCasualWear.enums.OrderType.ONLINE
          AND o.paymentMethod = 'VNPAY'
          AND o.isPaid = true
          AND o.status NOT IN (com.datn.TheCasualWear.enums.OrderStatus.CANCELLED,
                                com.datn.TheCasualWear.enums.OrderStatus.RETURNED)
          AND (:from IS NULL OR o.orderDate >= :from)
          AND (:to   IS NULL OR o.orderDate <= :to)
        """)
    List<Object[]> sumOnlineVnpayRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0), COUNT(o)
        FROM AppOrder o
        WHERE o.orderType = com.datn.TheCasualWear.enums.OrderType.ONLINE
          AND o.paymentMethod = 'COD'
          AND o.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED
          AND (:from IS NULL OR o.orderDate >= :from)
          AND (:to   IS NULL OR o.orderDate <= :to)
        """)
    List<Object[]> sumOnlineCodRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(od.variant.costPrice * od.quantity), 0)
        FROM OrderDetail od
        JOIN od.order o
        WHERE (:from IS NULL OR o.orderDate >= :from)
          AND (:to   IS NULL OR o.orderDate <= :to)
          AND (
               (o.orderType = com.datn.TheCasualWear.enums.OrderType.COUNTER
                AND o.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED)
            OR (o.orderType = com.datn.TheCasualWear.enums.OrderType.ONLINE
                AND o.paymentMethod = 'VNPAY'
                AND o.isPaid = true
                AND o.status NOT IN (com.datn.TheCasualWear.enums.OrderStatus.CANCELLED,
                                      com.datn.TheCasualWear.enums.OrderStatus.RETURNED))
            OR (o.orderType = com.datn.TheCasualWear.enums.OrderType.ONLINE
                AND o.paymentMethod = 'COD'
                AND o.status = com.datn.TheCasualWear.enums.OrderStatus.COMPLETED)
          )
        """)
    BigDecimal sumCostForRevenueOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    boolean existsByOrderCode(String orderCode);
    Optional<AppOrder> findByOrderCode(String orderCode);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM AppOrder o " +
            "WHERE o.shippingAddress.id = :addressId OR o.billingAddress.id = :addressId")
    boolean existsByAddressId(@Param("addressId") Integer addressId);

    @Query("SELECT o FROM AppOrder o LEFT JOIN o.shippingAddress a " +
            "WHERE o.orderCode = :orderCode AND (a.phone = :contact OR o.guestEmail = :contact)")
    Optional<AppOrder> findByOrderCodeAndContact(@Param("orderCode") String orderCode,
                                                 @Param("contact") String contact);

    @Query("SELECT o FROM AppOrder o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.shippingAddress " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.variant v " +
            "LEFT JOIN FETCH v.product " +
            "LEFT JOIN FETCH v.size " +
            "LEFT JOIN FETCH v.color " +
            "WHERE o.id = :id")
    Optional<AppOrder> findByIdWithDetailsForEmail(@Param("id") Integer id);

    // ── MỚI: dùng cho AccountCleanupScheduler — kiểm tra 1 AppUser (tài
    // khoản cashier tạo, chưa kích hoạt, đã hết hạn) có đơn hàng nào tham
    // chiếu tới không, để quyết định chỉ giải phóng email (còn đơn) hay
    // xóa hẳn record (chưa từng có đơn) — tránh vi phạm FK constraint.
    boolean existsByCustomerId(Integer customerId);
}