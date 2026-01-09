package org.example.docfiller.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}")private val secret: String,
    @Value("\${jwt.expiration}")private val expiration: Long,
) {
    private fun key() =
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    fun generateToken(username: String, role: String): String {
        val claims = mapOf("roles" to role)
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key(), SignatureAlgorithm.HS512)
            .compact()
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(key())
            .build()
            .parseClaimsJws(token)
            .body

    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun extractRole(token: String): String {
        return extractAllClaims(token).get("role", String::class.java)
    }

    fun isTokenValid(token: String): Boolean {
        return !extractAllClaims(token).expiration.before(Date())
    }
}

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userDetailService: UserDetailsService,
) : OncePerRequestFilter(){

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        val phoneNumber = jwtService.extractUsername(jwt)

        if (phoneNumber != null && SecurityContextHolder.getContext().authentication == null) {
            val userDetails = userDetailService.loadUserByUsername(phoneNumber)
            println(userDetails.authorities)
            if (jwtService.isTokenValid(jwt)) {
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )
                SecurityContextHolder.getContext().authentication = authToken
            }
        }
        filterChain.doFilter(request, response)
    }

}

