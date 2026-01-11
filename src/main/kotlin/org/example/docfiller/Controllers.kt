package org.example.docfiller

import org.example.docfiller.dtos.*
import org.example.docfiller.services.PlaceHolderService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import org.example.docfiller.dtos.LoginRequest
import org.example.docfiller.dtos.LoginResponse
import org.example.docfiller.services.EmployeeService
import org.example.docfiller.services.OrganizationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/placeholder")
class PlaceHolderController(
    private val placeHolderService: PlaceHolderService
) {


    @GetMapping("/keys")
    fun getPlaceHolderKeys(@RequestParam hash: String): ResponseEntity<PlaceHolderKeysResponse> {
        val keys = placeHolderService.getPlaceHolderKeys(hash)
        return ResponseEntity.ok(PlaceHolderKeysResponse(keys))
    }


    @GetMapping("/locations")
    fun getPlaceHolderLocations(@RequestParam hash: String): ResponseEntity<List<PlaceHolderLocationResponse>> {
        val locations = placeHolderService.getPlaceHolderLocations(hash)
        return ResponseEntity.ok(locations)
    }


    @GetMapping("/statistics")
    fun getStatistics(@RequestParam hash: String): ResponseEntity<ScanStatisticsResponse> {
        val stats = placeHolderService.getStatistics(hash)
        return ResponseEntity.ok(stats)
    }


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

    }
}


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val employeeService: EmployeeService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse = employeeService.login(request)
}

@RestController
@RequestMapping("/api/organization")
class OrganizationController(
    private val service: OrganizationService
){
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    fun create(@RequestBody request: OrganizationCreate) = service.create(request)

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): OrganizationResponse = service.getOne(id)

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: OrganizationUpdate) = service.update(id, request)

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

