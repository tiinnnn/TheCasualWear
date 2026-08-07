package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.OpenShiftPreviewDTO;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/shifts")
@RequiredArgsConstructor
public class AdminShiftController {

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

    @GetMapping
    public String list(Model model) {
        model.addAttribute("shifts", shiftService.getAllHistory());
        model.addAttribute("view", "admin/shift/list");
        return "layouts/admin-layout";
    }

    // Admin xem được log sản phẩm đã bán/hủy của bất kỳ ca nào (không giới
    // hạn như phía cashier chỉ xem được ca của chính mình).
    @GetMapping("/{id}/items")
    public String items(@PathVariable Integer id, Model model) {
        model.addAttribute("shift", shiftService.getShiftById(id));
        model.addAttribute("orders", shiftService.getOrdersForShift(id));
        model.addAttribute("backUrl", "/admin/shifts");
        model.addAttribute("view", "shared/shift/items");
        return "layouts/admin-layout";
    }

    // Admin tự xác nhận bàn giao thay ca sau — dùng cho trường hợp ca cuối
    // ngày ở 1 quầy, không có ai mở ca tiếp theo để tự xác nhận ngay được.
    @PostMapping("/{id}/confirm-handover")
    public String confirmHandoverByAdmin(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            shiftService.confirmHandover(id, getCurrentUser(), true,
                    "Xác nhận bởi admin (không có ca sau tại thời điểm này)");
            ra.addFlashAttribute("successMessage", "Đã xác nhận bàn giao ca (bởi admin)!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/shifts";
    }

    // ── TỔNG KẾT CUỐI NGÀY ───────────────────────────────────────────────

    @GetMapping("/daily-summary")
    public String dailySummary(@RequestParam(required = false) String date, Model model) {
        LocalDate selectedDate = (date == null || date.isBlank())
                ? LocalDate.now() : LocalDate.parse(date);

        List<Shift> closedShifts = shiftService.getShiftsClosedOnDate(selectedDate);

        model.addAttribute("summary", shiftService.getDailySummary(selectedDate));
        model.addAttribute("closedShifts", closedShifts);
        model.addAttribute("selectedDate", selectedDate);

        // Chỉ hiện phần "đang diễn ra" khi xem đúng ngày hôm nay — xem lại
        // 1 ngày trong quá khứ thì mọi ca của ngày đó chắc chắn đã đóng rồi.
        if (selectedDate.equals(LocalDate.now())) {
            List<OpenShiftPreviewDTO> openPreviews = shiftService.getAllOpenShifts().stream()
                    .map(s -> new OpenShiftPreviewDTO(
                            s,
                            shiftService.previewExpectedCash(s),
                            shiftService.previewItemsSold(s)))
                    .toList();
            model.addAttribute("openPreviews", openPreviews);
        }

        model.addAttribute("view", "admin/shift/daily-summary");
        return "layouts/admin-layout";
    }
}