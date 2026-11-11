package com.example.oweowe.ui.components

import android.content.Context
import androidx.annotation.StringRes

/**
 * A one-off message for the user (snackbar). ViewModels emit these instead of
 * holding a Context: the screen owns the Context and resolves the text when
 * the message is shown.
 */
data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<String> = emptyList(),
)

/** Resolves the message to display text. */
fun UiMessage.asString(context: Context): String =
    if (args.isEmpty()) {
        context.getString(resId)
    } else {
        context.getString(resId, *args.toTypedArray())
    }
