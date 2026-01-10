package org.example.docfiller.services

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.example.docfiller.*
import org.example.docfiller.dtos.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

interface PlaceHolderService {
    fun extractAndSavePlaceHolders(attach: Attach): Int
    fun extractAndSavePlaceHolders(file: File, attach: Attach): Int
    fun extractAndSavePlaceHolders(fileBytes: ByteArray, attach: Attach): Int
    fun getPlaceHolderKeys(attachHash: String): List<String>
    fun getPlaceHolderLocations(attachHash: String): List<PlaceHolderLocationResponse>
    fun getStatistics(attachHash: String): ScanStatisticsResponse
    fun createFilledDocument(request: CreateFileRequest): ByteArray
}

@Service
class PlaceHolderServiceImpl(
    private val placeHolderRepository: PlaceHolderRepository,
    private val attachRepository: AttachRepository,
    private val pdfConverterService: PdfConverterService
) : PlaceHolderService {

    private val placeholderRegex = Regex("#\\w+")

    @Transactional
    override fun extractAndSavePlaceHolders(attach: Attach): Int {
        val file = File(attach.fullPath)
        if (!file.exists()) {
            throw FileReadException()
        }
        return extractAndSavePlaceHolders(file, attach)
    }

    @Transactional
    override fun extractAndSavePlaceHolders(file: File, attach: Attach): Int {
        placeHolderRepository.deleteAllByAttachHash(attach.hash)

        var savedCount = 0

        FileInputStream(file).use { fis ->
            val document = XWPFDocument(fis)
            savedCount = scanAndSaveFromDocument(document, attach)
            document.close()
        }

        return savedCount
    }

    @Transactional
    override fun extractAndSavePlaceHolders(fileBytes: ByteArray, attach: Attach): Int {
        placeHolderRepository.deleteAllByAttachHash(attach.hash)

        var savedCount = 0

        ByteArrayInputStream(fileBytes).use { bis ->
            val document = XWPFDocument(bis)
            savedCount = scanAndSaveFromDocument(document, attach)
            document.close()
        }

        return savedCount
    }

    private fun scanAndSaveFromDocument(document: XWPFDocument, attach: Attach): Int {
        val placeholders = mutableListOf<PlaceHolder>()

        // 1. Scan Headers
        document.headerList.forEachIndexed { headerIndex, header ->
            header.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                val text = paragraph.text
                placeholderRegex.findAll(text).forEach { match ->
                    placeholders.add(
                        PlaceHolder(
                            attach = attach,
                            key = match.value,
                            locationType = "header",
                            headerIndex = headerIndex,
                            paragraphIndex = paragraphIndex
                        )
                    )
                }
            }
        }

        // 2. Scan Paragraphs
        document.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
            val text = paragraph.text
            placeholderRegex.findAll(text).forEach { match ->
                placeholders.add(
                    PlaceHolder(
                        attach = attach,
                        key = match.value,
                        locationType = "paragraph",
                        paragraphIndex = paragraphIndex
                    )
                )
            }
        }

        // 3. Scan Tables
        document.tables.forEachIndexed { tableIndex, table ->
            table.rows.forEachIndexed { rowIndex, row ->
                row.tableCells.forEachIndexed { columnIndex, cell ->
                    cell.paragraphs.forEach { paragraph ->
                        val text = paragraph.text
                        placeholderRegex.findAll(text).forEach { match ->
                            placeholders.add(
                                PlaceHolder(
                                    attach = attach,
                                    key = match.value,
                                    locationType = "table",
                                    tableIndex = tableIndex,
                                    rowIndex = rowIndex,
                                    columnIndex = columnIndex
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Scan Footers
        document.footerList.forEachIndexed { footerIndex, footer ->
            footer.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                val text = paragraph.text
                placeholderRegex.findAll(text).forEach { match ->
                    placeholders.add(
                        PlaceHolder(
                            attach = attach,
                            key = match.value,
                            locationType = "footer",
                            footerIndex = footerIndex,
                            paragraphIndex = paragraphIndex
                        )
                    )
                }
            }
        }

        placeHolderRepository.saveAll(placeholders)
        return placeholders.size
    }

    override fun getPlaceHolderKeys(attachHash: String): List<String> {
        val placeholders = placeHolderRepository.findAllByAttachHashAndDeletedFalse(attachHash)
        if (placeholders.isEmpty()) {
            val attach = attachRepository.findByHashAndDeletedFalse(attachHash)
                ?: throw AttachNotFoundException()
            extractAndSavePlaceHolders(attach)
            return placeHolderRepository.findAllByAttachHashAndDeletedFalse(attachHash)
                .map { it.key }
                .distinct()
                .sorted()
        }
        return placeholders.map { it.key }.distinct().sorted()
    }

    override fun getPlaceHolderLocations(attachHash: String): List<PlaceHolderLocationResponse> {
        val placeholders = placeHolderRepository.findAllByAttachHashAndDeletedFalse(attachHash)
        return placeholders.map { ph ->
            PlaceHolderLocationResponse(
                id = ph.id,
                key = ph.key,
                locationType = ph.locationType,
                headerIndex = ph.headerIndex,
                paragraphIndex = ph.paragraphIndex,
                tableIndex = ph.tableIndex,
                rowIndex = ph.rowIndex,
                columnIndex = ph.columnIndex,
                footerIndex = ph.footerIndex
            )
        }
    }

    override fun getStatistics(attachHash: String): ScanStatisticsResponse {
        val placeholders = placeHolderRepository.findAllByAttachHashAndDeletedFalse(attachHash)

        return ScanStatisticsResponse(
            totalPlaceholders = placeholders.size,
            uniqueKeys = placeholders.map { it.key }.distinct().size,
            inHeaders = placeholders.count { it.locationType == "header" },
            inParagraphs = placeholders.count { it.locationType == "paragraph" },
            inTables = placeholders.count { it.locationType == "table" },
            inFooters = placeholders.count { it.locationType == "footer" }
        )
    }

    override fun createFilledDocument(request: CreateFileRequest): ByteArray {
        val attach = attachRepository.findByHashAndDeletedFalse(request.attachHash)
            ?: throw AttachNotFoundException()

        val locations = placeHolderRepository.findAllByAttachHashAndDeletedFalse(request.attachHash)
        if (locations.isEmpty()) {
            throw PlaceHolderNotFoundException()
        }

        val file = File(attach.fullPath)
        if (!file.exists()) {
            throw FileReadException()
        }

        val outputBytes: ByteArray

        FileInputStream(file).use { fis ->
            val document = XWPFDocument(fis)

            // Group locations by type
            val grouped = locations.groupBy { it.locationType }

            // Replace in Headers
            grouped["header"]?.forEach { loc ->
                val header = document.headerList.getOrNull(loc.headerIndex ?: -1)
                val paragraph = header?.paragraphs?.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

            // Replace in Paragraphs
            grouped["paragraph"]?.forEach { loc ->
                val paragraph = document.paragraphs.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

            // Replace in Tables
            grouped["table"]?.forEach { loc ->
                val table = document.tables.getOrNull(loc.tableIndex ?: -1)
                val row = table?.rows?.getOrNull(loc.rowIndex ?: -1)
                val cell = row?.tableCells?.getOrNull(loc.columnIndex ?: -1)
                cell?.paragraphs?.forEach { paragraph ->
                    if (request.placeholders.containsKey(loc.key)) {
                        replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                    }
                }
            }

            // Replace in Footers
            grouped["footer"]?.forEach { loc ->
                val footer = document.footerList.getOrNull(loc.footerIndex ?: -1)
                val paragraph = footer?.paragraphs?.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

            // Write to ByteArray
            ByteArrayOutputStream().use { baos ->
                document.write(baos)
                outputBytes = baos.toByteArray()
            }

            document.close()
        }

        // Agar PDF kerak bo'lsa, convert qilish
        if (request.fileType == FileType.PDF) {
            return pdfConverterService.convertDocxToPdf(outputBytes)
        }

        return outputBytes
    }

    private fun replaceInParagraph(paragraph: XWPFParagraph, placeholder: String, value: String) {
        val originalText = paragraph.text

        if (!originalText.contains(placeholder)) {
            return
        }

        val modifiedText = originalText.replace(placeholder, value)

        if (paragraph.runs.isNotEmpty()) {
            val firstRun = paragraph.runs[0]

            // Remove other runs
            for (i in paragraph.runs.size - 1 downTo 1) {
                paragraph.removeRun(i)
            }

            firstRun.setText(modifiedText, 0)
        } else {
            val run = paragraph.createRun()
            run.setText(modifiedText, 0)
        }
    }
}
