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

interface AttachService{
    fun upload(file: MultipartFile): AttachUploadResponse
    fun get(id: Long): Attach
    fun getByHash(hash: UUID): Attach
    fun getAllUserList(): List<FileDto>
    fun download(hash: UUID): Pair<Resource, Attach>
}
@Service
class AttachServiceImpl(
    private val attachRepository: AttachRepository,
    private val employeeRepository: EmployeeRepository,
    private val placeHolderService: PlaceHolderService,
    private val securityUtil: SecurityUtils,
): AttachService {

    private val uploadRoot: Path = Paths.get("uploads")

    @Transactional
    override fun upload(file: MultipartFile): AttachUploadResponse {
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

    override fun get(id: Long): Attach =
        attachRepository.findByIdAndDeletedFalse(id)
            ?: throw AttachNotFoundException()

    override fun getByHash(hash: UUID): Attach =
        attachRepository.findByHashAndDeletedFalse(hash)
            ?: throw AttachNotFoundException()

    override fun getAllUserList(): List<FileDto>{
        val employee = employeeRepository.findByIdAndDeletedFalse(securityUtil.getCurrentUserId())
            ?: throw EmployeeNotFoundException()

        val attaches = attachRepository.findAllByEmployeeIdAndStatusAndDeletedFalse(
            employeeId = employee.id!!,
            status = DocStatus.TEMPLATE
        )

        return attaches.map {
            FileDto(
                id = it.id!!,
                hash = it.hash,
                originalName = it.originName,
                size = it.size,
                type = it.type,
                path= it.fullPath,
            )
        }
    }

    override fun download(hash: UUID): Pair<Resource, Attach> {
        val attach = getByHash(hash)
        val path = Paths.get(attach.fullPath)
        if (!Files.exists(path)) {
            throw FileReadException()
        }
        val resource = UrlResource(path.toUri())
        return Pair(resource, attach)
    }
}
