package org.example.docfiller.dtos

import org.example.docfiller.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserDetailsResponse(
    val id: Long,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String?,
    val role: UserRole,
    val mypassword: String,
) : UserDetails{
    override fun getAuthorities(): Collection<GrantedAuthority?> {
        return listOf(SimpleGrantedAuthority(role.name))
    }

    override fun getPassword(): String {
        return mypassword
    }

    override fun getUsername(): String {
        return phoneNumber
    }

}