package com.example.oweowe.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason

/** Maps a named [ErrorReason] to user-facing text. Only call from a composable. */
@Composable
fun errorReasonMessage(reason: ErrorReason): String = stringResource(
    when (reason) {
        ErrorReason.INVALID_EMAIL -> R.string.error_invalid_email
        ErrorReason.EMAIL_ALREADY_IN_USE -> R.string.error_email_in_use
        ErrorReason.WEAK_PASSWORD -> R.string.error_weak_password
        ErrorReason.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
        ErrorReason.REAUTH_REQUIRED -> R.string.error_reauth_required
        ErrorReason.NO_ACCOUNT_WITH_EMAIL -> R.string.error_no_account_with_email
        ErrorReason.NO_ACCOUNT_WITH_PHONE -> R.string.error_no_account_with_phone
        ErrorReason.ALREADY_MEMBER -> R.string.error_already_member
        ErrorReason.BALANCE_NOT_ZERO -> R.string.error_balance_not_zero
        ErrorReason.INVALID_AMOUNT -> R.string.error_invalid_amount
        ErrorReason.NOTHING_TO_SETTLE -> R.string.error_nothing_to_settle
        ErrorReason.NOT_ALLOWED -> R.string.error_not_allowed
        ErrorReason.NETWORK -> R.string.error_network
        ErrorReason.UNKNOWN -> R.string.error_unknown
    },
)

/** The resource id of an error message, for ViewModels that emit [UiMessage]s. */
@StringRes
fun errorReasonRes(reason: ErrorReason): Int = when (reason) {
    ErrorReason.INVALID_EMAIL -> R.string.error_invalid_email
    ErrorReason.EMAIL_ALREADY_IN_USE -> R.string.error_email_in_use
    ErrorReason.WEAK_PASSWORD -> R.string.error_weak_password
    ErrorReason.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
    ErrorReason.REAUTH_REQUIRED -> R.string.error_reauth_required
    ErrorReason.NO_ACCOUNT_WITH_EMAIL -> R.string.error_no_account_with_email
    ErrorReason.NO_ACCOUNT_WITH_PHONE -> R.string.error_no_account_with_phone
    ErrorReason.ALREADY_MEMBER -> R.string.error_already_member
    ErrorReason.BALANCE_NOT_ZERO -> R.string.error_balance_not_zero
    ErrorReason.INVALID_AMOUNT -> R.string.error_invalid_amount
    ErrorReason.NOTHING_TO_SETTLE -> R.string.error_nothing_to_settle
    ErrorReason.NOT_ALLOWED -> R.string.error_not_allowed
    ErrorReason.NETWORK -> R.string.error_network
    ErrorReason.UNKNOWN -> R.string.error_unknown
}
