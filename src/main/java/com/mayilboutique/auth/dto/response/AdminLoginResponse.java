package com.mayilboutique.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long adminId;
    private String mobile;
    private String role;
}
