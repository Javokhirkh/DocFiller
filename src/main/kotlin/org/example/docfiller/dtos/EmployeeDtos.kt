package org.example.docfiller.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.example.docfiller.UserRole

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val role: String? = null,
)

data class LoginRequest(
    @field:NotBlank(message = "{login.phoneNumber.notBlank}")
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "{phone.number.regex}"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "{login.password.notBlank}")
    @field:Size(min = 6, max = 50, message = "{login.password.size}")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "{register.firstName.notBlank}")
    @field:Size(min = 4, max = 50, message = "{register.firstName.size}")
    val firstName: String,

    @field:Size(min = 1, max = 50, message = "{register.lastName.size}")
    val lastName: String? = null,

    @field:NotBlank(message = "{register.phoneNumber.notBlank}")
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "{phone.number.regex}"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "{register.password.notBlank}")
    @field:Size(min = 6, max = 50, message = "{register.password.size}")
    val password: String,

    @field:NotNull(message = "{register.organizationId.notNull}")
    @field:Min(value = 1, message = "{register.organizationId.min}")
    var organizationId: Long
)

data class EmployeeCreate(
    @field:NotBlank(message = "{employee.firstName.notBlank}")
    @field:Size(min = 4, max = 50, message = "{employee.firstName.size}")
    val firstName: String,

    @field:Size(min = 1, max = 50, message = "{employee.lastName.size}")
    val lastName: String? = null,

    @field:NotBlank(message = "{employee.phoneNumber.notBlank}")
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "{phone.number.regex}"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "{employee.password.notBlank}")
    @field:Size(min = 6, max = 50, message = "{employee.password.size}")
    val password: String,

    @field:NotBlank(message = "{employee.role.notBlank}")
    val role: UserRole,

    @field:Min(value = 1, message = "{employee.organizationId.min}")
    var organizationId: Long? = null
)

data class EmployeeResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val organizationId: Long,
    val deleted: Boolean,
)

data class EmployeeUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val organizationId: Long? = null,
)