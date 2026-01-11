package org.example.docfiller.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val role: String? = null,
)

data class LoginRequest(
    @field:NotBlank
    val phoneNumber: String,
    @field:NotBlank
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "phone.number.regex"
    )
    val password: String
)

data class RegisterRequest(
    @field:NotBlank
    @field:Size(min = 4, max = 50)
    val firstName: String,
    @field:Size(min = 1, max = 50)
    val lastName: String? = null,
    @field:NotBlank
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "phone.number.regex"
    )
    val phoneNumber: String,
    @field:NotBlank
    @field:Size(min = 6, max = 50)
    val password: String,
    @field:NotNull
    @field:Min(value = 1)
    var organizationId: Long
)