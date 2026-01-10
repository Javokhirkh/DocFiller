package org.example.docfiller.services

import org.example.docfiller.EmployeeNotFoundException
import org.example.docfiller.EmployeeRepository
import org.example.docfiller.dtos.UserDetailsResponse
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserService(
    private val repository: EmployeeRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return repository.findByPhoneNumberAndDeletedFalse(username)?.let {
            UserDetailsResponse(
                id = it.id!!,
                phoneNumber = it.phoneNumber,
                firstName = it.firstName,
                lastName = it.lastName,
                role = it.role,
                mypassword = it.password
            )
        } ?: throw EmployeeNotFoundException()
    }
}