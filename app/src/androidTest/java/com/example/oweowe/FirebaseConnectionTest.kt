package com.example.oweowe

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.firebase.FirebaseAuthRepository
import com.example.oweowe.data.repository.firebase.FirebaseUserRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseConnectionTest {

    private val auth = FirebaseAuthRepository()
    private val users = FirebaseUserRepository()

    @Test
    fun registerReadProfileSignInAndRejectWrongPassword() = runBlocking {
        val email = "smoketest${System.currentTimeMillis()}@test.dk"   // new user every run

        assertEquals(AppResult.Success(Unit), auth.register("Smoke Test", email, "password123", "+45 00 00 00 00"))

        val me = withTimeout(15_000) { users.observeMe().filterNotNull().first() }
        assertEquals(email, me.email)
        assertEquals("Smoke Test", me.fullName)

        auth.signOut()
        assertEquals(AppResult.Success(Unit), auth.signIn(email, "password123"))
        auth.signOut()
        assertEquals(AppResult.Error(ErrorReason.WRONG_CREDENTIALS), auth.signIn(email, "wrong-password"))
    }
}