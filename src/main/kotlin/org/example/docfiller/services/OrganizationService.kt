package org.example.docfiller.services

import org.example.docfiller.Organization
import org.example.docfiller.OrganizationNameAlreadyExistsException
import org.example.docfiller.OrganizationNotFoundException
import org.example.docfiller.OrganizationRepository
import org.example.docfiller.PhoneNumberAlreadyExists
import org.example.docfiller.dtos.OrganizationCreate
import org.example.docfiller.dtos.OrganizationResponse
import org.example.docfiller.dtos.OrganizationUpdate
import org.springframework.stereotype.Service

interface OrganizationService{
    fun create(request: OrganizationCreate)
    fun getOne(id: Long): OrganizationResponse
    fun update(id: Long, request: OrganizationUpdate)
    fun delete(id: Long)
}

@Service
class OrganizationServiceImpl(
    private val repository: OrganizationRepository
) : OrganizationService{
    override fun create(request: OrganizationCreate) {
        repository.existsByNameAndDeletedFalse(request.name).takeIf { it }?.let {
            throw OrganizationNameAlreadyExistsException()
        }
        repository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber).takeIf { it }?.let {
            throw PhoneNumberAlreadyExists()
        }
        repository.save(Organization(
            name = request.name,
            phoneNumber = request.phoneNumber,
        ))
    }

    override fun getOne(id: Long): OrganizationResponse {
        repository.findByIdAndDeletedFalse(id)?.let {
            return OrganizationResponse(
                id = it.id!!,
                name = it.name,
                phoneNumber = it.phoneNumber,
                createDate = it.createdDate
            )
        }
        throw OrganizationNotFoundException()
    }

    override fun update(id: Long, request: OrganizationUpdate) {
        repository.findByIdAndDeletedFalse(id)?.let { findOrganization ->
            if (request.name?.isEmpty() != true){
                repository.existsByNameAndDeletedFalse(request.name!!).takeIf { it }?.let {
                    throw OrganizationNameAlreadyExistsException()
                }
                findOrganization.name = request.name
            }

            if (request.phoneNumber?.isEmpty() != true){
                repository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber!!).takeIf { it }?.let {
                    throw PhoneNumberAlreadyExists()
                }
                findOrganization.phoneNumber = request.phoneNumber
            }
            repository.save(findOrganization)
            return
        }
        throw OrganizationNotFoundException()
    }

    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            repository.trash(id)
            return
        }
        throw OrganizationNotFoundException()
    }

}