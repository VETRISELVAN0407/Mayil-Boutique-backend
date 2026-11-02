package com.mayilboutique.stock.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;
import com.mayilboutique.stock.dto.AddStockRequest;
import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.service.StockUpdateService;

class AdminStockUpdateControllerTest {

    private StockUpdateService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(StockUpdateService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminStockUpdateController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions add(String body) throws Exception {
        return mockMvc.perform(post("/api/admin/stock-updates").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void addStockReturnsCreatedHistoryRow() throws Exception {
        given(service.addStock(any(AddStockRequest.class))).willReturn(
                new StockUpdateResponse(1L, 3L, "Kurta", 4L, "SKU-M", "M", "Red", 10, 25, Instant.now()));

        add("{\"productId\":3,\"variantId\":4,\"quantity\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockAdded").value(10))
                .andExpect(jsonPath("$.totalStock").value(25));
    }

    @Test
    void invalidQuantitiesAreRejectedBeforeReachingTheService() throws Exception {
        for (String qty : new String[] {"0", "-5", "2.5", "100001", "null", "\"abc\""}) {
            add("{\"productId\":3,\"variantId\":4,\"quantity\":" + qty + "}").andExpect(status().isBadRequest());
        }
        add("{\"productId\":3,\"variantId\":4}").andExpect(status().isBadRequest());
        add("{\"variantId\":4,\"quantity\":5}").andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mismatchedVariantIsReportedAsBadRequest() throws Exception {
        given(service.addStock(any(AddStockRequest.class)))
                .willThrow(new BusinessException(HttpStatus.BAD_REQUEST, "The variant does not belong to the selected product"));

        add("{\"productId\":3,\"variantId\":4,\"quantity\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The variant does not belong to the selected product"));
    }

    @Test
    void historyIsReturnedAsAPage() throws Exception {
        given(service.history(null, 0, 20)).willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/stock-updates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
