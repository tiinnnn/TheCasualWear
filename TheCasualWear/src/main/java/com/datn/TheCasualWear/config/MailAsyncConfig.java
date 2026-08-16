package com.datn.TheCasualWear.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor riêng cho việc gửi email (4.3) — KHÔNG dùng @Async mặc định của
 * Spring (SimpleAsyncTaskExecutor), vì nó tạo 1 thread MỚI cho MỖI lần gọi,
 * không giới hạn số lượng, nguy hiểm nếu nhiều đơn được xác nhận cùng lúc
 * (VD giờ cao điểm sale). Quy mô demo DATN nên corePoolSize/maxPoolSize nhỏ.
 *
 * ⚠️ Nếu @EnableAsync đã được khai báo ở nơi khác trong project (main class
 * hoặc config khác, VD cho @Scheduled job), XÓA dòng @EnableAsync ở đây để
 * tránh khai báo trùng — chỉ cần 1 nơi trong toàn bộ Spring context.
 */
@Configuration
@EnableAsync
public class MailAsyncConfig {

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}
