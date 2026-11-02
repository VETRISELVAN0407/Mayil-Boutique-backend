package com.mayilboutique.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.admin.dto.response.DashboardSummaryResponse;
import com.mayilboutique.admin.dto.response.OrderSummaryResponse;
import com.mayilboutique.admin.service.AdminDashboardService;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;

class AdminDashboardControllerTest {

    private AdminDashboardService adminDashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminDashboardService = org.mockito.Mockito.mock(AdminDashboardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminDashboardService))
                .setControllerAdvice(new com.mayilboutique.core.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void summaryReturnsDashboardData() throws Exception {
        OrderSummaryResponse order = new OrderSummaryResponse("MB-1001", "9999999999", "Test Customer",
                new BigDecimal("1500.00"), OrderStatus.PENDING, PaymentStatus.PENDING, Instant.parse("2026-01-01T10:00:00Z"));
        DashboardSummaryResponse response = new DashboardSummaryResponse(1L, new BigDecimal("1500.00"), 3L, 12L, 2L, List.of(order));

        given(adminDashboardService.getSummary()).willReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.sales").value(1500.00))
                .andExpect(jsonPath("$.customers").value(3))
                .andExpect(jsonPath("$.products").value(12))
                .andExpect(jsonPath("$.lowStock").value(2))
                .andExpect(jsonPath("$.recentOrders[0].orderRef").value("MB-1001"));
    }
}
