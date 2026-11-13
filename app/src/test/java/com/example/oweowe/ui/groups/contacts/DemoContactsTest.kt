package com.example.oweowe.ui.groups.contacts

import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.SeedData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the demo list (D3) against drifting away from the fake backend. */
class DemoContactsTest {

    @Test
    fun everyDemoContactMatchesAFakeProfile() {
        val emails = SeedData.users.map { it.email.lowercase() }.toSet()
        DEMO_CONTACTS.forEach { contact ->
            assertTrue(
                "${contact.email} has no seeded profile",
                contact.email.lowercase() in emails,
            )
        }
    }

    @Test
    fun demoContactsAreNotAlreadyMembersOfTheSeedGroup() {
        val flat = SeedData.groups.first { it.id == SeedData.FLAT }
        val memberEmails = SeedData.users
            .filter { it.uid in flat.memberIds }
            .map { it.email.lowercase() }
            .toSet()
        DEMO_CONTACTS.forEach { contact ->
            assertFalse(
                "${contact.email} is already in ${flat.name}",
                contact.email.lowercase() in memberEmails,
            )
        }
    }

    @Test
    fun addingADemoContactSucceeds() = runTest {
        val db = FakeBackend(startSignedInAs = SeedData.TIM, scope = backgroundScope)
            .apply { delayMs = 0 }
        val repository = FakeGroupRepository(db)

        DEMO_CONTACTS.forEach { contact ->
            val result = repository.addMemberByEmail(SeedData.FLAT, contact.email)
            assertTrue("${contact.email}: $result", result is AppResult.Success)
        }
    }
}
