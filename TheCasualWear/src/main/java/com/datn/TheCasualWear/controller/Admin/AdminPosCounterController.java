package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.PosCounter;
import com.datn.TheCasualWear.service.PosCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pos-counters")
@RequiredArgsConstructor
public class AdminPosCounterController {

    private final PosCounterService counterService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("counters", counterService.getAllCounters());
        model.addAttribute("view", "admin/pos-counter/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("counter", new PosCounter());
        model.addAttribute("view", "admin/pos-counter/form");
        return "layouts/admin-layout";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("counter", counterService.getCounterById(id));
        model.addAttribute("view", "admin/pos-counter/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PosCounter counter, RedirectAttributes ra) {
        try {
            if (counter.getId() == null) {
                counterService.createCounter(counter);
                ra.addFlashAttribute("successMessage", "Đã thêm quầy mới!");
            } else {
                counterService.updateCounter(counter.getId(), counter);
                ra.addFlashAttribute("successMessage", "Đã cập nhật quầy!");
            }
            return "redirect:/admin/pos-counters";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return counter.getId() == null
                    ? "redirect:/admin/pos-counters/add"
                    : "redirect:/admin/pos-counters/edit/" + counter.getId();
        }
    }

    @GetMapping("/toggle/{id}")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        counterService.toggleActive(id);
        ra.addFlashAttribute("successMessage", "Đã đổi trạng thái quầy!");
        return "redirect:/admin/pos-counters";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            counterService.deleteCounter(id);
            ra.addFlashAttribute("successMessage", "Đã xóa quầy!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/pos-counters";
    }
}