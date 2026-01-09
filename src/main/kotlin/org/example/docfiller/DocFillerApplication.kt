package org.example.docfiller

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DocFillerApplication

fun main(args: Array<String>) {
    runApplication<DocFillerApplication>(*args)
}
