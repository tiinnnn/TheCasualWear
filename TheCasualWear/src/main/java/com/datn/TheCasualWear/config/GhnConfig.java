package com.datn.TheCasualWear.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate riêng cho GhnService — timeout NGẮN và tách khỏi RestTemplate
 * dùng cho các API khác trong app (nếu có), vì gọi lúc checkout: GHN
 * chậm/down không được kéo dài thời gian chờ của khách. GhnService bắt
 * timeout này rồi fallback về phí region-based (xem
 * OrderService.calculateShippingFeeRegionBased).
 *
 * Dùng SimpleClientHttpRequestFactory (JDK HttpURLConnection có sẵn, không
 * cần thêm dependency Apache HttpClient) thay vì RestTemplateBuilder —
 * RestTemplateBuilder đổi tên method (connectTimeout/setConnectTimeout)
 * giữa các version Spring Boot, dễ lỗi compile khi khác version.
 */
@Configuration
public class GhnConfig {

    @Bean
    public RestTemplate ghnRestTemplate(
            @Value("${ghn.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${ghn.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}