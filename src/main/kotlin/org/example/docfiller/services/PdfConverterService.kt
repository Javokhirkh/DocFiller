package org.example.docfiller.services

import com.documents4j.api.DocumentType
import com.documents4j.api.IConverter
import com.documents4j.job.LocalConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import jakarta.annotation.PreDestroy

interface PdfConverterService {
    fun convertDocxToPdf(docxBytes: ByteArray): ByteArray
    fun convertDocxToPdf(docxFile: File): ByteArray
    fun isAvailable(): Boolean
}

@Service
class PdfConverterServiceImpl : PdfConverterService {

    private val logger = LoggerFactory.getLogger(PdfConverterServiceImpl::class.java)

    @Volatile
    private var converterInitialized = false

    private val converter: IConverter by lazy {
        converterInitialized = true
        LocalConverter.builder()
            .baseFolder(File(System.getProperty("java.io.tmpdir")))
            .workerPool(20, 25, 2, TimeUnit.SECONDS)
            .processTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override fun convertDocxToPdf(docxBytes: ByteArray): ByteArray {
        logger.info("Converting DOCX to PDF (${docxBytes.size} bytes)")

        ByteArrayInputStream(docxBytes).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val success = converter.convert(inputStream)
                    .`as`(DocumentType.MS_WORD)
                    .to(outputStream)
                    .`as`(DocumentType.PDF)
                    .execute()

                if (!success) {
                    logger.error("DOCX to PDF conversion failed")
                    throw PdfConversionException("DOCX to PDF conversion failed")
                }

                logger.info("Conversion successful, PDF size: ${outputStream.size()} bytes")
                return outputStream.toByteArray()
            }
        }
    }

    override fun convertDocxToPdf(docxFile: File): ByteArray {
        logger.info("Converting DOCX file to PDF: ${docxFile.absolutePath}")

        if (!docxFile.exists()) {
            throw PdfConversionException("DOCX file not found: ${docxFile.absolutePath}")
        }

        ByteArrayOutputStream().use { outputStream ->
            val success = converter.convert(docxFile)
                .`as`(DocumentType.MS_WORD)
                .to(outputStream)
                .`as`(DocumentType.PDF)
                .execute()

            if (!success) {
                logger.error("DOCX to PDF conversion failed for file: ${docxFile.absolutePath}")
                throw PdfConversionException("DOCX to PDF conversion failed")
            }

            logger.info("Conversion successful, PDF size: ${outputStream.size()} bytes")
            return outputStream.toByteArray()
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            converter.isOperational
        } catch (e: Exception) {
            logger.warn("PDF converter is not available: ${e.message}")
            false
        }
    }

    @PreDestroy
    fun shutdown() {
        if (converterInitialized) {
            try {
                logger.info("Shutting down PDF converter")
                converter.shutDown()
            } catch (e: Exception) {
                logger.warn("Error during PDF converter shutdown: ${e.message}")
            }
        }
    }
}

class PdfConversionException(message: String) : RuntimeException(message)
