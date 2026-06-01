package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Collection;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.service.CloudinaryService;
import com.datn.TheCasualWear.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/collections")
@RequiredArgsConstructor
public class AdminCollectionController {

    private final CollectionService collectionService;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;


    @GetMapping
    public String list(Model model) {
        model.addAttribute("collections", collectionService.getAllCollections());
        model.addAttribute("view", "admin/collection/list");
        return "layouts/admin-layout";
    }


    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Collection c = collectionService.getById(id);
        model.addAttribute("collection", c);
        model.addAttribute("collectionProducts", c.getProducts());

        var availableProducts = productRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())
                        && !c.getProducts().contains(p))
                .toList();
        model.addAttribute("availableProducts", availableProducts);
        model.addAttribute("view", "admin/collection/detail");
        return "layouts/admin-layout";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("collection", new Collection());
        model.addAttribute("view", "admin/collection/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Collection collection,
                         @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                         RedirectAttributes ra) {
        try {
            Collection saved = collectionService.create(collection);
            if (coverFile != null && !coverFile.isEmpty()) {
                cloudinaryService.uploadCollectionCover(saved, coverFile);
            }
            ra.addFlashAttribute("successMessage", "Tạo collection thành công!");
            return "redirect:/admin/collections/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/collections/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("collection", collectionService.getById(id));
        model.addAttribute("view", "admin/collection/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @ModelAttribute Collection collection,
                         @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                         RedirectAttributes ra) {
        try {
            Collection updated = collectionService.update(id, collection);
            if (coverFile != null && !coverFile.isEmpty()) {
                cloudinaryService.uploadCollectionCover(updated, coverFile);
            }
            ra.addFlashAttribute("successMessage", "Cập nhật thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/collections/" + id;
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            collectionService.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa collection!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/collections";
    }

    // Toggle active

    @GetMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        collectionService.toggleActive(id);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái!");
        return "redirect:/admin/collections";
    }

    // Quan ly product

    @PostMapping("/{id}/products/add")
    public String addProducts(@PathVariable Integer id,
                              @RequestParam List<Integer> productIds,
                              RedirectAttributes ra) {
        try {
            collectionService.addProducts(id, productIds);
            ra.addFlashAttribute("successMessage",
                    "Đã thêm " + productIds.size() + " sản phẩm!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/collections/" + id;
    }

    @GetMapping("/{id}/products/{productId}/remove")
    public String removeProduct(@PathVariable Integer id,
                                @PathVariable Integer productId,
                                RedirectAttributes ra) {
        collectionService.removeProduct(id, productId);
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi collection!");
        return "redirect:/admin/collections/" + id;
    }
}