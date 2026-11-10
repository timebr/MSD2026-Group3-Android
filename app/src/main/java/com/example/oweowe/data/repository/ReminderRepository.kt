package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
import com.example.oweowe.data.model.Reminder

interface ReminderRepository {
    suspend fun sendReminder(groupId: String, toUserId: String, amountOre: Long): AppResult<Unit>

    /** Reminders addressed to the current user, for the notification listener (stage 8). */
    fun observeRemindersForMe(): Flow<List<Reminder>>
}