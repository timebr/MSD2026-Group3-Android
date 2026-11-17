package com.example.oweowe

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.firebase.FirebaseAuthRepository
import com.example.oweowe.data.repository.firebase.FirebaseExpenseRepository
import com.example.oweowe.data.repository.firebase.FirebaseGroupRepository
import com.example.oweowe.data.repository.firebase.FirebaseReminderRepository
import com.example.oweowe.data.repository.firebase.FirebaseUserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 * first. The test creates its own named FirebaseApp, so it also runs on a
 * checkout without google-services.json.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun emulatorApp(): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }?.let { return it }
        return FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("1:1234567890:android:emulator")
                .setProjectId("demo-oweowe")
                .build(),
            APP_NAME,
        )!!
    }

    @Test
    fun authGroupsExpensesAndRemindersWorkAgainstTheEmulator() = runBlocking {
        val app = emulatorApp()
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app)
        auth.useEmulator(HOST, 9099)
        db.useEmulator(HOST, 8080)

        val authRepo = FirebaseAuthRepository(auth, db)
        val usersRepo = FirebaseUserRepository(auth, db)
        val groupsRepo = FirebaseGroupRepository(auth, db)
        val expensesRepo = FirebaseExpenseRepository(auth = auth, db = db)
        val remindersRepo = FirebaseReminderRepository(auth, db)

        val stamp = System.currentTimeMillis()
        val annaEmail = "anna$stamp@test.dk"
        val majaEmail = "maja$stamp@test.dk"
        val timEmail = "tim$stamp@test.dk"
        val password = "password123"
        val phoneBase = (stamp % 1_000_000L).toString().padStart(6, '0')
        val annaPhone = "+45 11$phoneBase"
        val majaPhone = "+45 22$phoneBase"
        val timPhone = "+45 33$phoneBase"

        auth.signOut()
        assertEquals(AppResult.Success(Unit), authRepo.register("Anna", annaEmail, password, annaPhone))
        val annaUid = auth.currentUser!!.uid
        assertEquals(annaEmail, withTimeout(15_000) { usersRepo.observeMe().filterNotNull().first() }.email)

        auth.signOut()
        assertEquals(AppResult.Success(Unit), authRepo.register("Maja", majaEmail, password, majaPhone))
        val majaUid = auth.currentUser!!.uid

        auth.signOut()
        assertEquals(AppResult.Success(Unit), authRepo.register("Tim", timEmail, password, timPhone))
        assertEquals(ErrorReason.WRONG_CREDENTIALS, (authRepo.signIn(timEmail, "wrong") as AppResult.Error).reason)
        auth.signOut()
        assertEquals(AppResult.Success(Unit), authRepo.signIn(timEmail, password))
        val timUid = auth.currentUser!!.uid

        val groupId = (groupsRepo.createGroup("Emulator flat", "Test") as AppResult.Success).value
        assertEquals(
            annaUid,
            (groupsRepo.addMemberByEmail(groupId, annaEmail) as AppResult.Success).value.uid,
        )
        assertEquals(
            majaUid,
            (groupsRepo.addMemberByPhone(groupId, majaPhone) as AppResult.Success).value.uid,
        )

        assertEquals(
            AppResult.Success(Unit),
            expensesRepo.addExpense(
                groupId = groupId,
                description = "Dinner",
                category = "Food",
                amountOre = 10_000,
                paidBy = timUid,
                splitAmong = listOf(timUid, annaUid),
            ),
        )
        val expense = withTimeout(15_000) {
            expensesRepo.observeExpenses(groupId).first { it.any { e -> e.description == "Dinner" } }
                .first { it.description == "Dinner" }
        }
        assertEquals(10_000L, expense.amountOre)

        assertEquals(
            AppResult.Success(Unit),
            expensesRepo.updateExpense(
                groupId = groupId,
                expenseId = expense.id,
                description = "Dinner edited",
                category = "Food",
                amountOre = 12_000,
                paidBy = timUid,
                splitAmong = listOf(timUid, annaUid),
            ),
        )
        val edited = withTimeout(15_000) {
            expensesRepo.observeExpenses(groupId).first { list -> list.any { it.id == expense.id && it.amountOre == 12_000L } }
                .first { it.id == expense.id }
        }
        assertEquals("Dinner edited", edited.description)

        assertEquals(
            AppResult.Success(Unit),
            remindersRepo.sendReminder(groupId, annaUid, 5_000),
        )
        auth.signOut()
        assertEquals(AppResult.Success(Unit), authRepo.signIn(annaEmail, password))

        // Any member may edit or delete an expense, not only its creator.
        assertEquals(
            AppResult.Success(Unit),
            expensesRepo.updateExpense(
                groupId = groupId,
                expenseId = expense.id,
                description = "Dinner by Anna",
                category = "Food",
                amountOre = 12_000,
                paidBy = timUid,
                splitAmong = listOf(timUid, annaUid),
            ),
        )
        assertEquals(AppResult.Success(Unit), expensesRepo.settleUp(groupId, timUid))

        val reminder = withTimeout(15_000) {
            remindersRepo.observeRemindersForMe().first { it.isNotEmpty() }.first()
        }
        assertEquals(timUid, reminder.fromUserId)
        assertEquals(5_000L, reminder.amountOre)

        assertEquals(AppResult.Success(Unit), expensesRepo.deleteExpense(groupId, expense.id))

        app.delete()
    }

    private companion object {
        const val APP_NAME = "emulator-test"
        const val HOST = "127.0.0.1"
    }
}
