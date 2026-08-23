package com.guvi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    private String name;

    private String phone;

    /** Optional. When blank the existing password is kept. */
    @Size(min = 6, message = "must be at least 6 characters")
    private String newPassword;

    /** Required only when newPassword is supplied. */
    private String currentPassword;
}
