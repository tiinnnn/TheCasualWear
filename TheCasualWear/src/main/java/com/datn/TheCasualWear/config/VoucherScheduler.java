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

//    @Scheduled(cron = "0 0 0 * * *") sick
//    public void deactivateExpiredVouchers() {
//        System.out.println(">>> Voucher scheduler chạy lúc: " + LocalDateTime.now());
//        voucherRepository.findAll().stream()
//                .filter(v -> v.getIsActive()
//                        && v.getEndDate() != null
//                        && v.getEndDate().isBefore(LocalDateTime.now()))
//                .forEach(v -> {
//                    v.setIsActive(false);
//                    voucherRepository.save(v);
//                });
//    }
    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateExpiredVouchers() {
        System.out.println(">>> Voucher scheduler chạy lúc: " + LocalDateTime.now());
        voucherRepository.deactivateExpiredVouchers(LocalDateTime.now());
    }

}