package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/shifts")
@RequiredArgsConstructor
public class AdminShiftController {

    private final ShiftService shiftService;

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
}