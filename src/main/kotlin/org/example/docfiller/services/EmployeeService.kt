package org.example.docfiller.services

import org.example.docfiller.Employee
import org.example.docfiller.EmployeeNotFoundException
import org.example.docfiller.EmployeeRepository
import org.example.docfiller.InvalidPasswordException
import org.example.docfiller.OrganizationNotFoundException
import org.example.docfiller.OrganizationRepository
import org.example.docfiller.PhoneNumberAlreadyExists
import org.example.docfiller.UserRole
import org.example.docfiller.dtos.EmployeeCreate
import org.example.docfiller.dtos.EmployeeResponse
import org.example.docfiller.dtos.EmployeeUpdate
import org.example.docfiller.dtos.LoginRequest
import org.example.docfiller.dtos.LoginResponse
import org.example.docfiller.dtos.RegisterRequest
import org.example.docfiller.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface EmployeeService {
    fun login(request: LoginRequest): LoginResponse
    fun register(request: RegisterRequest)
    fun create(request: EmployeeCreate)
    fun getOne(id: Long): EmployeeResponse
    fun getAll(): List<EmployeeResponse>
    fun update(id: Long, request: EmployeeUpdate)
    fun delete(id: Long)
}

@Service
class EmployeeServiceImpl(
    private val repository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val organizationRepo: OrganizationRepository,
) : EmployeeService{

    override fun login(request: LoginRequest): LoginResponse {
        val user = (repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)
            ?: throw EmployeeNotFoundException())

        if (!passwordEncoder.matches(request.password, user.password )){
            throw InvalidPasswordException()
        }

        val token = jwtService.generateToken(user.phoneNumber, user.role.name)
        val role = jwtService.extractRole(token)
        return LoginResponse(token, role = role)
    }

    override fun register(request: RegisterRequest) {
        organizationRepo.findByIdAndDeletedFalse(request.organizationId)?.let { organization ->
            repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)?.let { phone ->
                throw PhoneNumberAlreadyExists()
            }
            request.let { requestI ->
                repository.save(Employee(
                    firstName = requestI.firstName,
                    lastName = requestI.lastName?.let { it } as String,
                    phoneNumber = requestI.phoneNumber,
                    password = passwordEncoder.encode(requestI.password),
                    role = UserRole.ROLE_USER,
                    organization = organization,
                ))
                return
            }
        }
        throw OrganizationNotFoundException()
    }

    override fun create(request: EmployeeCreate) {
        repository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)?.let {
            throw PhoneNumberAlreadyExists()
        }
        request.organizationId?.let { organizationId ->
            organizationRepo.findByIdAndDeletedFalse(organizationId)?.let { organization ->
                repository.save(Employee(
                    firstName = request.firstName,
                    lastName = request.lastName?.let { it } as String,
                    phoneNumber = request.phoneNumber,
                    password = passwordEncoder.encode(request.password),
                    role = request.role,
                    organization = organization,
                ))
                return
            }
            throw OrganizationNotFoundException()
        }
        repository.save(Employee(
            firstName = request.firstName,
            lastName = request.lastName?.let { it } as String,
            phoneNumber = request.phoneNumber,
            password = passwordEncoder.encode(request.password),
            role = request.role,
        ))
        return
    }

    override fun getOne(id: Long): EmployeeResponse {
        repository.findByIdAndDeletedFalse(id)?.let { employee ->
            return EmployeeResponse(
                employee.id!!,
                employee.firstName,
                employee.lastName,
                employee.phoneNumber,
                employee.organization?.let { it.id } as Long,
                employee.deleted
            )
        }
        throw EmployeeNotFoundException()
    }

    override fun getAll(): List<EmployeeResponse> {
        val findEmployeeList = mutableListOf<EmployeeResponse>()
        repository.findAll().forEach { employee ->
            findEmployeeList.add(EmployeeResponse(
                employee.id!!,
                employee.firstName,
                employee.lastName,
                employee.phoneNumber,
                employee.organization?.let { it.id } as Long,
                employee.deleted
            ))
        }
        return findEmployeeList
    }

    override fun update(id: Long, request: EmployeeUpdate) {
        repository.findByIdAndDeletedFalse(id)?.let { employee ->
            request.run {
                //set phone number
                phoneNumber?.let { requestPhone ->
                    repository.findByPhoneNumberAndDeletedFalse(phoneNumber)?.let { phone ->
                        throw PhoneNumberAlreadyExists()
                    }
                    employee.phoneNumber = requestPhone
                }
                //set organization
                organizationId?.let { organizationId ->
                    organizationRepo.findByIdAndDeletedFalse(organizationId)?.let { organization ->
                        employee.organization = organization
                    }
                    throw OrganizationNotFoundException()
                }

                firstName?.let { requestFirstName ->
                    employee.firstName = requestFirstName
                }

                lastName?.let { requestLastName ->
                    employee.lastName = requestLastName
                }

                repository.save(employee)
                return
            }
        }
        throw EmployeeNotFoundException()
    }

    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let { employee ->
            repository.trash(id)
            return
        }
        throw EmployeeNotFoundException()
    }
}