package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Collection;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.repository.CollectionRepository;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.service.CloudinaryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ProductRepository    productRepository;
    private final CloudinaryService    cloudinaryService;

    public CollectionService(CollectionRepository collectionRepository,
                             ProductRepository productRepository,
                             @Lazy CloudinaryService cloudinaryService) {
        this.collectionRepository = collectionRepository;
        this.productRepository    = productRepository;
        this.cloudinaryService    = cloudinaryService;
    }

    // QUERY

    public List<Collection> getActiveCollections() {
        return collectionRepository.findActiveCollections();
    }

    public List<Collection> getAllCollections() {
        return collectionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Collection getById(Integer id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay collection id: " + id));
    }

    //ADMIN CRUD

    @Transactional
    public Collection create(Collection collection) {
        return collectionRepository.save(collection);
    }

    @Transactional
    public Collection update(Integer id, Collection data) {
        Collection c = getById(id);
        c.setName(data.getName());
        c.setDescription(data.getDescription());
        c.setStartDate(data.getStartDate());
        c.setEndDate(data.getEndDate());
        c.setIsActive(data.getIsActive());
        if (data.getCoverImage() != null && !data.getCoverImage().isBlank()) {
            c.setCoverImage(data.getCoverImage());
        }
        return collectionRepository.save(c);
    }

    @Transactional
    public void delete(Integer id) {
        Collection c = getById(id);
        // Xóa ảnh bìa trên Cloudinary nếu có
        if (c.getCoverImage() != null && !c.getCoverImage().isBlank()) {
            try {
                cloudinaryService.deleteImage(c.getCoverImage());
            } catch (Exception ignored) {
                // Không dừng lại nếu xóa ảnh thất bại
            }
        }
        c.getProducts().clear();
        collectionRepository.delete(c);
    }

    @Transactional
    public void toggleActive(Integer id) {
        Collection c = getById(id);
        c.setIsActive(!Boolean.TRUE.equals(c.getIsActive()));
        collectionRepository.save(c);
    }

    //QUAN LY PRODUCT

    @Transactional
    public void addProducts(Integer collectionId, List<Integer> productIds) {
        Collection c = getById(collectionId);
        for (Integer pid : productIds) {
            productRepository.findById(pid).ifPresent(p -> c.getProducts().add(p));
        }
        collectionRepository.save(c);
    }

    @Transactional
    public void removeProduct(Integer collectionId, Integer productId) {
        Collection c = getById(collectionId);
        c.getProducts().removeIf(p -> p.getId().equals(productId));
        collectionRepository.save(c);
    }

    public Set<Product> getProducts(Integer collectionId) {
        return getById(collectionId).getProducts();
    }
}