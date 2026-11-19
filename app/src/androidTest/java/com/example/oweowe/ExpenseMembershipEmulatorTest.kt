package com.example.oweowe

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.firebase.FirebaseAuthRepository
import com.example.oweowe.data.repository.firebase.FirebaseExpenseRepository
import com.example.oweowe.data.repository.firebase.FirebaseGroupRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against the Firebase Emulator Suite on the host:
 *
 *   firebase emulators:start --project demo-oweowe
 *
 * Run `adb reverse tcp:9099 tcp:9099` and `adb reverse tcp:8080 tcp:8080`
 * first. Each test creates its own named FirebaseApp, so the tests also run on
 * a checkout without google-services.json.
 */
@RunWith(AndroidJUnit4::class)
class ExpenseMembershipEmulatorTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private class Harness(
        val app: FirebaseApp,
        val auth: FirebaseAuth,
        val db: FirebaseFirestore,
        val authRepo: FirebaseAuthRepository,
        val groups: FirebaseGroupRepository,
        val expenses: FirebaseExpenseRepository,
    )

    private fun harness(): Harness {
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("1:1234567890:android:emulator")
                .setProjectId("demo-oweowe")
                .build(),
            "membership-test-${System.nanoTime()}",
        )!!
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app)
        auth.useEmulator(HOST, AUTH_PORT)
        db.useEmulator(HOST, FIRESTORE_PORT)
        return Harness(
            app = app,
            auth = auth,
            db = db,
            authRepo = FirebaseAuthRepository(auth, db),
            groups = FirebaseGroupRepository(auth, db),
            expenses = FirebaseExpenseRepository(auth = auth, db = db),
        )
    }

    @Test
    fun membershipGuardsRejectUsersOutsideTheGroup() = runBlocking {
        val h = harness()
        try {
            val password = "password123"
            val ownerEmail = newEmail("owner")
            val outsiderEmail = newEmail("outsider")
            assertEquals(
                AppResult.Success(Unit),
                h.authRepo.register("Owner", ownerEmail, password, "+45 11 11 11 11"),
            )
            val ownerUid = h.auth.currentUser!!.uid
            h.auth.signOut()
            assertEquals(
                AppResult.Success(Unit),
                h.authRepo.register("Outsider", outsiderEmail, password, "+45 12 12 12 12"),
            )
            val outsiderUid = h.auth.currentUser!!.uid
            h.auth.signOut()
            assertEquals(AppResult.Success(Unit), h.authRepo.signIn(ownerEmail, password))

            val groupId = (h.groups.createGroup("Membership", "") as AppResult.Success).value
            h.db.waitForPendingWrites().await()

            assertEquals(
                AppResult.Success(Unit),
                h.expenses.addExpense(groupId, "Dinner", "Food", 1_000, ownerUid, listOf(ownerUid)),
            )
            val expenseId = withTimeout(15_000) {
                h.expenses.observeExpenses(groupId)
                    .first { list -> list.any { it.description == "Dinner" } }
                    .first { it.description == "Dinner" }
                    .id
            }

            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.addExpense(groupId, "Rejected payer", "Food", 1_000, outsiderUid, listOf(ownerUid)),
            )
            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.addExpense(
                    groupId, "Rejected split", "Food", 1_000, ownerUid, listOf(ownerUid, outsiderUid),
                ),
            )
            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.updateExpense(
                    groupId, expenseId, "Rejected update payer", "Food", 1_000, outsiderUid, listOf(ownerUid),
                ),
            )
            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.updateExpense(
                    groupId, expenseId, "Rejected update split", "Food", 1_000, ownerUid,
                    listOf(ownerUid, outsiderUid),
                ),
            )
            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.settleUp(groupId, outsiderUid),
            )

            h.auth.signOut()
            assertEquals(AppResult.Success(Unit), h.authRepo.signIn(outsiderEmail, password))
            assertEquals(
                AppResult.Error(ErrorReason.NOT_ALLOWED),
                h.expenses.addExpense(groupId, "Outsider write", "Food", 1_000, outsiderUid, listOf(outsiderUid)),
            )

            h.auth.signOut()
            assertEquals(AppResult.Success(Unit), h.authRepo.signIn(ownerEmail, password))
            h.db.waitForPendingWrites().await()
            val descriptions = h.db.collection("groups").document(groupId).collection("expenses")
                .get(Source.SERVER).await()
                .documents.mapNotNull { it.getString("description") }
            assertEquals(listOf("Dinner"), descriptions)
        } finally {
            h.auth.signOut()
        }
    }

    @Test
    fun offlineWriteToJustCreatedGroupUsesTheCachedMembers() = runBlocking {
        val h = harness()
        try {
            val email = newEmail("offline-owner")
            assertEquals(
                AppResult.Success(Unit),
                h.authRepo.register("Owner", email, "password123", "+45 21 21 21 21"),
            )
            val uid = h.auth.currentUser!!.uid

            h.db.disableNetwork().await()
            val groupId = try {
                // The group exists only as a pending local write at this point.
                (h.groups.createGroup("Offline group", "") as AppResult.Success).value.also {
                    assertEquals(
                        AppResult.Success(Unit),
                        h.expenses.addExpense(it, "Offline dinner", "Food", 1_650, uid, listOf(uid)),
                    )
                }
            } finally {
                h.db.enableNetwork().await()
            }

            h.db.waitForPendingWrites().await()
            val documents = h.db.collection("groups").document(groupId).collection("expenses")
                .get(Source.SERVER).await().documents
            assertEquals(1, documents.size)
            assertEquals("Offline dinner", documents.first().getString("description"))
        } finally {
            h.auth.signOut()
        }
    }

    @Test
    fun offlineMembershipReadWithoutCachedGroupReturnsNetwork() = runBlocking {
        val h = harness()
        try {
            val email = newEmail("network")
            assertEquals(
                AppResult.Success(Unit),
                h.authRepo.register("Owner", email, "password123", "+45 31 31 31 31"),
            )
            val uid = h.auth.currentUser!!.uid

            h.db.disableNetwork().await()
            val result = try {
                withTimeout(15_000) {
                    h.expenses.addExpense(
                        "missing-group-${System.nanoTime()}", "Dinner", "Food", 1_000, uid, listOf(uid),
                    )
                }
            } finally {
                h.db.enableNetwork().await()
            }

            assertEquals(AppResult.Error(ErrorReason.NETWORK), result)
        } finally {
            h.auth.signOut()
        }
    }

    private fun newEmail(prefix: String) = "$prefix${System.nanoTime()}@test.dk"

    private companion object {
        const val HOST = "127.0.0.1"
        const val AUTH_PORT = 9099
        const val FIRESTORE_PORT = 8080
    }
}
