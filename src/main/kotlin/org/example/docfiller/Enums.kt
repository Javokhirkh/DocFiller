package org.example.docfiller

enum class UserRole{
    ROLE_USER, ROLE_ADMIN
}

enum class ErrorCode(val code: Int, val message: String) {
    //1
    EMPLOYEE_NOT_FOUND(100, "EMPLOYEE_NOT_FOUND"),
    EMPLOYEE_ALREADY_EXISTS(101, "EMPLOYEE_ALREADY_EXISTS"),
    EMPLOYEE_NOT_AUTHENTICATED(102, "EMPLOYEE_NOT_AUTHENTICATED"),
    INVALID_PASSWORD(103, "INVALID_PASSWORD"),
}

enum class DocStatus{
    TEMPLATED, READY
}