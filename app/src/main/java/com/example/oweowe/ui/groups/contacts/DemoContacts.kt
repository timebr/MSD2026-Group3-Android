package com.example.oweowe.ui.groups.contacts

/**
 * Demo-only suggestions (D3): every email here exists in the fake backend, so
 * "Add" succeeds on a clean emulator. Shown only when the device has no
 * contacts with an email address. Delete once the real backend can look up
 * people by phone/email.
 */
internal val DEMO_CONTACTS = listOf(
    ContactSuggestion(name = "Maja Contact", email = "maja@test.dk"),
    ContactSuggestion(name = "Niels Contact", email = "niels@test.dk"),
    ContactSuggestion(name = "Sofie Contact", email = "sofie@test.dk"),
)
