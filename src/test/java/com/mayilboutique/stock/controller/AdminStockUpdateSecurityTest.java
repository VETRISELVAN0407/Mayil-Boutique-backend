package com.mayilboutique.stock.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mayilboutique.core.config.SecurityConfig;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.security.JwtTokenProvider;
import com.mayilboutique.stock.dto.AddStockRequest;
import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.service.StockUpdateService;

/** Real security filter chain: only an ADMIN token may add stock or read the history. */
@WebMvcTest(controllers = AdminStockUpdateController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-0123456789-0123456789-0123456789",
        "jwt.access-token-validity-ms=900000",
        "jwt.refresh-token-validity-ms=2592000000"
})
class AdminStockUpdateSecurityTest {

    private static final String BODY = "{\"productId\":3,\"variantId\":4,\"quantity\":10}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokens;

    @MockBean
    private StockUpdateService service;

    private String tokenFor(Role role) {
        User user = new User("9876543210", "x", role, UserStatus.ACTIVE);
        user.setId(1L);
        return "Bearer " + tokens.generateAccessToken(user);
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/stock-updates").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stock-updates")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/stock-updates").header("Authorization", tokenFor(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stock-updates").header("Authorization", tokenFor(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAddStock() throws Exception {
        given(service.addStock(any(AddStockRequest.class))).willReturn(
                new StockUpdateResponse(1L, 3L, "Kurta", 4L, "SKU-M", "M", "Red", 10, 25, Instant.now()));

        mockMvc.perform(post("/api/admin/stock-updates").header("Authorization", tokenFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }
}
