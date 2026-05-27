package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Color;
import com.datn.TheCasualWear.repository.ColorRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService {
    private final ColorRepository colorRepository;
    private final ProductVariantRepository variantRepository;


    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    public Color getColorById(Integer id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu với id: " + id));
    }

    public Color createColor(Color color) {
        if (colorRepository.findByName(color.getName()).isPresent()) {
            throw new IllegalArgumentException("Màu đã tồn tại: " + color.getName());
        }
        return colorRepository.save(color);
    }

    public Color updateColor(Integer id, Color details) {
        Color color = getColorById(id);
        color.setName(details.getName());
        return colorRepository.save(color);
    }

    public void deleteColor(Integer id) {
        Color color = getColorById(id);
        if (variantRepository.existsByColorId(id)) {
            throw new IllegalStateException("Không thể xóa màu đang được dùng bởi sản phẩm!");
        }
        colorRepository.delete(color);
    }
}