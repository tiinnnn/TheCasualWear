package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.entity.VariantImage;
import com.datn.TheCasualWear.repository.VariantImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariantImageService {

    private final VariantImageRepository variantImageRepository;
    private final CloudinaryService      cloudinaryService;

    @Transactional
    public void uploadImages(ProductVariant variant, List<MultipartFile> files) throws Exception {
        if (files == null || files.isEmpty()) return;

        // Lấy sort_order tiếp theo
        int nextOrder = variantImageRepository
                .findByVariantIdOrderBySortOrderAsc(variant.getId()).size() + 1;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String url = cloudinaryService.uploadFile(file);
            VariantImage image = new VariantImage();
            image.setVariant(variant);
            image.setImageUrl(url);
            image.setSortOrder(nextOrder++);
            variantImageRepository.save(image);
        }
    }

    @Transactional
    public void deleteImage(Integer imageId) throws Exception {
        VariantImage image = variantImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ảnh với id: " + imageId));
        cloudinaryService.deleteImage(image.getImageUrl());
        variantImageRepository.delete(image);
    }

    @Transactional
    public void deleteAllByVariant(Integer variantId) throws Exception {
        List<VariantImage> images = variantImageRepository
                .findByVariantIdOrderBySortOrderAsc(variantId);
        for (VariantImage image : images) {
            cloudinaryService.deleteImage(image.getImageUrl());
        }
        variantImageRepository.deleteByVariantId(variantId);
    }

    public List<VariantImage> getImagesByVariant(Integer variantId) {
        return variantImageRepository.findByVariantIdOrderBySortOrderAsc(variantId);
    }
}