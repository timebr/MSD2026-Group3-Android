# Architecture

Architecture of the app and its data layer. It is the basis for the
architecture diagram in the implementation document, which must show how the
app supports the Reliability QA (NFR1–NFR5). The reasons behind each choice
are in [design-decisions.md](design-decisions.md). Change this file in the
same PR as any change to the structure it describes.

## Stack

| Concern | Choice | Why |
| --- | --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 | Course target is native Android. |
| Navigation | Navigation Compose | Bottom bar + bottom sheets from the wireframe. |
| State | ViewModel + `StateFlow` | Survives configuration changes, easy to test. |
| Auth | Firebase Auth (email + password) | FR9; passwords never stored by the app. |
| Cloud DB + offline | Cloud Firestore with offline persistence | Source of truth, offline cache and write queue in one (NFR1–NFR4). |
| Plan | Firebase Spark (free) | No credit card; no Cloud Functions, FCM or Storage. |
| Notifications | Local Android notifications from Firestore listeners | FR7, FR8, NFR5 while the app is running. |
| DI | Hilt (proposal) or manual wiring in `di/` | Switches fake and Firestore repositories in one place. |
| Tests | JUnit, kotlinx-coroutines-test, Turbine, Firebase Emulator Suite, Compose UI test | |

Room, a custom outbox table and WorkManager were part of the first draft and
were dropped: Firestore already provides the offline cache and the retry queue.

## Layers

```
┌──────────────── UI (Compose) ────────────────┐
│ Screens S1–S11  ←  ViewModels (UiState flow) │
└──────────────────────┬───────────────────────┘
                       │  repository interfaces (the contract)
┌──────────────── Repositories ────────────────┐
│ Auth · User · Group · Expense · Reminder     │
│ Fake… (in memory)   |   Firestore…           │
└───────┬──────────────────────────┬───────────┘
        │ uses                     │ read / write
┌───────▼────────┐        ┌────────▼───────────┐
│ Domain (pure   │        │ Firestore SDK      │
│ Kotlin):       │        │ offline cache +    │
│ SplitCalculator│        │ pending-write queue│
│ BalanceCalc.   │        └────────┬───────────┘
└────────────────┘                 │ when online
                          ┌────────▼───────────┐
                          │ Cloud Firestore    │
                          │ + Security Rules   │
                          └────────────────────┘

NotificationService: listens to expenses, reminders and the sync state of
own writes and shows local notifications.
```

## ViewModel layer

ViewModels sit between the views (Compose screens) and the backend
(repositories and Firebase). Neither side knows the other.

- **Views** only render a `UiState` and send user events to their ViewModel.
  They never call a repository or Firebase directly.
- **ViewModels** hold the screen's `UiState` as a `StateFlow`, call the
  repository interfaces, use the domain calculators (e.g. balances), and turn
  named errors and sync status into what the screen shows (loading, pending,
  synced, failed, "No account with this email").
- **Repositories** know nothing about screens or Android UI classes.

| ViewModel | Screens | Main work |
| --- | --- | --- |
| `AuthViewModel` | S1, S2 | sign in, register, form validation |
| `HomeViewModel` | S3 | current group, own balance, balances per member, remind |
| `GroupsViewModel` | S4, S9 | my groups, selected group overview, create group |
| `GroupDetailsViewModel` | S7, S8, S10 | edit details, members, add by email, remove (balance 0) |
| `AddExpenseViewModel` | S11 | group, amount, payer, split selection, add or edit an expense |
| `ActivityViewModel` | S5 | total spent, balance, transaction list with sync status, delete, settle up |
| `ProfileViewModel` | S6 | profile, push setting |

ViewModels get their repositories through constructor injection, so the same
ViewModel runs against the `Fake…` or the `Firestore…` implementation. That
makes them unit-testable without any UI and without Firebase.

## Data model

Balances are never stored; they are calculated from the expenses. Amounts are
DKK in whole øre (`Long`).

```
users/{uid}                   fullName, email (lower case), phone,
                              pushEnabled, createdAt
groups/{groupId}              name, description, createdBy, createdAt,
                              memberIds[]
  expenses/{expenseId}        type (EXPENSE | SETTLEMENT), description,
                              category, amountOre, paidBy, splitAmong[],
                              createdBy, createdAt
  reminders/{reminderId}      from, to, amountOre, createdAt
```

- "My groups" is the query `memberIds array-contains <my uid>`.
- A member is added by looking up their email in `users`; only registered
  users can be added.
- Settling up is an expense with `type = SETTLEMENT`: the debtor pays, the
  creditor is the only entry in `splitAmong`, the amount is the open balance.
- Any group member may edit or delete an expense (purpose, category, amount,
  payer, split, receipt); the expense stays in its group and keeps its creator.
  Deleting removes the row for everyone. Settlements are balance corrections,
  so they can be neither edited nor deleted.
- A member can only be removed when all their pairwise balances are 0.

## Repository interfaces

The interfaces are the contract between frontend and backend. Each has a
`Fake…` implementation with seed data (plus a switchable delay and error) and
a `Firestore…` implementation. Read functions return a `Flow`; errors are
returned as named results, not thrown.

| Interface | Responsibility | Screens |
| --- | --- | --- |
| `AuthRepository` | register, sign in, sign out, current user | S1, S2 |
| `UserRepository` | own profile, push setting, find user by email | S6, S10 |
| `GroupRepository` | my groups, create, edit details, add / remove member | S4, S7–S10 |
| `ExpenseRepository` | expenses with sync status, add, edit, delete, settle up | S3, S5, S11 |
| `ReminderRepository` | send a reminder | S3, S4 |
| `NotificationService` | local notifications for FR7, FR8, NFR5 | all |

## Write path (NFR1, NFR2, NFR4, NFR5)

1. ViewModel calls e.g. `expenseRepository.addExpense(...)`.
2. The repository writes the document to Firestore and does not wait for the
   server; the expense is in the local cache and visible at once (NFR2).
3. Without a network, Firestore keeps the write in its pending queue and sends
   it automatically when the connection returns, also after an app restart
   (NFR1).
4. The UI shows each document's state: pending while `hasPendingWrites` is
   true, synced when it turns false, failed when the write task fails (e.g.
   rejected by the Security Rules), with a Retry action (NFR4).
5. When an own document switches from pending to synced, the
   `NotificationService` shows "Your expense … was synced" (NFR5).

## Read path (NFR3)

- Screens observe repository `Flow`s backed by Firestore snapshot listeners.
- Firestore serves the last known data from its offline cache when the device
  is offline; the UI shows an offline banner.

## Notifications (FR7, FR8)

- Each app listens to `expenses` and `reminders` in the user's groups and
  shows a local notification for new documents created by someone else, or
  reminders addressed to the user.
- Only documents newer than the last app start are notified.
- `pushEnabled = false` suppresses FR7 and FR8 notifications.
- Limitation: notifications only arrive while the app is running. Push with
  the app closed would need FCM plus Cloud Functions (Blaze plan).

## Domain

- `SplitCalculator`: splits an amount in øre equally among the selected
  members; the remainder goes 1 øre each to the first members sorted by UID,
  so the shares always add up and all devices agree.
- `BalanceCalculator`: pairwise net balances ("Anna owes Tim 120 kr."), each
  user's total, and the group's total spent (expenses only, no settlements).

All of it is pure Kotlin with unit tests.

## Security Rules

| Path | Read | Write |
| --- | --- | --- |
| `users/{uid}` | any signed-in user | only the user themselves |
| `groups/{id}` | members only | create: creator in `memberIds`; update: members |
| `…/expenses/{id}` | members only | create: member, `createdBy` = own uid, amount > 0; update and delete: members (any member may edit or delete an expense) |
| `…/reminders/{id}` | members only | create: member, `from` = own uid |

The rules cannot calculate balances, so "remove only with balance 0" and
"settlement = open balance" are enforced by the app only.

## Testing the backend without a frontend

| Level | What | Where |
| --- | --- | --- |
| Unit | `SplitCalculator`, `BalanceCalculator`, validation | JVM |
| ViewModel | Each ViewModel against `Fake…` repositories: UiState for loading, success, pending and error (Turbine) | JVM |
| Integration | Firestore repositories and Security Rules, seeded with a fixed scenario | Instrumented tests on the Android emulator against the Firebase Emulator Suite (`10.0.2.2`) |
| Contract | The same test cases against `Fake…` and `Firestore…` | Both of the above |
| Manual / demo | Airplane mode on, add an expense, see pending, reconnect, see synced and the notification; second emulator receives the FR7 notification | Two Android emulators against the real Spark project; recorded for the demo video |
