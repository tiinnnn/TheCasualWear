package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.PosCounter;
import com.datn.TheCasualWear.repository.PosCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosCounterService {

    private final PosCounterRepository counterRepository;

    public List<PosCounter> getAllCounters() {
        return counterRepository.findAll();
    }

    // Dùng cho dropdown chọn quầy ở bước "vào bán hàng" (Việc 2)
    public List<PosCounter> getActiveCounters() {
        return counterRepository.findByIsActiveTrueOrderByCodeAsc();
    }

    public PosCounter getCounterById(Integer id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy quầy với id: " + id));
    }

    public PosCounter createCounter(PosCounter counter) {
        if (counter.getCode() == null || counter.getCode().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã quầy!");
        }
        String code = counter.getCode().trim().toUpperCase();
        if (counterRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã quầy đã tồn tại: " + code);
        }
        counter.setCode(code);
        counter.setIsActive(true);
        return counterRepository.save(counter);
    }

    public PosCounter updateCounter(Integer id, PosCounter details) {
        PosCounter counter = getCounterById(id);

        if (details.getCode() == null || details.getCode().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã quầy!");
        }
        String code = details.getCode().trim().toUpperCase();
        if (counterRepository.existsByCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Mã quầy đã tồn tại: " + code);
        }

        counter.setCode(code);
        counter.setName(details.getName());
        return counterRepository.save(counter);
    }

    public void toggleActive(Integer id) {
        PosCounter counter = getCounterById(id);
        counter.setIsActive(!counter.getIsActive());
        counterRepository.save(counter);
    }

    // LƯU Ý: chưa check "quầy đang có ca OPEN" ở đây vì Shift chưa gắn
    // counter (sẽ làm ở Việc 2) — lúc đó cần bổ sung throw
    // IllegalStateException nếu còn ca OPEN tại quầy này trước khi cho xóa.
    public void deleteCounter(Integer id) {
        counterRepository.delete(getCounterById(id));
    }
}