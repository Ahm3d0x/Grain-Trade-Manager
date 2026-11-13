package com.example.domain

import com.example.data.local.FullInvoiceDetails
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.SectionWithWeights
import com.example.data.local.WeightEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class WeightGroup(
    val groupIndex: Int, // 1-based column index
    val weights: List<WeightEntity>, // Up to 5 weights
    val groupTotalKg: Double,
    val remainingSlots: Int
)

data class SectionCalculation(
    val section: InvoiceSectionEntity,
    val sortedWeights: List<WeightEntity>,
    val groups: List<WeightGroup>,
    val bagCount: Int,
    val totalWeightKg: Double,
    val pricePerKg: Double,
    val sectionTotalAmount: Double
)

data class ProductWeightSummary(
    val productName: String,
    val bagCount: Int,
    val totalWeightKg: Double,
    val totalAmount: Double
)

data class InvoiceCalculation(
    val invoiceId: Long,
    val sectionCalculations: List<SectionCalculation>,
    val productSummaries: List<ProductWeightSummary>,
    val totalBagCount: Int,
    val totalWeightKg: Double,
    val totalInvoiceAmount: Double,
    val grossPaidAmount: Double,
    val refundedAmount: Double,
    val creditTransferredAmount: Double,
    val netPaidAmount: Double,
    val remainingAmount: Double,
    val overpaidAmount: Double,
    val paymentStatus: PaymentStatus
)

object CalculationEngine {
    const val GROUP_SIZE = 5

    /**
     * Groups weights in chunks of 5 and calculates each group's sum,
     * section total weight, price per kg, and section total financial value.
     */
    fun calculateSection(sectionWithWeights: SectionWithWeights): SectionCalculation {
        val section = sectionWithWeights.section
        val sorted = sectionWithWeights.weights.sortedBy { it.sequenceNumber }

        val groups = sorted.chunked(GROUP_SIZE).mapIndexed { index, chunk ->
            val groupSum = chunk.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(BigDecimal.valueOf(item.weightKg))
            }.setScale(3, RoundingMode.HALF_UP).toDouble()

            WeightGroup(
                groupIndex = index + 1,
                weights = chunk,
                groupTotalKg = groupSum,
                remainingSlots = (GROUP_SIZE - chunk.size).coerceAtLeast(0)
            )
        }

        val totalWeightBd = groups.fold(BigDecimal.ZERO) { acc, group ->
            acc.add(BigDecimal.valueOf(group.groupTotalKg))
        }.setScale(3, RoundingMode.HALF_UP)

        val unitWeightBd = if (section.unitWeightSnapshot > 0.0) {
            BigDecimal.valueOf(section.unitWeightSnapshot)
        } else {
            BigDecimal.ONE
        }
        val unitPriceBd = BigDecimal.valueOf(section.unitPrice)

        // Price per kg = P_unit / W_unit
        val pricePerKgBd = unitPriceBd.divide(unitWeightBd, 6, RoundingMode.HALF_UP)

        // Total section value = W_section * P_unit / W_unit
        val sectionTotalBd = totalWeightBd.multiply(unitPriceBd)
            .divide(unitWeightBd, 2, RoundingMode.HALF_UP)

        return SectionCalculation(
            section = section,
            sortedWeights = sorted,
            groups = groups,
            bagCount = sorted.size,
            totalWeightKg = totalWeightBd.toDouble(),
            pricePerKg = pricePerKgBd.setScale(4, RoundingMode.HALF_UP).toDouble(),
            sectionTotalAmount = sectionTotalBd.toDouble()
        )
    }

    /**
     * Calculates the entire invoice across all independent sections,
     * aggregates totals per grain type, and evaluates payment/overpayment status.
     */
    fun calculateInvoice(details: FullInvoiceDetails): InvoiceCalculation {
        val orderedSections = details.sections.sortedBy { it.section.sectionOrder }
        val sectionCalcs = orderedSections.map { calculateSection(it) }

        val totalBags = sectionCalcs.sumOf { it.bagCount }
        val totalWeightBd = sectionCalcs.fold(BigDecimal.ZERO) { acc, sec ->
            acc.add(BigDecimal.valueOf(sec.totalWeightKg))
        }.setScale(3, RoundingMode.HALF_UP)

        val totalInvoiceBd = sectionCalcs.fold(BigDecimal.ZERO) { acc, sec ->
            acc.add(BigDecimal.valueOf(sec.sectionTotalAmount))
        }.setScale(2, RoundingMode.HALF_UP)

        val productSummaries = sectionCalcs
            .groupBy { it.section.productNameSnapshot }
            .map { (productName, secs) ->
                ProductWeightSummary(
                    productName = productName,
                    bagCount = secs.sumOf { it.bagCount },
                    totalWeightKg = secs.fold(BigDecimal.ZERO) { acc, s ->
                        acc.add(BigDecimal.valueOf(s.totalWeightKg))
                    }.setScale(3, RoundingMode.HALF_UP).toDouble(),
                    totalAmount = secs.fold(BigDecimal.ZERO) { acc, s ->
                        acc.add(BigDecimal.valueOf(s.sectionTotalAmount))
                    }.setScale(2, RoundingMode.HALF_UP).toDouble()
                )
            }

        return evaluatePayments(
            invoiceId = details.invoice.id,
            sectionCalcs = sectionCalcs,
            productSummaries = productSummaries,
            totalBags = totalBags,
            totalWeightBd = totalWeightBd,
            totalInvoiceBd = totalInvoiceBd,
            payments = details.payments
        )
    }

    private fun evaluatePayments(
        invoiceId: Long,
        sectionCalcs: List<SectionCalculation>,
        productSummaries: List<ProductWeightSummary>,
        totalBags: Int,
        totalWeightBd: BigDecimal,
        totalInvoiceBd: BigDecimal,
        payments: List<PaymentEntity>
    ): InvoiceCalculation {
        var grossPaidBd = BigDecimal.ZERO
        var refundedBd = BigDecimal.ZERO
        var creditedBd = BigDecimal.ZERO

        payments.forEach { payment ->
            val amt = BigDecimal.valueOf(payment.amount)
            when (payment.type) {
                PaymentRecordType.PAYMENT.name -> grossPaidBd = grossPaidBd.add(amt)
                PaymentRecordType.REFUND.name -> refundedBd = refundedBd.add(amt)
                PaymentRecordType.CREDIT_TRANSFER.name -> creditedBd = creditedBd.add(amt)
            }
        }

        // Net amount currently applied to this invoice
        val netPaidBd = grossPaidBd.subtract(refundedBd).subtract(creditedBd)
            .setScale(2, RoundingMode.HALF_UP)

        val diffBd = totalInvoiceBd.subtract(netPaidBd).setScale(2, RoundingMode.HALF_UP)

        val remainingBd = if (diffBd > BigDecimal.ZERO) diffBd else BigDecimal.ZERO
        val overpaidBd = if (diffBd < BigDecimal.ZERO) diffBd.abs() else BigDecimal.ZERO

        val status = when {
            overpaidBd > BigDecimal("0.01") -> PaymentStatus.OVERPAID_NEEDS_SETTLEMENT
            totalInvoiceBd > BigDecimal.ZERO && remainingBd <= BigDecimal("0.01") -> PaymentStatus.PAID
            netPaidBd > BigDecimal("0.01") -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }

        return InvoiceCalculation(
            invoiceId = invoiceId,
            sectionCalculations = sectionCalcs,
            productSummaries = productSummaries,
            totalBagCount = totalBags,
            totalWeightKg = totalWeightBd.toDouble(),
            totalInvoiceAmount = totalInvoiceBd.toDouble(),
            grossPaidAmount = grossPaidBd.setScale(2, RoundingMode.HALF_UP).toDouble(),
            refundedAmount = refundedBd.setScale(2, RoundingMode.HALF_UP).toDouble(),
            creditTransferredAmount = creditedBd.setScale(2, RoundingMode.HALF_UP).toDouble(),
            netPaidAmount = netPaidBd.toDouble(),
            remainingAmount = remainingBd.toDouble(),
            overpaidAmount = overpaidBd.toDouble(),
            paymentStatus = status
        )
    }

    fun formatNumber(value: Double, maxDecimals: Int = 2): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0." + "#".repeat(maxDecimals)
        return DecimalFormat(pattern, symbols).format(value)
    }

    fun formatWeight(weightKg: Double): String {
        return formatNumber(weightKg, 2)
    }

    fun formatCurrency(amount: Double, symbol: String = "ج.م"): String {
        return "${formatNumber(amount, 2)} $symbol"
    }
}
