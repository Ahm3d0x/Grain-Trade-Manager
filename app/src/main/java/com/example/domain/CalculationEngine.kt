package com.example.domain

import com.example.data.local.FullInvoiceDetails
import com.example.data.local.InventoryAdjustmentEntity
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
    val isSaleInvoice: Boolean,
    val sectionCalculations: List<SectionCalculation>,
    val productSummaries: List<ProductWeightSummary>,
    val totalBagCount: Int,
    val totalWeightKg: Double,
    val totalInvoiceAmount: Double,
    val saleExpenses: Double,
    val costPerKgSnapshot: Double,
    val estimatedCostOfGoods: Double,
    val directMarginProfit: Double,
    val netProfitAfterExpenses: Double,
    val grossPaidAmount: Double,
    val refundedAmount: Double,
    val creditTransferredAmount: Double,
    val netPaidAmount: Double,
    val remainingAmount: Double,
    val overpaidAmount: Double,
    val paymentStatus: PaymentStatus
) {
    fun getEffectiveProfit(mode: ProfitCalculationMode): Double {
        return when (mode) {
            ProfitCalculationMode.WITH_EXPENSES -> netProfitAfterExpenses
            ProfitCalculationMode.DIRECT_MARGIN -> directMarginProfit
        }
    }
}

data class ProductWarehouseStock(
    val productId: Long,
    val productName: String,
    val standardUnitName: String,
    val standardUnitWeightKg: Double,
    val purchasedWeightKg: Double,
    val purchasedTotalCost: Double,
    val soldWeightKg: Double,
    val soldTotalRevenue: Double,
    val soldTotalCostOfGoods: Double,
    val soldTotalExpenses: Double,
    val manualShrinkageKg: Double,
    val batchZeroOutShrinkageKg: Double,
    val totalShrinkageKg: Double,
    val shrinkagePercentage: Double,
    val availableStockKg: Double,
    val availableStockTons: Double,
    val availableStandardUnits: Double,
    val avgPurchaseCostPerKg: Double,
    val avgPurchaseCostPerTon: Double,
    val avgPurchaseCostPerStandardUnit: Double,
    val currentStockValuation: Double,
    val directProfitFromSales: Double,
    val netProfitAfterExpensesFromSales: Double
) {
    fun getProfitByMode(mode: ProfitCalculationMode): Double {
        return when (mode) {
            ProfitCalculationMode.WITH_EXPENSES -> netProfitAfterExpensesFromSales
            ProfitCalculationMode.DIRECT_MARGIN -> directProfitFromSales
        }
    }
}

data class WarehouseSummary(
    val productStocks: List<ProductWarehouseStock>,
    val totalAvailableWeightKg: Double,
    val totalStockValuation: Double,
    val totalPurchasedWeightKg: Double,
    val totalSoldWeightKg: Double,
    val totalShrinkageWeightKg: Double,
    val overallShrinkagePercent: Double,
    val totalSalesRevenue: Double,
    val totalSalesCostOfGoods: Double,
    val totalSalesExpenses: Double,
    val totalDirectProfit: Double,
    val totalNetProfitAfterExpenses: Double
) {
    fun getTotalProfitByMode(mode: ProfitCalculationMode): Double {
        return when (mode) {
            ProfitCalculationMode.WITH_EXPENSES -> totalNetProfitAfterExpenses
            ProfitCalculationMode.DIRECT_MARGIN -> totalDirectProfit
        }
    }
}

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
            }.setScale(0, RoundingMode.HALF_UP).toDouble()

            WeightGroup(
                groupIndex = index + 1,
                weights = chunk,
                groupTotalKg = groupSum,
                remainingSlots = (GROUP_SIZE - chunk.size).coerceAtLeast(0)
            )
        }

        val totalWeightBd = groups.fold(BigDecimal.ZERO) { acc, group ->
            acc.add(BigDecimal.valueOf(group.groupTotalKg))
        }.setScale(0, RoundingMode.HALF_UP)

        val unitWeightBd = if (section.unitWeightSnapshot > 0.0) {
            BigDecimal.valueOf(section.unitWeightSnapshot)
        } else {
            BigDecimal.ONE
        }
        val unitPriceBd = BigDecimal.valueOf(section.unitPrice)

        // Price per kg = P_unit / W_unit
        val pricePerKgBd = unitPriceBd.divide(unitWeightBd, 4, RoundingMode.HALF_UP)

        // Total section value = W_section * P_unit / W_unit (whole currency units, no piastres)
        val sectionTotalBd = totalWeightBd.multiply(unitPriceBd)
            .divide(unitWeightBd, 0, RoundingMode.HALF_UP)

        return SectionCalculation(
            section = section,
            sortedWeights = sorted,
            groups = groups,
            bagCount = sorted.size,
            totalWeightKg = totalWeightBd.toDouble(),
            pricePerKg = pricePerKgBd.toDouble(),
            sectionTotalAmount = sectionTotalBd.toDouble()
        )
    }

    /**
     * Calculates the entire invoice across all independent sections,
     * aggregates totals per grain type, evaluates payment/overpayment status,
     * and computes sale profit metrics when the invoice is a SALE invoice.
     */
    fun calculateInvoice(details: FullInvoiceDetails): InvoiceCalculation {
        val orderedSections = details.sections.sortedBy { it.section.sectionOrder }
        val sectionCalcs = orderedSections.map { calculateSection(it) }

        val totalBags = sectionCalcs.sumOf { it.bagCount }
        val totalWeightBd = sectionCalcs.fold(BigDecimal.ZERO) { acc, sec ->
            acc.add(BigDecimal.valueOf(sec.totalWeightKg))
        }.setScale(0, RoundingMode.HALF_UP)

        val totalInvoiceBd = sectionCalcs.fold(BigDecimal.ZERO) { acc, sec ->
            acc.add(BigDecimal.valueOf(sec.sectionTotalAmount))
        }.setScale(0, RoundingMode.HALF_UP)

        val productSummaries = sectionCalcs
            .groupBy { it.section.productNameSnapshot }
            .map { (productName, secs) ->
                ProductWeightSummary(
                    productName = productName,
                    bagCount = secs.sumOf { it.bagCount },
                    totalWeightKg = secs.fold(BigDecimal.ZERO) { acc, s ->
                        acc.add(BigDecimal.valueOf(s.totalWeightKg))
                    }.setScale(0, RoundingMode.HALF_UP).toDouble(),
                    totalAmount = secs.fold(BigDecimal.ZERO) { acc, s ->
                        acc.add(BigDecimal.valueOf(s.sectionTotalAmount))
                    }.setScale(0, RoundingMode.HALF_UP).toDouble()
                )
            }

        val isSale = details.invoice.invoiceType == InvoiceType.SALE.name
        val saleExpensesBd = BigDecimal.valueOf(details.invoice.saleExpenses.coerceAtLeast(0.0))
            .setScale(0, RoundingMode.HALF_UP)
        val costPerKgBd = BigDecimal.valueOf(details.invoice.costPerKgSnapshot.coerceAtLeast(0.0))
        val estimatedCostBd = if (isSale) {
            totalWeightBd.multiply(costPerKgBd).setScale(0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val directProfitBd = if (isSale) {
            totalInvoiceBd.subtract(estimatedCostBd).setScale(0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val netProfitBd = if (isSale) {
            directProfitBd.subtract(saleExpensesBd).setScale(0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return evaluatePayments(
            invoiceId = details.invoice.id,
            isSaleInvoice = isSale,
            sectionCalcs = sectionCalcs,
            productSummaries = productSummaries,
            totalBags = totalBags,
            totalWeightBd = totalWeightBd,
            totalInvoiceBd = totalInvoiceBd,
            saleExpenses = saleExpensesBd.toDouble(),
            costPerKgSnapshot = details.invoice.costPerKgSnapshot,
            estimatedCostOfGoods = estimatedCostBd.toDouble(),
            directMarginProfit = directProfitBd.toDouble(),
            netProfitAfterExpenses = netProfitBd.toDouble(),
            payments = details.payments
        )
    }

    @Suppress("LongParameterList")
    private fun evaluatePayments(
        invoiceId: Long,
        isSaleInvoice: Boolean,
        sectionCalcs: List<SectionCalculation>,
        productSummaries: List<ProductWeightSummary>,
        totalBags: Int,
        totalWeightBd: BigDecimal,
        totalInvoiceBd: BigDecimal,
        saleExpenses: Double,
        costPerKgSnapshot: Double,
        estimatedCostOfGoods: Double,
        directMarginProfit: Double,
        netProfitAfterExpenses: Double,
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

        // Net amount currently applied to this invoice (whole currency units, no piastres)
        val netPaidBd = grossPaidBd.subtract(refundedBd).subtract(creditedBd)
            .setScale(0, RoundingMode.HALF_UP)

        val diffBd = totalInvoiceBd.subtract(netPaidBd).setScale(0, RoundingMode.HALF_UP)

        val remainingBd = if (diffBd > BigDecimal.ZERO) diffBd else BigDecimal.ZERO
        val overpaidBd = if (diffBd < BigDecimal.ZERO) diffBd.abs() else BigDecimal.ZERO

        val status = when {
            overpaidBd >= BigDecimal.ONE -> PaymentStatus.OVERPAID_NEEDS_SETTLEMENT
            totalInvoiceBd > BigDecimal.ZERO && remainingBd == BigDecimal.ZERO -> PaymentStatus.PAID
            netPaidBd > BigDecimal.ZERO -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }

        return InvoiceCalculation(
            invoiceId = invoiceId,
            isSaleInvoice = isSaleInvoice,
            sectionCalculations = sectionCalcs,
            productSummaries = productSummaries,
            totalBagCount = totalBags,
            totalWeightKg = totalWeightBd.toDouble(),
            totalInvoiceAmount = totalInvoiceBd.toDouble(),
            saleExpenses = saleExpenses,
            costPerKgSnapshot = costPerKgSnapshot,
            estimatedCostOfGoods = estimatedCostOfGoods,
            directMarginProfit = directMarginProfit,
            netProfitAfterExpenses = netProfitAfterExpenses,
            grossPaidAmount = grossPaidBd.setScale(0, RoundingMode.HALF_UP).toDouble(),
            refundedAmount = refundedBd.setScale(0, RoundingMode.HALF_UP).toDouble(),
            creditTransferredAmount = creditedBd.setScale(0, RoundingMode.HALF_UP).toDouble(),
            netPaidAmount = netPaidBd.toDouble(),
            remainingAmount = remainingBd.toDouble(),
            overpaidAmount = overpaidBd.toDouble(),
            paymentStatus = status
        )
    }

    /**
     * Calculates live warehouse stock, weighted average purchase cost, shrinkage/dryness loss,
     * and realized sales profits across all grain products.
     */
    fun calculateWarehouseSummary(
        products: List<ProductEntity>,
        pricingUnits: List<PricingUnitEntity>,
        allInvoices: List<Pair<FullInvoiceDetails, InvoiceCalculation>>,
        adjustments: List<InventoryAdjustmentEntity>
    ): WarehouseSummary {
        // Only approved invoices affect physical warehouse inventory & financial profits
        val approvedInvoices = allInvoices.filter {
            it.first.invoice.status == InvoiceStatus.APPROVED.name
        }

        val purchaseInvoices = approvedInvoices.filter { !it.second.isSaleInvoice }
        val saleInvoices = approvedInvoices.filter { it.second.isSaleInvoice }

        // Gather all distinct product names from defined products + any historical invoice sections
        val productMapByName = linkedMapOf<String, ProductEntity>()
        products.forEach { p ->
            productMapByName[p.name.trim()] = p
        }
        approvedInvoices.forEach { (details, _) ->
            details.sections.forEach { sec ->
                val name = sec.section.productNameSnapshot.trim()
                if (name.isNotBlank() && !productMapByName.containsKey(name)) {
                    productMapByName[name] = ProductEntity(
                        id = sec.section.productId,
                        name = name,
                        isActive = true
                    )
                }
            }
        }
        adjustments.forEach { adj ->
            val name = adj.productNameSnapshot.trim()
            if (name.isNotBlank() && !productMapByName.containsKey(name)) {
                productMapByName[name] = ProductEntity(
                    id = adj.productId ?: 0L,
                    name = name,
                    isActive = true
                )
            }
        }

        val stockRows = productMapByName.values.map { prod ->
            val pName = prod.name.trim()

            // Find standard pricing unit for this crop (e.g., أردب قمح = 150 kg), or fallback to طن (1000 kg)
            val linkedUnit = pricingUnits.firstOrNull {
                it.linkedProductId == prod.id || it.linkedProductName?.trim() == pName
            } ?: pricingUnits.firstOrNull { it.weightKg == 1000.0 }
                ?: PricingUnitEntity(name = "طن", weightKg = 1000.0)

            // 1. Purchased weight & cost for this product
            var purchasedKg = 0.0
            var purchasedCost = 0.0
            purchaseInvoices.forEach { (_, calc) ->
                calc.sectionCalculations
                    .filter { it.section.productNameSnapshot.trim() == pName }
                    .forEach { secCalc ->
                        purchasedKg += secCalc.totalWeightKg
                        purchasedCost += secCalc.sectionTotalAmount
                    }
            }

            val avgCostPerKg = if (purchasedKg > 0.0) purchasedCost / purchasedKg else 0.0

            // 2. Sold weight, revenue, cost of goods, and expenses for this product
            var soldKg = 0.0
            var soldRevenue = 0.0
            var soldCostOfGoods = 0.0
            var soldExpenses = 0.0

            saleInvoices.forEach { (details, calc) ->
                val matchingSections = calc.sectionCalculations.filter {
                    it.section.productNameSnapshot.trim() == pName
                }
                if (matchingSections.isNotEmpty()) {
                    val productWeightInInv = matchingSections.sumOf { it.totalWeightKg }
                    val productRevInInv = matchingSections.sumOf { it.sectionTotalAmount }
                    soldKg += productWeightInInv
                    soldRevenue += productRevInInv

                    // Use snapshot cost if recorded, or current weighted average purchase cost
                    val effectiveCostPerKg = if (details.invoice.costPerKgSnapshot > 0.0) {
                        details.invoice.costPerKgSnapshot
                    } else {
                        avgCostPerKg
                    }
                    soldCostOfGoods += BigDecimal.valueOf(productWeightInInv)
                        .multiply(BigDecimal.valueOf(effectiveCostPerKg))
                        .setScale(0, RoundingMode.HALF_UP)
                        .toDouble()

                    // Allocate invoice sale expenses proportionally by weight
                    val weightRatio = if (calc.totalWeightKg > 0.0) {
                        productWeightInInv / calc.totalWeightKg
                    } else 1.0
                    soldExpenses += details.invoice.saleExpenses * weightRatio
                }
            }

            // 3. Shrinkage / Dryness Adjustments for this product
            val prodAdjustments = adjustments.filter {
                it.productNameSnapshot.trim() == pName || (prod.id > 0 && it.productId == prod.id)
            }
            val manualShrinkageKg = prodAdjustments
                .filter { it.adjustmentType == ShrinkageAdjustmentType.MANUAL_SHRINKAGE.name }
                .sumOf { it.weightLossKg }
            val batchZeroOutKg = prodAdjustments
                .filter { it.adjustmentType == ShrinkageAdjustmentType.BATCH_ZERO_OUT.name }
                .sumOf { it.weightLossKg }
            val totalShrinkageKg = manualShrinkageKg + batchZeroOutKg

            val shrinkagePct = if (purchasedKg > 0.0) {
                (totalShrinkageKg / purchasedKg) * 100.0
            } else 0.0

            val availableKg = (purchasedKg - soldKg - totalShrinkageKg).coerceAtLeast(0.0)
            val availableTons = availableKg / 1000.0
            val availableStdUnits = if (linkedUnit.weightKg > 0.0) {
                availableKg / linkedUnit.weightKg
            } else 0.0

            val currentValuation = BigDecimal.valueOf(availableKg)
                .multiply(BigDecimal.valueOf(avgCostPerKg))
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            val directProfit = BigDecimal.valueOf(soldRevenue - soldCostOfGoods)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()
            val netProfitAfterExpenses = BigDecimal.valueOf(soldRevenue - soldCostOfGoods - soldExpenses)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            ProductWarehouseStock(
                productId = prod.id,
                productName = prod.name,
                standardUnitName = linkedUnit.name,
                standardUnitWeightKg = linkedUnit.weightKg,
                purchasedWeightKg = purchasedKg,
                purchasedTotalCost = purchasedCost,
                soldWeightKg = soldKg,
                soldTotalRevenue = soldRevenue,
                soldTotalCostOfGoods = soldCostOfGoods,
                soldTotalExpenses = soldExpenses,
                manualShrinkageKg = manualShrinkageKg,
                batchZeroOutShrinkageKg = batchZeroOutKg,
                totalShrinkageKg = totalShrinkageKg,
                shrinkagePercentage = shrinkagePct,
                availableStockKg = availableKg,
                availableStockTons = availableTons,
                availableStandardUnits = availableStdUnits,
                avgPurchaseCostPerKg = avgCostPerKg,
                avgPurchaseCostPerTon = avgCostPerKg * 1000.0,
                avgPurchaseCostPerStandardUnit = avgCostPerKg * linkedUnit.weightKg,
                currentStockValuation = currentValuation,
                directProfitFromSales = directProfit,
                netProfitAfterExpensesFromSales = netProfitAfterExpenses
            )
        }.sortedWith(
            compareByDescending<ProductWarehouseStock> { it.availableStockKg }
                .thenByDescending { it.purchasedWeightKg }
        )

        val totalAvailKg = stockRows.sumOf { it.availableStockKg }
        val totalValuation = stockRows.sumOf { it.currentStockValuation }
        val totalPurchasedKg = stockRows.sumOf { it.purchasedWeightKg }
        val totalSoldKg = stockRows.sumOf { it.soldWeightKg }
        val totalShrinkageKg = stockRows.sumOf { it.totalShrinkageKg }
        val overallShrinkagePct = if (totalPurchasedKg > 0.0) {
            (totalShrinkageKg / totalPurchasedKg) * 100.0
        } else 0.0

        val totalSalesRev = stockRows.sumOf { it.soldTotalRevenue }
        val totalSalesCost = stockRows.sumOf { it.soldTotalCostOfGoods }
        val totalSalesExp = stockRows.sumOf { it.soldTotalExpenses }
        val totalDirectProfit = stockRows.sumOf { it.directProfitFromSales }
        val totalNetProfit = stockRows.sumOf { it.netProfitAfterExpensesFromSales }

        return WarehouseSummary(
            productStocks = stockRows,
            totalAvailableWeightKg = totalAvailKg,
            totalStockValuation = totalValuation,
            totalPurchasedWeightKg = totalPurchasedKg,
            totalSoldWeightKg = totalSoldKg,
            totalShrinkageWeightKg = totalShrinkageKg,
            overallShrinkagePercent = overallShrinkagePct,
            totalSalesRevenue = totalSalesRev,
            totalSalesCostOfGoods = totalSalesCost,
            totalSalesExpenses = totalSalesExp,
            totalDirectProfit = totalDirectProfit,
            totalNetProfitAfterExpenses = totalNetProfit
        )
    }

    fun formatNumber(value: Double, maxDecimals: Int = 0): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val rounded = if (maxDecimals <= 0) {
            BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toDouble()
        } else {
            BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP).toDouble()
        }
        val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0." + "#".repeat(maxDecimals)
        return DecimalFormat(pattern, symbols).format(rounded)
    }

    fun formatPlainInt(value: Double): String {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toLong().toString()
    }

    fun formatWeight(weightKg: Double): String {
        return formatNumber(weightKg, 0)
    }

    fun formatCurrency(amount: Double, symbol: String = "ج.م"): String {
        return "${formatNumber(amount, 0)} $symbol"
    }
}
