package org.example.docfiller.services

import org.example.docfiller.EmployeeNotFoundException
import org.example.docfiller.EmployeeRepository
import org.example.docfiller.InvalidPasswordException
import org.example.docfiller.dtos.LoginRequest
import org.example.docfiller.dtos.LoginResponse
import org.example.docfiller.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface EmployeeService {
    fun login(request: LoginRequest): LoginResponse
}

@Service
class EmployeeServiceImpl(
    private val repository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) : EmployeeService{

    override fun login(request: LoginRequest): LoginResponse {
        val user = (repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)
            ?: throw EmployeeNotFoundException())

        if (!passwordEncoder.matches(request.password, user.password )){
            throw InvalidPasswordException()
        }

        val token = jwtService.generateToken(user.phoneNumber, user.role.name)

        return LoginResponse(token)
    }
}