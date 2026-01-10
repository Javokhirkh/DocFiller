package org.example.docfiller

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
@Configuration
class MyConfiguration{
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}