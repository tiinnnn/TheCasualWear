package com.datn.TheCasualWear.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductImage;
import com.datn.TheCasualWear.repository.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final ProductImageRepository productImageRepository;

    public CloudinaryService(Cloudinary cloudinary,
                             ProductImageRepository productImageRepository) {
        this.cloudinary = cloudinary;
        this.productImageRepository = productImageRepository;
    }

    // Upload 1 ảnh lên Cloudinary, trả về URL
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,       // lưu vào folder trên Cloudinary
                        "resource_type", "auto"
                )
        );
        return uploadResult.get("secure_url").toString();
    }

    // Upload nhiều ảnh cho 1 sản phẩm
    public void uploadProductImages(Product product, List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String url = uploadImage(file, "products");

            ProductImage image = new ProductImage();
            image.setImageUrl(url);
            image.setProduct(product);
            productImageRepository.save(image);
        }
    }

    // Lấy publicId từ URL để xóa trên Cloudinary
    // URL dạng: https://res.cloudinary.com/cloud_name/image/upload/v123/products/abc.jpg
    // PublicId: products/abc
    private String extractPublicId(String imageUrl) {
        String[] parts = imageUrl.split("/upload/");
        String afterUpload = parts[1];                          // v123/products/abc.jpg
        String withoutVersion = afterUpload.replaceFirst("v\\d+/", ""); // products/abc.jpg
        return withoutVersion.substring(0, withoutVersion.lastIndexOf(".")); // products/abc
    }
    // Xóa ảnh trên Cloudinary theo publicId
    public void deleteImage(String imageUrl) throws IOException {
        String publicId = extractPublicId(imageUrl);
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
    // Xóa 1 ảnh sản phẩm (xóa trên Cloudinary + xóa trong DB)
    public void deleteProductImage(Integer imageId) throws IOException {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với id: " + imageId));

        deleteImage(image.getImageUrl());
        productImageRepository.delete(image);
    }

    // Xóa tất cả ảnh của 1 sản phẩm || ko dùng vì chỉ đang soft delete tránh lỗi order cart
    public void deleteAllProductImages(Product product) throws IOException {
        List<ProductImage> images = productImageRepository.findByProductId(product.getId());
        for (ProductImage image : images) {
            deleteImage(image.getImageUrl());
        }
        productImageRepository.deleteByProduct(product);
    }

    // Copy ảnh từ URL cũ sang sản phẩm mới (chỉ lưu URL, không upload lại Cloudinary)
    public void copyImagesFromUrls(Product product, List<String> imageUrls) {
        for (String url : imageUrls) {
            ProductImage image = new ProductImage();
            image.setImageUrl(url);
            image.setProduct(product);
            productImageRepository.save(image);
        }
    }
}