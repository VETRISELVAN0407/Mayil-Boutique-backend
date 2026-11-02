package com.mayilboutique.admin.dto.response;

import java.util.List;

import com.mayilboutique.order.dto.response.OrderResponse;

public record CustomerDetailResponse(CustomerSummaryResponse customer, List<OrderResponse> orders) {
}
