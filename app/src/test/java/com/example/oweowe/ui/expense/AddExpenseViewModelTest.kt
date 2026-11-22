package com.example.oweowe.ui.expense

import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.ui.MainDispatcherRule
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class ControlledUserRepository(
        private val delegate: UserRepository,
        private var emissionsLeft: Int = 1,
    ) : UserRepository {
        override fun observeMe(): Flow<User?> = delegate.observeMe()

        override fun observeUsers(uids: List<String>): Flow<List<User>> = flow {
            if (emissionsLeft > 0) {
                emissionsLeft--
                emitAll(delegate.observeUsers(uids))
            }
        }

        override suspend fun updateProfile(
            fullName: String,
            email: String,
            photoUri: String?,
        ): AppResult<Unit> = delegate.updateProfile(fullName, email, photoUri)

        override suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit> =
            delegate.setPushEnabled(enabled)
    }

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    private fun viewModel(
        db: FakeBackend,
        initialGroupId: String? = null,
        expenseId: String? = null,
        userRepository: UserRepository = FakeUserRepository(db),
    ) = AddExpenseViewModel(
        initialGroupId = initialGroupId,
        expenseId = expenseId,
        expenseRepository = FakeExpenseRepository(db),
        userRepository = userRepository,
        groupRepository = FakeGroupRepository(db),
        connectivity = AlwaysOnlineObserver,
    )

    /** `submit()` reads the shared state, so it must be warmed up like the screen does. */
    private suspend fun AddExpenseViewModel.warmUp() {
        uiState.first { !it.loading }
    }

    @Test
    fun uiState_defaultsToFirstGroupAllMembersAndMeAsPayer() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(SeedData.FLAT, state.selectedGroupId)
        assertEquals(SeedData.TIM, state.payerId)
        assertEquals(listOf(SeedData.ANNA, SeedData.JONAS, SeedData.TIM), state.members.map { it.uid })
        assertEquals(setOf(SeedData.ANNA, SeedData.JONAS, SeedData.TIM), state.splitAmong)
    }

    @Test
    fun initialGroupId_preselectsThatGroup() = runTest {
        val db = backend(backgroundScope)
        val second = (FakeGroupRepository(db).createGroup("Trip", "") as AppResult.Success<String>).value
        val viewModel = viewModel(db, initialGroupId = second)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(second, state.selectedGroupId)
        assertEquals(listOf(SeedData.TIM), state.members.map { it.uid })   // the creator is alone
    }

    @Test
    fun typingUpdatesTextImmediately_whenProfilesStopArriving() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(
            db,
            userRepository = ControlledUserRepository(FakeUserRepository(db)),
        )
        viewModel.warmUp()

        viewModel.updateDescription("1")
        assertEquals("1", viewModel.uiState.value.description)

        viewModel.updateAmount("10")
        assertEquals("10", viewModel.uiState.value.amountText)

        viewModel.updateCategory("Food")
        assertEquals("Food", viewModel.uiState.value.category)
    }

    @Test
    fun selectingAnotherGroup_stillLoadsMembers_whenTypingDoesNotConsumeProfileEmissions() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(
            db,
            userRepository = ControlledUserRepository(FakeUserRepository(db), emissionsLeft = 2),
        )
        viewModel.warmUp()

        viewModel.updateDescription("Trip")
        assertEquals("Trip", viewModel.uiState.value.description)

        val second = (FakeGroupRepository(db).createGroup("Trip", "") as AppResult.Success<String>).value
        viewModel.selectGroup(second)

        val state = viewModel.uiState.first {
            it.selectedGroupId == second && it.members.map { member -> member.uid } == listOf(SeedData.TIM)
        }
        assertEquals("Trip", state.description)
        assertEquals(listOf(SeedData.TIM), state.members.map { it.uid })
    }

    /**
     * FR5: the split must never contain members of the group the user just left.
     * While the new group's members load, the form shows the new group with no
     * members (and the submit button disabled) instead of the old member set.
     */
    @Test
    fun switchingGroup_neverKeepsThePreviousGroupsMembersInTheSplit() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(
            db,
            userRepository = ControlledUserRepository(FakeUserRepository(db)),
        )
        viewModel.warmUp()
        assertEquals(3, viewModel.uiState.value.splitAmong.size)

        val second = (FakeGroupRepository(db).createGroup("Trip", "") as AppResult.Success<String>).value
        viewModel.selectGroup(second)

        val state = viewModel.uiState.first { it.selectedGroupId == second }
        assertEquals(emptyList<String>(), state.members.map { it.uid })
        assertEquals(emptySet<String>(), state.splitAmong)
    }

    @Test
    fun submit_invalidAmount_marksTheFieldInsteadOfSubmitting() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.warmUp()
        val expensesBefore = db.expenses.value.size

        viewModel.updateDescription("Dinner")
        viewModel.updateAmount("not a number")
        viewModel.submit()

        assertEquals(true, viewModel.uiState.value.amountInvalid)
        assertEquals(WriteState.Idle, viewModel.uiState.value.writeState)
        assertEquals(expensesBefore, db.expenses.value.size)
    }

    @Test
    fun submit_blankPurpose_marksTheFieldInsteadOfSubmitting() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.warmUp()
        val expensesBefore = db.expenses.value.size

        viewModel.updateAmount("16")            // no purpose given
        viewModel.submit()

        assertEquals(true, viewModel.uiState.value.descriptionInvalid)
        assertEquals(WriteState.Idle, viewModel.uiState.value.writeState)
        assertEquals(expensesBefore, db.expenses.value.size)
    }

    @Test
    fun submit_withoutCategory_stillSaves() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.warmUp()

        viewModel.updateDescription("Dinner")   // the category stays optional
        viewModel.updateAmount("16")
        viewModel.submit()

        assertEquals(WriteState.Done, viewModel.uiState.value.writeState)
        assertEquals("", db.expenses.value.last().category)
    }

    @Test
    fun submit_valid_addsPendingExpenseInOreWhichThenSyncs() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.warmUp()

        viewModel.updateDescription("Dinner")
        viewModel.updateCategory("Food")
        viewModel.updateAmount("16,5")
        viewModel.toggleSplitMember(SeedData.ANNA)   // split with Tim + Jonas only
        viewModel.submit()

        assertEquals(WriteState.Done, viewModel.uiState.value.writeState)

        val expense = db.expenses.value.last()
        assertEquals(1_650L, expense.amountOre)      // "16,5" parsed to øre, never a Double (FR4)
        assertEquals(SeedData.TIM, expense.paidBy)
        assertEquals(setOf(SeedData.JONAS, SeedData.TIM), expense.splitAmong.toSet())
        assertEquals("Dinner", expense.description)
        assertEquals(SyncStatus.PENDING, expense.syncStatus)  // pending first (NFR4)
        advanceUntilIdle()
        assertEquals(SyncStatus.SYNCED, db.expenses.value.last().syncStatus)
    }

    @Test
    fun submit_networkFailure_showsFailedStateWithRetry() = runTest {
        val db = backend(backgroundScope)
        db.failNextWith = ErrorReason.NETWORK
        val viewModel = viewModel(db)
        viewModel.warmUp()
        val expensesBefore = db.expenses.value.size

        viewModel.updateDescription("Dinner")
        viewModel.updateAmount("16")
        viewModel.submit()

        assertEquals(
            WriteState.Failed(ErrorReason.NETWORK),
            viewModel.uiState.value.writeState,
        )
        assertEquals(expensesBefore, db.expenses.value.size)
        assertTrue(db.expenses.value.none { it.description == "Dinner" })
    }

    // ---- editing an existing expense (any group member) ----

    @Test
    fun edit_prefillsTheStoredExpense() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, initialGroupId = SeedData.FLAT, expenseId = "e2")

        val state = viewModel.uiState.first { !it.loading }

        assertEquals("Pizza night", state.description)
        assertEquals("Food", state.category)
        assertEquals("450,00", state.amountText)
        assertEquals(SeedData.ANNA, state.payerId)
        assertEquals(setOf(SeedData.TIM, SeedData.ANNA, SeedData.JONAS), state.splitAmong)
        assertEquals(listOf(SeedData.FLAT), state.groups.map { it.id })
        assertEquals(false, state.expenseNotFound)
    }

    @Test
    fun edit_savesChangesAsAnyMemberAndKeepsTheCreator() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, SeedData.FLAT, "e2")   // created by Anna
        viewModel.warmUp()

        viewModel.updateDescription("Pizza and drinks")
        viewModel.updateAmount("50")
        viewModel.submit()

        assertEquals(WriteState.Done, viewModel.uiState.value.writeState)
        val edited = db.expenses.value.first { it.id == "e2" }
        assertEquals("Pizza and drinks", edited.description)
        assertEquals(5_000L, edited.amountOre)
        assertEquals(SeedData.ANNA, edited.createdBy)
        assertEquals(SyncStatus.PENDING, edited.syncStatus)    // NFR4
        advanceUntilIdle()
        assertEquals(SyncStatus.SYNCED, db.expenses.value.first { it.id == "e2" }.syncStatus)
    }

    @Test
    fun edit_unknownExpense_showsNotFoundInsteadOfAnEmptyForm() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, SeedData.FLAT, "missing")

        val state = viewModel.uiState.first { !it.loading }

        assertTrue(state.expenseNotFound)
    }
}
