package org.example.docfiller.services

import org.example.docfiller.*
import org.example.docfiller.dtos.AttachUploadResponse
import org.example.docfiller.dtos.FileDto
import org.example.docfiller.security.SecurityUtils
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*

@Service
class AttachService(
    private val attachRepository: AttachRepository,
    private val employeeRepository: EmployeeRepository,
    private val placeHolderService: PlaceHolderService,
    private val securityUtil: SecurityUtils,
) {

    private val uploadRoot: Path = Paths.get("uploads")

    @Transactional
    fun upload(file: MultipartFile): AttachUploadResponse {
        val employee = employeeRepository.findByIdAndDeletedFalse(securityUtil.getCurrentUserId())
            ?: throw EmployeeNotFoundException()

        val organization = employee.organization
            ?: throw UserHasNoOrganizationException()

        if (file.isEmpty) {
            throw FileIsEmptyException()
        }

        val hash = UUID.randomUUID()

        if (attachRepository.existsByHash(hash)) {
            throw FileAlreadyExistsException()
        }

        val now = LocalDate.now()
        val orgName = organization.name.replace(" ", "_")
        val datePath = uploadRoot
            .resolve(orgName)
            .resolve(now.year.toString())
            .resolve(String.format("%02d", now.monthValue))
            .resolve(String.format("%02d", now.dayOfMonth))

        Files.createDirectories(datePath)

        val extension = file.originalFilename?.substringAfterLast('.', "docx") ?: "docx"
        val storedName = "$hash.$extension"
        val fullPath = datePath.resolve(storedName)

        Files.write(fullPath, file.bytes)

        val attach = Attach(
            originName = file.originalFilename,
            size = file.size,
            type = file.contentType,
            path = datePath.toString(),
            fullPath = fullPath.toString(),
            hash = hash,
            status = DocStatus.TEMPLATE,
            employee = employee,
            organization = organization
        )

        val savedAttach = attachRepository.save(attach)

        val placeholderCount = placeHolderService.extractAndSavePlaceHolders(savedAttach)

        return AttachUploadResponse(
            id = savedAttach.id!!,
            hash = savedAttach.hash,
            originalName = savedAttach.originName,
            placeholderCount = placeholderCount
        )
    }

    fun get(id: Long): Attach =
        attachRepository.findByIdAndDeletedFalse(id)
            ?: throw AttachNotFoundException()

    fun getByHash(hash: UUID): Attach =
        attachRepository.findByHashAndDeletedFalse(hash)
            ?: throw AttachNotFoundException()

    fun getFileDto(hash: UUID): FileDto {
        val attach = getByHash(hash)
        return FileDto(
            id = attach.id!!,
            hash = attach.hash,
            originalName = attach.originName,
            size = attach.size,
            type = attach.type,
            path = attach.path
        )
    }

    fun download(hash: UUID): Pair<Resource, Attach> {
        val attach = getByHash(hash)
        val path = Paths.get(attach.fullPath)
        if (!Files.exists(path)) {
            throw FileReadException()
        }
        val resource = UrlResource(path.toUri())
        return Pair(resource, attach)
    }
}
