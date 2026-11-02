package com.mayilboutique.wishlist.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class MergeWishlistRequest {

    @NotNull
    private List<Long> productIds;

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
}
