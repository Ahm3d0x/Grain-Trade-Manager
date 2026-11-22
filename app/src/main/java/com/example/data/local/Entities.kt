package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class InvoiceType(val labelAr: String) {
    PURCHASE("فاتورة شراء"),
    SALE("فاتورة بيع وتوريد")
}

enum class InvoiceStatus(val labelAr: String) {
    DRAFT("مسودة"),
    APPROVED("معتمدة")
}

enum class PaymentStatus(val labelAr: String) {
    UNPAID("غير مدفوع"),
    PARTIAL("مدفوع جزئيًا"),
    PAID("مدفوع بالكامل"),
    OVERPAID_NEEDS_SETTLEMENT("فائض يحتاج تسوية")
}

enum class PaymentRecordType(val labelAr: String) {
    PAYMENT("دفعة سداد"),
    REFUND("رد مبلغ فائض نقدًا"),
    CREDIT_TRANSFER("تحويل الفائض لرصيد العميل")
}

enum class ProfitCalculationMode(val labelAr: String, val descriptionAr: String) {
    WITH_EXPENSES(
        labelAr = "النظام الشامل (خصم مصاريف النقلة والمشال)",
        descriptionAr = "يحسب صافي الربح بعد خصم متوسط تكلفة الشراء ومصاريف النقل والمشال والغربلة لكل نقلة بيع"
    ),
    DIRECT_MARGIN(
        labelAr = "النظام المباشر (فرق السعر بدون مصاريف)",
        descriptionAr = "يحسب الربح المباشر كفرق بين سعر البيع ومتوسط تكلفة الشراء فقط مع إخفاء خانة المصاريف"
    )
}

enum class ShrinkageAdjustmentType(val labelAr: String) {
    MANUAL_SHRINKAGE("تسوية جفاف / غربلة يدوية"),
    BATCH_ZERO_OUT("تصفير الدفعة عند نفاد المخزن")
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val creditBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val defaultUnitId: Long? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "pricing_units",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedProductId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("linkedProductId")]
)
data class PricingUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weightKg: Double,
    val linkedProductId: Long? = null,
    val linkedProductName: String? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customerId"), Index("invoiceNumber", unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String = InvoiceType.PURCHASE.name,
    val customerId: Long? = null,
    val customerNameSnapshot: String = "عميل نقدي (بدون تسجيل)",
    val customerPhoneSnapshot: String = "",
    val invoiceDate: Long = System.currentTimeMillis(),
    val status: String = InvoiceStatus.DRAFT.name,
    val saleExpenses: Double = 0.0,
    val costPerKgSnapshot: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_sections",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val sectionOrder: Int,
    val productId: Long,
    val productNameSnapshot: String,
    val unitId: Long,
    val unitNameSnapshot: String,
    val unitWeightSnapshot: Double,
    val unitPrice: Double
)

@Entity(
    tableName = "weights",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId")]
)
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val sequenceNumber: Int,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val amount: Double,
    val type: String = PaymentRecordType.PAYMENT.name,
    val paymentMethod: String = "نقدي",
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val actionType: String,
    val targetSection: String,
    val oldValue: String,
    val newValue: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("productId")]
)
data class InventoryAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val adjustmentType: String = ShrinkageAdjustmentType.MANUAL_SHRINKAGE.name,
    val weightLossKg: Double,
    val referencePurchasedKg: Double = 0.0,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val merchantName: String = "",
    val merchantPhone: String = "",
    val merchantAddress: String = "",
    val currencySymbol: String = "ج.م",
    val startingInvoiceNumber: Int = 1001,
    val invoiceFooterNotes: String = "",
    val profitCalculationMode: String = ProfitCalculationMode.WITH_EXPENSES.name
)

data class SectionWithWeights(
    @Embedded val section: InvoiceSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId"
    )
    val weights: List<WeightEntity>
)

data class FullInvoiceDetails(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        entity = InvoiceSectionEntity::class,
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val sections: List<SectionWithWeights>,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val payments: List<PaymentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val auditLogs: List<AuditLogEntity>
)
