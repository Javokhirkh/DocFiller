package org.example.docfiller.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
)

data class LoginRequest(
    @field:NotBlank
    val phoneNumber: String,
    @field:NotBlank
    val password: String
)