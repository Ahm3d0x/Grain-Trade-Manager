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
    version = 2,
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
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialDataIfNeeded(instance.grainDao())
                }
                instance
            }
        }

        suspend fun seedInitialDataIfNeeded(dao: GrainDao) {
            val currentSettings = dao.getSettingsOnce()
            if (currentSettings == null) {
                dao.saveSettings(AppSettingsEntity())
            } else if (
                currentSettings.merchantName == "شونة ومضرب البركة لتجارة الحبوب والغلال" ||
                currentSettings.merchantPhone == "01001234567"
            ) {
                // Clean out old pre-filled demo merchant info so fields are empty and ready to fill
                dao.saveSettings(
                    currentSettings.copy(
                        merchantName = "",
                        merchantPhone = "",
                        merchantAddress = "",
                        invoiceFooterNotes = ""
                    )
                )
            }

            if (dao.getAllProductsOnce().isNotEmpty()) return

            // 1. Seed Core Grain Products
            val wheatId = dao.insertProduct(ProductEntity(name = "قمح", description = "قمح بلدي"))
            dao.insertProduct(ProductEntity(name = "أرز", description = "أرز شعير"))
            val yellowCornId = dao.insertProduct(ProductEntity(name = "ذرة صفراء", description = "ذرة صفراء"))
            val whiteCornId = dao.insertProduct(ProductEntity(name = "ذرة بيضاء", description = "ذرة بيضاء"))
            val beansId = dao.insertProduct(ProductEntity(name = "فول", description = "فول بلدي"))
            val barleyId = dao.insertProduct(ProductEntity(name = "شعير", description = "شعير بلدي"))

            // 2. Seed Core Pricing Units
            dao.insertPricingUnit(
                PricingUnitEntity(name = "طن", weightKg = 1000.0, linkedProductId = null, linkedProductName = "جميع المحاصيل")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب قمح", weightKg = 150.0, linkedProductId = wheatId, linkedProductName = "قمح")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب ذرة", weightKg = 140.0, linkedProductId = yellowCornId, linkedProductName = "ذرة صفراء")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب فول", weightKg = 155.0, linkedProductId = beansId, linkedProductName = "فول")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب شعير", weightKg = 120.0, linkedProductId = barleyId, linkedProductName = "شعير")
            )
            dao.insertPricingUnit(
                PricingUnitEntity(name = "قنطار", weightKg = 50.0, linkedProductId = whiteCornId, linkedProductName = "ذرة بيضاء")
            )
        }
    }
}
