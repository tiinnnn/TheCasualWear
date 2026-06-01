package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Size;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;
    private final ProductVariantRepository variantRepository;

    public List<Size> getAllSizes() {
        return sizeRepository.findAll();
    }

    public Size getSizeById(Integer id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy size với id: " + id));
    }

    public Size createSize(Size size) {
        if (sizeRepository.findByName(size.getName()).isPresent()) {
            throw new IllegalArgumentException("Size đã tồn tại: " + size.getName());
        }
        return sizeRepository.save(size);
    }

    public Size updateSize(Integer id, Size details) {
        Size size = getSizeById(id);
        size.setName(details.getName());
        return sizeRepository.save(size);
    }

    public void deleteSize(Integer id) {
        Size size = getSizeById(id);
        if (variantRepository.existsBySizeId(id)) {
            throw new IllegalStateException(
                    "Không thể xóa size đang được dùng bởi biến thể sản phẩm!");
        }
        sizeRepository.delete(size);
    }
}