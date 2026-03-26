package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Category;
import com.datn.TheCasualWear.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));
    }

    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Danh mục đã tồn tại: " + category.getName());
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(Integer id, Category details) {
        Category category = getCategoryById(id);
        category.setName(details.getName());
        category.setDescription(details.getDescription());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Integer id) {
        Category category = getCategoryById(id);
        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException("Không thể xóa danh mục đang có sản phẩm!");
        }
        categoryRepository.delete(category);
    }
}