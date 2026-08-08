package com.datn.TheCasualWear.controller.Cashier;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.PosCounter;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.PosCounterService;
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
    private final PosCounterService counterService;
    private final AppUserRepository appUserRepository;

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập");
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    // ── CHỌN QUẦY (bước đầu tiên trước khi mở ca) ───────────────────────

    @GetMapping("/select-counter")
    public String selectCounterForm(Model model) {
        if (shiftService.hasOpenShift(getCurrentUser())) {
            return "redirect:/cashier";
        }
        model.addAttribute("counters", counterService.getActiveCounters());
        model.addAttribute("view", "cashier/shift/select-counter");
        return "layouts/cashier-layout";
    }

    @PostMapping("/select-counter")
    public String selectCounter(@RequestParam Integer counterId, RedirectAttributes ra) {
        if (shiftService.hasOpenShift(getCurrentUser())) {
            return "redirect:/cashier";
        }
        if (shiftService.isCounterOccupied(counterId)) {
            ra.addFlashAttribute("errorMessage",
                    "Quầy này đang có người sử dụng, vui lòng chọn quầy khác!");
            return "redirect:/cashier/shift/select-counter";
        }

        var pending = shiftService.getLatestUnconfirmedClosedShiftForCounter(counterId);
        if (pending.isPresent()) {
            return "redirect:/cashier/shift/confirm-handover/" + pending.get().getId();
        }
        return "redirect:/cashier/shift/open?counterId=" + counterId;
    }

    // ── MỞ CA (đã chọn quầy xong) ────────────────────────────────────────

    @GetMapping("/open")
    public String openForm(@RequestParam(required = false) Integer counterId, Model model) {
        if (shiftService.hasOpenShift(getCurrentUser())) {
            return "redirect:/cashier";
        }
        if (counterId == null) {
            return "redirect:/cashier/shift/select-counter";
        }

        PosCounter counter = counterService.getCounterById(counterId);

        // Check lại phòng trường hợp vào thẳng URL bằng tay hoặc bấm Back
        if (shiftService.isCounterOccupied(counterId)) {
            return "redirect:/cashier/shift/select-counter";
        }
        var pending = shiftService.getLatestUnconfirmedClosedShiftForCounter(counterId);
        if (pending.isPresent()) {
            return "redirect:/cashier/shift/confirm-handover/" + pending.get().getId();
        }

        model.addAttribute("counter", counter);
        model.addAttribute("view", "cashier/shift/open");
        return "layouts/cashier-layout";
    }

    @PostMapping("/open")
    public String open(@RequestParam Integer counterId,
                       @RequestParam BigDecimal openingCash,
                       RedirectAttributes ra) {
        try {
            shiftService.openShift(getCurrentUser(), counterId, openingCash);
            ra.addFlashAttribute("successMessage", "Đã mở ca làm việc!");
            return "redirect:/cashier";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/shift/open?counterId=" + counterId;
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
            // Đã có người xác nhận rồi (VD: 2 tab cùng mở) -> quay lại đúng quầy đó
            return "redirect:/cashier/shift/open?counterId=" + shift.getCounter().getId();
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
            Shift shift = shiftService.confirmHandover(id, getCurrentUser(), isMatch, note);
            ra.addFlashAttribute("successMessage",
                    isMatch ? "Đã xác nhận bàn giao ca!"
                            : "Đã ghi nhận báo cáo sai lệch, tiếp tục mở ca mới.");
            return "redirect:/cashier/shift/open?counterId=" + shift.getCounter().getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/shift/confirm-handover/" + id;
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("shifts", shiftService.getHistoryThisWeek(getCurrentUser()));
        model.addAttribute("view", "cashier/shift/history");
        return "layouts/cashier-layout";
    }

    // Xem danh sách sản phẩm đã bán/hủy trong 1 ca — được xem nếu là ca của
    // chính mình, HOẶC ca đó đang chờ xác nhận bàn giao (cần xem để đối
    // chiếu trước khi xác nhận/báo sai lệch). Ca của người khác nhưng đã
    // được xác nhận bàn giao xong thì không cho xem nữa.
    @GetMapping("/{id}/items")
    public String items(@PathVariable Integer id, Model model) {
        Shift shift = shiftService.getShiftById(id);
        boolean isOwnShift = shift.getCashier().getId().equals(getCurrentUser().getId());
        if (!isOwnShift && shift.isHandoverConfirmed()) {
            throw new ResourceNotFoundException("Không tìm thấy ca làm việc");
        }
        model.addAttribute("shift", shift);
        model.addAttribute("orders", shiftService.getOrdersForShift(id));
        model.addAttribute("backUrl", "/cashier/shift/history");
        model.addAttribute("view", "shared/shift/items");
        return "layouts/cashier-layout";
    }
}