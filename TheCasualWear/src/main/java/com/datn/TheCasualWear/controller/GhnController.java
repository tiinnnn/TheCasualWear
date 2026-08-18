package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.dto.GhnLocationDTO;
import com.datn.TheCasualWear.service.GhnApiException;
import com.datn.TheCasualWear.service.GhnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Proxy GHN cho frontend — bắt buộc phải qua backend (không gọi thẳng GHN từ
 * JS trình duyệt) vì master-data/* cần header Token, lộ token ra frontend là
 * rủi ro bảo mật (ai cũng lấy được token, gọi API tốn phí/tạo đơn giả thay
 * shop). Dùng cho cả checkout (user + guest) -> PHẢI permitAll() trong
 * SecurityConfig, xem ghi chú ở OrderController cho các route guest khác.
 */
@RestController
@RequestMapping("/api/ghn")
public class GhnController {

    private final GhnService ghnService;

    public GhnController(GhnService ghnService) {
        this.ghnService = ghnService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        try {
            List<GhnLocationDTO> provinces = ghnService.getProvinces();
            return ResponseEntity.ok(provinces);
        } catch (GhnApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Không tải được danh sách tỉnh/thành, vui lòng thử lại sau."));
        }
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam int provinceId) {
        try {
            List<GhnLocationDTO> districts = ghnService.getDistricts(provinceId);
            return ResponseEntity.ok(districts);
        } catch (GhnApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Không tải được danh sách quận/huyện, vui lòng thử lại sau."));
        }
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam int districtId) {
        try {
            List<GhnLocationDTO> wards = ghnService.getWards(districtId);
            return ResponseEntity.ok(wards);
        } catch (GhnApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Không tải được danh sách phường/xã, vui lòng thử lại sau."));
        }
    }
}
