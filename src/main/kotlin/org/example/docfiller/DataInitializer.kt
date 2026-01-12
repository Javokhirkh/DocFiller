package org.example.docfiller

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        createAdminUserIfNotExists()
    }

    private fun createAdminUserIfNotExists() {
        val adminPhoneNumber = "+998990000102"

        if (employeeRepository.findByPhoneNumberAndDeletedFalse(adminPhoneNumber) != null) {
            return
        }

        val adminOrganization = organizationRepository.findAll()
            .firstOrNull { !it.deleted && it.name == "Admin Organization" }
            ?: organizationRepository.save(
                Organization(
                    name = "Admin Organization",
                    phoneNumber = "000000000"
                )
            )

        val adminUser = Employee(
            firstName = "Admin",
            lastName = "User",
            phoneNumber = adminPhoneNumber,
            password = passwordEncoder.encode("admin123"),
            role = UserRole.ROLE_ADMIN,
            organization = adminOrganization
        )

        employeeRepository.save(adminUser)
    }
}
