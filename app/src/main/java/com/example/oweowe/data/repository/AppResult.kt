package com.example.oweowe.data.repository

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val reason: ErrorReason) : AppResult<Nothing>
}

enum class ErrorReason {
    // Auth
    INVALID_EMAIL,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,          // Firebase requires at least 6 characters
    WRONG_CREDENTIALS,
    // Groups and members
    NO_ACCOUNT_WITH_EMAIL,
    NO_ACCOUNT_WITH_PHONE,
    ALREADY_MEMBER,
    BALANCE_NOT_ZERO,
    // Expenses
    INVALID_AMOUNT,
    NOTHING_TO_SETTLE,
    // General
    NOT_ALLOWED,            // rejected by the Security Rules
    NETWORK,
    UNKNOWN,
}