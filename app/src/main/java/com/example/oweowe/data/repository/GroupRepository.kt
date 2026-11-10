package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
interface GroupRepository {
    fun observeMyGroups(): Flow<List<Group>>

    fun observeGroup(groupId: String): Flow<Group?>

    /** Returns the new group's ID. The creator is the first member. */
    suspend fun createGroup(name: String, description: String): AppResult<String>

    suspend fun updateDetails(groupId: String, name: String, description: String): AppResult<Unit>

    /** NO_ACCOUNT_WITH_EMAIL or ALREADY_MEMBER on failure; returns the added user. */
    suspend fun addMemberByEmail(groupId: String, email: String): AppResult<User>

    /** BALANCE_NOT_ZERO if the member still owes or is owed money. */
    suspend fun removeMember(groupId: String, userId: String): AppResult<Unit>
}
