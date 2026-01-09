package org.example.docfiller

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
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

@Entity
@Table(name = "users")
class Employee(
    @Column(nullable = false) var firstName: String,
    @Column(nullable = false) var lastName: String,
    @Column(nullable = false) var email: String,
    @Column(nullable = false, unique = true) var username: String,
    @Column(nullable = false) var password: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) var role: UserRole,
) : BaseEntity()

@Entity
@Table(name = "attach")
class Attach(
    @Column(nullable = false, name = "origin_name") var originName: String?,
    @Column(nullable = false) var size: Long,
    @Column(nullable = false) var type: String?,
    @Column(nullable = false) var path: String,
    @Column(nullable = false) var fullPath: String,
    @Column(nullable = false, unique = true) var hash: String,
    @ManyToOne(fetch = FetchType.LAZY)
    var employee : Employee
) : BaseEntity()

@Entity
@Table(name = "place_holder")
class PlaceHolder(
    @ManyToOne(fetch = FetchType.LAZY)
    var attach: Attach,

    @Column(name = "key", nullable = false)
    val key: String,

    @Column(name = "location_type", nullable = false)
    val locationType: String,

    @Column(name = "header_index")
    val headerIndex: Int? = null,

    @Column(name = "paragraph_index")
    val paragraphIndex: Int? = null,

    @Column(name = "table_index")
    val tableIndex: Int? = null,

    @Column(name = "row_index")
    val rowIndex: Int? = null,

    @Column(name = "column_index")
    val columnIndex: Int? = null,

    @Column(name = "footer_index")
    val footerIndex: Int? = null,
) : BaseEntity()