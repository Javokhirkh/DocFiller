package org.example.docfiller.dtos

import org.example.docfiller.FileType

data class GetPlaceHolderKeysRequest(
    val attachHash: String
)

data class PlaceHolderKeysResponse(
    val keys: List<String>
)

data class CreateFileRequest(
    val attachHash: String,
    val fileName: String,
    val fileType: FileType,
    val placeholders: Map<String, String>
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
    val fileName: String,
    val fileType: FileType,
    val message: String
    // TODO: FileService dan qaytadigan ma'lumotlar qo'shiladi
)

data class ScanStatisticsResponse(
    val totalPlaceholders: Int,
    val uniqueKeys: Int,
    val inHeaders: Int,
    val inParagraphs: Int,
    val inTables: Int,
    val inFooters: Int
)
