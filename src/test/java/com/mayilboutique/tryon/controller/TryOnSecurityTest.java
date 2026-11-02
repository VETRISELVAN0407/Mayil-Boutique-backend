package com.mayilboutique.tryon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mayilboutique.core.config.SecurityConfig;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.security.JwtTokenProvider;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.dto.TryOnJobResponse;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.service.AdminTryOnStatsService;
import com.mayilboutique.tryon.service.TryOnConfigService;
import com.mayilboutique.tryon.service.TryOnJobService;

/** Real security filter chain: jobs are customer-only and always scoped to the token's user; the product list is public. */
@WebMvcTest(controllers = {CustomerTryOnController.class, PublicTryOnController.class, AdminTryOnConfigController.class,
        AdminTryOnStatsController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-0123456789-0123456789-0123456789",
        "jwt.access-token-validity-ms=900000",
        "jwt.refresh-token-validity-ms=2592000000"
})
class TryOnSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokens;

    @MockBean
    private TryOnJobService jobService;

    @MockBean
    private TryOnConfigService configService;

    @MockBean
    private TryOnCapabilities capabilities;

    @MockBean
    private AdminTryOnStatsService statsService;

    private String tokenFor(Role role, long userId) {
        User user = new User("9876543210", "x", role, UserStatus.ACTIVE);
        user.setId(userId);
        return "Bearer " + tokens.generateAccessToken(user);
    }

    private static MockMultipartFile photo() {
        return new MockMultipartFile("photo", "me.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void anonymousCannotUseJobs() throws Exception {
        mockMvc.perform(multipart("/api/customer/try-on/jobs").file(photo()).param("productId", "7").param("consent", "true"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/try-on/jobs/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/try-on/jobs/1/result")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/customer/try-on/jobs/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotUseCustomerJobs() throws Exception {
        mockMvc.perform(get("/api/customer/try-on/jobs/1").header("Authorization", tokenFor(Role.ADMIN, 9)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCreatesAJobAsThemselves() throws Exception {
        given(jobService.create(eq(42L), eq(7L), any(), eq(true))).willReturn(new TryOnJobResponse(
                5L, 7L, "Kurti", JobStatus.PENDING, null, false, Instant.now(), null, Instant.now()));

        mockMvc.perform(multipart("/api/customer/try-on/jobs").file(photo()).param("productId", "7").param("consent", "true")
                        .header("Authorization", tokenFor(Role.CUSTOMER, 42)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.originalPhotoPath").doesNotExist());
    }

    @Test
    void lookupsAndDeleteUseTheTokensUserId() throws Exception {
        given(jobService.get(42L, 5L)).willReturn(new TryOnJobResponse(
                5L, 7L, "Kurti", JobStatus.PROCESSING, null, false, Instant.now(), null, Instant.now()));

        mockMvc.perform(get("/api/customer/try-on/jobs/5").header("Authorization", tokenFor(Role.CUSTOMER, 42)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        mockMvc.perform(delete("/api/customer/try-on/jobs/5").header("Authorization", tokenFor(Role.CUSTOMER, 42)))
                .andExpect(status().isNoContent());
        verify(jobService).delete(42L, 5L);
    }

    @Test
    void productListIsPublic() throws Exception {
        given(configService.listEligible(any())).willReturn(List.of());
        mockMvc.perform(get("/api/products/try-on")).andExpect(status().isOk());
    }

    @Test
    void statusIsPublicAndReflectsAvailability() throws Exception {
        given(capabilities.available()).willReturn(false);
        mockMvc.perform(get("/api/products/try-on/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
        given(capabilities.available()).willReturn(true);
        mockMvc.perform(get("/api/products/try-on/status")).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void statsAreAdminOnly() throws Exception {
        mockMvc.perform(get("/api/admin/try-on/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/try-on/stats").header("Authorization", tokenFor(Role.CUSTOMER, 42)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tryOnSettingsAreAdminOnly() throws Exception {
        mockMvc.perform(get("/api/admin/products/1/try-on")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/products/1/try-on").header("Authorization", tokenFor(Role.CUSTOMER, 42)))
                .andExpect(status().isForbidden());
    }
}
