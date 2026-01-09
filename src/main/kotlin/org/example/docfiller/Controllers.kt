package org.example.docfiller

import org.example.docfiller.dtos.*
import org.example.docfiller.services.PlaceHolderService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/placeholder")
class PlaceHolderController(
    private val placeHolderService: PlaceHolderService
) {

    /**
     * Template hashini yuborib, ichidagi barcha placeholder keylarni olish
     * GET /api/placeholder/keys?hash=abc123
     */
    @GetMapping("/keys")
    fun getPlaceHolderKeys(@RequestParam hash: String): ResponseEntity<PlaceHolderKeysResponse> {
        val keys = placeHolderService.getPlaceHolderKeys(hash)
        return ResponseEntity.ok(PlaceHolderKeysResponse(keys))
    }

    /**
     * Template hashini yuborib, placeholder locationlarini olish (debug/info uchun)
     * GET /api/placeholder/locations?hash=abc123
     */
    @GetMapping("/locations")
    fun getPlaceHolderLocations(@RequestParam hash: String): ResponseEntity<List<PlaceHolderLocationResponse>> {
        val locations = placeHolderService.getPlaceHolderLocations(hash)
        return ResponseEntity.ok(locations)
    }

    /**
     * Template statistikasini olish
     * GET /api/placeholder/statistics?hash=abc123
     */
    @GetMapping("/statistics")
    fun getStatistics(@RequestParam hash: String): ResponseEntity<ScanStatisticsResponse> {
        val stats = placeHolderService.getStatistics(hash)
        return ResponseEntity.ok(stats)
    }

    /**
     * Placeholderlarni to'ldirib file yaratish
     * POST /api/placeholder/create-file
     *
     * Request body:
     * {
     *   "attachHash": "abc123",
     *   "fileName": "filled_document",
     *   "fileType": "DOCX",
     *   "placeholders": {
     *     "#company_name": "IT Solutions",
     *     "#date": "2026-01-10"
     *   }
     * }
     */
    @PostMapping("/create-file")
    fun createFile(@RequestBody request: CreateFileRequest): ResponseEntity<ByteArray> {
        val fileBytes = placeHolderService.createFilledDocument(request)

        val contentType = when (request.fileType) {
            FileType.PDF -> MediaType.APPLICATION_PDF
            FileType.DOCX -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        }

        val extension = when (request.fileType) {
            FileType.PDF -> "pdf"
            FileType.DOCX -> "docx"
        }

        val fileName = "${request.fileName}.$extension"

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .body(fileBytes)

        // TODO: FileService.save() ni chaqirib file saqlash kerak
        // Bu boshqa jamoa a'zosi tomonidan yoziladi
        // Misol:
        // val savedFile = fileService.save(fileBytes, fileName, contentType)
        // return ResponseEntity.ok(CreatedFileResponse(
        //     fileName = savedFile.name,
        //     fileType = request.fileType,
        //     message = "File created successfully"
        // ))
    }
}
