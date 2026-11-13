package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppSettingsEntity
import com.example.data.local.FullInvoiceDetails
import com.example.data.local.InvoiceStatus
import com.example.domain.CalculationEngine
import com.example.domain.SectionCalculation
import com.example.domain.WeightGroup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ERP-Grade Tabular PDF Invoice Generator:
 * 1. Top Merchant / Office ERP Header Box
 * 2. Customer & Invoice Metadata Header Table
 * 3. Per-Section Tabular Grid of 5-Weight Columns (drawn as vertical columns side-by-side from right to left,
 *    each with 5 rows + a bottom column total row) followed by the Section Pricing Summary Table.
 * 4. Atomic Page-Break Protection: A 5-weight column block (or section pricing summary / final summary)
 *    is NEVER split across two pages; if the remaining page height cannot fit the entire block intact,
 *    it moves cleanly to the next page.
 */
object PdfInvoiceExporter {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN_H = 28f
    private const val TOP_MARGIN = 30f
    private const val BOTTOM_MARGIN = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)
    private const val COLUMNS_PER_ROW_BLOCK = 4 // 4 vertical weight columns side-by-side per table block

    fun generateInvoicePdfFile(
        context: Context,
        details: FullInvoiceDetails,
        settings: AppSettingsEntity
    ): File {
        val calc = CalculationEngine.calculateInvoice(details)
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = TOP_MARGIN

        val rightEdge = PAGE_WIDTH - MARGIN_H
        val leftEdge = MARGIN_H
        val maxContentY = PAGE_HEIGHT - BOTTOM_MARGIN

        // Paints
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            style = Paint.Style.FILL
        }
        val subHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3EFE6")
            style = Paint.Style.FILL
        }
        val totalCellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
        }
        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val strongBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }

        val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F5E9")
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
        }
        val whiteLeftBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val boldRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val regularRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val boldCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val footerPageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }

        fun drawPageFooter(pNum: Int) {
            canvas.drawText(
                "فاتورة رقم #${details.invoice.invoiceNumber}  —  صفحة $pNum",
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 16f,
                footerPageNumPaint
            )
        }

        fun startNewPage() {
            drawPageFooter(pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = TOP_MARGIN

            // Draw compact continuation header at top of new page
            val contRect = RectF(leftEdge, currentY, rightEdge, currentY + 26f)
            canvas.drawRoundRect(contRect, 4f, 4f, subHeaderBgPaint)
            canvas.drawRoundRect(contRect, 4f, 4f, borderPaint)
            canvas.drawText(
                "${settings.merchantName} — تابع فاتورة شراء رقم #${details.invoice.invoiceNumber} (العميل: ${details.invoice.customerNameSnapshot})",
                rightEdge - 10f,
                currentY + 17f,
                boldRightPaint
            )
            currentY += 36f
        }

        fun ensureAtomicBlockFits(requiredHeight: Float) {
            if (currentY + requiredHeight > maxContentY) {
                startNewPage()
            }
        }

        // =========================================================================
        // 1. ERP HEADER 1: MERCHANT / OFFICE HEADER BOX
        // =========================================================================
        val merchantBoxHeight = 64f
        val merchantRect = RectF(leftEdge, currentY, rightEdge, currentY + merchantBoxHeight)
        canvas.drawRoundRect(merchantRect, 6f, 6f, headerBgPaint)

        canvas.drawText(settings.merchantName, rightEdge - 14f, currentY + 24f, whiteTitlePaint)
        canvas.drawText(
            "الهاتف: ${settings.merchantPhone}   |   العنوان: ${settings.merchantAddress}",
            rightEdge - 14f,
            currentY + 43f,
            whiteSubPaint
        )
        canvas.drawText(
            "مستند رسمي لتوريد ووزن الحبوب والغلال",
            rightEdge - 14f,
            currentY + 57f,
            whiteSubPaint
        )

        val statusAr = if (details.invoice.status == InvoiceStatus.APPROVED.name) "معتمدة" else "مسودة"
        canvas.drawText(
            "فاتورة شراء #${details.invoice.invoiceNumber}",
            leftEdge + 14f,
            currentY + 26f,
            whiteLeftBadgePaint
        )
        canvas.drawText(
            "الحالة: $statusAr  |  ${calc.paymentStatus.labelAr}",
            leftEdge + 14f,
            currentY + 46f,
            whiteSubPaint.apply { textAlign = Paint.Align.LEFT }
        )
        whiteSubPaint.textAlign = Paint.Align.RIGHT
        currentY += merchantBoxHeight + 8f

        // =========================================================================
        // 2. ERP HEADER 2: CUSTOMER & INVOICE METADATA TABLE
        // =========================================================================
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG"))
        val dateStr = dateFormat.format(Date(details.invoice.invoiceDate))

        val custBoxHeight = 48f
        val custRect = RectF(leftEdge, currentY, rightEdge, currentY + custBoxHeight)
        canvas.drawRect(custRect, whiteFillPaint)
        canvas.drawRect(custRect, strongBorderPaint)

        // Vertical divider in the middle of the customer box
        val midX = leftEdge + (CONTENT_WIDTH / 2f)
        canvas.drawLine(midX, currentY, midX, currentY + custBoxHeight, borderPaint)
        // Horizontal divider in the middle of the customer box
        canvas.drawLine(leftEdge, currentY + 24f, rightEdge, currentY + 24f, borderPaint)

        // Right column (Customer info)
        canvas.drawText(
            "اسم العميل / المورد: ${details.invoice.customerNameSnapshot}",
            rightEdge - 10f,
            currentY + 16f,
            boldRightPaint
        )
        canvas.drawText(
            "رقم هاتف العميل: ${details.invoice.customerPhoneSnapshot.ifBlank { "غير مسجل" }}",
            rightEdge - 10f,
            currentY + 40f,
            regularRightPaint
        )

        // Left column (Date & Sections summary)
        canvas.drawText(
            "تاريخ ووقت العملية: $dateStr",
            midX - 10f,
            currentY + 16f,
            boldRightPaint
        )
        canvas.drawText(
            "عدد الأقسام: ${calc.sectionCalculations.size} قسم  |  إجمالي الشكاير: ${calc.totalBagCount} شكارة",
            midX - 10f,
            currentY + 40f,
            regularRightPaint
        )
        currentY += custBoxHeight + 12f

        // =========================================================================
        // 3. PER-SECTION ERP TABLES (5-WEIGHT VERTICAL COLUMNS + SECTION SUMMARY)
        // =========================================================================
        val colBlockHeaderH = 20f
        val weightRowH = 18f
        val colSumFooterH = 22f
        // Atomic height for a row-block of 5-weight columns (1 header + 5 weight rows + 1 sum row = 132pt)
        val singleColumnBlockHeight = colBlockHeaderH + (5 * weightRowH) + colSumFooterH
        val sectionHeaderBarH = 26f
        val sectionPricingTableH = 42f

        calc.sectionCalculations.forEach { secCalc ->
            drawSectionErpTables(
                secCalc = secCalc,
                currencySymbol = settings.currencySymbol,
                leftEdge = leftEdge,
                rightEdge = rightEdge,
                sectionHeaderBarH = sectionHeaderBarH,
                singleColumnBlockHeight = singleColumnBlockHeight,
                colBlockHeaderH = colBlockHeaderH,
                weightRowH = weightRowH,
                colSumFooterH = colSumFooterH,
                sectionPricingTableH = sectionPricingTableH,
                ensureAtomicBlockFits = ::ensureAtomicBlockFits,
                getCanvas = { canvas },
                getCurrentY = { currentY },
                setCurrentY = { currentY = it },
                subHeaderBgPaint = subHeaderBgPaint,
                tableHeaderBgPaint = tableHeaderBgPaint,
                totalCellBgPaint = totalCellBgPaint,
                whiteFillPaint = whiteFillPaint,
                borderPaint = borderPaint,
                strongBorderPaint = strongBorderPaint,
                boldRightPaint = boldRightPaint,
                boldCenterPaint = boldCenterPaint,
                cellCenterPaint = cellCenterPaint
            )
        }

        // =========================================================================
        // 4. FINAL ERP INVOICE TOTALS & SIGNATURES BLOCK (ATOMIC - NEVER SPLIT)
        // =========================================================================
        val finalBlockHeight = 140f
        ensureAtomicBlockFits(finalBlockHeight)

        val finalRect = RectF(leftEdge, currentY, rightEdge, currentY + 82f)
        canvas.drawRect(finalRect, subHeaderBgPaint)
        canvas.drawRect(finalRect, strongBorderPaint)

        // Header strip inside Final Summary
        val finalHeaderRect = RectF(leftEdge, currentY, rightEdge, currentY + 24f)
        canvas.drawRect(finalHeaderRect, headerBgPaint)
        canvas.drawText(
            "الملخص المالي والمحاسبي النهائي للفاتورة",
            rightEdge - 10f,
            currentY + 16f,
            whiteSubPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        )

        // 4 Summary columns from Right to Left:
        // [إجمالي الوزن والشكاير] | [إجمالي قيمة الفاتورة] | [إجمالي المدفوع الصافي] | [المتبقي المستحق]
        val summaryColW = CONTENT_WIDTH / 4f
        val summaryLabels = listOf(
            "إجمالي الوزن (${calc.totalBagCount} شكارة)",
            "إجمالي قيمة الفاتورة",
            "المبلغ المدفوع",
            "المتبقي المستحق"
        )
        val summaryValues = listOf(
            "${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم",
            CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol),
            CalculationEngine.formatCurrency(calc.netPaidAmount, settings.currencySymbol),
            CalculationEngine.formatCurrency(calc.remainingAmount, settings.currencySymbol)
        )

        val labelRowY = currentY + 24f
        val valRowY = currentY + 48f
        canvas.drawLine(leftEdge, valRowY, rightEdge, valRowY, borderPaint)

        for (i in 0 until 4) {
            val colRight = rightEdge - (i * summaryColW)
            val colLeft = colRight - summaryColW
            val centerX = (colLeft + colRight) / 2f
            if (i > 0) {
                canvas.drawLine(colRight, labelRowY, colRight, currentY + 82f, borderPaint)
            }
            canvas.drawText(summaryLabels[i], centerX, labelRowY + 16f, cellCenterPaint)
            canvas.drawText(summaryValues[i], centerX, valRowY + 22f, boldCenterPaint)
        }
        currentY += 94f

        // Notes & Signatures Row
        if (settings.invoiceFooterNotes.isNotBlank()) {
            canvas.drawText(
                "ملاحظات: ${settings.invoiceFooterNotes}",
                rightEdge - 4f,
                currentY + 10f,
                regularRightPaint
            )
            currentY += 20f
        }

        canvas.drawText(
            "توقيع المورد / العميل: ....................................",
            rightEdge - 10f,
            currentY + 16f,
            boldRightPaint
        )
        canvas.drawText(
            "توقيع وختم المكتب / التاجر: ....................................",
            leftEdge + 240f,
            currentY + 16f,
            boldRightPaint
        )

        drawPageFooter(pageNumber)
        pdfDocument.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${details.invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    @Suppress("LongParameterList")
    private fun drawSectionErpTables(
        secCalc: SectionCalculation,
        currencySymbol: String,
        leftEdge: Float,
        rightEdge: Float,
        sectionHeaderBarH: Float,
        singleColumnBlockHeight: Float,
        colBlockHeaderH: Float,
        weightRowH: Float,
        colSumFooterH: Float,
        sectionPricingTableH: Float,
        ensureAtomicBlockFits: (Float) -> Unit,
        getCanvas: () -> Canvas,
        getCurrentY: () -> Float,
        setCurrentY: (Float) -> Unit,
        subHeaderBgPaint: Paint,
        tableHeaderBgPaint: Paint,
        totalCellBgPaint: Paint,
        whiteFillPaint: Paint,
        borderPaint: Paint,
        strongBorderPaint: Paint,
        boldRightPaint: Paint,
        boldCenterPaint: Paint,
        cellCenterPaint: Paint
    ) {
        val sec = secCalc.section

        // Ensure Section Header + at least the first row of 5-weight columns fit together!
        ensureAtomicBlockFits(sectionHeaderBarH + singleColumnBlockHeight + 8f)

        var y = getCurrentY()
        var c = getCanvas()

        // Section Title Bar
        val secBarRect = RectF(leftEdge, y, rightEdge, y + sectionHeaderBarH)
        c.drawRect(secBarRect, subHeaderBgPaint)
        c.drawRect(secBarRect, strongBorderPaint)
        c.drawText(
            "القسم ${sec.sectionOrder}: ${sec.productNameSnapshot}   |   وحدة التسعير: ${sec.unitNameSnapshot} (${CalculationEngine.formatWeight(sec.unitWeightSnapshot)} كجم)   |   سعر الوحدة: ${CalculationEngine.formatCurrency(sec.unitPrice, currencySymbol)}",
            rightEdge - 10f,
            y + 17f,
            boldRightPaint
        )
        y += sectionHeaderBarH
        setCurrentY(y)

        // Chunk the 5-weight groups into horizontal blocks of COLUMNS_PER_ROW_BLOCK (4 columns per row block)
        val groupBlocks: List<List<WeightGroup>> = if (secCalc.groups.isEmpty()) {
            emptyList()
        } else {
            secCalc.groups.chunked(COLUMNS_PER_ROW_BLOCK)
        }

        val colWidth = CONTENT_WIDTH / COLUMNS_PER_ROW_BLOCK

        groupBlocks.forEach { blockGroups ->
            // ATOMIC CHECK: Never split a 5-weight column block across two pages!
            ensureAtomicBlockFits(singleColumnBlockHeight + 4f)
            c = getCanvas()
            y = getCurrentY()

            // Draw the 4 column slots from Right to Left
            blockGroups.forEachIndexed { colSlotIndex, group ->
                val colRight = rightEdge - (colSlotIndex * colWidth)
                val colLeft = colRight - colWidth
                val colCenterX = (colLeft + colRight) / 2f

                // 1. Column Header Cell ("العمود X")
                val headerCell = RectF(colLeft, y, colRight, y + colBlockHeaderH)
                c.drawRect(headerCell, tableHeaderBgPaint)
                c.drawRect(headerCell, borderPaint)
                c.drawText("العمود ${group.groupIndex}", colCenterX, y + 14f, boldCenterPaint)

                // 2. Five Weight Cells Vertically (Rows 1..5)
                for (rowIdx in 0 until CalculationEngine.GROUP_SIZE) {
                    val rowTop = y + colBlockHeaderH + (rowIdx * weightRowH)
                    val rowBottom = rowTop + weightRowH
                    val cellRect = RectF(colLeft, rowTop, colRight, rowBottom)
                    c.drawRect(cellRect, whiteFillPaint)
                    c.drawRect(cellRect, borderPaint)

                    val weightItem = group.weights.getOrNull(rowIdx)
                    if (weightItem != null) {
                        val txt = "(${weightItem.sequenceNumber})  ${CalculationEngine.formatWeight(weightItem.weightKg)} كجم"
                        c.drawText(txt, colCenterX, rowTop + 13f, cellCenterPaint)
                    } else {
                        c.drawText("—", colCenterX, rowTop + 13f, cellCenterPaint)
                    }
                }

                // 3. Column Total Footer Cell ("مجموع العمود")
                val sumTop = y + colBlockHeaderH + (5 * weightRowH)
                val sumBottom = sumTop + colSumFooterH
                val sumRect = RectF(colLeft, sumTop, colRight, sumBottom)
                c.drawRect(sumRect, totalCellBgPaint)
                c.drawRect(sumRect, strongBorderPaint)
                c.drawText(
                    "المجموع: ${CalculationEngine.formatWeight(group.groupTotalKg)} كجم",
                    colCenterX,
                    sumTop + 15f,
                    boldCenterPaint
                )
            }

            y += singleColumnBlockHeight
            setCurrentY(y)
        }

        // Section Pricing & Totals Table Row (Atomic check so it stays intact)
        ensureAtomicBlockFits(sectionPricingTableH + 10f)
        c = getCanvas()
        y = getCurrentY()

        val secPriceRect = RectF(leftEdge, y, rightEdge, y + sectionPricingTableH)
        c.drawRect(secPriceRect, tableHeaderBgPaint)
        c.drawRect(secPriceRect, strongBorderPaint)
        c.drawLine(leftEdge, y + 20f, rightEdge, y + 20f, borderPaint)

        val secCols = 4
        val secColW = CONTENT_WIDTH / secCols
        val secHeaders = listOf(
            "عدد شكاير القسم ${sec.sectionOrder}",
            "إجمالي وزن القسم",
            "سعر الكيلو (${sec.unitNameSnapshot})",
            "إجمالي قيمة القسم ${sec.sectionOrder}"
        )
        val secVals = listOf(
            "${secCalc.bagCount} شكارة",
            "${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم",
            "${CalculationEngine.formatNumber(secCalc.pricePerKg, 3)} $currencySymbol",
            CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, currencySymbol)
        )

        for (i in 0 until secCols) {
            val cRight = rightEdge - (i * secColW)
            val cLeft = cRight - secColW
            val cCenter = (cLeft + cRight) / 2f
            if (i > 0) {
                c.drawLine(cRight, y, cRight, y + sectionPricingTableH, borderPaint)
            }
            c.drawText(secHeaders[i], cCenter, y + 14f, cellCenterPaint)
            c.drawText(secVals[i], cCenter, y + 35f, boldCenterPaint)
        }

        setCurrentY(y + sectionPricingTableH + 14f)
    }

    fun shareInvoicePdf(
        context: Context,
        details: FullInvoiceDetails,
        settings: AppSettingsEntity,
        whatsAppOnly: Boolean = false
    ) {
        try {
            val file = generateInvoicePdfFile(context, details, settings)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val calc = CalculationEngine.calculateInvoice(details)
            val shareText = buildString {
                appendLine("فاتورة شراء حبوب رقم #${details.invoice.invoiceNumber}")
                appendLine("العميل: ${details.invoice.customerNameSnapshot}")
                appendLine("إجمالي الوزن: ${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم (${calc.totalBagCount} شكارة)")
                appendLine("الإجمالي النهائي: ${CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol)}")
                appendLine("المدفوع: ${CalculationEngine.formatCurrency(calc.netPaidAmount, settings.currencySymbol)} | المتبقي: ${CalculationEngine.formatCurrency(calc.remainingAmount, settings.currencySymbol)}")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (whatsAppOnly) {
                    setPackage("com.whatsapp")
                }
            }

            try {
                context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة PDF"))
            } catch (e: Exception) {
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة PDF"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر إنشاء ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun printInvoicePdf(
        context: Context,
        details: FullInvoiceDetails,
        settings: AppSettingsEntity
    ) {
        try {
            val pdfFile = generateInvoicePdfFile(context, details, settings)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val jobName = "Invoice_${details.invoice.invoiceNumber}"

            printManager?.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("invoice_${details.invoice.invoiceNumber}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            FileInputStream(pdfFile).use { input ->
                                FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        }
                    }
                },
                null
            )
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الطابعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
