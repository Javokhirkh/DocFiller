package org.example.docfiller

import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class MyConfiguration{
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}