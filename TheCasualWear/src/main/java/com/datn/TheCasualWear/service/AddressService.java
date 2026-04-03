package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Address;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }
    //validation
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{10}$");
    }

    // Lấy tất cả địa chỉ của user
    public List<Address> getAddressesByUser(AppUser user) {
        return addressRepository.findByUserId(user.getId());
    }

    // Lấy địa chỉ theo id (kiểm tra quyền sở hữu)
    public Address getAddressById(Integer id, AppUser user) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + id));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập địa chỉ này!");
        }
        return address;
    }

    // Lấy địa chỉ mặc định
    public Address getDefaultAddress(AppUser user) {
        return addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElse(null); // chưa có địa chỉ mặc định thì trả null
    }

    // Thêm địa chỉ mới
    public Address addAddress(AppUser user, Address address) {
        address.setUser(user);
        if (!isValidPhone(address.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }
        // Nếu là địa chỉ đầu tiên → tự động set làm mặc định
        List<Address> existing = addressRepository.findByUserId(user.getId());
        if (existing.isEmpty()) {
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        return addressRepository.save(address);
    }

    // Sửa địa chỉ
    public Address updateAddress(Integer id, AppUser user, Address details) {
        Address address = getAddressById(id, user);
        address.setFullName(details.getFullName());
        address.setPhone(details.getPhone());
        address.setStreet(details.getStreet());
        address.setCity(details.getCity());
        address.setDistrict(details.getDistrict());
        address.setCountry(details.getCountry());
        if (!isValidPhone(address.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }

        return addressRepository.save(address);
    }

    // Xóa địa chỉ
    public void deleteAddress(Integer id, AppUser user) {
        Address address = getAddressById(id, user);

        if (address.getIsDefault()) {
            throw new IllegalStateException("Không thể xóa địa chỉ mặc định! Hãy đặt địa chỉ khác làm mặc định trước.");
        }

        addressRepository.delete(address);
    }

    // Đặt địa chỉ mặc định
    public void setDefaultAddress(Integer id, AppUser user) {
        // Bỏ mặc định của địa chỉ cũ
        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(old -> {
                    old.setIsDefault(false);
                    addressRepository.save(old);
                });

        // Set mặc định cho địa chỉ mới
        Address address = getAddressById(id, user);
        address.setIsDefault(true);
        addressRepository.save(address);
    }
}