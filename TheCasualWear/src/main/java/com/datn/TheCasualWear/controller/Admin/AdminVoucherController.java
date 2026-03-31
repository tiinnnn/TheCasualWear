package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Voucher;
import com.datn.TheCasualWear.service.VoucherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherService.getAllVouchers());
        model.addAttribute("newVoucher", new Voucher());
        model.addAttribute("view", "admin/voucher/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/edit/{id}")
    public String editVoucherPage(@PathVariable Integer id, Model model) {
        model.addAttribute("voucher", voucherService.getVoucherById(id));
        model.addAttribute("view", "admin/voucher/form");
        return "layouts/admin-layout";
    }

    @GetMapping("/edit")
    public String editNewVoucherPage(Model model) {
        model.addAttribute("view", "admin/voucher/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute Voucher voucher,
                              RedirectAttributes redirectAttributes) {
        if (voucher.getId() == null) {
            voucherService.createVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm voucher thành công!");
        } else {
            voucherService.updateVoucher(voucher.getId(), voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật voucher thành công!");
        }
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/toggle/{id}")
    public String toggleVoucher(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        voucherService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái voucher!");
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        voucherService.deleteVoucher(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa voucher!");
        return "redirect:/admin/vouchers";
    }
}