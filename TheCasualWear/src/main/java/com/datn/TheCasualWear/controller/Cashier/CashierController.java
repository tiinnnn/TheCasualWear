package com.datn.TheCasualWear.controller.Cashier;

import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.service.CashierService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/cashier")
public class CashierController {

    private static final String SESSION_CART_KEY = "COUNTER_CART";

    private final CashierService           cashierService;
    private final ProductVariantRepository productVariantRepository;
    private final AppUserRepository        appUserRepository;

    public CashierController(CashierService cashierService,
                             ProductVariantRepository productVariantRepository,
                             AppUserRepository appUserRepository) {
        this.cashierService = cashierService;
        this.productVariantRepository = productVariantRepository;
        this.appUserRepository = appUserRepository;
    }

    @SuppressWarnings("unchecked")
    private List<CounterCartItemDTO> getCart(HttpSession session) {
        Object cart = session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            List<CounterCartItemDTO> newCart = new ArrayList<>();
            session.setAttribute(SESSION_CART_KEY, newCart);
            return newCart;
        }
        return (List<CounterCartItemDTO>) cart;
    }

    // ─────────────────────────────────────────────────────────────
    // TRANG BÁN HÀNG CHÍNH
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public String posPage(Model model, HttpSession session) {
        List<CounterCartItemDTO> cart = getCart(session);
        BigDecimal grandTotal = cart.stream()
                .map(CounterCartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cart);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("view", "cashier/pos");
        return "layouts/cashier-layout";
    }

    // ─────────────────────────────────────────────────────────────
    // TÌM SẢN PHẨM (autocomplete)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/search-variants")
    @ResponseBody
    public List<VariantSearchResultDTO> searchVariants(@RequestParam String keyword) {
        return productVariantRepository.searchAvailableByKeyword(keyword)
                .stream()
                .map(v -> new VariantSearchResultDTO(
                        v.getId(),
                        v.getProduct().getName(),
                        v.getSize() != null ? v.getSize().getName() : null,
                        v.getColor() != null ? v.getColor().getName() : null,
                        v.getSku(),
                        v.getProduct().getPrice()
                                .add(v.getPriceAdjustment() != null
                                        ? v.getPriceAdjustment() : BigDecimal.ZERO),
                        v.getStock()
                ))
                .toList();
    }

    public record VariantSearchResultDTO(
            Integer variantId,
            String productName,
            String sizeName,
            String colorName,
            String sku,
            BigDecimal price,
            Integer stock
    ) {}

    // ─────────────────────────────────────────────────────────────
    // TÌM KHÁCH HÀNG THEO TÊN / EMAIL / SĐT (autocomplete)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/search-customers")
    @ResponseBody
    public List<CustomerSearchResultDTO> searchCustomers(@RequestParam String keyword) {
        return appUserRepository.searchCustomersByKeyword(keyword)
                .stream()
                .map(u -> new CustomerSearchResultDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getPhone(),
                        u.getEmail()
                ))
                .toList();
    }

    public record CustomerSearchResultDTO(
            Integer id,
            String username,
            String phone,
            String email
    ) {}

    // ─────────────────────────────────────────────────────────────
    // VALIDATE VOUCHER (AJAX preview — không trừ lượt dùng)
    //
    // Gọi trước khi submit để hiển thị số tiền được giảm ngay trên màn hình.
    // orderTotal được tính ở client từ giỏ tạm hiện tại.
    // customerId bắt buộc có giá trị — khách vãng lai không được dùng voucher.
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/validate-voucher")
    @ResponseBody
    public CashierService.VoucherPreviewDTO validateVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal,
            @RequestParam(required = false) Integer customerId) {
        return cashierService.validateVoucher(code, orderTotal, customerId);
    }

    // ─────────────────────────────────────────────────────────────
    // THÊM / SỬA SỐ LƯỢNG / XÓA / XÓA HẾT GIỎ TẠM (chưa trừ kho)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes ra) {
        try {
            List<CounterCartItemDTO> cart = getCart(session);
            for (CounterCartItemDTO item : cart) {
                if (item.getVariantId().equals(variantId)) {
                    CounterCartItemDTO updated = cashierService.buildCartItem(
                            variantId, item.getQuantity() + quantity);
                    item.setQuantity(updated.getQuantity());
                    session.setAttribute(SESSION_CART_KEY, cart);
                    return "redirect:/cashier";
                }
            }
            cart.add(cashierService.buildCartItem(variantId, quantity));
            session.setAttribute(SESSION_CART_KEY, cart);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    // ── MỚI: sửa số lượng 1 item đã có trong giỏ (nút +/- hoặc nhập tay) ──
    @PostMapping("/cart/update")
    public String updateCartQuantity(@RequestParam Integer variantId,
                                     @RequestParam Integer quantity,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        try {
            List<CounterCartItemDTO> cart = getCart(session);

            if (quantity < 1) {
                cart.removeIf(i -> i.getVariantId().equals(variantId));
            } else {
                // Build lại item từ DB để lấy giá/tồn kho mới nhất và validate số lượng mới
                CounterCartItemDTO updated = cashierService.buildCartItem(variantId, quantity);
                for (int i = 0; i < cart.size(); i++) {
                    if (cart.get(i).getVariantId().equals(variantId)) {
                        cart.set(i, updated);
                        break;
                    }
                }
            }
            session.setAttribute(SESSION_CART_KEY, cart);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Integer variantId, HttpSession session) {
        List<CounterCartItemDTO> cart = getCart(session);
        cart.removeIf(i -> i.getVariantId().equals(variantId));
        session.setAttribute(SESSION_CART_KEY, cart);
        return "redirect:/cashier";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute(SESSION_CART_KEY);
        return "redirect:/cashier";
    }

    // ─────────────────────────────────────────────────────────────
    // THANH TOÁN — trừ kho + áp voucher (nếu có) + tạo AppOrder
    // voucherCode chỉ có tác dụng khi customerId != null (check lại trong service)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) Integer customerId,
                           @RequestParam String paymentMethod,
                           @RequestParam(required = false) String voucherCode,
                           HttpSession session,
                           RedirectAttributes ra) {
        try {
            List<CounterCartItemDTO> cart = getCart(session);
            AppOrder order = cashierService.checkout(customerId, cart, paymentMethod, voucherCode);
            session.removeAttribute(SESSION_CART_KEY);
            return "redirect:/cashier/invoice/" + order.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HÓA ĐƠN — trang in (window.print())
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/invoice/{orderId}")
    public String invoice(@PathVariable Integer orderId, Model model) {
        model.addAttribute("order", cashierService.getOrderForInvoice(orderId));
        return "cashier/invoice";
    }

    // ─────────────────────────────────────────────────────────────
    // DANH SÁCH ĐƠN ĐÃ BÁN TẠI QUẦY
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myRecentOrders(@RequestParam(defaultValue = "3") int days, Model model) {
        model.addAttribute("orders", cashierService.getRecentOrdersByCurrentCashier(days));
        model.addAttribute("days", days);
        model.addAttribute("view", "cashier/orders");
        return "layouts/cashier-layout";
    }

    @GetMapping("/orders/{id}")
    public String myOrderDetail(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("order", cashierService.getOwnOrderDetail(id));
            model.addAttribute("view", "cashier/order-detail");
            return "layouts/cashier-layout";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/orders";
        }
    }
}