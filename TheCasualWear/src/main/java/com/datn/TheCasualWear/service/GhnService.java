package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.dto.GhnLocationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gọi GHN API — master-data (tỉnh/quận-huyện/phường-xã, dùng cho dropdown
 * checkout) + Calculate Fee (tính phí ship thật).
 *
 * QUAN TRỌNG: GHN vẫn dùng cấu trúc 3 cấp cũ (Tỉnh -> Quận/Huyện -> Phường/
 * Xã) với DistrictID riêng của họ, KHÔNG theo sáp nhập hành chính 07/2025.
 * Không liên quan gì tới provinces.open-api.vn đang dùng cho city/district
 * String hiện có trên Address — 2 hệ mã hoàn toàn độc lập.
 *
 * Mọi lỗi (timeout, HTTP lỗi, response không hợp lệ) đều ném GhnApiException
 * — nơi gọi (GhnController cho master-data, OrderService cho fee) phải tự
 * xử lý fallback, KHÔNG để lỗi này làm vỡ luồng checkout.
 *
 * MỚI: dùng Map/List thuần (ParameterizedTypeReference<Map<String,Object>>)
 * thay vì com.fasterxml.jackson.databind.JsonNode — Spring vẫn dùng Jackson
 * bên dưới để convert JSON (đã có sẵn, chạy được ở apply-voucher endpoint),
 * chỉ là code không cần import trực tiếp package databind nữa, tránh lỗi
 * "Cannot resolve symbol 'databind'" nếu project không expose package đó
 * trực tiếp cho compiler.
 */
@Service
public class GhnService {

    private final RestTemplate ghnRestTemplate;
    private final String apiBaseUrl;
    private final String token;
    private final String shopId;
    private final String fromDistrictId;
    private final String fromWardCode;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    // service_type_id = 2 ("Chuẩn") — phù hợp hàng nhẹ dưới 20kg (quần áo).
    // Không gọi Get Service để tra service_id cụ thể theo từng tuyến, vì
    // service_type_id cố định đã đủ dùng cho catalogue toàn hàng nhẹ của
    // shop — xác nhận lại bằng 1 lệnh gọi thật trên tài khoản test trước
    // khi lên production, phòng trường hợp tuyến nào đó GHN không hỗ trợ.
    private static final int SERVICE_TYPE_ID_STANDARD = 2;

    public GhnService(RestTemplate ghnRestTemplate,
                      @Value("${ghn.api-base-url}") String apiBaseUrl,
                      @Value("${ghn.token}") String token,
                      @Value("${ghn.shop-id}") String shopId,
                      @Value("${ghn.from-district-id}") String fromDistrictId,
                      @Value("${ghn.from-ward-code}") String fromWardCode) {
        this.ghnRestTemplate = ghnRestTemplate;
        this.apiBaseUrl = apiBaseUrl;
        this.token = token;
        this.shopId = shopId;
        this.fromDistrictId = fromDistrictId;
        this.fromWardCode = fromWardCode;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Token", token);
        // ShopId chỉ bắt buộc cho Calculate Fee / tạo đơn, không cần cho
        // master-data (province/district/ward) — nhưng gửi kèm cũng không
        // sao, GHN bỏ qua nếu endpoint không dùng tới.
        headers.set("ShopId", shopId);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> requireDataList(ResponseEntity<Map<String, Object>> res) {
        Map<String, Object> body = res.getBody();
        if (body == null || !(body.get("data") instanceof List)) {
            throw new GhnApiException("Response GHN không có field 'data' hợp lệ (dạng list): " + body);
        }
        return (List<Map<String, Object>>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireDataObject(ResponseEntity<Map<String, Object>> res) {
        Map<String, Object> body = res.getBody();
        if (body == null || !(body.get("data") instanceof Map)) {
            throw new GhnApiException("Response GHN không có field 'data' hợp lệ (dạng object): " + body);
        }
        return (Map<String, Object>) body.get("data");
    }

    public List<GhnLocationDTO> getProvinces() {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map<String, Object>> res = ghnRestTemplate.exchange(
                    apiBaseUrl + "/master-data/province", HttpMethod.GET, entity, MAP_TYPE);
            List<Map<String, Object>> data = requireDataList(res);
            List<GhnLocationDTO> result = new ArrayList<>();
            for (Map<String, Object> item : data) {
                result.add(new GhnLocationDTO(
                        String.valueOf(item.get("ProvinceID")),
                        String.valueOf(item.get("ProvinceName"))));
            }
            return result;
        } catch (RestClientException e) {
            throw new GhnApiException("Không lấy được danh sách tỉnh/thành từ GHN", e);
        }
    }

    public List<GhnLocationDTO> getDistricts(int provinceId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("province_id", provinceId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map<String, Object>> res = ghnRestTemplate.exchange(
                    apiBaseUrl + "/master-data/district", HttpMethod.POST, entity, MAP_TYPE);
            List<Map<String, Object>> data = requireDataList(res);
            List<GhnLocationDTO> result = new ArrayList<>();
            for (Map<String, Object> item : data) {
                result.add(new GhnLocationDTO(
                        String.valueOf(item.get("DistrictID")),
                        String.valueOf(item.get("DistrictName"))));
            }
            return result;
        } catch (RestClientException e) {
            throw new GhnApiException("Không lấy được danh sách quận/huyện từ GHN", e);
        }
    }

    public List<GhnLocationDTO> getWards(int districtId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("district_id", districtId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map<String, Object>> res = ghnRestTemplate.exchange(
                    apiBaseUrl + "/master-data/ward?district_id=" + districtId,
                    HttpMethod.POST, entity, MAP_TYPE);
            List<Map<String, Object>> data = requireDataList(res);
            List<GhnLocationDTO> result = new ArrayList<>();
            for (Map<String, Object> item : data) {
                result.add(new GhnLocationDTO(
                        String.valueOf(item.get("WardCode")),
                        String.valueOf(item.get("WardName"))));
            }
            return result;
        } catch (RestClientException e) {
            throw new GhnApiException("Không lấy được danh sách phường/xã từ GHN", e);
        }
    }

    /**
     * @param toDistrictId DistrictID (GHN) của địa chỉ nhận hàng
     * @param toWardCode   WardCode (GHN) của địa chỉ nhận hàng
     * @param weightGrams  tổng cân nặng giỏ hàng (gram) — tối thiểu 1, GHN
     *                     từ chối weight = 0
     * @return phí ship (đã làm tròn, đơn vị đồng)
     */
    public BigDecimal calculateFee(int toDistrictId, String toWardCode, int weightGrams) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from_district_id", Integer.parseInt(fromDistrictId));
            body.put("from_ward_code", fromWardCode);
            body.put("service_type_id", SERVICE_TYPE_ID_STANDARD);
            body.put("to_district_id", toDistrictId);
            body.put("to_ward_code", toWardCode);
            body.put("weight", Math.max(weightGrams, 1));
            // height/length/width bỏ qua — GHN tự áp mặc định theo cân nặng
            // khi thiếu, đủ dùng cho quần áo (không cần đo kích thước gói).

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map<String, Object>> res = ghnRestTemplate.exchange(
                    apiBaseUrl + "/v2/shipping-order/fee", HttpMethod.POST, entity, MAP_TYPE);
            Map<String, Object> data = requireDataObject(res);
            if (!(data.get("total") instanceof Number)) {
                throw new GhnApiException("Response GHN Calculate Fee thiếu/sai field 'total': " + data);
            }
            return BigDecimal.valueOf(((Number) data.get("total")).longValue());
        } catch (RestClientException e) {
            throw new GhnApiException("Gọi GHN Calculate Fee thất bại", e);
        }
    }
}