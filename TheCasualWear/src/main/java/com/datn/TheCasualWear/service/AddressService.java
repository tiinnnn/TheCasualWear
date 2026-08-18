package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Address;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AddressRepository;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 3;

    private final AddressRepository addressRepository;
    private final AppOrderRepository appOrderRepository;

    public AddressService(AddressRepository addressRepository, AppOrderRepository appOrderRepository) {
        this.addressRepository = addressRepository;
        this.appOrderRepository = appOrderRepository;
    }
    //validation
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{10}$");
    }

    // Lấy tất cả địa chỉ của user — chỉ lấy địa chỉ còn active (xem
    // Address.active). Địa chỉ đã "ẩn" vì từng dùng để đặt hàng sẽ không
    // hiện trong sổ địa chỉ nữa, dù vẫn còn trong DB cho lịch sử đơn hàng.
    public List<Address> getAddressesByUser(AppUser user) {
        return addressRepository.findByUserIdAndActiveTrue(user.getId());
    }

    // Lấy địa chỉ theo id (kiểm tra quyền sở hữu + phải còn active — dùng
    // cho các thao tác khách tự làm trên sổ địa chỉ: sửa, đặt mặc định...).
    // Nếu cần lấy Address bất kể active/quyền sở hữu (luồng đơn hàng), dùng
    // getAddressByIdForOrder() bên dưới.
    public Address getAddressById(Integer id, AppUser user) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + id));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập địa chỉ này!");
        }
        if (!Boolean.TRUE.equals(address.getActive())) {
            // Địa chỉ đã bị ẩn khỏi sổ — coi như không tồn tại với khách,
            // tránh lộ chi tiết "đã archive vì dùng trong đơn hàng cũ".
            throw new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + id);
        }
        return address;
    }

    // MỚI: lấy Address theo id KHÔNG kiểm tra quyền sở hữu, KHÔNG lọc active
    // — chỉ dùng nội bộ ở luồng VNPay callback (OrderController.vnpayReturn)
    // để lấy lại địa chỉ đã resolve/tạo lúc POST /checkout (trước khi
    // redirect sang VNPay). Không dùng getAddressById() thường ở đây vì tại
    // thời điểm này address có thể là loại "dùng 1 lần" với user = null
    // (xem createAddressForOrder), gọi address.getUser().getId() sẽ
    // NullPointerException — và cũng có thể đã bị set active=false ở lượt
    // sửa/xóa khác diễn ra sau khi đơn được tạo, nhưng đơn cũ vẫn cần đọc
    // đúng dữ liệu địa chỉ tại thời điểm đặt hàng.
    public Address getAddressByIdForOrder(Integer id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + id));
    }

    public Address getDefaultAddress(AppUser user) {
        return addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElse(null);
    }

    // Thêm địa chỉ mới
    public Address addAddress(AppUser user, Address address) {
        address.setUser(user);
        address.setActive(true);
        if (!isValidPhone(address.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }
        List<Address> existing = addressRepository.findByUserIdAndActiveTrue(user.getId());
        if (existing.size() >= MAX_ADDRESSES_PER_USER) {
            throw new IllegalStateException("Bạn chỉ có thể lưu tối đa 3 địa chỉ!");
        }
        // Nếu là địa chỉ đầu tiên → tự động set làm mặc định
        if (existing.isEmpty()) {
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }
        return addressRepository.save(address);
    }

    // MỚI: tạo Address từ dữ liệu khách vừa nhập/sửa ở trang checkout, dùng
    // chung cho 2 trường hợp (xem CustomerCheckoutFormDTO):
    //  - saveToBook = true  → lưu vào sổ địa chỉ, đặt làm mặc định (bỏ mặc
    //    định của địa chỉ cũ nếu có). Vẫn áp giới hạn tối đa 3 địa chỉ như
    //    addAddress() ở trên.
    //  - saveToBook = false → KHÔNG gắn vào sổ (user = null), chỉ dùng để
    //    gắn vào order.shippingAddress cho đơn hàng đang đặt — giống hệt
    //    cách guest checkout tạo Address (OrderService.placeOrderGuest).
    //    Loại địa chỉ này không hiện trong /account/address vì
    //    getAddressesByUser() lọc theo user_id, và được
    //    AddressRepository.findOrphanAddresses() + OrderScheduler dọn định
    //    kỳ nếu lỡ không đơn nào tham chiếu tới.
    public Address createAddressForOrder(AppUser user, Address input, boolean saveToBook) {
        if (!isValidPhone(input.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }
        input.setActive(true);

        if (!saveToBook) {
            input.setUser(null);
            input.setIsDefault(false);
            return addressRepository.save(input);
        }

        List<Address> existing = addressRepository.findByUserIdAndActiveTrue(user.getId());
        if (existing.size() >= MAX_ADDRESSES_PER_USER) {
            throw new IllegalStateException(
                    "Bạn đã lưu tối đa 3 địa chỉ! Bỏ chọn 'Đặt làm địa chỉ mặc định' để chỉ dùng " +
                            "địa chỉ này cho đơn hàng này, hoặc xóa bớt địa chỉ cũ trong 'Địa chỉ của tôi'.");
        }
        existing.stream()
                .filter(Address::getIsDefault)
                .forEach(old -> {
                    old.setIsDefault(false);
                    addressRepository.save(old);
                });

        input.setUser(user);
        input.setIsDefault(true);
        return addressRepository.save(input);
    }

    // MỚI: nếu địa chỉ CHƯA từng dùng để đặt đơn nào → sửa đè tại chỗ như
    // trước (case phổ biến nhất: vừa thêm, sửa lại ngay, hoàn toàn an toàn).
    // Nếu ĐÃ dùng để đặt ít nhất 1 đơn → KHÔNG sửa đè (sẽ làm sai lệch dữ
    // liệu giao hàng của đơn cũ), thay vào đó tạo 1 Address mới với dữ liệu
    // vừa sửa và "ẩn" (active = false) bản cũ khỏi sổ địa chỉ. Với khách
    // nhìn từ ngoài (trang /account/address) thì y hệt như sửa tại chỗ —
    // chỉ khác ở chỗ id đổi, còn đơn hàng cũ vẫn tham chiếu đúng dữ liệu tại
    // thời điểm đặt hàng.
    public Address updateAddress(Integer id, AppUser user, Address details) {
        Address address = getAddressById(id, user);

        if (!isValidPhone(details.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }

        if (!appOrderRepository.existsByAddressId(id)) {
            address.setFullName(details.getFullName());
            address.setPhone(details.getPhone());
            address.setStreet(details.getStreet());
            address.setCity(details.getCity());
            address.setDistrict(details.getDistrict());
            address.setCountry(details.getCountry());
            // MỚI (4.5): copy mã GHN — thiếu bước này thì sửa địa chỉ xong
            // sẽ mất mã GHN đã chọn, checkout sau đó rơi về fallback
            // region-based dù khách đã chọn đúng GHN cascade ở form.
            address.setGhnProvinceId(details.getGhnProvinceId());
            address.setGhnDistrictId(details.getGhnDistrictId());
            address.setGhnWardCode(details.getGhnWardCode());
            return addressRepository.save(address);
        }

        Address replacement = new Address();
        replacement.setUser(user);
        replacement.setFullName(details.getFullName());
        replacement.setPhone(details.getPhone());
        replacement.setStreet(details.getStreet());
        replacement.setCity(details.getCity());
        replacement.setDistrict(details.getDistrict());
        replacement.setCountry(details.getCountry());
        replacement.setActive(true);
        // MỚI (4.5): tương tự nhánh trên — replacement là Address MỚI hoàn
        // toàn (address cũ bị ẩn đi), không copy thì mã GHN luôn NULL dù
        // khách vừa chọn đúng ở form.
        replacement.setGhnProvinceId(details.getGhnProvinceId());
        replacement.setGhnDistrictId(details.getGhnDistrictId());
        replacement.setGhnWardCode(details.getGhnWardCode());
        // Địa chỉ mới kế thừa vị trí mặc định của địa chỉ cũ (nếu có), giữ
        // đúng bất biến "chỉ 1 địa chỉ active là mặc định".
        replacement.setIsDefault(address.getIsDefault());

        address.setIsDefault(false);
        address.setActive(false);
        addressRepository.save(address);

        return addressRepository.save(replacement);
    }

    // MỚI: nếu địa chỉ CHƯA dùng để đặt đơn nào → xóa cứng như trước. Nếu
    // ĐÃ dùng để đặt ít nhất 1 đơn → không thể xóa cứng (vi phạm FK
    // constraint vì AppOrder.shippingAddress/billingAddress vẫn trỏ tới id
    // này, và xóa sẽ làm mất luôn dữ liệu giao hàng của đơn cũ) — thay vào
    // đó chỉ ẩn khỏi sổ địa chỉ (active = false). Với khách nhìn từ ngoài
    // thì giống hệt đã xóa.
    public void deleteAddress(Integer id, AppUser user) {
        Address address = getAddressById(id, user);

        if (address.getIsDefault()) {
            throw new IllegalStateException("Không thể xóa địa chỉ mặc định! Hãy đặt địa chỉ khác làm mặc định trước.");
        }

        if (appOrderRepository.existsByAddressId(id)) {
            address.setActive(false);
            addressRepository.save(address);
            return;
        }

        addressRepository.delete(address);
    }

    public void setDefaultAddress(Integer id, AppUser user) {
        // Bỏ mặc định của địa chỉ cũ
        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(old -> {
                    old.setIsDefault(false);
                    addressRepository.save(old);
                });

        // Set mặc định cho địa chỉ mới — getAddressById() đã tự đảm bảo
        // address còn active (không thể đặt mặc định 1 địa chỉ đã bị ẩn).
        Address address = getAddressById(id, user);
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    // Dọn Address "dùng 1 lần" (user = null) không còn đơn nào tham chiếu —
    // gọi từ OrderScheduler mỗi ngày, xem ghi chú ở
    // AddressRepository.findOrphanAddresses().
    public void deleteOrphanAddresses() {
        List<Address> orphans = addressRepository.findOrphanAddresses();
        if (!orphans.isEmpty()) {
            addressRepository.deleteAll(orphans);
        }
    }
}