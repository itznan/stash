package com.nan.stash.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.nan.stash.data.backup.BackupManager
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfStatementGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateStatement(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String
    ): File {
        val document = PdfDocument()
        try {
            val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
            val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)
            val generatedDateStr = DateUtils.formatPdfDateTime(System.currentTimeMillis())

            val balances = AnalyticsCalculator.calculateWalletBalances(transactions)
            val totalIncome = balances.totalIncome
            val totalExpense = balances.totalExpense
            val netSavings = balances.netBalance
            val onlineBal = balances.onlineBalance
            val cashBal = balances.cashBalance

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeaderAndSummary(c: Canvas) {
            // Header Title
            paintText.color = Color.rgb(30, 41, 59)
            paintText.textSize = 22f
            paintText.isFakeBoldText = true
            c.drawText("STASH", MARGIN, 50f, paintText)

            paintText.textSize = 10f
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(100, 116, 139)
            c.drawText("Personal Financial Statement • 100% Offline & Private", MARGIN, 65f, paintText)

            paintText.textAlign = Paint.Align.RIGHT
            c.drawText("Generated: $generatedDateStr", PAGE_WIDTH - MARGIN, 50f, paintText)
            c.drawText("Records: ${transactions.size}", PAGE_WIDTH - MARGIN, 65f, paintText)
            paintText.textAlign = Paint.Align.LEFT

            // Summary Card
            paintBg.color = Color.rgb(241, 245, 249)
            val summaryRect = RectF(MARGIN, 80f, PAGE_WIDTH - MARGIN, 150f)
            c.drawRoundRect(summaryRect, 10f, 10f, paintBg)

            val colW = (PAGE_WIDTH - 2 * MARGIN) / 4f

            // Total Income
            paintText.textSize = 9f
            paintText.color = Color.rgb(100, 116, 139)
            c.drawText("TOTAL INCOME", MARGIN + 14f, 102f, paintText)
            paintText.textSize = 13f
            paintText.isFakeBoldText = true
            paintText.color = Color.rgb(22, 163, 74)
            c.drawText("+$currencySymbol${String.format(Locale.US, "%.2f", totalIncome)}", MARGIN + 14f, 122f, paintText)

            // Total Expense
            paintText.textSize = 9f
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(100, 116, 139)
            c.drawText("TOTAL EXPENSE", MARGIN + colW + 10f, 102f, paintText)
            paintText.textSize = 13f
            paintText.isFakeBoldText = true
            paintText.color = Color.rgb(220, 38, 38)
            c.drawText("-$currencySymbol${String.format(Locale.US, "%.2f", totalExpense)}", MARGIN + colW + 10f, 122f, paintText)

            // Net Savings
            paintText.textSize = 9f
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(100, 116, 139)
            c.drawText("NET SAVINGS", MARGIN + 2 * colW + 10f, 102f, paintText)
            paintText.textSize = 13f
            paintText.isFakeBoldText = true
            paintText.color = if (netSavings >= 0) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            c.drawText(AnalyticsCalculator.formatSignedCurrency(netSavings, currencySymbol, showPlus = true), MARGIN + 2 * colW + 10f, 122f, paintText)

            // Wallet Split
            paintText.textSize = 9f
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(100, 116, 139)
            c.drawText("BALANCES (ONLINE / CASH)", MARGIN + 3 * colW + 10f, 102f, paintText)
            paintText.textSize = 10f
            paintText.isFakeBoldText = true
            paintText.color = Color.rgb(37, 99, 235)
            c.drawText("Bank: ${AnalyticsCalculator.formatSignedCurrency(onlineBal, currencySymbol)}", MARGIN + 3 * colW + 10f, 120f, paintText)
            paintText.color = Color.rgb(5, 150, 105)
            c.drawText("Cash: ${AnalyticsCalculator.formatSignedCurrency(cashBal, currencySymbol)}", MARGIN + 3 * colW + 10f, 136f, paintText)
        }

        fun drawTableHeader(c: Canvas, y: Float) {
            paintBg.color = Color.rgb(226, 232, 240)
            val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f)
            c.drawRect(headerRect, paintBg)

            paintText.textSize = 9f
            paintText.isFakeBoldText = true
            paintText.color = Color.rgb(51, 65, 85)
            c.drawText("DATE", MARGIN + 6f, y + 14f, paintText)
            c.drawText("TYPE", MARGIN + 75f, y + 14f, paintText)
            c.drawText("CATEGORY", MARGIN + 140f, y + 14f, paintText)
            c.drawText("WALLET", MARGIN + 245f, y + 14f, paintText)
            c.drawText("NOTE", MARGIN + 315f, y + 14f, paintText)
            paintText.textAlign = Paint.Align.RIGHT
            c.drawText("AMOUNT", PAGE_WIDTH - MARGIN - 6f, y + 14f, paintText)
            paintText.textAlign = Paint.Align.LEFT
        }

        drawHeaderAndSummary(canvas)
        var currentY = 165f
        drawTableHeader(canvas, currentY)
        currentY += 24f

        val sorted = transactions.sortedByDescending { it.timestamp }
        val rowHeight = 20f

        for ((index, t) in sorted.withIndex()) {
            if (currentY + rowHeight > PAGE_HEIGHT - 45f) {
                // Draw footer for current page
                paintText.textSize = 8f
                paintText.color = Color.rgb(148, 163, 184)
                canvas.drawLine(MARGIN, PAGE_HEIGHT - 35f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 35f, paintText)
                canvas.drawText("Stash Offline Ledger • Page $pageNumber", MARGIN, PAGE_HEIGHT - 22f, paintText)

                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                currentY = 40f
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            // Alternating zebra stripe background
            if (index % 2 == 1) {
                paintBg.color = Color.rgb(248, 250, 252)
                canvas.drawRect(RectF(MARGIN, currentY - 2f, PAGE_WIDTH - MARGIN, currentY + rowHeight - 2f), paintBg)
            }

            paintText.textSize = 8.5f
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(71, 85, 105)

            // Date
            canvas.drawText(DateUtils.formatDateKey(t.timestamp), MARGIN + 6f, currentY + 12f, paintText)

            // Type
            paintText.isFakeBoldText = true
            when (t.type) {
                TransactionType.INCOME -> paintText.color = Color.rgb(22, 163, 74)
                TransactionType.EXPENSE -> paintText.color = Color.rgb(220, 38, 38)
                TransactionType.TRANSFER -> paintText.color = Color.rgb(37, 99, 235)
                TransactionType.LEND -> paintText.color = Color.rgb(234, 88, 12)
                TransactionType.REPAYMENT -> paintText.color = Color.rgb(5, 150, 105)
            }
            canvas.drawText(t.type.name, MARGIN + 75f, currentY + 12f, paintText)

            // Category (Tag)
            paintText.isFakeBoldText = false
            paintText.color = Color.rgb(51, 65, 85)
            val cleanTag = if (t.tag.length > 18) t.tag.substring(0, 16) + "…" else t.tag
            canvas.drawText(cleanTag, MARGIN + 140f, currentY + 12f, paintText)

            // Wallet
            paintText.color = if (t.paymentMode == PaymentMode.ONLINE) Color.rgb(37, 99, 235) else Color.rgb(5, 150, 105)
            canvas.drawText(t.paymentMode.name, MARGIN + 245f, currentY + 12f, paintText)

            // Note (truncate if long)
            paintText.color = Color.rgb(100, 116, 139)
            val cleanNote = t.note.replace("\n", " ")
            val truncatedNote = if (cleanNote.length > 28) cleanNote.substring(0, 26) + "…" else cleanNote
            canvas.drawText(truncatedNote, MARGIN + 315f, currentY + 12f, paintText)

            // Amount
            paintText.isFakeBoldText = true
            when (t.type) {
                TransactionType.INCOME, TransactionType.REPAYMENT -> paintText.color = Color.rgb(22, 163, 74)
                TransactionType.EXPENSE, TransactionType.LEND -> paintText.color = Color.rgb(220, 38, 38)
                TransactionType.TRANSFER -> paintText.color = Color.rgb(37, 99, 235)
            }
            paintText.textAlign = Paint.Align.RIGHT
            val prefix = if (t.type == TransactionType.INCOME || t.type == TransactionType.REPAYMENT) "+" else if (t.type == TransactionType.EXPENSE || t.type == TransactionType.LEND) "-" else ""
            canvas.drawText("$prefix$currencySymbol${String.format(Locale.US, "%.2f", t.amount)}", PAGE_WIDTH - MARGIN - 6f, currentY + 12f, paintText)
            paintText.textAlign = Paint.Align.LEFT

            currentY += rowHeight
        }

        // Final page footer
        paintText.textSize = 8f
        paintText.isFakeBoldText = false
        paintText.color = Color.rgb(148, 163, 184)
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 35f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 35f, paintText)
        canvas.drawText("Stash Offline Ledger • Page $pageNumber", MARGIN, PAGE_HEIGHT - 22f, paintText)

        document.finishPage(page)

        val filename = "stash_statement_${DateUtils.formatFileTimestamp()}.pdf"
        val outputFile = BackupManager.createShareFile(context, filename)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        return outputFile
    } finally {
        document.close()
    }
}
}
