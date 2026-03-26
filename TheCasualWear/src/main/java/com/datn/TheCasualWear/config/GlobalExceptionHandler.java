package com.datn.TheCasualWear.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Không tìm thấy entity → trang 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error/404";
    }

    // Lỗi nghiệp vụ (xóa category còn sản phẩm, SKU trùng...) → redirect về trang trước
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException e,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin");
    }

    // Lỗi validate (SKU trùng khi tạo/sửa sản phẩm...)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin");
    }

    // Lỗi không mong muốn → trang 500
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception e, Model model) {
        model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        return "error/500";
    }
}