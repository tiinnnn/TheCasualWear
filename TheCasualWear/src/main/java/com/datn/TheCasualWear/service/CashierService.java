package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import com.datn.TheCasualWear.pos.PosCart;
import com.datn.TheCasualWear.pos.PosCartRegistry;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.OrderDetailRepository;
import com.datn.TheCasualWear.repository.OrderVoucherRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashierService {

    private final AppOrderRepository       orderRepository;
    private final AppUserRepository        appUserRepository;
    private final ProductVariantRepository variantRepository;
    private final VoucherRepository        voucherRepository;
    private final OrderDetailRepository    orderDetailRepository;
    private final OrderVoucherRepository   orderVoucherRepository;
    private final StockMovementLogService  stockMovementLogService;
    private final ProductSaleService       productSaleService; // MỚI: giá sale áp cho cả bán tại quầy
    private final PosCartRegistry          cartRegistry; // MỚI: giỏ POS đa-cart, in-memory

    // Cashier tự hủy đơn trong vòng bao nhiêu phút kể từ lúc tạo.
    // Admin/Owner không bị giới hạn bởi mốc thời gian này.
    public static final int CANCEL_WINDOW_MINUTES = 30;

    // ─────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập");
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    /** Wrapper public để CashierController lấy cashier hiện tại (VD gán cashierId cho giỏ mới). */
    public AppUser getCurrentCashier() {
        return getCurrentUser();
    }

    private BigDecimal calcDiscount(Voucher v, BigDecimal orderTotal) {
        BigDecimal discount = orderTotal
                .multiply(v.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) {
            discount = v.getMaxDiscount();
        }
        return discount;
    }

    // ─────────────────────────────────────────────────────────────
    // GIÁ BÁN — đã áp sale nếu sản phẩm đang có sale chạy. Dùng chung
    // cho cả buildCartItem() (thêm vào giỏ) và ô tìm kiếm autocomplete
    // ở CashierController, để giá hiển thị lúc tìm và giá lúc thêm
    // vào giỏ luôn khớp nhau.
    // ─────────────────────────────────────────────────────────────

    public BigDecimal getEffectivePrice(ProductVariant variant) {
        return productSaleService.getEffectivePrice(variant.getProduct());
    }

    // Sale đang chạy của sản phẩm (nếu có) — dùng để hiện badge "-X%" +
    // giá gốc gạch ngang cho cashier, cả lúc tìm kiếm lẫn trong giỏ tạm.
    public ProductSale getActiveSale(ProductVariant variant) {
        return productSaleService.getActiveSale(variant.getProduct()).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // XÂY DỰNG ITEM GIỎ TẠM (không tự trừ kho — dùng khi chỉ cần preview,
    // không đi qua registry. Luồng add-to-cart thật sự dùng addItemToCart()
    // bên dưới vì cần trừ/giữ chỗ kho đúng lúc.)
    // ─────────────────────────────────────────────────────────────

    public CounterCartItemDTO buildCartItem(Integer variantId, int quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (quantity < 1) {
            throw new IllegalStateException("Số lượng phải lớn hơn 0");
        }
        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                    "Sản phẩm '" + variant.getProduct().getName()
                            + "' chỉ còn " + variant.getStock() + " trong kho!");
        }

        return buildCartItemNoStockCheck(variant, quantity);
    }

    // Dựng DTO từ 1 variant đã load sẵn, KHÔNG validate lại tồn kho — dùng
    // sau khi đã reserve kho thành công (addItemToCart), vì lúc đó
    // variant.getStock() đã bị trừ đi đúng bằng quantity nên so sánh lại
    // "stock < quantity" sẽ sai ý nghĩa (đó là tồn CÒN LẠI, không phải điều
    // kiện hợp lệ của lần thêm này).
    private CounterCartItemDTO buildCartItemNoStockCheck(ProductVariant variant, int quantity) {
        BigDecimal unitPrice = getEffectivePrice(variant);
        ProductSale activeSale = getActiveSale(variant);

        return new CounterCartItemDTO(
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSize() != null ? variant.getSize().getName() : null,
                variant.getColor() != null ? variant.getColor().getName() : null,
                variant.getSku(),
                unitPrice,
                quantity,
                variant.getStock(),
                resolveImageUrl(variant),
                activeSale != null ? variant.getProduct().getPrice() : null,
                activeSale != null ? activeSale.getDiscountPercent() : null
        );
    }

    // ─────────────────────────────────────────────────────────────
    // ẢNH HIỂN THỊ — ưu tiên ảnh riêng của variant (variant_image,
    // theo sortOrder) để phân biệt đúng màu/kiểu; nếu variant chưa có
    // ảnh nào thì tạm dùng ảnh đầu tiên của Product làm ảnh thay thế.
    // Public để CashierController tái sử dụng cho /search-variants.
    // ─────────────────────────────────────────────────────────────

    public String resolveImageUrl(ProductVariant variant) {
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().get(0).getImageUrl();
        }
        Product product = variant.getProduct();
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0).getImageUrl();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // GIỎ HÀNG TẠM POS (đa giỏ — object trong PosCartRegistry, KHÔNG phải
    // AppOrder trong DB). Stock bị trừ NGAY khi thêm vào giỏ (giữ chỗ) và
    // hoàn lại khi xóa item / xóa giỏ / đóng tab / timeout — xem
    // StockMovementLogService.reserveForPosCart()/releaseForPosCart().
    // Chỉ tạo AppOrder thật khi checkout() thành công.
    // ─────────────────────────────────────────────────────────────

    public PosCart createCart(String label) {
        return cartRegistry.create(getCurrentUser().getId(), label);
    }

    /** Trả về null nếu không tồn tại (đã timeout/đóng) — dùng để kiểm tra tồn tại, không ném lỗi. */
    public PosCart getCart(String cartId) {
        return cartRegistry.get(cartId);
    }

    public PosCart getCartOrThrow(String cartId) {
        PosCart cart = cartRegistry.get(cartId);
        if (cart == null) {
            throw new IllegalStateException(
                    "Giỏ hàng không tồn tại hoặc đã hết hạn do treo quá lâu không hoạt động!");
        }
        return cart;
    }

    @Transactional
    public void addItemToCart(String cartId, Integer variantId, int addQty) {
        if (addQty < 1) {
            throw new IllegalStateException("Số lượng phải lớn hơn 0");
        }
        PosCart cart = getCartOrThrow(cartId);

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (variant.getStock() < addQty) {
            throw new IllegalStateException(
                    "Sản phẩm '" + variant.getProduct().getName()
                            + "' chỉ còn " + variant.getStock() + " trong kho!");
        }

        AppUser cashier = getCurrentUser();
        // Giữ chỗ kho trước (có pessimistic lock bên trong) — nếu variant vừa
        // hết hàng do cashier khác giữ chỗ trước, sẽ ném lỗi tồn kho âm ở đây.
        stockMovementLogService.reserveForPosCart(variantId, addQty, cartId, cashier);

        CounterCartItemDTO existing = cart.getItems().get(variantId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + addQty);
        } else {
            // variant.getStock() ở đây đã phản ánh số dư SAU khi trừ addQty ở trên
            cart.getItems().put(variantId, buildCartItemNoStockCheck(variant, addQty));
        }
        cart.touch();
    }

    @Transactional
    public void setItemQuantity(String cartId, Integer variantId, int newQty) {
        PosCart cart = getCartOrThrow(cartId);
        CounterCartItemDTO item = cart.getItems().get(variantId);
        if (item == null) {
            throw new IllegalStateException("Sản phẩm không có trong giỏ!");
        }

        if (newQty < 1) {
            removeItemFromCart(cartId, variantId);
            return;
        }

        int delta = newQty - item.getQuantity();
        if (delta == 0) return;

        AppUser cashier = getCurrentUser();

        if (delta > 0) {
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
            if (variant.getStock() < delta) {
                throw new IllegalStateException(
                        "Sản phẩm '" + variant.getProduct().getName() + "' chỉ còn "
                                + variant.getStock() + " trong kho, không đủ để tăng thêm " + delta + "!");
            }
            stockMovementLogService.reserveForPosCart(variantId, delta, cartId, cashier);
        } else {
            stockMovementLogService.releaseForPosCart(
                    variantId, -delta, cartId, "Giảm số lượng trong giỏ", cashier);
        }

        item.setQuantity(newQty);
        cart.touch();
    }

    @Transactional
    public void removeItemFromCart(String cartId, Integer variantId) {
        PosCart cart = getCartOrThrow(cartId);
        CounterCartItemDTO item = cart.getItems().remove(variantId);
        if (item != null) {
            stockMovementLogService.releaseForPosCart(
                    variantId, item.getQuantity(), cartId, "Xóa khỏi giỏ POS", getCurrentUser());
        }
        cart.touch();
    }

    @Transactional
    public void clearCartItems(String cartId) {
        PosCart cart = getCartOrThrow(cartId);
        AppUser cashier = getCurrentUser();
        for (CounterCartItemDTO item : cart.getItemList()) {
            stockMovementLogService.releaseForPosCart(
                    item.getVariantId(), item.getQuantity(), cartId, "Xóa toàn bộ giỏ POS", cashier);
        }
        cart.getItems().clear();
        cart.touch();
    }

    /** Đóng hẳn 1 tab giỏ (khác clearCartItems — giỏ này biến mất luôn khỏi registry). */
    @Transactional
    public void closeCart(String cartId) {
        PosCart cart = cartRegistry.get(cartId);
        if (cart == null) return; // đã bị timeout release trước đó — không có gì để hoàn thêm
        AppUser cashier = getCurrentUser();
        for (CounterCartItemDTO item : cart.getItemList()) {
            stockMovementLogService.releaseForPosCart(
                    item.getVariantId(), item.getQuantity(), cartId, "Đóng tab giỏ POS", cashier);
        }
        cartRegistry.remove(cartId);
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATE VOUCHER (preview trước khi checkout — không trừ lượt dùng)
    //
    // Quy tắc bảo vệ:
    //   • Voucher chỉ áp dụng khi khách có tài khoản (customerId != null).
    //     Không cho khách vãng lai dùng voucher để tránh bị lợi dụng.
    //   • Kiểm tra isActive, thời hạn, usageLimit, minOrderValue.
    //   • Trả VoucherPreviewDTO để JS hiển thị số tiền giảm ngay trên màn hình.
    // ─────────────────────────────────────────────────────────────

    public VoucherPreviewDTO validateVoucher(String code,
                                             BigDecimal orderTotal,
                                             Integer customerId) {
        // Voucher chỉ dành cho khách có tài khoản
        if (customerId == null) {
            return VoucherPreviewDTO.fail("Voucher chỉ áp dụng cho khách có tài khoản.");
        }

        Voucher v = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElse(null);
        if (v == null) {
            return VoucherPreviewDTO.fail("Mã voucher không tồn tại.");
        }

        if (Boolean.FALSE.equals(v.getIsActive())) {
            return VoucherPreviewDTO.fail("Mã voucher đã bị vô hiệu hóa.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            return VoucherPreviewDTO.fail("Mã voucher chưa đến ngày áp dụng.");
        }
        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            return VoucherPreviewDTO.fail("Mã voucher đã hết hạn.");
        }

        // Kiểm tra số lượng còn lại
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return VoucherPreviewDTO.fail("Mã voucher đã hết lượt sử dụng.");
        }

        if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) {
            return VoucherPreviewDTO.fail(
                    "Đơn tối thiểu " + v.getMinOrderValue().toPlainString() + " đ để dùng mã này.");
        }

        BigDecimal discount = calcDiscount(v, orderTotal);
        String msg = "Giảm " + v.getDiscountPercent().stripTrailingZeros().toPlainString() + "%";
        if (v.getMaxDiscount() != null) {
            msg += " (tối đa " + v.getMaxDiscount().toPlainString() + " đ)";
        }
        // Còn lại bao nhiêu lượt
        if (v.getUsageLimit() != null) {
            int remaining = v.getUsageLimit() - v.getUsedCount();
            msg += " · Còn " + remaining + " lượt";
        }

        return VoucherPreviewDTO.ok(discount, msg);
    }

    /** DTO trả về cho endpoint /validate-voucher */
    public record VoucherPreviewDTO(boolean valid, String message, BigDecimal discountAmount) {
        static VoucherPreviewDTO ok(BigDecimal discount, String msg) {
            return new VoucherPreviewDTO(true, msg, discount);
        }
        static VoucherPreviewDTO fail(String msg) {
            return new VoucherPreviewDTO(false, msg, BigDecimal.ZERO);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TÍNH TỔNG TIỀN GIỎ HÀNG (dùng để tạo số tiền thanh toán VNPay)
    // Không trừ kho, không trừ lượt voucher — chỉ preview.
    // ─────────────────────────────────────────────────────────────

    public BigDecimal previewCartTotal(List<CounterCartItemDTO> items,
                                       String voucherCode,
                                       Integer customerId) {
        BigDecimal subtotal = items.stream()
                .map(CounterCartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank() && customerId != null) {
            VoucherPreviewDTO preview = validateVoucher(voucherCode, subtotal, customerId);
            if (preview.valid()) {
                discount = preview.discountAmount();
            }
        }
        return subtotal.subtract(discount).max(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────
    // CHECKOUT — tạo AppOrder từ giỏ POS (cartId). KHÔNG trừ kho thêm nữa:
    // stock của từng item đã bị trừ (giữ chỗ) ngay từ lúc add-to-cart, nên
    // ở đây chỉ ghi 1 dòng log audit (changeQty = 0) đánh dấu "giữ chỗ này
    // đã chốt thành đơn hàng #X", để dễ tra cứu lịch sử biến động kho.
    //
    // Toàn bộ nằm trong 1 @Transactional: tạo order → ghi log/OrderDetail
    // từng dòng → áp voucher (nếu có) → save. Nếu bất kỳ bước nào fail →
    // rollback toàn bộ; giỏ trong registry KHÔNG bị xóa (cashier có thể
    // thử checkout lại), vì stock vẫn đang được giữ chỗ đúng.
    //
    // Giá lưu vào order_detail lấy từ item.getUnitPrice() — đã được tính
    // theo giá sale ngay khi thêm vào giỏ tạm, giữ đúng giá lúc khách được
    // báo giá, tránh lệch nếu sale hết hạn giữa lúc thao tác.
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AppOrder checkout(String cartId,
                             Integer customerId,
                             String paymentMethod,
                             String voucherCode) {

        PosCart cart = getCartOrThrow(cartId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống!");
        }
        List<CounterCartItemDTO> items = cart.getItemList();

        AppUser cashier = getCurrentUser();

        AppOrder order = new AppOrder();
        // MỚI: order_code là NOT NULL ở DB nhưng trước đây không được set
        // khi tạo đơn POS -> lỗi "Cannot insert the value NULL into column
        // 'order_code'". Sinh mã giống cách OrderService.generateUniqueOrderCode()
        // đang làm cho luồng online/guest.
        order.setOrderCode(generateUniqueOrderCode());
        order.setOrderType(OrderType.COUNTER);
        order.setCashier(cashier);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentMethod(paymentMethod);
        order.setIsPaid(true);
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveredAt(LocalDateTime.now());

        if (customerId != null) {
            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
            order.setCustomer(customer);
        }

        // Lưu trước để có order.getId() dùng làm refId khi ghi log biến động kho
        orderRepository.save(order);

        // ── Tính tổng hàng + ghi OrderDetail (không trừ kho — đã trừ lúc add) ──
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CounterCartItemDTO item : items) {
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm không tồn tại: " + item.getVariantId()));

            // changeQty = 0: không đổi số dư (đã trừ từ lúc add-to-cart), chỉ
            // ghi audit "chốt giữ chỗ giỏ POS #cartId thành đơn hàng #order.getId()"
            stockMovementLogService.logMovement(
                    variant,
                    StockMovementType.SALE,
                    0,
                    StockRefType.ORDER,
                    order.getId(),
                    "Chốt đơn từ giỏ POS #" + cartId + " - đơn #" + order.getId(),
                    cashier
            );

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice());
            detail.setOriginalPrice(item.getOriginalPrice() != null
                    ? item.getOriginalPrice() : item.getUnitPrice());
            order.getOrderDetails().add(detail);

            subtotal = subtotal.add(item.getLineTotal());
        }

        // ── Áp voucher (chỉ khi khách có tài khoản + nhập mã) ──
        BigDecimal discount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.isBlank() && customerId != null) {
            // Validate lại lần nữa trong transaction để chống race condition
            // (ví dụ 2 cashier dùng cùng mã cùng lúc ở 2 tab)
            VoucherPreviewDTO preview = validateVoucher(voucherCode, subtotal, customerId);
            if (!preview.valid()) {
                throw new IllegalStateException("Voucher không hợp lệ: " + preview.message());
            }

            Voucher voucher = voucherRepository.findByCode(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalStateException("Voucher không hợp lệ: không tìm thấy mã"));
            discount = preview.discountAmount();
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);

            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

            OrderVoucher orderVoucher = new OrderVoucher();
            orderVoucher.setOrder(order);
            orderVoucher.setVoucher(voucher);
            orderVoucher.setCustomer(customer);
            orderVoucher.setDiscountAmount(discount);

            order.setOrderVoucher(orderVoucher);
        }

        order.setTotalPrice(subtotal.subtract(discount).max(BigDecimal.ZERO));
        orderRepository.save(order);

        // Đơn đã tạo thành công — giỏ tạm không cần nữa, không release
        // (hàng đã bán thật, khác với clearCart/timeout là bỏ dở không mua).
        cartRegistry.remove(cartId);

        return order;
    }

    public AppOrder getOrderForInvoice(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    public List<AppOrder> getRecentOrdersByCurrentCashier(int days) {
        AppUser cashier = getCurrentUser();
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return orderRepository.findRecentCounterOrdersByCashier(cashier.getId(), from);
    }

    public boolean isCurrentUserAdminOrOwner() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_OWNER"));
    }

    // Đơn đã tạo quá CANCEL_WINDOW_MINUTES chưa — dùng cả trong cancelOrder()
    // lẫn ở Controller để quyết định hiển thị/bắt buộc ô ghi chú trên view.
    public boolean isOrderPastCancelWindow(AppOrder order) {
        LocalDateTime deadline = order.getOrderDate().plusMinutes(CANCEL_WINDOW_MINUTES);
        return LocalDateTime.now().isAfter(deadline);
    }

    public AppOrder getOwnOrderDetail(Integer orderId) {
        AppOrder order = getOrderForInvoice(orderId);
        if (isCurrentUserAdminOrOwner()) return order;

        AppUser cashier = getCurrentUser();
        if (order.getCashier() == null || !order.getCashier().getId().equals(cashier.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }

    // ─────────────────────────────────────────────────────────────
    // HỦY ĐƠN POS — khách đổi ý không thanh toán / thu ngân lỡ tạo nhầm.
    //
    // Quy tắc:
    //   • Chỉ hủy được đơn COUNTER, đang ở trạng thái COMPLETED
    //     (đơn POS luôn tạo thẳng COMPLETED, không có trạng thái PENDING).
    //   • Cashier chỉ hủy được đơn của chính mình, trong vòng
    //     CANCEL_WINDOW_MINUTES kể từ lúc tạo. Admin/Owner hủy được
    //     bất kỳ lúc nào (không giới hạn thời gian), NHƯNG nếu hủy đơn đã
    //     quá CANCEL_WINDOW_MINUTES thì bắt buộc nhập ghi chú chi tiết
    //     (không được để trống dù chọn lý do khác OTHER) — để minh bạch,
    //     vì hủy đơn cũ có thể làm lệch số liệu doanh thu đã tính trước đó.
    //   • Hoàn kho từng dòng sản phẩm + hoàn lượt dùng voucher (nếu có).
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void cancelOrder(Integer orderId, CancelReason reason, String note) {
        AppOrder order = getOwnOrderDetail(orderId); // đã kiểm tra quyền sở hữu / admin-owner

        if (order.getOrderType() != OrderType.COUNTER) {
            throw new IllegalStateException("Chỉ có thể hủy đơn bán tại quầy!");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái có thể hủy!");
        }
        if (reason == null) {
            throw new IllegalStateException("Vui lòng chọn lý do hủy!");
        }

        boolean isAdmin = isCurrentUserAdminOrOwner();
        boolean isPastWindow = isOrderPastCancelWindow(order);

        if (!isAdmin && isPastWindow) {
            throw new IllegalStateException(
                    "Đơn hàng đã tạo quá " + CANCEL_WINDOW_MINUTES
                            + " phút, không thể tự hủy. Vui lòng liên hệ quản lý!");
        }

        // Bắt buộc ghi chú khi: lý do "Khác", HOẶC admin/owner hủy đơn đã
        // quá cửa sổ thời gian chuẩn (đơn cũ, cần lý giải rõ ràng cho việc
        // hủy trễ, tránh lạm dụng quyền không giới hạn thời gian).
        boolean noteRequired = reason == CancelReason.OTHER || (isAdmin && isPastWindow);
        if (noteRequired && (note == null || note.isBlank())) {
            throw new IllegalStateException(isAdmin && isPastWindow
                    ? "Đơn hàng đã tạo quá " + CANCEL_WINDOW_MINUTES
                    + " phút — vui lòng nhập ghi chú lý do hủy trễ!"
                    : "Vui lòng nhập ghi chú khi chọn lý do 'Khác'!");
        }

        // Hoàn kho từng dòng sản phẩm
        AppUser actor = getCurrentUser();
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            stockMovementLogService.logMovement(
                    detail.getVariant(),
                    StockMovementType.CANCEL,
                    detail.getQuantity(),
                    StockRefType.ORDER,
                    order.getId(),
                    "Hủy đơn quầy #" + order.getId(),
                    actor
            );
        }

        // Hoàn lượt dùng voucher (nếu đơn có áp mã)
        orderVoucherRepository.findByOrderId(orderId).ifPresent(ov -> {
            Voucher voucher = ov.getVoucher();
            if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
            }
            order.setOrderVoucher(null);
            orderVoucherRepository.delete(ov);
        });

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelNote((note == null || note.isBlank()) ? null : note.trim());
        order.setCancelledBy(actor);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    // Sinh order_code duy nhất (8 ký tự hex viết hoa) — cùng logic với
    // OrderService.generateUniqueOrderCode() (luồng online/guest), tách
    // riêng ở đây vì method đó là private bên OrderService, không gọi
    // chéo được từ CashierService.
    private String generateUniqueOrderCode() {
        String code;
        do {
            code = java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }
}