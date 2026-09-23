package org.morah.morah.login.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /auth/refresh. */
public record RefreshRequest(@NotBlank(message = "informe o refreshToken") String refreshToken) {
}
