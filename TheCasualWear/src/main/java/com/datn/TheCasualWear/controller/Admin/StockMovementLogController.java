package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.StockMovementLog;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.service.StockMovementLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/warehouse/stock-log")
@RequiredArgsConstructor
public class StockMovementLogController {

    private final StockMovementLogService stockMovementLogService;

    // Tìm kiếm theo tên sản phẩm / SKU / loại biến động trong khoảng ngày
    // (mặc định 30 ngày gần nhất nếu không truyền fromDate/toDate)
    @GetMapping
    public String view(@RequestParam(required = false) String productName,
                       @RequestParam(required = false) String sku,
                       @RequestParam(required = false) StockMovementType changeType,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       Model model) {

        LocalDateTime from = (fromDate == null || fromDate.isBlank())
                ? LocalDate.now().minusDays(30).atStartOfDay()
                : LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime to = (toDate == null || toDate.isBlank())
                ? LocalDateTime.now()
                : LocalDate.parse(toDate).atTime(23, 59, 59);

        List<StockMovementLog> logs = stockMovementLogService.searchLogs(productName, sku, changeType, from, to);

        model.addAttribute("logs", logs);
        model.addAttribute("productName", productName);
        model.addAttribute("sku", sku);
        model.addAttribute("changeType", changeType);
        model.addAttribute("changeTypes", StockMovementType.values());
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("view", "admin/warehouse/stock-log");
        return "layouts/admin-layout";
    }
}