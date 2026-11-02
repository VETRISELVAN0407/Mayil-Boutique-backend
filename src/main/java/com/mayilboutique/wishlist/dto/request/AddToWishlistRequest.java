package com.mayilboutique.wishlist.dto.request;

import jakarta.validation.constraints.NotNull;

public class AddToWishlistRequest {

    @NotNull
    private Long productId;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
