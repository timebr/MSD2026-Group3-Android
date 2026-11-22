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
| DI | Manual wiring in `AppContainer.kt` | Picks Firebase when configured, otherwise the in-memory fakes. |
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

NotificationCoordinator: listens to expenses, reminders and the sync state of
own writes and posts local notifications (FR7, FR8, NFR5). SyncStatusStore
turns Firestore metadata and write tasks into PENDING / SYNCED / FAILED.
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
                              phoneNormalized, photoUri, pushEnabled, createdAt
groups/{groupId}              name, description, createdBy, createdAt,
                              memberIds[]
  expenses/{expenseId}        type (EXPENSE | SETTLEMENT), description,
                              category, amountOre, paidBy, splitAmong[],
                              createdBy, createdAt, updatedBy, updatedAt,
                              receiptUri
  reminders/{reminderId}      fromUserId, toUserId, amountOre, createdAt
```

Times are epoch milliseconds (UTC); money is whole øre. `phoneNormalized` is
the digits-only phone (a leading Danish +45 removed) so members can be found
by phone without depending on formatting. `photoUri` and `receiptUri` are
device-local paths for now (see the limitation below).

- "My groups" is the query `memberIds array-contains <my uid>`.
- A member is added by looking up their email or normalized phone in
  `users`; only registered users can be added.
- Settling up is an expense with `type = SETTLEMENT`: the debtor pays, the
  creditor is the only entry in `splitAmong`, the amount is the open balance.
- Any group member may edit or delete an expense (purpose, category, amount,
  payer, split, receipt); the expense stays in its group and keeps its creator.
  Deleting removes the row for everyone. Settlements are balance corrections,
  so they can be neither edited nor deleted.
- A member can only be removed when all their pairwise balances are 0.

## Repository interfaces

The interfaces are the contract between frontend and backend. Each has a
`Fake…` implementation with seed data (plus switchable delay and error knobs)
and a `Firestore…` implementation.

- Suspend operations (writes, lookups such as "find user by email") return an
  `AppResult` with a named `ErrorReason`; they never throw.
- Read functions return a `Flow`. A failing listener is **not** hidden by the
  repository: it propagates to the collector, so a screen can tell "empty"
  from "could not read". Every collector must therefore handle it — including
  the one-shot collectors in `init` blocks (see the read path below).

| Interface | Responsibility | Screens |
| --- | --- | --- |
| `AuthRepository` | register, sign in, sign out, current user | S1, S2 |
| `UserRepository` | own profile, push setting, find user by email | S6, S10 |
| `GroupRepository` | my groups, create, edit details, add / remove member | S4, S7–S10 |
| `ExpenseRepository` | expenses with sync status, add, edit, delete, settle up | S3, S5, S11 |
| `ReminderRepository` | send a reminder | S3, S4 |
| `NotificationCoordinator` | local notifications for FR7, FR8, NFR5 | all |

## Write path (NFR1, NFR2, NFR4, NFR5)

1. ViewModel calls e.g. `expenseRepository.addExpense(...)`.
2. The repository writes the document to Firestore and does not wait for the
   server; the expense is in the local cache and visible at once (NFR2).
3. Without a network, Firestore keeps the write in its pending queue and sends
   it automatically when the connection returns, also after an app restart
   (NFR1).
4. `SyncStatusStore` combines Firestore's `hasPendingWrites` with the write
   task callbacks: PENDING while queued, SYNCED after acknowledgement, FAILED
   when the task is rejected (e.g. by Security Rules). Forms show the failure
   reason; the user can retry the action (NFR4).
5. When an own document switches from pending (or failed) to synced, the
   `NotificationCoordinator` posts "… was synced in the background" (NFR5).

## Read path (NFR3, NFR4)

- Screens observe repository `Flow`s backed by Firestore snapshot listeners.
- Firestore serves the last known data from its offline cache when the device
  is offline; the UI shows an offline banner. Serving the cache does not throw,
  so offline still shows data rather than the error state.
- A listener error (e.g. Security Rules, a revoked session) reaches the
  ViewModel as a thrown exception. `retryOnReadError`
  (`ui/components/ReadErrorRetry.kt`) turns it into the screen's `loadError`
  state and then waits: each `retry()` tap re-collects the same cold flows and
  restores the data. The error state keeps the offline flag from the last good
  state, so the banner does not disappear.
- All six screens with reads implement that state (`loading`, content, empty,
  `loadError` with a Retry button, plus `@Preview`s). Two collectors are not
  part of a `UiState` and handle the error themselves: the S7 edit-form
  pre-fill and the "remind" amount lookup report it as a message, the NFR5
  background-sync watcher skips the snackbar, and `NotificationCoordinator`
  retries after 30 s instead of ending notifications for the session.
- To demonstrate the error path without Firebase, the developer options on S6
  arm `FakeBackend.failReadsWith` ("Fail every read"); the fake repositories
  then throw like a failed listener, so the error state and its Retry button
  can be shown and recorded.

## Notifications (FR7, FR8)

- `NotificationCoordinator` listens to `expenses` and `reminders` in the
  user's groups. `NotificationDecider` compares successive snapshots and
  produces a notification for a new expense, an expense edited by another
  member, or a reminder addressed to the user.
- The first snapshot of each group is only used to seed the known ids, so
  opening the app does not notify about old data. Own writes are ignored.
- `pushEnabled = false` suppresses FR7/FR8 notifications and the NFR5 sync
  notice.
- Limitation: notifications only arrive while the app process is running.
  Push with the app closed would need FCM plus Cloud Functions (Blaze plan).
  Profile photos and receipts are also device-local until Firebase Storage is
  enabled.

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
| Integration | Auth, group, expense, reminder repositories and Security Rules, seeded with a fixed scenario | Instrumented tests on the Android emulator against the Firebase Emulator Suite (`adb reverse tcp:9099 tcp:9099` and `tcp:8080`) |
| Contract | The same test cases against `Fake…` and `Firestore…` | Both of the above |
| Manual / demo | Airplane mode on, add an expense, see pending, reconnect, see synced and the notification; second emulator receives the FR7 notification | Two Android emulators against the real Spark project; recorded for the demo video |
