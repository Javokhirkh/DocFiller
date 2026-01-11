package org.example.docfiller.security

import org.example.docfiller.EmployeeNotAuthenticatedException
import org.example.docfiller.UserRole
import org.example.docfiller.dtos.UserDetailsResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityUtils {

    fun getCurrentUser(): UserDetailsResponse? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserDetailsResponse?
    }

    fun getCurrentUserId(): Long{
        return getCurrentUser()?.id
            ?: throw EmployeeNotAuthenticatedException()
    }

    fun getCurrentUserName(): String {
        return getCurrentUser()?.username
            ?: throw EmployeeNotAuthenticatedException()
    }

    fun getCurrentUserRole(): UserRole {
        return getCurrentUser()?.role
            ?: throw RuntimeException("User not authenticated")
    }

    fun isAdmin(): Boolean {
        return getCurrentUser()?.role == UserRole.ROLE_ADMIN
    }

    /*fun isUserActive(): Boolean {
        return getCurrentUser()?. == Status.ACTIVE
    }*/
}