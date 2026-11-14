# Design decisions

Decisions for the backend and data layer, agreed on 9 November 2026. Each one
has a short justification so it can go straight into the report. The full
plan is in the backend plan document; the resulting structure is in
[architecture.md](architecture.md).

## Architecture

1. **Firebase (Auth + Cloud Firestore) instead of a custom server or Supabase.**
   Built-in offline cache on Android and no server to run.
2. **No Room, no custom outbox, no WorkManager.** Firestore's offline
   persistence and write queue cover NFR1–NFR4, which means less code.
3. **Free Spark plan, no Cloud Functions or FCM.** No credit card needed, and
   the demo runs on two emulators.
4. **Repository interfaces with fake and Firestore implementations.** Frontend
   and backend can be built in parallel.
5. **MVVM: a ViewModel layer between views and backend.** Screens only render
   state, ViewModels talk to the repositories, so UI and data layer stay
   independent and ViewModels can be unit-tested against fakes.

## Data model

6. **Balances are calculated from expenses, never stored.** There is one
   source of truth, so there is nothing to keep in sync.
7. **Amounts are DKK only, stored as whole øre (`Long`).** This avoids
   floating-point rounding errors.
8. **Expenses split equally only, with the remainder assigned by sorted UID.**
   Shares always add up exactly, and all devices agree.
9. **Members must already have an account and are found by email.** A member
   is always a UID, with no placeholder users.
10. **Settling up is a special expense (`SETTLEMENT`), marked by the debtor,
   with no verification.** No extra logic is needed in the balance
   calculation.
11. **Any group member can edit or delete an expense; settlements are excluded.**
    Editing fixes a wrong amount, payer or split without deleting and
    re-adding the row, which would lose its history; the expense keeps its
    group and `createdBy`. Deleting is confirmed first and removes the row for
    the whole group. A concurrent offline edit resolves last write wins,
    because each edit replaces the whole row.
12. **A member can only be removed when their balance is 0.** This avoids
    orphaned debts.

## Known limitations

- Notifications are local and only arrive while the app is running. Real push
  would need FCM plus Cloud Functions.
- Balance rules are only checked in the app; the Security Rules cannot
  calculate them.
- Any signed-in user can read other users' emails, which the member search
  needs.
- Profile photos and receipts are probably dropped, because Firebase Storage
  likely requires the Blaze plan (not verified yet).

## Testing

- Unit tests for the calculation logic, plus integration tests against the
  Firebase Emulator Suite.
- A manual airplane-mode test on two emulators serves as the Reliability QA
  test and is recorded for the video.
