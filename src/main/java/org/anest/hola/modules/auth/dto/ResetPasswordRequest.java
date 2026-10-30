package org.anest.hola.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 150, message = "{validation.email.max_length}")
    private String email;

    @NotBlank(message = "{validation.otp.required}")
    @Pattern(regexp = "^\\d{6}$", message = "{validation.otp.invalid}")
    private String otp;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 72, message = "{validation.password.size}")
    private String newPassword;
}

