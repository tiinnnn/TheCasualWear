package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
}