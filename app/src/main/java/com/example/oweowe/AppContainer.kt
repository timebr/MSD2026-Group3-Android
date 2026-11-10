package com.example.oweowe

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

class AppContainer {
    /** Public so a debug menu or a test can turn the knobs (delay, failNextWith). */
    val fakeBackend = FakeBackend(startSignedInAs = SeedData.TIM)

    val authRepository: AuthRepository = FakeAuthRepository(fakeBackend)
    val userRepository: UserRepository = FakeUserRepository(fakeBackend)
    val groupRepository: GroupRepository = FakeGroupRepository(fakeBackend)
    val expenseRepository: ExpenseRepository = FakeExpenseRepository(fakeBackend)
    val reminderRepository: ReminderRepository = FakeReminderRepository(fakeBackend)
}