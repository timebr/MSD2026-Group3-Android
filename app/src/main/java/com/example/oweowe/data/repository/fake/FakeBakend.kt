package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.repository.ErrorReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    var failReadsWith: ErrorReason? = null // while set, every repository read fails

    /**
     * Applies [failReadsWith] to a read flow. A failed Firestore listener throws
     * to its collector, so the fake does the same; the ViewModels turn that into
     * the retryable error state (NFR4), which lets the states be demoed without
     * Firebase.
     */
    fun <T> read(flow: Flow<T>): Flow<T> = flow.map { value ->
        failReadsWith?.let { reason ->
            throw IllegalStateException("Simulated ${reason.name} read failure")
        }
        value
    }

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