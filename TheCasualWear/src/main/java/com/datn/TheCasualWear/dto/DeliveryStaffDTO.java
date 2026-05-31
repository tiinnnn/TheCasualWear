package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.DeliveryProfile;
import lombok.Getter;

@Getter
public class DeliveryStaffDTO {

    private final Integer id;
    private final String  username;
    private final String  phone;
    private final String  area;        // khu vuc hoat dong
    private final boolean isAvailable;

    public DeliveryStaffDTO(AppUser user, DeliveryProfile profile) {
        this.id          = user.getId();
        this.username    = user.getUsername();
        this.phone       = user.getPhone();
        this.area        = profile != null ? profile.getArea()        : null;
        this.isAvailable = profile != null
                ? Boolean.TRUE.equals(profile.getIsAvailable())
                : true;
    }

    // Label hien thi trong dropdown
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder(username);
        if (phone  != null && !phone.isBlank())  sb.append(" — ").append(phone);
        if (area   != null && !area.isBlank())   sb.append(" [").append(area).append("]");
        if (!isAvailable) sb.append(" (Đang tắt)");
        return sb.toString();
    }
}