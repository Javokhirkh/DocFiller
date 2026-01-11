package org.example.docfiller

import jakarta.validation.Valid
import org.example.docfiller.dtos.*
import org.example.docfiller.services.AttachService
import org.example.docfiller.services.EmployeeService
import org.example.docfiller.services.OrganizationService
import org.example.docfiller.services.PlaceHolderService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/placeholder")
class PlaceHolderController(
    private val placeHolderService: PlaceHolderService
) {
    @GetMapping("/keys")
    fun getPlaceHolderKeys(@RequestParam hash: UUID) = placeHolderService.getPlaceHolderKeys(hash)

    @GetMapping("/statistics")
    fun getStatistics(@RequestParam hash: UUID) = placeHolderService.getStatistics(hash)


    @PostMapping("/create-file")
    fun createFile(@RequestBody request: CreateFileRequest) = placeHolderService.createFilledDocument(request)
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

@RestController
@RequestMapping("/api/attach")
class AttachController(
    private val attachService: AttachService
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam file: MultipartFile,
        @RequestParam userId: Long
    ) = attachService.upload(file, userId)

    @GetMapping("/download/{id}")
    fun download(@PathVariable id: Long) = attachService.getFileDto(id)
}
