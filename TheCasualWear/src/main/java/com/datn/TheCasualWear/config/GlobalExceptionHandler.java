package com.datn.TheCasualWear.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
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

    // Vi phạm ràng buộc ở tầng DB (UNIQUE, FK, NOT NULL...) — bắt riêng
    // TRƯỚC handler Exception chung để không lộ message SQL thô ra ngoài
    // UI. Vì @ControllerAdvice này là GLOBAL (áp dụng cho mọi controller,
    // mọi entity trong hệ thống — không riêng AppUser), message ở đây phải
    // giữ trung tính, không hardcode theo 1 entity/field cụ thể nào. Nguyên
    // nhân phổ biến: race condition (2 request cùng lúc vượt qua check ở
    // service), thiếu check trùng trước khi save, hoặc còn bản ghi con
    // tham chiếu (FK) khi xóa.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException e,
                                               RedirectAttributes redirectAttributes,
                                               HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Dữ liệu không hợp lệ (có thể bị trùng hoặc đang được dữ liệu khác tham chiếu), vui lòng kiểm tra lại!");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin");
    }

    // Lỗi nghiệp vụ → redirect về trang trước
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