package org.example.docfiller

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: LocalDateTime? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: LocalDateTime? = null,
    @CreatedBy var createdBy: String? = null,
    @LastModifiedBy var lastModifiedBy: String? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)