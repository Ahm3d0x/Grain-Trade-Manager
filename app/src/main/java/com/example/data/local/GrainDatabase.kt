package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        ProductEntity::class,
        PricingUnitEntity::class,
        InvoiceEntity::class,
        InvoiceSectionEntity::class,
        WeightEntity::class,
        PaymentEntity::class,
        AuditLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GrainDatabase : RoomDatabase() {
    abstract fun grainDao(): GrainDao

    companion object {
        @Volatile
        private var INSTANCE: GrainDatabase? = null

        fun getInstance(context: Context): GrainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrainDatabase::class.java,
                    "grain_trade_manager.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialDataIfNeeded(instance.grainDao())
                }
                instance
            }
        }

        suspend fun seedInitialDataIfNeeded(dao: GrainDao) {
            if (dao.getSettingsOnce() == null) {
                dao.saveSettings(AppSettingsEntity())
            }
            if (dao.getAllProductsOnce().isNotEmpty()) return

            // 1. Seed Grain Products
            val wheatId = dao.insertProduct(ProductEntity(name = "قمح", description = "قمح بلدي فرز أول"))
            val riceId = dao.insertProduct(ProductEntity(name = "أرز", description = "أرز شعير عريض ورفيع"))
            val yellowCornId = dao.insertProduct(ProductEntity(name = "ذرة صفراء", description = "ذرة صفراء مجففة"))
            val whiteCornId = dao.insertProduct(ProductEntity(name = "ذرة بيضاء", description = "ذرة بيضاء بلدي"))
            val beansId = dao.insertProduct(ProductEntity(name = "فول", description = "فول بلدي ثلاجات"))
            val barleyId = dao.insertProduct(ProductEntity(name = "شعير", description = "شعير بلدي"))

            // 2. Seed Pricing Units
            val tonUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "طن", weightKg = 1000.0, linkedProductId = null, linkedProductName = "جميع المحاصيل")
            )
            val ardabWheatId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب قمح", weightKg = 150.0, linkedProductId = wheatId, linkedProductName = "قمح")
            )
            val ardabCornId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب ذرة", weightKg = 140.0, linkedProductId = yellowCornId, linkedProductName = "ذرة صفراء")
            )
            val ardabBeansId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب فول", weightKg = 155.0, linkedProductId = beansId, linkedProductName = "فول")
            )
            val ardabBarleyId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب شعير", weightKg = 120.0, linkedProductId = barleyId, linkedProductName = "شعير")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "قنطار", weightKg = 50.0, linkedProductId = whiteCornId, linkedProductName = "ذرة بيضاء")
            )

            // 3. Seed Customers
            val now = System.currentTimeMillis()
            val cust1Id = dao.insertCustomer(
                CustomerEntity(
                    name = "محمد أحمد المنشاوي",
                    phone = "01002345678",
                    address = "قرية ميت خميس - المنصورة",
                    notes = "مورد قمح وأرز موسمي منتظم",
                    createdAt = now - 86400_000L * 10
                )
            )
            val cust2Id = dao.insertCustomer(
                CustomerEntity(
                    name = "الحاج عبد الرحمن الشرقاوي",
                    phone = "01098765432",
                    address = "مركز بلقاس - الدقهلية",
                    notes = "توريد كميات كبيرة بالشاحنة",
                    createdAt = now - 86400_000L * 7
                )
            )
            val cust3Id = dao.insertCustomer(
                CustomerEntity(
                    name = "إبراهيم حسن أبو العلا",
                    phone = "01112223344",
                    address = "السنبلاوين - الدقهلية",
                    notes = "تاجر فرعي - ذرة وفول",
                    createdAt = now - 86400_000L * 4
                )
            )

            // 4. Seed Invoice #1025 (Matches user specification example with 2 sections & 5-weight grouping)
            val inv1Id = dao.insertInvoice(
                InvoiceEntity(
                    invoiceNumber = "1025",
                    customerId = cust1Id,
                    customerNameSnapshot = "محمد أحمد المنشاوي",
                    customerPhoneSnapshot = "01002345678",
                    invoiceDate = now - 3600_000L * 3,
                    status = InvoiceStatus.APPROVED.name,
                    notes = "توريد قمح وأرز - تم فحص الرطوبة والنظافة"
                )
            )

            // Section 1: Wheat (12 bags grouped in 3 columns: 5 + 5 + 2 = 625 kg @ 4,000 EGP/Ton)
            val sec1Id = dao.insertSection(
                InvoiceSectionEntity(
                    invoiceId = inv1Id,
                    sectionOrder = 1,
                    productId = wheatId,
                    productNameSnapshot = "قمح",
                    unitId = tonUnitId,
                    unitNameSnapshot = "طن",
                    unitWeightSnapshot = 1000.0,
                    unitPrice = 4000.0
                )
            )
            val sec1Weights = listOf(
                50.0, 52.0, 48.0, 55.0, 51.0, // Column 1 = 256 kg
                49.0, 53.0, 54.0, 50.0, 52.0, // Column 2 = 258 kg
                51.0, 60.0                     // Column 3 = 111 kg (Total = 625 kg)
            )
            sec1Weights.forEachIndexed { index, w ->
                dao.insertWeight(
                    WeightEntity(
                        sectionId = sec1Id,
                        sequenceNumber = index + 1,
                        weightKg = w
                    )
                )
            }

            // Section 2: Rice (6 bags grouped in 2 columns: 5 + 1 = 350 kg @ 5,000 EGP/Ton)
            val sec2Id = dao.insertSection(
                InvoiceSectionEntity(
                    invoiceId = inv1Id,
                    sectionOrder = 2,
                    productId = riceId,
                    productNameSnapshot = "أرز",
                    unitId = tonUnitId,
                    unitNameSnapshot = "طن",
                    unitWeightSnapshot = 1000.0,
                    unitPrice = 5000.0
                )
            )
            val sec2Weights = listOf(58.0, 60.0, 57.0, 59.0, 56.0, 60.0) // 350 kg
            sec2Weights.forEachIndexed { index, w ->
                dao.insertWeight(
                    WeightEntity(
                        sectionId = sec2Id,
                        sequenceNumber = index + 1,
                        weightKg = w
                    )
                )
            }

            // Partial payment on Invoice #1025 (Total = 2,500 + 1,750 = 4,250 EGP; Paid = 3,000 EGP)
            dao.insertPayment(
                PaymentEntity(
                    invoiceId = inv1Id,
                    amount = 3000.0,
                    type = PaymentRecordType.PAYMENT.name,
                    paymentMethod = "نقدي",
                    paymentDate = now - 3600_000L * 2,
                    notes = "دفعة أولى عند الاستلام"
                )
            )

            // 5. Seed Invoice #1026 (Ardab pricing unit example, Paid in full)
            val inv2Id = dao.insertInvoice(
                InvoiceEntity(
                    invoiceNumber = "1026",
                    customerId = cust2Id,
                    customerNameSnapshot = "الحاج عبد الرحمن الشرقاوي",
                    customerPhoneSnapshot = "01098765432",
                    invoiceDate = now - 3600_000L * 1,
                    status = InvoiceStatus.APPROVED.name,
                    notes = "شراء قمح بحساب الأردب (150 كجم)"
                )
            )
            val inv2Sec1Id = dao.insertSection(
                InvoiceSectionEntity(
                    invoiceId = inv2Id,
                    sectionOrder = 1,
                    productId = wheatId,
                    productNameSnapshot = "قمح",
                    unitId = ardabWheatId,
                    unitNameSnapshot = "أردب قمح",
                    unitWeightSnapshot = 150.0,
                    unitPrice = 2100.0 // 14 EGP/kg
                )
            )
            listOf(75.0, 75.0, 74.5, 75.5, 75.0, 75.0).forEachIndexed { idx, w ->
                dao.insertWeight(
                    WeightEntity(
                        sectionId = inv2Sec1Id,
                        sequenceNumber = idx + 1,
                        weightKg = w
                    )
                )
            }
            // Total 450 kg = 3 Ardabs * 2100 = 6300 EGP
            dao.insertPayment(
                PaymentEntity(
                    invoiceId = inv2Id,
                    amount = 6300.0,
                    type = PaymentRecordType.PAYMENT.name,
                    paymentMethod = "نقدي",
                    paymentDate = now - 1800_000L,
                    notes = "سداد كامل الفاتورة نقدًا"
                )
            )

            // Save next starting invoice number
            dao.saveSettings(AppSettingsEntity(startingInvoiceNumber = 1027))
        }
    }
}
