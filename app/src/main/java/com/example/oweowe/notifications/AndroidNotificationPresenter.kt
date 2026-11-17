package com.example.oweowe.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.oweowe.MainActivity
import com.example.oweowe.R
import com.example.oweowe.domain.formatKr
import com.example.oweowe.sync.SyncEvent

/** Posts the local notifications behind FR7, FR8 and NFR5. */
class AndroidNotificationPresenter(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    fun show(action: NotificationAction) {
        when (action) {
            is NotificationAction.NewExpense -> notify(
                id = action.expenseId.hashCode(),
                title = context.getString(R.string.notification_new_expense_title, action.groupName),
                body = context.getString(
                    R.string.notification_new_expense_body,
                    action.payerName.ifBlank { context.getString(R.string.common_someone) },
                    action.description,
                    formatKr(action.amountOre),
                ),
            )
            is NotificationAction.UpdatedExpense -> notify(
                id = action.expenseId.hashCode() + UPDATE_OFFSET,
                title = context.getString(R.string.notification_expense_updated_title, action.groupName),
                body = context.getString(
                    R.string.notification_expense_updated_body,
                    action.editorName.ifBlank { context.getString(R.string.common_someone) },
                    action.description,
                    formatKr(action.amountOre),
                ),
            )
            is NotificationAction.ReminderReceived -> notify(
                id = action.reminderId.hashCode() + REMINDER_OFFSET,
                title = context.getString(
                    R.string.notification_reminder_title,
                    action.fromName.ifBlank { context.getString(R.string.common_someone) },
                ),
                body = context.getString(
                    R.string.notification_reminder_body,
                    formatKr(action.amountOre),
                    action.groupName,
                ),
            )
        }
    }

    fun showSynced(event: SyncEvent) = notify(
        id = event.documentId.hashCode() + SYNC_OFFSET,
        title = context.getString(R.string.notification_synced_title),
        body = context.getString(R.string.home_synced_in_background, event.label),
    )

    private fun notify(id: Int, title: String, body: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was refused; the in-app flow still works.
        }
    }

    private companion object {
        const val CHANNEL_ID = "group_activity"
        const val UPDATE_OFFSET = 100_000
        const val REMINDER_OFFSET = 200_000
        const val SYNC_OFFSET = 300_000
    }
}
