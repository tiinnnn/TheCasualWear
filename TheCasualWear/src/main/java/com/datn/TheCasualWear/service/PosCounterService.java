package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.PosCounter;

import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.PosCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosCounterService {

    private final PosCounterRepository counterRepository;
    private final AppOrderRepository orderRepository;

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

    // Chặn xóa nếu quầy đã từng có đơn hàng (qua Shift đã tạo ra đơn tại quầy này),
    // vì AppOrder không gắn trực tiếp PosCounter mà thông qua Shift.counter.
    public void deleteCounter(Integer id) {
        PosCounter counter = getCounterById(id);
        if (orderRepository.existsByShift_Counter_Id(id)) {
            throw new IllegalStateException(
                    "Không thể xóa quầy \"" + counter.getCode() + "\" vì đã có đơn hàng phát sinh tại quầy này!");
        }
        counterRepository.delete(counter);
    }
}