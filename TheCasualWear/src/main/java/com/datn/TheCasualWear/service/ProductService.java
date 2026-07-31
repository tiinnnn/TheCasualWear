package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;
    private final CartItemRepository       cartItemRepository;
    private final ProductImageRepository   productImageRepository;
    private final OrderDetailRepository    orderDetailRepository;
    private final CloudinaryService        cloudinaryService;

    private static final int SHOP_PAGE_SIZE  = 12;
    private static final int ADMIN_PAGE_SIZE = 15;


    public Product getProductById(Integer id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));
    }

    // Bọc "%...%" quanh keyword ở tầng Java thay vì dùng CONCAT() trong JPQL,
    // để tránh Hibernate + SQL Server dialect sinh CAST(? AS VARCHAR(MAX))
    // làm hỏng ký tự tiếng Việt có dấu khi so khớp LIKE.
    private String toLikePattern(String keyword) {
        return (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim() + "%";
    }

    //USER

    public Page<Product> getShopProducts(String keyword, String sort,
                                         Integer categoryId, int page) {
        Sort sortObj = switch (sort != null ? sort : "newest") {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default           -> Sort.by("createdAt").descending();
        };
        String kw = toLikePattern(keyword);
        Pageable pageable = PageRequest.of(page, SHOP_PAGE_SIZE, sortObj);
        return productRepository.searchProducts(kw, categoryId, pageable);
    }

    public List<Product> getNewestProducts() {
        return productRepository.findTop8Newest(PageRequest.of(0, 8));
    }

    //ADMIN

    public Page<Product> getAdminProducts(String keyword, int page) {
        String kw = toLikePattern(keyword);
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE,
                Sort.by("createdAt").descending());
        return productRepository.searchProductsForAdmin(kw, pageable);
    }

    public List<Product> getAdminProductsList() {
        return productRepository.findByIsDeletedFalse();
    }

    public List<Product> getDeletedProducts() {
        return productRepository.findByIsDeletedTrue();
    }

    // CRUD PRODUCT

    @Transactional
    public Product createProduct(Product product) {
        product.setIsDeleted(false);
        return productRepository.save(product);
    }

    @Transactional
    public Product createProductWithVariants(Product product,
                                             List<ProductVariant> variants) {
        product.setIsDeleted(false);
        Product saved = productRepository.save(product);

        for (ProductVariant v : variants) {
            if (v.getSku() != null && variantRepository.existsBySku(v.getSku())) {
                throw new IllegalArgumentException("SKU đã tồn tại: " + v.getSku());
            }
            v.setProduct(saved);
            variantRepository.save(v);
        }
        return saved;
    }

    @Transactional
    public Product updateProduct(Integer id, Product details) {
        Product product = getProductById(id);
        product.setName(details.getName());
        product.setPrice(details.getPrice());
        product.setDescription(details.getDescription());
        product.setCategory(details.getCategory());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        Product product = getProductById(id);
        product.setIsDeleted(true);
        productRepository.save(product);

        // Xóa cart item của tất cả variant thuộc product này
        variantRepository.findByProductId(id)
                .forEach(v -> cartItemRepository.deleteByVariantId(v.getId()));

        // Xóa sản phẩm khỏi tất cả collection đang chứa nó (nếu có)
        for (Collection collection : product.getCollections()) {
            collection.getProducts().remove(product);
        }
    }

    public void restoreProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));
        product.setIsDeleted(false);
        productRepository.save(product);
    }

    @Transactional
    public void hardDeleteProduct(Integer id) throws Exception {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm!"));

        boolean hasActiveOrder = orderDetailRepository
                .existsByProductIdAndOrderStatusNot(id, OrderStatus.CANCELLED);
        if (hasActiveOrder) {
            throw new IllegalStateException(
                    "Không thể xóa! Sản phẩm đang có trong đơn hàng chưa hủy.");
        }
        variantRepository.findByProductId(id)
                .forEach(v -> cartItemRepository.deleteByVariantId(v.getId()));

        // Xóa sản phẩm khỏi tất cả collection đang chứa nó (nếu có)
        for (Collection collection : product.getCollections()) {
            collection.getProducts().remove(product);
        }

        // Xóa order_detail của đơn CANCELLED
        orderDetailRepository.deleteByProductId(id);

        // Xóa ảnh trên Cloudinary nếu không dùng bởi SP khác
        List<ProductImage> images = productImageRepository.findByProductId(id);
        for (ProductImage image : images) {
            boolean usedByOther = productImageRepository
                    .existsByImageUrlAndProductIdNot(image.getImageUrl(), id);
            if (!usedByOther) {
                cloudinaryService.deleteImage(image.getImageUrl());
            }
        }

        productImageRepository.deleteByProduct(product);
        productRepository.delete(product);
    }
}