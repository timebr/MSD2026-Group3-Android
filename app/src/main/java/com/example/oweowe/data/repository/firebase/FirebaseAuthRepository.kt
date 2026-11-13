package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.AuthRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
) : AuthRepository {

    override val currentUserId: Flow<String?> = auth.uidFlow()

    override suspend fun register(
        fullName: String, email: String, password: String, phone: String,
    ): AppResult<Unit> = firebaseCall {
        val mail = email.trim().lowercase()

        // 1. Firebase Auth creates the account (and signs the user in)
        val result = auth.createUserWithEmailAndPassword(mail, password).await()
        val uid = result.user?.uid ?: error("Firebase returned no user after registration")

        // 2. our profile document users/{uid}
        val profile = User(
            uid = uid,
            fullName = fullName.trim(),
            email = mail,
            phone = phone.trim(),
            createdAt = System.currentTimeMillis(),
        )
        db.collection("users").document(uid).set(profile).await()
        Unit
    }

    override suspend fun signIn(email: String, password: String): AppResult<Unit> = firebaseCall {
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
        Unit
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}