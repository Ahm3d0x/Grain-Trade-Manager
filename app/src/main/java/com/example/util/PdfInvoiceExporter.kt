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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceExporter {

    fun generateInvoicePdfFile(
        context: Context,
        details: FullInvoiceDetails,
        settings: AppSettingsEntity
    ): File {
        val calc = CalculationEngine.calculateInvoice(details)
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val rightMargin = pageWidth - 36f
        val leftMargin = 36f
        var currentY = 42f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#57534E")
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bodyRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        val bodyLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3EFE6")
            style = Paint.Style.FILL
        }
        val greenBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6D0C4")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > pageHeight - 60f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 42f
            }
        }

        // 1. Merchant Header Box
        val headerRect = RectF(leftMargin, currentY, rightMargin, currentY + 74f)
        canvas.drawRoundRect(headerRect, 8f, 8f, greenBoxPaint)
        canvas.drawRoundRect(headerRect, 8f, 8f, borderPaint)

        canvas.drawText(settings.merchantName, rightMargin - 14f, currentY + 26f, titlePaint)
        canvas.drawText(
            "هاتف: ${settings.merchantPhone}  |  العنوان: ${settings.merchantAddress}",
            rightMargin - 14f,
            currentY + 46f,
            subtitlePaint
        )
        val statusLabel = if (details.invoice.status == InvoiceStatus.APPROVED.name) "فاتورة معتمدة" else "مسودة فاتورة"
        canvas.drawText(
            "فاتورة شراء حبوب وغلال رقم #${details.invoice.invoiceNumber} ($statusLabel)",
            rightMargin - 14f,
            currentY + 64f,
            sectionTitlePaint
        )
        currentY += 88f

        // 2. Customer & Date Row
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar", "EG"))
        val dateStr = dateFormat.format(Date(details.invoice.invoiceDate))

        val infoRect = RectF(leftMargin, currentY, rightMargin, currentY + 48f)
        canvas.drawRoundRect(infoRect, 6f, 6f, boxFillPaint)
        canvas.drawRoundRect(infoRect, 6f, 6f, borderPaint)

        canvas.drawText("العميل: ${details.invoice.customerNameSnapshot}", rightMargin - 12f, currentY + 20f, sectionTitlePaint)
        if (details.invoice.customerPhoneSnapshot.isNotBlank()) {
            canvas.drawText("هاتف العميل: ${details.invoice.customerPhoneSnapshot}", rightMargin - 12f, currentY + 38f, subtitlePaint)
        }
        canvas.drawText("التاريخ: $dateStr", leftMargin + 220f, currentY + 20f, bodyRightPaint)
        canvas.drawText("حالة السداد: ${calc.paymentStatus.labelAr}", leftMargin + 220f, currentY + 38f, bodyRightPaint)
        currentY += 62f

        // 3. Sections & 5-Weight Groups
        calc.sectionCalculations.forEach { secCalc ->
            val sec = secCalc.section
            ensureSpace(95f)

            val secHeaderRect = RectF(leftMargin, currentY, rightMargin, currentY + 32f)
            canvas.drawRoundRect(secHeaderRect, 6f, 6f, greenBoxPaint)
            canvas.drawRoundRect(secHeaderRect, 6f, 6f, borderPaint)

            canvas.drawText(
                "القسم ${sec.sectionOrder}: ${sec.productNameSnapshot} — السعر: ${CalculationEngine.formatNumber(sec.unitPrice)} ${settings.currencySymbol} / ${sec.unitNameSnapshot} (${CalculationEngine.formatWeight(sec.unitWeightSnapshot)} كجم)",
                rightMargin - 10f,
                currentY + 21f,
                sectionTitlePaint
            )
            currentY += 38f

            // Render 5-weight groups summary rows
            secCalc.groups.forEach { group ->
                ensureSpace(24f)
                val weightsListStr = group.weights.joinToString("  |  ") { w ->
                    "(${w.sequenceNumber}) ${CalculationEngine.formatWeight(w.weightKg)} كجم"
                }
                canvas.drawText(
                    "العمود ${group.groupIndex}:  $weightsListStr",
                    rightMargin - 10f,
                    currentY + 14f,
                    bodyRightPaint
                )
                canvas.drawText(
                    "مجموع العمود: ${CalculationEngine.formatWeight(group.groupTotalKg)} كجم",
                    leftMargin + 10f,
                    currentY + 14f,
                    bodyLeftPaint
                )
                currentY += 20f
            }

            // Section summary bar
            ensureSpace(32f)
            val secSumRect = RectF(leftMargin, currentY, rightMargin, currentY + 26f)
            canvas.drawRoundRect(secSumRect, 4f, 4f, boxFillPaint)
            canvas.drawText(
                "إجمالي القسم ${sec.sectionOrder}: ${secCalc.bagCount} شكارة  •  الوزن: ${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم  •  سعر الكيلو: ${CalculationEngine.formatNumber(secCalc.pricePerKg, 3)} ${settings.currencySymbol}",
                rightMargin - 10f,
                currentY + 17f,
                bodyRightPaint
            )
            canvas.drawText(
                "القيمة: ${CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, settings.currencySymbol)}",
                leftMargin + 10f,
                currentY + 17f,
                bodyLeftPaint
            )
            currentY += 38f
        }

        // 4. Overall Invoice Summary Table
        ensureSpace(130f)
        val summaryRect = RectF(leftMargin, currentY, rightMargin, currentY + 105f)
        canvas.drawRoundRect(summaryRect, 8f, 8f, greenBoxPaint)
        canvas.drawRoundRect(summaryRect, 8f, 8f, borderPaint)

        canvas.drawText("ملخص الفاتورة النهائي", rightMargin - 14f, currentY + 22f, titlePaint)
        canvas.drawText(
            "إجمالي عدد الشكاير: ${calc.totalBagCount} شكارة   |   إجمالي الوزن الصافي: ${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم",
            rightMargin - 14f,
            currentY + 44f,
            sectionTitlePaint
        )
        canvas.drawText(
            "إجمالي قيمة الفاتورة: ${CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol)}",
            rightMargin - 14f,
            currentY + 66f,
            sectionTitlePaint
        )
        canvas.drawText(
            "إجمالي المدفوع الصافي: ${CalculationEngine.formatCurrency(calc.netPaidAmount, settings.currencySymbol)}   |   المتبقي المستحق: ${CalculationEngine.formatCurrency(calc.remainingAmount, settings.currencySymbol)}",
            rightMargin - 14f,
            currentY + 88f,
            sectionTitlePaint
        )
        currentY += 120f

        // 5. Footer Notes & Signatures
        ensureSpace(70f)
        if (settings.invoiceFooterNotes.isNotBlank()) {
            canvas.drawText("ملاحظات: ${settings.invoiceFooterNotes}", rightMargin, currentY + 14f, subtitlePaint)
            currentY += 28f
        }
        canvas.drawText("توقيع المستلم / العميل: ............................", rightMargin - 10f, currentY + 24f, bodyRightPaint)
        canvas.drawText("توقيع وختم التاجر: ............................", leftMargin + 220f, currentY + 24f, bodyRightPaint)

        pdfDocument.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${details.invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
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
                // Fallback to general share chooser if WhatsApp is not installed
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
