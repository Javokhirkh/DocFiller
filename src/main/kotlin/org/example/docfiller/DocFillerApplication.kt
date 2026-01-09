package org.example.docfiller

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class DocFillerApplication

fun main(args: Array<String>) {
    runApplication<DocFillerApplication>(*args)
}
