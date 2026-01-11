package org.example.docfiller.services

import org.example.docfiller.Employee
import org.example.docfiller.EmployeeNotFoundException
import org.example.docfiller.EmployeeRepository
import org.example.docfiller.InvalidPasswordException
import org.example.docfiller.OrganizationNotFoundException
import org.example.docfiller.OrganizationRepository
import org.example.docfiller.PhoneNumberAlreadyExists
import org.example.docfiller.UserRole
import org.example.docfiller.dtos.LoginRequest
import org.example.docfiller.dtos.LoginResponse
import org.example.docfiller.dtos.RegisterRequest
import org.example.docfiller.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface EmployeeService {
    fun login(request: LoginRequest): LoginResponse
    fun register(request: RegisterRequest)
}

@Service
class EmployeeServiceImpl(
    private val repository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val organizationRepo: OrganizationRepository,
) : EmployeeService{

    override fun login(request: LoginRequest): LoginResponse {
        val user = (repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)
            ?: throw EmployeeNotFoundException())

        if (!passwordEncoder.matches(request.password, user.password )){
            throw InvalidPasswordException()
        }

        val token = jwtService.generateToken(user.phoneNumber, user.role.name)
        val role = jwtService.extractRole(token)
        return LoginResponse(token, role = role)
    }

    override fun register(request: RegisterRequest) {
        organizationRepo.findByIdAndDeletedFalse(request.organizationId)?.let { organization ->
            repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)?.let { phone ->
                throw PhoneNumberAlreadyExists()
            }
            request.let { requestI ->
                repository.save(Employee(
                    firstName = requestI.firstName,
                    lastName = requestI.lastName?.let { it } as String,
                    phoneNumber = requestI.phoneNumber,
                    password = passwordEncoder.encode(requestI.password),
                    role = UserRole.ROLE_USER,
                    organization = organization,
                ))
                return
            }
        }
        throw OrganizationNotFoundException()
    }
}