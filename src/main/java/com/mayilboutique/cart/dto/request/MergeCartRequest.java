package com.mayilboutique.cart.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class MergeCartRequest {

    @NotNull
    private List<MergeCartItem> items;

    public List<MergeCartItem> getItems() {
        return items;
    }

    public void setItems(List<MergeCartItem> items) {
        this.items = items;
    }

    public static class MergeCartItem {

        @NotNull
        private Long variantId;

        @NotNull
        @jakarta.validation.constraints.Min(1)
        private Integer quantity;

        public Long getVariantId() {
            return variantId;
        }

        public void setVariantId(Long variantId) {
            this.variantId = variantId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
