package org.example.docfiller

import jakarta.validation.Valid
import org.example.docfiller.dtos.*
import org.example.docfiller.services.AttachService
import org.example.docfiller.services.EmployeeService
import org.example.docfiller.services.OrganizationService
import org.example.docfiller.services.PlaceHolderService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/placeholder")
class PlaceHolderController(
    private val placeHolderService: PlaceHolderService
) {
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/keys")
    fun getPlaceHolderKeys(@RequestParam hash: UUID) = placeHolderService.getPlaceHolderKeys(hash)

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/statistics")
    fun getStatistics(@RequestParam hash: UUID) = placeHolderService.getStatistics(hash)


    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest) = employeeService.register(request)
}

@RestController
@RequestMapping("/api/organizations")
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
    @GetMapping("")
    fun getAll(): List<OrganizationResponse> = service.getAll()

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

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam file: MultipartFile,
        @RequestParam userId: Long
    ) = attachService.upload(file, userId)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/download/{hash}")
    fun download(@PathVariable hash: UUID): ResponseEntity<Resource> {
        val (resource, attach) = attachService.download(hash)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${attach.originName}\"")
            .header(HttpHeaders.CONTENT_TYPE, attach.type)
            .body(resource)
    }
}

@RestController
@RequestMapping("/api/employees")
class EmployeeController(
    private val service: EmployeeService
){
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    fun create(@RequestBody request: EmployeeCreate) = service.create(request)

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): EmployeeResponse = service.getOne(id)

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("")
    fun getAll(): List<EmployeeResponse> = service.getAll()

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: EmployeeUpdate) = service.update(id, request)

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
