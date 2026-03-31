package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ==================== DÙNG CHUNG ====================

    public Product getProductById(Integer id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
    }

    // ==================== PHÍA USER ====================

    // Trang shop: search + sort
    public List<Product> getShopProducts(String keyword, String sort) {
        Sort sortObj = switch (sort != null ? sort : "newest") {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default           -> Sort.by("createdAt").descending(); // newest + popular
        };
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        return productRepository.searchProducts(kw, sortObj);
    }

    public List<Product> getProductVariants(Integer id) {
        Product product = getProductById(id);
        Integer colorId = product.getColor() != null ? product.getColor().getId() : null;
        return productRepository.findVariantsByNameAndColor(product.getName(), colorId);
    }

    // Trang chủ: 8 sản phẩm mới nhất
    public List<Product> getNewestProducts() {
        Pageable top8 = PageRequest.of(0, 8);
        return productRepository.findTop8Newest(top8);
    }

    // ==================== PHÍA ADMIN ====================

    public List<Product> getAdminProducts() {
        return productRepository.findByIsDeletedFalse();
    }

    public List<Product> getAdminProducts(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        return productRepository.searchProductsForAdmin(kw);
    }

    public List<Product> getDeletedProducts() {
        return productRepository.findByIsDeletedTrue();
    }

    public Product createProduct(Product product) {
        if (product.getSku() != null && productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + product.getSku());
        }
        product.setIsDeleted(false);
        return productRepository.save(product);
    }

    public Product updateProduct(Integer id, Product productDetails) {
        Product product = getProductById(id);

        if (productDetails.getSku() != null
                && productRepository.existsBySkuAndIdNot(productDetails.getSku(), id)) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + productDetails.getSku());
        }

        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setDescription(productDetails.getDescription());
        product.setSku(productDetails.getSku());
        product.setStock(productDetails.getStock());
        product.setCostPrice(productDetails.getCostPrice());
        product.setCategory(productDetails.getCategory());
        product.setSize(productDetails.getSize());
        product.setColor(productDetails.getColor());
        return productRepository.save(product);
    }

    public void deleteProduct(Integer id) {
        Product product = getProductById(id);
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    public void restoreProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        product.setIsDeleted(false);
        productRepository.save(product);
    }
}