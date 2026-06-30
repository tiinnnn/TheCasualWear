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
        java.math.BigDecimal grandTotal = cart.stream()
                .map(CounterCartItemDTO::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("cartItems", cart);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("view", "cashier/pos");
        return "layouts/cashier-layout";
    }

    // Tìm sản phẩm để hiển thị gợi ý (trả JSON, gọi bằng AJAX từ trang pos)
    // Lưu ý: trả DTO chứ không trả thẳng entity, tránh lỗi serialize lazy
    // proxy (đã từng gặp ở trang product detail trước đây).
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
                                .add(v.getPriceAdjustment() != null ? v.getPriceAdjustment() : java.math.BigDecimal.ZERO),
                        v.getStock()
                ))
                .toList();
    }

    // DTO nội bộ cho kết quả tìm kiếm sản phẩm — record gọn cho mục đích này
    public record VariantSearchResultDTO(
            Integer variantId,
            String productName,
            String sizeName,
            String colorName,
            String sku,
            java.math.BigDecimal price,
            Integer stock
    ) {}

    // ─────────────────────────────────────────────────────────────
    // TÌM KHÁCH HÀNG THEO TÊN / EMAIL / SĐT (autocomplete cho POS)
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

    // DTO nội bộ cho kết quả tìm kiếm khách hàng
    public record CustomerSearchResultDTO(
            Integer id,
            String username,
            String phone,
            String email
    ) {}

    // ─────────────────────────────────────────────────────────────
    // THÊM SẢN PHẨM VÀO GIỎ TẠM (chưa trừ kho)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes ra) {
        try {
            List<CounterCartItemDTO> cart = getCart(session);

            // Nếu variant đã có trong giỏ thì cộng dồn số lượng
            for (CounterCartItemDTO item : cart) {
                if (item.getVariantId().equals(variantId)) {
                    CounterCartItemDTO updated = cashierService.buildCartItem(
                            variantId, item.getQuantity() + quantity);
                    item.setQuantity(updated.getQuantity());
                    session.setAttribute(SESSION_CART_KEY, cart);
                    return "redirect:/cashier";
                }
            }

            CounterCartItemDTO newItem = cashierService.buildCartItem(variantId, quantity);
            cart.add(newItem);
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
    // THANH TOÁN — trừ kho + tạo AppOrder
    // customerId giờ được gán tự động qua JS sau khi người dùng chọn
    // khách hàng từ danh sách gợi ý (search-customers), không nhập tay nữa.
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) Integer customerId,
                           @RequestParam String paymentMethod,
                           HttpSession session,
                           RedirectAttributes ra) {
        try {
            List<CounterCartItemDTO> cart = getCart(session);
            AppOrder order = cashierService.checkout(customerId, cart, paymentMethod);

            session.removeAttribute(SESSION_CART_KEY); // xóa giỏ tạm sau khi thanh toán xong

            return "redirect:/cashier/invoice/" + order.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HÓA ĐƠN — trang để in (window.print())
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/invoice/{orderId}")
    public String invoice(@PathVariable Integer orderId, Model model) {
        AppOrder order = cashierService.getOrderForInvoice(orderId);
        model.addAttribute("order", order);
        return "cashier/invoice"; // trang riêng, KHÔNG dùng layout chung (để in gọn)
    }

    // ─────────────────────────────────────────────────────────────
    // DANH SÁCH ĐƠN ĐÃ BÁN TẠI QUẦY (mặc định 3 ngày gần đây)
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