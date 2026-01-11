package org.example.docfiller.dtos

import org.example.docfiller.FileType
import java.util.UUID

data class GetPlaceHolderKeysRequest(
    val attachHash: UUID
)

data class PlaceHolderKeysResponse(
    val keys: List<String>
)

data class CreateFileRequest(
    val userId: Long,
    val attachHash: UUID,
    val fileName: String,
    val fileType: FileType,
    val placeholders: Map<String, String>
)

data class AttachUploadResponse(
    val id: Long,
    val hash: UUID,
    val originalName: String?,
    val placeholderCount: Int
)

data class PlaceHolderLocationResponse(
    val id: Long?,
    val key: String,
    val locationType: String,
    val headerIndex: Int?,
    val paragraphIndex: Int?,
    val tableIndex: Int?,
    val rowIndex: Int?,
    val columnIndex: Int?,
    val footerIndex: Int?,
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
