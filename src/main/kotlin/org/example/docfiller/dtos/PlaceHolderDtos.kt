package org.example.docfiller.dtos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.docfiller.FileType
import java.util.UUID


data class CreateFileRequest(

    @field:NotNull(message = "{createFile.attachHash.notNull}")
    val attachHash: UUID,

    @field:NotBlank(message = "{createFile.fileName.notBlank}")
    @field:Size(
        max = 255,
        message = "{createFile.fileName.size}"
    )
    val fileName: String,

    @field:NotNull(message = "{createFile.fileType.notNull}")
    val fileType: FileType,

    @field:NotEmpty(message = "{createFile.placeholders.notEmpty}")
    val placeholders: Map<
            @NotBlank(message = "{createFile.placeholders.key.notBlank}")
            String,
            @NotBlank(message = "{createFile.placeholders.value.notBlank}")
            String
            >
)

data class CreatedFileResponse(
    val file: FileDto,
    val fileName: String,
    val fileType: FileType
)

data class ScanStatisticsResponse(
    val totalPlaceholders: Int,
    val uniqueKeys: Int,
    val inHeaders: Int,
    val inParagraphs: Int,
    val inTables: Int,
    val inFooters: Int
)
