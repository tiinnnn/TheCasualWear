package com.datn.TheCasualWear.dto;

/**
 * DTO gọn dùng để trả JSON cho address-cascade phía frontend (không trả
 * nguyên response GHN vì có nhiều field thừa: SupportType, CanUpdateCOD...).
 * Dùng chung cho cả 3 cấp (Tỉnh/Quận-Huyện/Phường-Xã) — chỉ khác id là
 * Integer (province/district) hay String (ward, vì GHN trả WardCode dạng
 * số nhưng để String cho an toàn — đã thấy field WardCode không có quotes
 * trong doc mẫu GHN, một số ward có thể có mã dạng khác).
 */
public record GhnLocationDTO(String id, String name) {
}
