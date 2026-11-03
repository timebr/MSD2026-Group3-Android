# Architecture

Proposed architecture. It is the basis for the architecture diagram in the
implementation document, which must show how the app supports the
Reliability QA (NFR1–NFR5). Change this file in the same PR as any change to
the structure it describes.

## Stack

| Concern | Choice | Why |
| --- | --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 | Course target is native Android. |
| Navigation | Navigation Compose | Bottom bar + bottom sheets from the wireframe. |
| State | ViewModel + `StateFlow` | Survives configuration changes, easy to test. |
| Local data | Room | Source of truth for the UI, offline availability (NFR3). |
| Background retry | WorkManager | Survives process death, network constraints, backoff (NFR1). |
| Auth + cloud DB | Firebase Auth + Cloud Firestore (FR9) | Proposal; Supabase is the alternative named in the design document. |
| DI | Hilt (proposal) or manual wiring in `di/` | Decide in the setup PR. |
| Tests | JUnit, kotlinx-coroutines-test, Turbine, Room in-memory, Compose UI test | |

## Layers

```
┌──────────────── UI (Compose) ────────────────┐
│ Screens S1–S11  ←  ViewModels (UiState flow) │
└──────────────────────┬───────────────────────┘
                       │
┌──────────────── Repositories ────────────────┐
│ read: Room Flow     write: Room + Outbox     │
└───────┬──────────────────────────┬───────────┘
        │                          │ enqueue
┌───────▼────────┐        ┌────────▼───────────┐
│ Room (cache +  │◄───────┤ SyncWorker         │
│ outbox table)  │ status │ (WorkManager,      │
└────────────────┘        │ backoff, network)  │
                          └────────┬───────────┘
                                   │
                          ┌────────▼───────────┐
                          │ RemoteDataSource   │
                          │ Firebase | Fake    │
                          └────────────────────┘
```

## Write path (NFR1, NFR2, NFR4, NFR5)

1. ViewModel calls e.g. `expenseRepository.addExpense(...)`.
2. Repository writes the entity and an outbox row (`PENDING`, attempt count,
   last error) in one Room transaction and returns at once.
3. Repository enqueues unique work `sync-outbox` (`ExistingWorkPolicy.KEEP`,
   `NetworkType.CONNECTED`, exponential backoff).
4. `SyncWorker` sends rows oldest first. Temporary failure (no network,
   timeout, 5xx) → `Result.retry()`. Permanent failure (validation, 4xx) →
   `FAILED` with the error, shown in the UI with Retry.
5. Row succeeds after earlier failures → status `SYNCED` and a notification
   "Your expense … was synced" (NFR5).
6. The UI reads each entity together with its outbox status and shows a
   pending / synced / failed indicator (NFR4).

## Read path (NFR3)

- Screens observe Room only.
- Repositories refresh from the remote when online (snapshot listener or
  pull on screen open) and upsert into Room.
- With no network the last data stays visible, plus an offline banner.

## Domain

- `BalanceCalculator`: net balance per member from a group's expenses (FR5).
- `SplitCalculator`: splits an amount in øre among selected members; the
  remainder of an uneven split is assigned deterministically (e.g. to the
  first members in a stable order) so all devices agree.
- Optional: `SettleUpSuggester` to minimise the number of transfers.

All of it is pure Kotlin with unit tests.

## Testing the Reliability QA

The implementation document must cover testing of at least one Reliability
QA. Planned tests:

- Unit: `SyncWorker` with `FakeRemoteDataSource` failing N times, then
  succeeding → retried, status ends as `SYNCED`, notification requested.
- Unit: repository writes while the remote is down → data is in Room, status
  `PENDING`, UI state shows it.
- Instrumented / manual: airplane mode, add expense, see pending, reconnect,
  see synced and the notification. Record it for the demo video.
