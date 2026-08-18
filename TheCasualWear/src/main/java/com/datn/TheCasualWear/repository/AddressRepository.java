package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository  extends JpaRepository<Address, Integer> {
    List<Address> findByUserId(Integer userId);

    // MỚI: chỉ lấy địa chỉ còn hiển thị trong sổ (active = true) — dùng cho
    // sổ địa chỉ của khách + check giới hạn tối đa 3 địa chỉ. Địa chỉ đã bị
    // "ẩn" (active = false, xem Address.active) vẫn còn trong DB để đơn
    // hàng cũ tham chiếu đúng, nhưng không tính vào đây.
    List<Address> findByUserIdAndActiveTrue(Integer userId);

    // Lấy địa chỉ mặc định
    Optional<Address> findByUserIdAndIsDefaultTrue(Integer userId);

    // MỚI: địa chỉ "dùng 1 lần" (user = null, tạo khi khách checkout không
    // tick "đặt làm mặc định" — xem AddressService.createAddressForOrder)
    // mà KHÔNG còn AppOrder nào tham chiếu tới — dọn bằng OrderScheduler
    // mỗi ngày.
    //
    // Về lý thuyết trường hợp này gần như không xảy ra: Address và AppOrder
    // được tạo/lưu trong cùng 1 @Transactional (OrderService.placeOrder /
    // placeOrderGuest) nên cùng commit hoặc cùng rollback — chỉ còn sót lại
    // nếu có thay đổi code sau này làm lệch giao dịch. Thêm job này như một
    // lớp phòng hờ (safety net), không kỳ vọng xóa nhiều mỗi lần chạy.
    @Query("SELECT a FROM Address a WHERE a.user IS NULL " +
            "AND NOT EXISTS (SELECT 1 FROM AppOrder o WHERE o.shippingAddress = a) " +
            "AND NOT EXISTS (SELECT 1 FROM AppOrder o WHERE o.billingAddress = a)")
    List<Address> findOrphanAddresses();
}