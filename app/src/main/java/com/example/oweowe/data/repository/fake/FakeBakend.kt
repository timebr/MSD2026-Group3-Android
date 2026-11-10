package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.repository.ErrorReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class FakeBackend(
    startSignedInAs: String? = null,
    /** Runs background work such as "pending -> synced". Tests pass their own scope. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    // ---- the "database" ----
    val currentUserId = MutableStateFlow(startSignedInAs)
    val users = MutableStateFlow(SeedData.users)
    val groups = MutableStateFlow(SeedData.groups)
    val expenses = MutableStateFlow(SeedData.expenses)
    val reminders = MutableStateFlow(emptyList<Reminder>())

    /** email -> password. Only the fake stores passwords; Firebase Auth does this later. */
    val passwords = SeedData.passwords.toMutableMap()

    // ---- knobs for the frontend team ----
    var delayMs = 300L               // every call waits this long, so spinners are visible
    var syncDelayMs = 2_000L         // new expenses stay PENDING this long
    var failNextWith: ErrorReason? = null  // e.g. ErrorReason.NETWORK: the next write fails once

    fun newId(): String = UUID.randomUUID().toString()
    fun now(): Long = System.currentTimeMillis()

    /** Call at the start of every write: waits, then returns an error if one was requested. */
    suspend fun simulateCall(): ErrorReason? {
        delay(delayMs)
        return failNextWith.also { failNextWith = null }
    }

    fun isMember(groupId: String, userId: String?): Boolean =
        userId != null && groups.value.any { it.id == groupId && userId in it.memberIds }
}