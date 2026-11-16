package com.example.oweowe

import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.ReminderRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.data.repository.fake.FakeAuthRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeReminderRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.data.repository.firebase.FirebaseAuthRepository
import com.example.oweowe.data.repository.firebase.FirebaseUserRepository

/**
 * The one place where implementations are wired. The UI only sees the repository
 * interfaces and [ConnectivityObserver]; swapping the Fake… implementations for
 * Firestore ones later is a change in this file only.
 */
class AppContainer(
    useFirebase: Boolean = false,
    val connectivityObserver: ConnectivityObserver = AlwaysOnlineObserver,
) {
    /** Public so a debug menu or a test can turn the knobs (delay, failNextWith). */
    val fakeBackend = FakeBackend(startSignedInAs = if (useFirebase) null else SeedData.TIM)

    /**
     * The signed-in user at process start, known synchronously so the first
     * screen can be chosen without waiting for the Flow (no flash of Sign in).
     * With Firebase this becomes `FirebaseAuth.getInstance().currentUser?.uid`.
     */
    val initialUserId: String? = fakeBackend.currentUserId.value

    /** Shared UI selection: the group picked on S4 drives the current group on S3. */
    val selectedGroupStore = SelectedGroupStore()

    val authRepository: AuthRepository =
        if (useFirebase) FirebaseAuthRepository() else FakeAuthRepository(fakeBackend)
    val userRepository: UserRepository =
        if (useFirebase) FirebaseUserRepository() else FakeUserRepository(fakeBackend)

    // Still fake until the Firestore repositories are wired:
    val groupRepository: GroupRepository = FakeGroupRepository(fakeBackend)
    val expenseRepository: ExpenseRepository = FakeExpenseRepository(fakeBackend)
    val reminderRepository: ReminderRepository = FakeReminderRepository(fakeBackend)
}
