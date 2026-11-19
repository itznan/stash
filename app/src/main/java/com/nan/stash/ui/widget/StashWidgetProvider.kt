package com.nan.stash.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nan.stash.MainActivity
import com.nan.stash.R
import com.nan.stash.data.local.AppDatabase
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class StashWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_ADD_INCOME = "com.nan.stash.ACTION_ADD_INCOME"
        const val ACTION_ADD_EXPENSE = "com.nan.stash.ACTION_ADD_EXPENSE"
        const val ACTION_TRANSFER = "com.nan.stash.ACTION_TRANSFER"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, StashWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids != null && ids.isNotEmpty()) {
                    val intent = Intent(context, StashWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {}
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val transactions = db.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()

                    var onlineBal = 0.0
                    var cashBal = 0.0
                    for (t in transactions) {
                        val isOnline = t.paymentMode == PaymentMode.ONLINE
                        when (t.type) {
                            TransactionType.INCOME, TransactionType.REPAYMENT -> {
                                if (isOnline) onlineBal += t.amount else cashBal += t.amount
                            }
                            TransactionType.EXPENSE, TransactionType.LEND -> {
                                if (isOnline) onlineBal -= t.amount else cashBal -= t.amount
                            }
                            TransactionType.TRANSFER -> {
                                if (isOnline) {
                                    onlineBal -= t.amount
                                    cashBal += t.amount
                                } else {
                                    cashBal -= t.amount
                                    onlineBal += t.amount
                                }
                            }
                        }
                    }

                    val views = RemoteViews(context.packageName, R.layout.widget_stash).apply {
                        setTextViewText(R.id.widget_online_val, String.format(Locale.US, "%.2f", onlineBal))
                        setTextViewText(R.id.widget_cash_val, String.format(Locale.US, "%.2f", cashBal))

                        // Open app on click header
                        val appIntent = PendingIntent.getActivity(
                            context, 0,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_header, appIntent)

                        // Quick Actions
                        val incomeIntent = PendingIntent.getActivity(
                            context, 1,
                            Intent(context, MainActivity::class.java).apply {
                                action = ACTION_ADD_INCOME
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_income, incomeIntent)

                        val expenseIntent = PendingIntent.getActivity(
                            context, 2,
                            Intent(context, MainActivity::class.java).apply {
                                action = ACTION_ADD_EXPENSE
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_expense, expenseIntent)

                        val transferIntent = PendingIntent.getActivity(
                            context, 3,
                            Intent(context, MainActivity::class.java).apply {
                                action = ACTION_TRANSFER
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_transfer, transferIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {}
            }
        }
    }
}
