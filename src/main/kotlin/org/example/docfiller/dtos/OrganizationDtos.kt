package org.example.docfiller.dtos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime


data class OrganizationCreate(

    @field:NotBlank(message = "{organization.name.notBlank}")
    @field:Size(min = 3, max = 100, message = "{organization.name.size}")
    val name: String,

    @field:NotBlank(message = "{organization.phoneNumber.notBlank}")
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "{phone.number.regex}"
    )
    val phoneNumber: String
)

data class OrganizationResponse(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val createDate: LocalDateTime?,
    val deleted: Boolean,
)

data class OrganizationUpdate(
    val name: String? = null,
    val phoneNumber: String? = null,
)