package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.service.ProductSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleScheduler {

    private final ProductSaleService productSaleService;

    @Scheduled(cron = "0 */5 * * * *")
    public void deactivateExpiredSales() {
        productSaleService.deactivateExpiredSales();
    }
}