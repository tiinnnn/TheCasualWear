package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.Wishlist;
import com.datn.TheCasualWear.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final AppUserService     appUserService;
    private final ProductService     productService;

    public List<Wishlist> getWishlist(String username) {
        AppUser user = appUserService.getUserByUsername(username);
        return wishlistRepository.findByUserId(user.getId());
    }

    @Transactional
    public boolean toggle(String username, Integer productId) {
        AppUser user    = appUserService.getUserByUsername(username);
        Product product = productService.getProductById(productId);

        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
            return false;
        } else {
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
            return true;
        }
    }

    public boolean isWishlisted(String username, Integer productId) {
        AppUser user = appUserService.getUserByUsername(username);
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    @Transactional
    public void remove(String username, Integer productId) {
        AppUser user = appUserService.getUserByUsername(username);
        if (!wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ResourceNotFoundException("Sản phẩm không có trong wishlist!");
        }
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public long countByProduct(Integer productId) {
        return wishlistRepository.countByProductId(productId);
    }
}