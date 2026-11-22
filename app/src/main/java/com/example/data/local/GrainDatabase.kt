package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        InventoryAdjustmentEntity::class,
        AppSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GrainDatabase : RoomDatabase() {
    abstract fun grainDao(): GrainDao

    companion object {
        @Volatile
        private var INSTANCE: GrainDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN invoiceType TEXT NOT NULL DEFAULT 'PURCHASE'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN saleExpenses REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN costPerKgSnapshot REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN profitCalculationMode TEXT NOT NULL DEFAULT 'WITH_EXPENSES'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_adjustments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER,
                        `productNameSnapshot` TEXT NOT NULL,
                        `adjustmentType` TEXT NOT NULL,
                        `weightLossKg` REAL NOT NULL,
                        `referencePurchasedKg` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_adjustments_productId` ON `inventory_adjustments` (`productId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN defaultUnitId INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    UPDATE products
                    SET defaultUnitId = (
                        SELECT pu.id FROM pricing_units pu
                        WHERE pu.linkedProductId = products.id
                        ORDER BY pu.id ASC
                        LIMIT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE invoice_sections
                    SET unitPrice = 0.0
                    WHERE unitPrice = 4000.0
                      AND invoiceId IN (SELECT id FROM invoices WHERE status = 'DRAFT')
                      AND id NOT IN (SELECT DISTINCT sectionId FROM weights)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): GrainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrainDatabase::class.java,
                    "grain_trade_manager.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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
            val riceId = dao.insertProduct(ProductEntity(name = "أرز", description = "أرز شعير"))
            val yellowCornId = dao.insertProduct(ProductEntity(name = "ذرة صفراء", description = "ذرة صفراء"))
            val whiteCornId = dao.insertProduct(ProductEntity(name = "ذرة بيضاء", description = "ذرة بيضاء"))
            val beansId = dao.insertProduct(ProductEntity(name = "فول", description = "فول بلدي"))
            val barleyId = dao.insertProduct(ProductEntity(name = "شعير", description = "شعير بلدي"))

            // 2. Seed Core Pricing Units and link each crop to its default pricing unit
            val tonUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "طن", weightKg = 1000.0, linkedProductId = null, linkedProductName = "جميع المحاصيل")
            )
            val wheatUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب قمح", weightKg = 150.0, linkedProductId = wheatId, linkedProductName = "قمح")
            )
            val cornUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب ذرة", weightKg = 140.0, linkedProductId = yellowCornId, linkedProductName = "ذرة صفراء")
            )
            val beansUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب فول", weightKg = 155.0, linkedProductId = beansId, linkedProductName = "فول")
            )
            val barleyUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "أردب شعير", weightKg = 120.0, linkedProductId = barleyId, linkedProductName = "شعير")
            )
            val qintarUnitId = dao.insertPricingUnit(
                PricingUnitEntity(name = "قنطار", weightKg = 50.0, linkedProductId = whiteCornId, linkedProductName = "ذرة بيضاء")
            )

            dao.updateProductDefaultUnit(wheatId, wheatUnitId)
            dao.updateProductDefaultUnit(riceId, tonUnitId)
            dao.updateProductDefaultUnit(yellowCornId, cornUnitId)
            dao.updateProductDefaultUnit(whiteCornId, qintarUnitId)
            dao.updateProductDefaultUnit(beansId, beansUnitId)
            dao.updateProductDefaultUnit(barleyId, barleyUnitId)
        }
    }
}
