package com.finsave.core.common.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.finsave.core.common.Constants
import com.finsave.core.common.formatter.IndianNumberFormatter
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PdfExporter @Inject constructor() {

    fun export(context: Context, data: PdfReportData): Result<Unit> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            drawReport(page.canvas, data)
            document.finishPage(page)

            val file = File(context.cacheDir, "finsave_report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = Constants.PDF_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export PDF Summary"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawReport(canvas: Canvas, data: PdfReportData) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 41, 55)
            textSize = 12f
            typeface = Typeface.DEFAULT
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 11f
            typeface = Typeface.DEFAULT
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 1f
        }

        var y = 44f
        canvas.drawText("FinSave - Monthly Report", PAGE_MARGIN, y, titlePaint)
        y += 18f
        canvas.drawText(data.monthYear, PAGE_MARGIN, y, mutedPaint)

        y += 38f
        val columnWidth = (PAGE_WIDTH - (PAGE_MARGIN * 2)) / 3f
        drawSummaryColumn(canvas, "Total Spent", data.totalDebitPaise, PAGE_MARGIN, y, columnWidth, sectionPaint, bodyPaint)
        drawSummaryColumn(canvas, "Total Received", data.totalCreditPaise, PAGE_MARGIN + columnWidth, y, columnWidth, sectionPaint, bodyPaint)
        drawSummaryColumn(
            canvas = canvas,
            label = "Net",
            amountPaise = data.totalCreditPaise - data.totalDebitPaise,
            x = PAGE_MARGIN + (columnWidth * 2),
            y = y,
            width = columnWidth,
            labelPaint = sectionPaint,
            amountPaint = bodyPaint
        )

        y += 48f
        canvas.drawLine(PAGE_MARGIN, y, PAGE_WIDTH - PAGE_MARGIN, y, linePaint)
        y += 28f

        canvas.drawText("Category Breakdown", PAGE_MARGIN, y, sectionPaint)
        y += 22f
        if (data.categoryBreakdown.isEmpty()) {
            canvas.drawText("No expenses recorded this month.", PAGE_MARGIN, y, mutedPaint)
            y += 24f
        } else {
            data.categoryBreakdown.take(10).forEach { row ->
                val percent = "${row.percent.toInt()}%"
                drawClippedText(canvas, "${row.emoji} ${row.name}", PAGE_MARGIN, y, 250f, bodyPaint)
                drawClippedText(canvas, IndianNumberFormatter.format(row.amountPaise), 330f, y, 110f, bodyPaint)
                canvas.drawText(percent, 470f, y, mutedPaint)
                y += 18f
            }
        }

        y += 10f
        canvas.drawLine(PAGE_MARGIN, y, PAGE_WIDTH - PAGE_MARGIN, y, linePaint)
        y += 28f

        canvas.drawText("Top 5 Merchants", PAGE_MARGIN, y, sectionPaint)
        y += 22f
        if (data.topMerchants.isEmpty()) {
            canvas.drawText("No merchant spending recorded this month.", PAGE_MARGIN, y, mutedPaint)
            y += 24f
        } else {
            data.topMerchants.take(5).forEach { row ->
                drawClippedText(canvas, row.name, PAGE_MARGIN, y, 260f, bodyPaint)
                drawClippedText(canvas, IndianNumberFormatter.format(row.amountPaise), 330f, y, 110f, bodyPaint)
                canvas.drawText("${row.count} tx", 470f, y, mutedPaint)
                y += 18f
            }
        }

        canvas.drawText(
            "Generated by FinSave - Your data never leaves your device",
            PAGE_MARGIN,
            PAGE_HEIGHT - 36f,
            mutedPaint
        )
    }

    private fun drawSummaryColumn(
        canvas: Canvas,
        label: String,
        amountPaise: Long,
        x: Float,
        y: Float,
        width: Float,
        labelPaint: Paint,
        amountPaint: Paint
    ) {
        drawClippedText(canvas, label, x, y, width - 8f, labelPaint)
        drawClippedText(canvas, IndianNumberFormatter.format(amountPaise), x, y + 20f, width - 8f, amountPaint)
    }

    private fun drawClippedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, y, paint)
            return
        }

        var clipped = text
        while (clipped.isNotEmpty() && paint.measureText("$clipped...") > maxWidth) {
            clipped = clipped.dropLast(1)
        }
        canvas.drawText("$clipped...", x, y, paint)
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val PAGE_MARGIN = 40f
    }
}

data class PdfReportData(
    val monthYear: String,
    val totalDebitPaise: Long,
    val totalCreditPaise: Long,
    val categoryBreakdown: List<CategoryPdfRow>,
    val topMerchants: List<MerchantPdfRow>
)

data class CategoryPdfRow(
    val emoji: String,
    val name: String,
    val amountPaise: Long,
    val percent: Float
)

data class MerchantPdfRow(
    val name: String,
    val amountPaise: Long,
    val count: Int
)
