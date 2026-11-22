package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class FakeReminderRepository(private val db: FakeBackend) : ReminderRepository {

    override suspend fun sendReminder(groupId: String, toUserId: String, amountOre: Long): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (!db.isMember(groupId, me) || !db.isMember(groupId, toUserId)) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }
        db.reminders.update {
            it + Reminder(
                id = db.newId(), groupId = groupId, fromUserId = me, toUserId = toUserId,
                amountOre = amountOre, createdAt = db.now(),
            )
        }
        return AppResult.Success(Unit)
    }

    override fun observeRemindersForMe(): Flow<List<Reminder>> =
        db.read(
            combine(db.currentUserId, db.reminders) { uid, reminders ->
                reminders.filter { it.toUserId == uid }
            },
        )
}