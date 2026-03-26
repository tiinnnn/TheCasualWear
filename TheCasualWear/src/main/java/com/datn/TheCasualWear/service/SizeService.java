package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Size;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.repository.SizeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SizeService {
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;

    public SizeService(SizeRepository sizeRepository, ProductRepository productRepository) {
        this.sizeRepository = sizeRepository;
        this.productRepository = productRepository;
    }

    public List<Size> getAllSizes() {
        return sizeRepository.findAll();
    }

    public Size getSizeById(Integer id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy size với id: " + id));
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
        // Size liên kết với product qua product.size_id
        // Cần check trong ProductRepository
        if (productRepository.existsBySizeIdAndIsDeletedFalse(id)) {
            throw new IllegalStateException("Không thể xóa size đang được dùng bởi sản phẩm!");
        }
        sizeRepository.delete(size);
    }
}