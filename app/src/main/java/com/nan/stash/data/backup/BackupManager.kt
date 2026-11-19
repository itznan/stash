package com.nan.stash.data.backup

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoredBackupData(
    val transactions: List<TransactionEntity>,
    val loans: List<LoanEntity>,
    val subscriptions: List<SubscriptionEntity>
)

object BackupManager {

    fun exportToCsv(context: Context, transactions: List<TransactionEntity>): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "stash_transactions_$timestampStr.csv"

        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,PaymentMode,Note,Date,Time\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        for (t in transactions) {
            val d = Date(t.timestamp)
            val cleanNote = t.note.replace("\"", "\"\"").replace("\n", " ")
            val cleanTag = t.tag.replace("\"", "\"\"")
            sb.append("${t.id},${t.type.name},${String.format(Locale.US, "%.2f", t.amount)},\"$cleanTag\",${t.paymentMode.name},\"$cleanNote\",${dateFormat.format(d)},${timeFormat.format(d)}\n")
        }

        return saveFileToStorage(context, filename, sb.toString())
    }

    fun createBackupJson(
        context: Context,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        subscriptions: List<SubscriptionEntity>
    ): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "stash_backup_$timestampStr.json"

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

        return saveFileToStorage(context, filename, root.toString(2))
    }

    fun parseBackupJson(jsonString: String): RestoredBackupData {
        val root = JSONObject(jsonString)
        val txList = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val type = try { TransactionType.valueOf(obj.getString("type")) } catch (_: Exception) { TransactionType.EXPENSE }
                val paymentMode = try { PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE")) } catch (_: Exception) { PaymentMode.ONLINE }
                txList.add(
                    TransactionEntity(
                        id = 0,
                        type = type,
                        amount = obj.getDouble("amount"),
                        tag = obj.getString("tag"),
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
                val paymentMode = try {
                    PaymentMode.valueOf(obj.optString("initialPaymentMode", "CASH"))
                } catch (_: Exception) {
                    PaymentMode.CASH
                }
                val dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null
                loanList.add(
                    LoanEntity(
                        id = 0,
                        personName = obj.getString("personName"),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        totalAmount = obj.getDouble("totalAmount"),
                        remainingAmount = obj.getDouble("remainingAmount"),
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
                val paymentMode = try {
                    PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE"))
                } catch (_: Exception) {
                    PaymentMode.ONLINE
                }
                val lastPaid = if (obj.has("lastPaidDate") && !obj.isNull("lastPaidDate")) obj.getLong("lastPaidDate") else null
                subList.add(
                    SubscriptionEntity(
                        id = 0,
                        name = obj.getString("name"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", "Bills"),
                        paymentMode = paymentMode,
                        billingCycle = obj.optString("billingCycle", "MONTHLY"),
                        dueDay = obj.optInt("dueDay", 1),
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
        // App-scoped file for FileProvider sharing
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!targetDir.exists()) targetDir.mkdirs()
        val file = File(targetDir, filename)
        file.writeText(content)

        // Save a permanent copy to public Downloads so backups survive app uninstall
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            }
        } catch (_: Exception) {}

        return file
    }
}
