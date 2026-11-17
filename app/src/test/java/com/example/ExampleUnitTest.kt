package com.example

import com.example.data.local.FullInvoiceDetails
import com.example.data.local.InventoryAdjustmentEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.InvoiceType
import com.example.data.local.PaymentEntity
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ProfitCalculationMode
import com.example.data.local.SectionWithWeights
import com.example.data.local.ShrinkageAdjustmentType
import com.example.data.local.WeightEntity
import com.example.domain.CalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun fiveWeights_createsSingleCompleteGroup() {
        val sec = InvoiceSectionEntity(
            id = 1, invoiceId = 1, sectionOrder = 1,
            productId = 1, productNameSnapshot = "قمح",
            unitId = 1, unitNameSnapshot = "طن",
            unitWeightSnapshot = 1000.0, unitPrice = 4000.0
        )
        val weights = listOf(50.0, 52.0, 48.0, 55.0, 51.0).mapIndexed { idx, w ->
            WeightEntity(id = (idx + 1).toLong(), sectionId = 1, sequenceNumber = idx + 1, weightKg = w)
        }
        val calc = CalculationEngine.calculateSection(SectionWithWeights(sec, weights))

        assertEquals(1, calc.groups.size)
        assertEquals(0, calc.groups[0].remainingSlots)
        assertEquals(256.0, calc.groups[0].groupTotalKg, 0.001)
        assertEquals(256.0, calc.totalWeightKg, 0.001)
    }

    @Test
    fun twelveWeights_createsThreeGroupsWithCorrectRemainingSlots() {
        val sec = InvoiceSectionEntity(
            id = 1, invoiceId = 1, sectionOrder = 1,
            productId = 1, productNameSnapshot = "قمح",
            unitId = 1, unitNameSnapshot = "طن",
            unitWeightSnapshot = 1000.0, unitPrice = 4000.0
        )
        val raw = listOf(50.0, 52.0, 48.0, 55.0, 51.0, 49.0, 53.0, 54.0, 50.0, 52.0, 51.0, 60.0)
        val weights = raw.mapIndexed { idx, w ->
            WeightEntity(id = (idx + 1).toLong(), sectionId = 1, sequenceNumber = idx + 1, weightKg = w)
        }
        val calc = CalculationEngine.calculateSection(SectionWithWeights(sec, weights))

        assertEquals(3, calc.groups.size)
        assertEquals(256.0, calc.groups[0].groupTotalKg, 0.001)
        assertEquals(258.0, calc.groups[1].groupTotalKg, 0.001)
        assertEquals(111.0, calc.groups[2].groupTotalKg, 0.001)
        assertEquals(3, calc.groups[2].remainingSlots)
        assertEquals(625.0, calc.totalWeightKg, 0.001)
        assertEquals(4.0, calc.pricePerKg, 0.001)
        assertEquals(2500.0, calc.sectionTotalAmount, 0.01)
    }

    @Test
    fun multiSectionInvoice_andOverpaymentDetection_workIndependently() {
        val invoice = InvoiceEntity(id = 10, invoiceNumber = "1025")
        val sec1 = InvoiceSectionEntity(
            id = 1, invoiceId = 10, sectionOrder = 1,
            productId = 1, productNameSnapshot = "قمح",
            unitId = 1, unitNameSnapshot = "طن",
            unitWeightSnapshot = 1000.0, unitPrice = 4000.0
        )
        val sec2 = InvoiceSectionEntity(
            id = 2, invoiceId = 10, sectionOrder = 2,
            productId = 2, productNameSnapshot = "أرز",
            unitId = 1, unitNameSnapshot = "طن",
            unitWeightSnapshot = 1000.0, unitPrice = 4200.0
        )

        val details = FullInvoiceDetails(
            invoice = invoice,
            sections = listOf(
                SectionWithWeights(sec1, listOf(WeightEntity(1, 1, 1, 1500.0))),
                SectionWithWeights(sec2, listOf(WeightEntity(2, 2, 1, 1000.0)))
            ),
            payments = listOf(
                PaymentEntity(id = 1, invoiceId = 10, amount = 11000.0, type = PaymentRecordType.PAYMENT.name)
            ),
            auditLogs = emptyList()
        )

        val calc = CalculationEngine.calculateInvoice(details)
        assertEquals(6000.0, calc.sectionCalculations[0].sectionTotalAmount, 0.01)
        assertEquals(4200.0, calc.sectionCalculations[1].sectionTotalAmount, 0.01)
        assertEquals(10200.0, calc.totalInvoiceAmount, 0.01)
        assertEquals(0.0, calc.remainingAmount, 0.01)
        assertEquals(800.0, calc.overpaidAmount, 0.01)
        assertEquals(PaymentStatus.OVERPAID_NEEDS_SETTLEMENT, calc.paymentStatus)
    }

    @Test
    fun warehouseLiveStock_bulkSaleProfitModes_andShrinkage_calculateAccurately() {
        val wheat = ProductEntity(id = 1, name = "قمح")
        val ardabUnit = PricingUnitEntity(id = 2, name = "أردب قمح", weightKg = 150.0, linkedProductId = 1, linkedProductName = "قمح")

        // 1. Purchase 10,000 kg of Wheat at 3,000 EGP per Ardab (150 kg) -> 20 EGP/kg -> 200,000 EGP total
        val purchaseDetails = FullInvoiceDetails(
            invoice = InvoiceEntity(
                id = 1,
                invoiceNumber = "1001",
                invoiceType = InvoiceType.PURCHASE.name,
                status = InvoiceStatus.APPROVED.name
            ),
            sections = listOf(
                SectionWithWeights(
                    section = InvoiceSectionEntity(
                        id = 1, invoiceId = 1, sectionOrder = 1,
                        productId = 1, productNameSnapshot = "قمح",
                        unitId = 2, unitNameSnapshot = "أردب قمح",
                        unitWeightSnapshot = 150.0, unitPrice = 3000.0
                    ),
                    weights = listOf(WeightEntity(1, 1, 1, 10000.0))
                )
            ),
            payments = emptyList(),
            auditLogs = emptyList()
        )
        val purchaseCalc = CalculationEngine.calculateInvoice(purchaseDetails)

        // 2. Bulk Sale of 6,000 kg of Wheat at 23,000 EGP per Ton (23 EGP/kg) -> 138,000 EGP revenue
        // Cost snapshot = 20 EGP/kg -> Cost of Goods = 120,000 EGP
        // Sale Expenses = 1,500 EGP
        val saleDetails = FullInvoiceDetails(
            invoice = InvoiceEntity(
                id = 2,
                invoiceNumber = "1002",
                invoiceType = InvoiceType.SALE.name,
                status = InvoiceStatus.APPROVED.name,
                saleExpenses = 1500.0,
                costPerKgSnapshot = 20.0
            ),
            sections = listOf(
                SectionWithWeights(
                    section = InvoiceSectionEntity(
                        id = 2, invoiceId = 2, sectionOrder = 1,
                        productId = 1, productNameSnapshot = "قمح",
                        unitId = 1, unitNameSnapshot = "طن",
                        unitWeightSnapshot = 1000.0, unitPrice = 23000.0
                    ),
                    weights = listOf(WeightEntity(2, 2, 1, 6000.0))
                )
            ),
            payments = emptyList(),
            auditLogs = emptyList()
        )
        val saleCalc = CalculationEngine.calculateInvoice(saleDetails)

        // Verify both profit calculation modes on the sale invoice
        assertEquals(18000.0, saleCalc.getEffectiveProfit(ProfitCalculationMode.DIRECT_MARGIN), 0.01)
        assertEquals(16500.0, saleCalc.getEffectiveProfit(ProfitCalculationMode.WITH_EXPENSES), 0.01)

        // 3. Manual shrinkage loss of 200 kg (2% of 10,000 kg purchased)
        val adjustments = listOf(
            InventoryAdjustmentEntity(
                id = 1,
                productId = 1,
                productNameSnapshot = "قمح",
                adjustmentType = ShrinkageAdjustmentType.MANUAL_SHRINKAGE.name,
                weightLossKg = 200.0,
                referencePurchasedKg = 10000.0,
                reason = "جفاف طبيعي"
            )
        )

        val summary = CalculationEngine.calculateWarehouseSummary(
            products = listOf(wheat),
            pricingUnits = listOf(ardabUnit),
            allInvoices = listOf(purchaseDetails to purchaseCalc, saleDetails to saleCalc),
            adjustments = adjustments
        )

        val wheatStock = summary.productStocks.first()
        assertEquals(3800.0, wheatStock.availableStockKg, 0.01) // 10,000 - 6,000 - 200
        assertEquals(20.0, wheatStock.avgPurchaseCostPerKg, 0.01)
        assertEquals(2.0, wheatStock.shrinkagePercentage, 0.01)
        assertEquals(18000.0, summary.getTotalProfitByMode(ProfitCalculationMode.DIRECT_MARGIN), 0.01)
        assertEquals(16500.0, summary.getTotalProfitByMode(ProfitCalculationMode.WITH_EXPENSES), 0.01)
    }
}
