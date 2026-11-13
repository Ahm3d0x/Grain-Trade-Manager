package com.example

import com.example.data.local.FullInvoiceDetails
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.SectionWithWeights
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
}
