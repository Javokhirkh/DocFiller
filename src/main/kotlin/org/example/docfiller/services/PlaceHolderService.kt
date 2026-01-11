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
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

interface PlaceHolderService {
    fun extractAndSavePlaceHolders(attach: Attach): Int
    fun extractAndSavePlaceHolders(file: File, attach: Attach): Int
    fun extractAndSavePlaceHolders(fileBytes: ByteArray, attach: Attach): Int
    fun getPlaceHolderKeys(attachHash: UUID): List<String>
    fun getStatistics(attachHash: UUID): ScanStatisticsResponse
    fun createFilledDocument(request: CreateFileRequest): CreatedFileResponse
}

@Service
class PlaceHolderServiceImpl(
    private val placeHolderRepository: PlaceHolderRepository,
    private val attachRepository: AttachRepository,
    private val employeeRepository: EmployeeRepository,
    private val pdfConverterService: PdfConverterService
) : PlaceHolderService {

    private val placeholderRegex = Regex("#\\w+")
    private val uploadRoot = Paths.get("uploads")

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

    override fun getPlaceHolderKeys(attachHash: UUID): List<String> {
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

    override fun getStatistics(attachHash: UUID): ScanStatisticsResponse {
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

    @Transactional
    override fun createFilledDocument(request: CreateFileRequest): CreatedFileResponse {
        val employee = employeeRepository.findByIdAndDeletedFalse(request.userId)
            ?: throw EmployeeNotFoundException()

        val organization = employee.organization
            ?: throw UserHasNoOrganizationException()

        val templateAttach = attachRepository.findByHashAndDeletedFalse(request.attachHash)
            ?: throw AttachNotFoundException()

        val locations = placeHolderRepository.findAllByAttachHashAndDeletedFalse(request.attachHash)
        if (locations.isEmpty()) {
            throw PlaceHolderNotFoundException()
        }

        val templateKeys = locations.map { it.key }.toSet()
        val requestKeys = request.placeholders.keys

        val missingKeys = templateKeys - requestKeys
        if (missingKeys.isNotEmpty()) {
            throw MissingPlaceholderValueException(missingKeys)
        }

        val unknownKeys = requestKeys - templateKeys
        if (unknownKeys.isNotEmpty()) {
            throw UnknownPlaceholderKeyException(unknownKeys)
        }

        val emptyKeys = request.placeholders.filter { it.value.isBlank() }.keys
        if (emptyKeys.isNotEmpty()) {
            throw EmptyPlaceholderValueException(emptyKeys)
        }

        val file = File(templateAttach.fullPath)
        if (!file.exists()) {
            throw FileReadException()
        }

        var outputBytes: ByteArray

        FileInputStream(file).use { fis ->
            val document = XWPFDocument(fis)

            val grouped = locations.groupBy { it.locationType }

            grouped["header"]?.forEach { loc ->
                val header = document.headerList.getOrNull(loc.headerIndex ?: -1)
                val paragraph = header?.paragraphs?.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

            grouped["paragraph"]?.forEach { loc ->
                val paragraph = document.paragraphs.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

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

            grouped["footer"]?.forEach { loc ->
                val footer = document.footerList.getOrNull(loc.footerIndex ?: -1)
                val paragraph = footer?.paragraphs?.getOrNull(loc.paragraphIndex ?: -1)
                if (paragraph != null && request.placeholders.containsKey(loc.key)) {
                    replaceInParagraph(paragraph, loc.key, request.placeholders[loc.key]!!)
                }
            }

            ByteArrayOutputStream().use { baos ->
                document.write(baos)
                outputBytes = baos.toByteArray()
            }

            document.close()
        }

        val (extension, contentType) = when (request.fileType) {
            FileType.PDF -> {
                outputBytes = pdfConverterService.convertDocxToPdf(outputBytes)
                "pdf" to "application/pdf"
            }
            FileType.DOCX -> "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        }

        val hash = UUID.randomUUID()
        val now = LocalDate.now()
        val orgName = organization.name.replace(" ", "_")
        val datePath = uploadRoot
            .resolve(orgName)
            .resolve(now.year.toString())
            .resolve(String.format("%02d", now.monthValue))
            .resolve(String.format("%02d", now.dayOfMonth))

        Files.createDirectories(datePath)

        val storedName = "$hash.$extension"
        val fullPath = datePath.resolve(storedName)
        Files.write(fullPath, outputBytes)

        val fileName = "${request.fileName}.$extension"

        val savedAttach = attachRepository.save(
            Attach(
                originName = fileName,
                size = outputBytes.size.toLong(),
                type = contentType,
                path = datePath.toString(),
                fullPath = fullPath.toString(),
                hash = hash,
                status = DocStatus.READY,
                employee = employee,
                organization = organization
            )
        )

        val fileDto = FileDto(
            id = savedAttach.id!!,
            hash = savedAttach.hash,
            originalName = savedAttach.originName,
            size = savedAttach.size,
            type = savedAttach.type,
            path = savedAttach.path
        )

        return CreatedFileResponse(
            file = fileDto,
            fileName = fileName,
            fileType = request.fileType
        )
    }

    private fun replaceInParagraph(paragraph: XWPFParagraph, placeholder: String, value: String) {
        val originalText = paragraph.text

        if (!originalText.contains(placeholder)) {
            return
        }

        val formattedValue = formatValue(placeholder, value)
        val modifiedText = originalText.replace(placeholder, formattedValue)

        if (paragraph.runs.isNotEmpty()) {
            val firstRun = paragraph.runs[0]

            for (i in paragraph.runs.size - 1 downTo 1) {
                paragraph.removeRun(i)
            }

            firstRun.setText(modifiedText, 0)
        } else {
            val run = paragraph.createRun()
            run.setText(modifiedText, 0)
        }
    }

    private fun formatValue(placeholder: String, value: String): String {
        val key = placeholder.removePrefix("#").lowercase()

        if (key.contains("date") || key.contains("sana")) {
            return formatDate(value)
        }

        return value
    }

    private fun formatDate(value: String): String {
        val inputPatterns = listOf(
            "dd-MM-yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "dd.MM.yyyy",
            "dd-MM-yy", "yy-MM-dd", "dd/MM/yy", "dd.MM.yy",
            "d-M-yyyy", "d-M-yy", "d/M/yyyy", "d/M/yy"
        )
        val outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'yil'", Locale("uz"))

        for (pattern in inputPatterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern)
                val date = LocalDate.parse(value, formatter)
                return date.format(outputFormatter)
            } catch (e: Exception) {
                continue
            }
        }

        return value
    }
}
