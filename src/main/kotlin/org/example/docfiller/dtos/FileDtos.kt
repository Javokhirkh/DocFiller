package org.example.docfiller.dtos

import java.util.UUID

data class FileDto(
    val id: Long,
    val hash: UUID,
    val originalName: String?,
    val size: Long,
    val type: String?,
    val path: String
)
