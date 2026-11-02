package com.mayilboutique.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    // bcrypt only uses the first 72 bytes, so longer passwords would silently be truncated.
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "New password must be 8 to 72 characters")
    private String newPassword;
}
