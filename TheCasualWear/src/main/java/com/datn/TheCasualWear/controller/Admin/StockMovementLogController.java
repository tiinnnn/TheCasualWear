package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.StockMovementLog;
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

    // Lọc theo variantId (ưu tiên nếu có) hoặc theo khoảng ngày (mặc định 30 ngày gần nhất)
    @GetMapping
    public String view(@RequestParam(required = false) Integer variantId,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       Model model) {

        List<StockMovementLog> logs;
        if (variantId != null) {
            logs = stockMovementLogService.getLogsByVariant(variantId);
        } else {
            LocalDateTime from = (fromDate == null || fromDate.isBlank())
                    ? LocalDate.now().minusDays(30).atStartOfDay()
                    : LocalDate.parse(fromDate).atStartOfDay();
            LocalDateTime to = (toDate == null || toDate.isBlank())
                    ? LocalDateTime.now()
                    : LocalDate.parse(toDate).atTime(23, 59, 59);
            logs = stockMovementLogService.getLogsBetween(from, to);
        }

        model.addAttribute("logs", logs);
        model.addAttribute("variantId", variantId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("view", "admin/warehouse/stock-log");
        return "layouts/admin-layout";
    }
}
