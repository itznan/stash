package com.nan.stash.data.backup

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class RestoredBackupData(
    val transactions: List<TransactionEntity>,
    val loans: List<LoanEntity>,
    val subscriptions: List<SubscriptionEntity>
)

object BackupManager {

    private const val SHARE_CACHE_TTL_MS = 60 * 60 * 1000L

    internal fun createShareFile(context: Context, filename: String): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val cutoff = System.currentTimeMillis() - SHARE_CACHE_TTL_MS
        dir.listFiles()?.forEach { existing ->
            if (existing.lastModified() < cutoff) existing.delete()
        }
        return File(dir, filename)
    }

    fun buildCsvContent(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,PaymentMode,Note,Date,Time\n")
        for (t in transactions) {
            val cleanNote = t.note.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
            val cleanTag = t.tag.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
            sb.append("${t.id},${t.type.name},${String.format(Locale.US, "%.2f", t.amount)},\"$cleanTag\",${t.paymentMode.name},\"$cleanNote\",${DateUtils.formatDateKey(t.timestamp)},${DateUtils.formatTime(t.timestamp)}\n")
        }
        return sb.toString()
    }

    fun exportToCsv(context: Context, transactions: List<TransactionEntity>): File {
        val filename = "stash_transactions_${DateUtils.formatFileTimestamp()}.csv"
        return saveFileToStorage(context, filename, buildCsvContent(transactions))
    }

    fun buildBackupJsonContent(
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        subscriptions: List<SubscriptionEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("appName", "Stash")
        root.put("timestamp", System.currentTimeMillis())

        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amount", t.amount)
            obj.put("tag", t.tag)
            obj.put("note", t.note)
            obj.put("timestamp", t.timestamp)
            obj.put("paymentMode", t.paymentMode.name)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val loanArray = JSONArray()
        for (l in loans) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("personName", l.personName)
            obj.put("phoneNumber", l.phoneNumber)
            obj.put("totalAmount", l.totalAmount)
            obj.put("remainingAmount", l.remainingAmount)
            obj.put("reason", l.reason)
            obj.put("lendDate", l.lendDate)
            if (l.dueDate != null) {
                obj.put("dueDate", l.dueDate)
            }
            obj.put("initialPaymentMode", l.initialPaymentMode.name)
            obj.put("isSettled", l.isSettled)
            loanArray.put(obj)
        }
        root.put("loans", loanArray)

        val subArray = JSONArray()
        for (s in subscriptions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("amount", s.amount)
            obj.put("category", s.category)
            obj.put("paymentMode", s.paymentMode.name)
            obj.put("billingCycle", s.billingCycle)
            obj.put("dueDay", s.dueDay)
            obj.put("startDate", s.startDate)
            obj.put("isActive", s.isActive)
            if (s.lastPaidDate != null) {
                obj.put("lastPaidDate", s.lastPaidDate)
            }
            subArray.put(obj)
        }
        root.put("subscriptions", subArray)

        return root.toString(2)
    }

    fun createBackupJson(
        context: Context,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        subscriptions: List<SubscriptionEntity>
    ): File {
        val filename = "stash_backup_${DateUtils.formatFileTimestamp()}.json"
        return saveFileToStorage(context, filename, buildBackupJsonContent(transactions, loans, subscriptions))
    }

    private fun requirePositiveAmount(amount: Double, label: String): Double {
        if (!amount.isFinite() || amount <= 0.0) {
            throw IllegalArgumentException("Invalid $label amount: $amount")
        }
        return amount
    }

    fun parseBackupJson(jsonString: String): RestoredBackupData {
        val root = JSONObject(jsonString)
        val hasValidHeader = root.optString("appName") == "Stash" || root.has("version")
        val hasKnownKeys = root.has("transactions") || root.has("loans") || root.has("subscriptions")
        if (!hasValidHeader || !hasKnownKeys) {
            throw IllegalArgumentException("Invalid backup file: not a recognized Stash backup")
        }
        val txList = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val type = TransactionType.valueOf(obj.getString("type"))
                val paymentMode = PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE"))
                val tag = obj.getString("tag").trim()
                if (tag.isBlank()) throw IllegalArgumentException("Transaction tag cannot be blank")
                txList.add(
                    TransactionEntity(
                        id = 0,
                        type = type,
                        amount = requirePositiveAmount(obj.getDouble("amount"), "transaction"),
                        tag = tag,
                        note = obj.optString("note", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        paymentMode = paymentMode
                    )
                )
            }
        }

        val loanList = mutableListOf<LoanEntity>()
        if (root.has("loans")) {
            val loanArray = root.getJSONArray("loans")
            for (i in 0 until loanArray.length()) {
                val obj = loanArray.getJSONObject(i)
                val personName = obj.getString("personName").trim()
                if (personName.isBlank()) throw IllegalArgumentException("Loan personName cannot be blank")
                val paymentMode = PaymentMode.valueOf(obj.optString("initialPaymentMode", "CASH"))
                val remaining = obj.getDouble("remainingAmount")
                if (!remaining.isFinite() || remaining < 0.0) {
                    throw IllegalArgumentException("Invalid loan remainingAmount: $remaining")
                }
                val dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null
                loanList.add(
                    LoanEntity(
                        id = 0,
                        personName = personName,
                        phoneNumber = obj.optString("phoneNumber", ""),
                        totalAmount = requirePositiveAmount(obj.getDouble("totalAmount"), "loan total"),
                        remainingAmount = remaining,
                        reason = obj.optString("reason", ""),
                        lendDate = obj.optLong("lendDate", System.currentTimeMillis()),
                        dueDate = dueDate,
                        initialPaymentMode = paymentMode,
                        isSettled = obj.optBoolean("isSettled", false)
                    )
                )
            }
        }

        val subList = mutableListOf<SubscriptionEntity>()
        if (root.has("subscriptions")) {
            val subArray = root.getJSONArray("subscriptions")
            for (i in 0 until subArray.length()) {
                val obj = subArray.getJSONObject(i)
                val name = obj.getString("name").trim()
                if (name.isBlank()) throw IllegalArgumentException("Subscription name cannot be blank")
                val paymentMode = PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE"))
                val lastPaid = if (obj.has("lastPaidDate") && !obj.isNull("lastPaidDate")) obj.getLong("lastPaidDate") else null
                subList.add(
                    SubscriptionEntity(
                        id = 0,
                        name = name,
                        amount = requirePositiveAmount(obj.getDouble("amount"), "subscription"),
                        category = obj.optString("category", "Bills"),
                        paymentMode = paymentMode,
                        billingCycle = obj.optString("billingCycle", "MONTHLY"),
                        dueDay = obj.optInt("dueDay", 1).coerceIn(1, 31),
                        startDate = obj.optLong("startDate", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true),
                        lastPaidDate = lastPaid
                    )
                )
            }
        }

        return RestoredBackupData(
            transactions = txList,
            loans = loanList,
            subscriptions = subList
        )
    }

    private fun saveFileToStorage(context: Context, filename: String, content: String): File {
        // Ephemeral cache file for FileProvider sharing (pruned automatically)
        val file = createShareFile(context, filename)
        file.writeText(content)

        // Save a permanent copy to public Downloads so backups survive app uninstall
        try {
            val mimeType = if (filename.endsWith(".csv")) "text/csv" else "application/json"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray())
                }
            }
        } catch (_: Exception) {}

        return file
    }
}
