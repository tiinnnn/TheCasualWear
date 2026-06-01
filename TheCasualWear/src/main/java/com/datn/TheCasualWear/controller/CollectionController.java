package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Collection;
import com.datn.TheCasualWear.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public String listCollections(Model model) {
        model.addAttribute("collections", collectionService.getActiveCollections());
        model.addAttribute("view", "shop/collections");
        return "layouts/shop-layout";
    }

    @GetMapping("/{id}")
    public String collectionDetail(@PathVariable Integer id, Model model) {
        Collection c = collectionService.getById(id);

        if (!Boolean.TRUE.equals(c.getIsActive())) {
            return "redirect:/collections";
        }

        model.addAttribute("collection", c);
        model.addAttribute("products", c.getProducts().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList());
        model.addAttribute("view", "shop/collection-detail");
        return "layouts/shop-layout";
    }
}