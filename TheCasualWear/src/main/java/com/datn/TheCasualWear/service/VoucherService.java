package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Voucher;
import com.datn.TheCasualWear.repository.OrderVoucherRepository;
import com.datn.TheCasualWear.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final OrderVoucherRepository orderVoucherRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          OrderVoucherRepository orderVoucherRepository) {
        this.voucherRepository = voucherRepository;
        this.orderVoucherRepository = orderVoucherRepository;
    }

    // ==================== DÙNG CHUNG ====================

    public Voucher getVoucherById(Integer id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với id: " + id));
    }

    // ==================== PHÍA CUSTOMER ====================

    // Kiểm tra và lấy voucher theo code
    public Voucher applyVoucher(String code, BigDecimal totalPrice, AppUser user) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại!"));

        // Kiểm tra còn hiệu lực không
        if (!voucher.getIsActive()) {
            throw new IllegalStateException("Mã giảm giá đã bị vô hiệu hóa!");
        }

        // Kiểm tra thời hạn
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new IllegalStateException("Mã giảm giá chưa đến thời gian sử dụng!");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new IllegalStateException("Mã giảm giá đã hết hạn!");
        }

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getMinOrderValue() != null
                && totalPrice.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalStateException("Đơn hàng tối thiểu "
                    + voucher.getMinOrderValue() + "đ mới được dùng mã này!");
        }

        // Kiểm tra user đã dùng voucher này chưa
        if (orderVoucherRepository.existsByCustomerIdAndVoucherId(user.getId(), voucher.getId())) {
            throw new IllegalStateException("Bạn đã sử dụng mã giảm giá này rồi!");
        }

        return voucher;
    }

    // Tính tiền sau khi áp dụng voucher
    public BigDecimal calcDiscountedPrice(BigDecimal totalPrice, Voucher voucher) {
        if (voucher == null) return totalPrice;

        BigDecimal discount = totalPrice
                .multiply(voucher.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        BigDecimal result = totalPrice.subtract(discount);

        // Không để âm
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    // Lấy danh sách voucher còn hiệu lực (hiển thị cho customer chọn)
    public List<Voucher> getActiveVouchers() {
        return voucherRepository.findByIsActiveTrueAndEndDateAfter(LocalDateTime.now());
    }

    // ==================== PHÍA ADMIN ====================

    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    public Voucher createVoucher(Voucher voucher) {
        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại: " + voucher.getCode());
        }
        return voucherRepository.save(voucher);
    }

    public Voucher updateVoucher(Integer id, Voucher details) {
        Voucher voucher = getVoucherById(id);
        voucher.setCode(details.getCode());
        voucher.setDescription(details.getDescription());
        voucher.setDiscountPercent(details.getDiscountPercent());
        voucher.setMinOrderValue(details.getMinOrderValue());
        voucher.setStartDate(details.getStartDate());
        voucher.setEndDate(details.getEndDate());
        return voucherRepository.save(voucher);
    }

    public void toggleActive(Integer id) {
        Voucher voucher = getVoucherById(id);
        voucher.setIsActive(!voucher.getIsActive()); // bật/tắt
        voucherRepository.save(voucher);
    }

    public void deleteVoucher(Integer id) {
        Voucher voucher = getVoucherById(id);
        if (orderVoucherRepository.existsByVoucherId(id)) {
            throw new IllegalStateException("Không thể xóa voucher đã được sử dụng!");
        }
        voucherRepository.delete(voucher);
    }
}