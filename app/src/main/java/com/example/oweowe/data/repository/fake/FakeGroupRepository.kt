package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeGroupRepository(private val db: FakeBackend) : GroupRepository {

    override fun observeMyGroups(): Flow<List<Group>> =
        combine(db.currentUserId, db.groups) { uid, groups ->
            groups.filter { uid != null && uid in it.memberIds }.sortedBy { it.name }
        }

    override fun observeGroup(groupId: String): Flow<Group?> =
        db.groups.map { groups -> groups.find { it.id == groupId } }

    override suspend fun createGroup(name: String, description: String): AppResult<String> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val group = Group(
            id = db.newId(), name = name.trim(), description = description.trim(),
            createdBy = me, createdAt = db.now(), memberIds = listOf(me),
        )
        db.groups.update { it + group }
        return AppResult.Success(group.id)
    }

    override suspend fun updateDetails(groupId: String, name: String, description: String): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        if (!db.isMember(groupId, db.currentUserId.value)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        changeGroup(groupId) { it.copy(name = name.trim(), description = description.trim()) }
        return AppResult.Success(Unit)
    }

    override suspend fun addMemberByEmail(groupId: String, email: String): AppResult<User> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        if (!db.isMember(groupId, db.currentUserId.value)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val user = db.users.value.find { it.email == email.trim().lowercase() }
            ?: return AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_EMAIL)
        if (db.isMember(groupId, user.uid)) return AppResult.Error(ErrorReason.ALREADY_MEMBER)
        changeGroup(groupId) { it.copy(memberIds = it.memberIds + user.uid) }
        return AppResult.Success(user)
    }

    override suspend fun removeMember(groupId: String, userId: String): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        if (!db.isMember(groupId, db.currentUserId.value)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        // TODO(stage 3): return BALANCE_NOT_ZERO if BalanceCalculator says userId still owes or is owed money.
        changeGroup(groupId) { it.copy(memberIds = it.memberIds - userId) }
        return AppResult.Success(Unit)
    }

    private fun changeGroup(groupId: String, change: (Group) -> Group) =
        db.groups.update { groups -> groups.map { if (it.id == groupId) change(it) else it } }
}