package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.repository.VoucherRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class VoucherScheduler {

    private final VoucherRepository voucherRepository;

    public VoucherScheduler(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    // Chạy mỗi ngày lúc 00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateExpiredVouchers() {
        voucherRepository.findAll().stream()
                .filter(v -> v.getIsActive()
                        && v.getEndDate() != null
                        && v.getEndDate().isBefore(LocalDateTime.now()))
                .forEach(v -> {
                    v.setIsActive(false);
                    voucherRepository.save(v);
                });
    }
}