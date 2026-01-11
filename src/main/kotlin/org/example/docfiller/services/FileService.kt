package org.example.docfiller.services

import org.example.docfiller.Attach
import org.example.docfiller.AttachRepository
import org.example.docfiller.EmployeeRepository
import org.example.docfiller.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

@Service
class AttachService(
    private val attachRepository: AttachRepository,
    private val employeeRepository: EmployeeRepository
) {

    private val uploadRoot: Path = Paths.get("uploads")

    @Transactional
    fun upload(file: MultipartFile, employeeId: Long): Long {
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { RuntimeException(ErrorCode.EMPLOYEE_NOT_FOUND.name) }

        if (file.isEmpty) {
            throw RuntimeException("FILE_IS_EMPTY")
        }

        Files.createDirectories(uploadRoot)

        val hash = calculateHash(file.bytes)

        if (attachRepository.existsByHash(hash)) {
            throw RuntimeException("FILE_ALREADY_EXISTS")
        }

        val storedName = UUID.randomUUID().toString()
        val fullPath = uploadRoot.resolve(storedName)

        Files.write(fullPath, file.bytes)

        val attach = Attach(
            originName = file.originalFilename,
            size = file.size,
            type = file.contentType,
            path = uploadRoot.toString(),
            fullPath = fullPath.toString(),
            hash = hash,
            employee = employee
        )

        return attachRepository.save(attach).id!!
    }

    fun get(id: Long): Attach =
        attachRepository.findById(id)
            .orElseThrow { RuntimeException("FILE_NOT_FOUND") }

    private fun calculateHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
