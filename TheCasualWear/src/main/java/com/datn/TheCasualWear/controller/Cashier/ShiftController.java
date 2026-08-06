package com.datn.TheCasualWear.controller.Cashier;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/cashier/shift")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService      shiftService;
    private final AppUserRepository appUserRepository;

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập");
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    @GetMapping("/open")
    public String openForm(Model model) {
        // Nếu đã có ca mở rồi thì không cho vào form mở ca nữa
        if (shiftService.hasOpenShift(getCurrentUser())) {
            return "redirect:/cashier";
        }
        // Bắt buộc xác nhận bàn giao ca trước (nếu có ca CLOSED nào chưa
        // ai xác nhận) trước khi được mở ca mới cho mình.
        var pending = shiftService.getLatestUnconfirmedClosedShift();
        if (pending.isPresent()) {
            return "redirect:/cashier/shift/confirm-handover/" + pending.get().getId();
        }
        model.addAttribute("view", "cashier/shift/open");
        return "layouts/cashier-layout";
    }

    @PostMapping("/open")
    public String open(@RequestParam BigDecimal openingCash, RedirectAttributes ra) {
        try {
            shiftService.openShift(getCurrentUser(), openingCash);
            ra.addFlashAttribute("successMessage", "Đã mở ca làm việc!");
            return "redirect:/cashier";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/shift/open";
        }
    }

    @GetMapping("/close")
    public String closeForm(Model model) {
        Shift shift = shiftService.getOpenShift(getCurrentUser()).orElse(null);
        if (shift == null) {
            return "redirect:/cashier/shift/open";
        }
        model.addAttribute("shift", shift);
        model.addAttribute("expectedCashPreview", shiftService.previewExpectedCash(shift));
        model.addAttribute("itemsSoldPreview", shiftService.previewItemsSold(shift));
        model.addAttribute("view", "cashier/shift/close");
        return "layouts/cashier-layout";
    }

    @PostMapping("/close")
    public String close(@RequestParam Integer shiftId,
                        @RequestParam BigDecimal actualCash,
                        @RequestParam(required = false) String note,
                        RedirectAttributes ra) {
        try {
            Shift closed = shiftService.closeShift(shiftId, getCurrentUser(), actualCash, note);
            String diffLabel = closed.getCashDifference().compareTo(BigDecimal.ZERO) >= 0
                    ? "dư " + closed.getCashDifference().toPlainString() + " đ"
                    : "thiếu " + closed.getCashDifference().abs().toPlainString() + " đ";
            ra.addFlashAttribute("successMessage",
                    "Đã đóng ca! Chênh lệch: " + diffLabel
                            + " — chờ ca sau xác nhận bàn giao.");
            return "redirect:/cashier/shift/history";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/shift/close";
        }
    }

    // ── XÁC NHẬN BÀN GIAO CA ────────────────────────────────────────────

    @GetMapping("/confirm-handover/{id}")
    public String confirmHandoverForm(@PathVariable Integer id, Model model) {
        Shift shift = shiftService.getShiftById(id);
        if (shift.isHandoverConfirmed()) {
            // Đã có người xác nhận rồi (VD: 2 tab cùng mở) -> cho qua thẳng
            return "redirect:/cashier/shift/open";
        }
        model.addAttribute("shift", shift);
        model.addAttribute("view", "cashier/shift/confirm-handover");
        return "layouts/cashier-layout";
    }

    @PostMapping("/confirm-handover/{id}")
    public String confirmHandover(@PathVariable Integer id,
                                  @RequestParam(defaultValue = "true") boolean isMatch,
                                  @RequestParam(required = false) String note,
                                  RedirectAttributes ra) {
        try {
            shiftService.confirmHandover(id, getCurrentUser(), isMatch, note);
            ra.addFlashAttribute("successMessage",
                    isMatch ? "Đã xác nhận bàn giao ca!"
                            : "Đã ghi nhận báo cáo sai lệch, tiếp tục mở ca mới.");
            return "redirect:/cashier/shift/open";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/shift/confirm-handover/" + id;
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("shifts", shiftService.getHistory(getCurrentUser()));
        model.addAttribute("view", "cashier/shift/history");
        return "layouts/cashier-layout";
    }
}