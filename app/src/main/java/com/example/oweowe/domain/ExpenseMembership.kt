package com.example.oweowe.domain

/**
 * NFR2/NFR4: client-side membership guard for expense writes. It mirrors the
 * Firestore Security Rules so a rejected write fails before it is queued or
 * shown as "saving", instead of failing later on the server.
 *
 * The caller, the payer and every person in the split must be current members.
 */
fun canWriteExpense(
    uid: String,
    memberIds: Collection<String>,
    paidBy: String,
    splitAmong: Collection<String>,
): Boolean =
    splitAmong.isNotEmpty() &&
        uid in memberIds &&
        paidBy in memberIds &&
        splitAmong.all { it in memberIds }

/** A settlement may only be written between two current members of the group. */
fun canSettleUp(
    uid: String,
    memberIds: Collection<String>,
    withUserId: String,
): Boolean =
    uid in memberIds && withUserId in memberIds
