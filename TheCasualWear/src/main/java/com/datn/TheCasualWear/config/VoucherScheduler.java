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

    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateExpiredVouchers() {
        voucherRepository.deactivateExpiredVouchers(LocalDateTime.now());
    }
}