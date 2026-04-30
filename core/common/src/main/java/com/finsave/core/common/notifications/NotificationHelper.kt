package com.finsave.core.common.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finsave.core.common.Constants.DAILY_DIGEST_ID
import com.finsave.core.common.Constants.NOTIFICATION_CHANNEL_BUDGET
import com.finsave.core.common.Constants.NOTIFICATION_CHANNEL_DAILY
import com.finsave.core.common.Constants.NOTIFICATION_CHANNEL_SPLITTER
import com.finsave.core.common.formatter.IndianNumberFormatter

/**
 * NotificationHelper — centralised notification posting for FinSave.
 *
 * Handles channel creation (Android O+) and posting notifications for:
 * - Budget threshold alerts (80% / 90%)
 * - Daily spending digest
 * - Splitter debt reminders
 *
 * All post methods are no-ops when POST_NOTIFICATIONS permission is not granted
 * (Android 13+ / TIRAMISU).
 */
object NotificationHelper {

    private const val NOTIFICATION_TITLE = "FinSave"

    /**
     * Creates the three notification channels required by FinSave.
     * Safe to call multiple times — Android deduplicates by channel ID.
     * Must be called before any notification is posted (typically in Application.onCreate).
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_BUDGET,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    NOTIFICATION_CHANNEL_DAILY,
                    "Daily Digest",
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    NOTIFICATION_CHANNEL_SPLITTER,
                    "Splitter Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannels(channels)
        }
    }

    /**
     * Posts a budget threshold alert notification.
     *
     * @param context       Application or service context
     * @param budgetId      Unique ID of the budget (used as notification ID)
     * @param categoryName  Display name of the category, or "Total" for overall budget
     * @param threshold     80 or 90 (percent used)
     */
    fun postBudgetAlert(
        context: Context,
        budgetId: Long,
        categoryName: String,
        threshold: Int
    ) {
        if (!hasPermission(context)) return
        val msg = if (threshold == 80) {
            "You've used 80% of your $categoryName budget"
        } else {
            "Warning: 90% of your $categoryName budget used"
        }
        notify(context, NOTIFICATION_CHANNEL_BUDGET, budgetId.toInt(), msg)
    }

    /**
     * Posts the daily spending digest notification.
     *
     * @param context       Application or service context
     * @param spentPaise    Total amount spent today in paise
     * @param txCount       Number of debit transactions today
     */
    fun postDailyDigest(context: Context, spentPaise: Long, txCount: Int) {
        if (!hasPermission(context)) return
        val formatted = IndianNumberFormatter.format(spentPaise)
        notify(
            context,
            NOTIFICATION_CHANNEL_DAILY,
            DAILY_DIGEST_ID,
            "Today's spending: $formatted across $txCount transactions"
        )
    }

    /**
     * Posts a splitter debt reminder notification.
     *
     * @param context       Application or service context
     * @param fromMemberName  Name of the member who owes money
     * @param toMemberName    Name of the member who is owed money
     * @param amountPaise   Amount owed in paise
     * @param groupName     Name of the splitter group
     * @param notificationId  Unique ID for this notification (e.g., debt.hashCode())
     */
    fun postSplitterReminder(
        context: Context,
        fromMemberName: String,
        toMemberName: String,
        amountPaise: Long,
        groupName: String,
        notificationId: Int
    ) {
        if (!hasPermission(context)) return
        val amount = IndianNumberFormatter.format(amountPaise)
        notify(
            context,
            NOTIFICATION_CHANNEL_SPLITTER,
            notificationId,
            "$fromMemberName owes $toMemberName $amount in $groupName"
        )
    }

    /**
     * Returns true if the app has permission to post notifications.
     * On Android < 13 (TIRAMISU), notifications are always allowed.
     */
    private fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * Builds and posts a notification with the given channel, ID, and message.
     */
    @Suppress("MissingPermission") // Permission is checked by caller (hasPermission guard)
    private fun notify(context: Context, channelId: String, id: Int, message: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
