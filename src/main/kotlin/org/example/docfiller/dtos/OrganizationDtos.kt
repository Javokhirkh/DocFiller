package org.example.docfiller.dtos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull

data class OrganizationCreate(
    @field:NotBlank
    @field:Size(min = 3, max = 100)
    val name: String,
    @field:Pattern(
        regexp = "^\\+998(90|91|93|94|95|97|98|99|33|50|88)\\d{7}$",
        message = "phone.number.regex"
    )
    val phoneNumber: String,
)