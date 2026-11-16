# AGENTS.md

Guidance for anyone (human or AI agent) changing this repository. Keep it
current: when an agent gets something wrong, add the rule here in the same PR
that fixes it. The git history of this file is graded as evidence of
engineering judgment, so prefer small, explained changes over rewrites.

## Project

Native Android app for the Mobile Software Development course (MSD2026,
Group 3, second project): a shared-expenses app in the spirit of WeShare or
MobilePay groups. Users create groups, log who paid what, split expenses
among all or selected members, see who owes whom, and remind people who owe
them money.

- Requirements (FR1–FR10, NFR#) and screen IDs (S1–S11):
  [docs/requirements.md](docs/requirements.md). The design document is the
  original; if the two disagree, the design document wins and this repo's copy
  gets fixed.
- Architecture and how it delivers the Reliability QA:
  [docs/architecture.md](docs/architecture.md).
- Reference FR/NFR and screen IDs in PR titles or descriptions, commit
  messages and KDoc where it helps (`// FR5: ...`).
- Deadlines: draft submission 2026-11-20, presentation 2026-11-24. Prefer a
  working Must-have (FR1–FR8 + reliability NFRs) over half-built extras.

## Hard constraints

- **Kotlin + Jetpack Compose (Material 3) only.** No XML layouts, no Java
  source files, no cross-platform frameworks.
- **Versions live in `gradle/libs.versions.toml`.** Declare every dependency
  and plugin through the version catalog, never inline in `build.gradle.kts`.
  Do not upgrade AGP, Kotlin, the Compose BOM or Firebase BOM as a side effect
  of another change; upgrades get their own PR.
- **Do not add a dependency without saying why in the PR.** Check first
  whether Jetpack/AndroidX or something already in the catalog covers it.
- **Never commit secrets.** `google-services.json`, keystores and
  `local.properties` are git-ignored. Document any new config key in the
  README instead.
- **Money is never a `Double` or `Float`.** Amounts are `Long` in minor units
  (øre) everywhere in domain, data and database; format to `kr` only in the UI.
- **The app must stay usable offline.** No screen may block on a network call,
  and no network failure may crash or dead-end the UI (see Reliability below).

## Architecture

```
app/src/main/java/com/example/oweowe/
  ui/<feature>/   Compose screens + ViewModel per feature (auth, home, groups,
                  activity, profile, expense)
  ui/components/  Shared composables
  ui/navigation/  NavHost, routes, bottom bar
  domain/         Pure Kotlin: balance/split/phone calculation. No Android imports.
  data/model/     User, Group, Expense, Reminder and sync-status types.
  data/repository/          Repository interfaces (the frontend/backend contract).
  data/repository/fake/     In-memory FakeBackend and repositories.
  data/repository/firebase/ Firebase Auth + Firestore implementations.
  data/connectivity/        ConnectivityObserver for the offline banner.
  sync/           SyncStatusStore: PENDING / SYNCED / FAILED for local writes.
  notifications/  Local FR7/FR8/NFR5 notifications and the snapshot decider.
  AppContainer.kt One place that picks Fake… or Firebase… implementations.
```

Data flow: Composable → ViewModel (`StateFlow<UiState>`) → repository
interface → Fake repositories or Firestore (offline cache + built-in write
queue). The UI only ever observes state from a `Flow`.

- Composables are stateless where possible: they take state and lambdas. Only
  screen-level composables get a ViewModel.
- Screens never touch a repository, DAO or Firebase directly.
- ViewModels expose one immutable `UiState` per screen and never hold a
  `Context`.
- Reads are repository `Flow`s backed by Firestore snapshot listeners, which
  also serve the last cached data offline. The UI never renders a network
  response directly.
- Writes go through a repository, which writes to Firestore without waiting
  for the server; Firestore shows the change locally and syncs it later.
- Balances and splits are computed only by the functions in `domain/`, so the
  home, groups and activity screens can't disagree (FR5, FR6).
- The implementation is chosen in `AppContainer.kt` only. The in-memory
  Fake… repositories (with delay/error/sync knobs) must keep working so the
  app and tests run without Firebase.

## Frontend (UI) workflow

Before changing anything under `ui/`, adding a screen or component, or
touching a screen's `UiState`/previews, read
[docs/frontend-workflow.md](docs/frontend-workflow.md) and follow its loop.
The short version:

1. Name the screen ID (S1–S11) and FR/NFR; copy the closest existing pattern.
2. Define one immutable `UiState` per screen with loading, content, empty and
   error states.
3. Use a fake repository (`data/repository/fake/`) or local preview data;
   never call a repository, DAO or Firebase from a `@Composable`.
4. Add `@Preview` functions for the states that can occur.
5. Run `./gradlew assembleDebug`, `./gradlew lint` and
   `./gradlew testDebugUnitTest`; check the screen on an emulator when
   possible, including airplane mode if it reads or writes data.

opencode loads the `android-compose-ui` skill automatically for UI requests.
Other agents should be pointed at `docs/frontend-workflow.md`, which is the
single source of truth. Do not add Showkase, Paparazzi, Roborazzi or token
tooling in a feature PR; propose it in a separate `chore/` PR.

## Reliability (NFR, graded)

The assignment requires fault tolerance, offline availability and user
feedback. Every feature that writes data follows this pattern:

1. The repository writes the change to Firestore and returns without waiting
   for the server. The change is in the local cache and visible immediately
   (NFR2), including for documents that were never fetched before.
2. Firestore's persistent cache and pending-write queue keep the write until a
   connection is available and retry it automatically, also after a restart
   (NFR1).
3. `sync/SyncStatusStore` maps Firestore metadata and write tasks to
   `PENDING` / `SYNCED` / `FAILED`. The UI shows that state next to rows and
   on forms, with a visible error reason (NFR4).
4. When a write that was pending or failed succeeds in the background,
   `NotificationCoordinator` posts a local notification (NFR5).
5. The Fake… repositories keep a switchable delay/failure/sync simulation so
   pending, failed and synced states can be demonstrated and unit-tested
   without Firebase.
6. Notifications are local and only work while the app process is alive; real
   push when the app is closed would need FCM plus Cloud Functions (Spark plan
   has neither). This limitation is documented in the architecture doc.

## Conventions

- Kotlin official code style; formatting and lint checked by
  `./gradlew ktlintCheck` (TBD with project setup) and `./gradlew lint`.
- Composables and classes in PascalCase, functions and properties in
  camelCase, one top-level screen per file (`GroupsScreen.kt`,
  `GroupsViewModel.kt`).
- Every screen-level composable has a `@Preview` with fake state.
- All user-facing text in `res/values/strings.xml`. App language: English.
- IDs are `String` UUIDs generated on the device, so offline-created objects
  have stable IDs before they sync.
- Times are epoch milliseconds (UTC) in data and domain; convert in the UI.
- Coroutines: use `viewModelScope`, inject dispatchers, never `GlobalScope`
  or `runBlocking` in app code.
- Unit tests in `app/src/test` (JUnit, kotlinx-coroutines-test, Turbine for
  flows); Firebase Emulator integration and Compose UI tests in
  `app/src/androidTest`.

## Commands

```bash
./gradlew assembleDebug              # build
./gradlew lint                       # Android lint
./gradlew testDebugUnitTest          # unit tests (domain, ViewModels, sync)
./gradlew connectedDebugAndroidTest  # instrumented tests (needs emulator)
# Firebase Emulator Suite: firebase emulators:start --project demo-oweowe
# then adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8080 tcp:8080
```

Run lint and unit tests before pushing. If you can't run a command (no SDK,
no emulator), say so in the PR instead of claiming it passed.

## Git and pull requests

- Never push to `main`. One branch per change, named
  `feat/…`, `fix/…`, `docs/…`, `test/…` or `chore/…`.
- Keep PRs small: one feature or fix, ideally under ~400 changed lines.
- Fill in the PR template, including the AI disclosure section.

## Definition of done

A change is done when:

1. It is merged into `main` through a pull request.
2. CI is green: build, lint, unit tests.
3. New logic (domain calculations, repositories, sync, ViewModels) has tests.
   Balance/split code needs tests for uneven splits and rounding.
4. A teammate who did not write or generate the code has reviewed and
   approved it, and can explain every line of it.
5. It runs on an emulator or a real device, including in airplane mode for
   anything that reads or writes data.
6. AI use is logged in the design document's Appendix A (use case, tool,
   mode, prompt or chat link, verification, decision). AI output that was
   wrong or off-spec goes into the failure log (at least 3 entries per
   project).

## Rules for AI agents

- Read this file, `docs/requirements.md` and `docs/architecture.md` before
  changing code.
- Do only what the task asks. Don't reformat, rename or "clean up" unrelated
  code.
- Don't invent requirements. If a task needs a decision the docs don't cover,
  stop and ask, or list the assumption in the PR.
- Don't disable, skip or weaken a test to make CI pass.
- Don't mark something as tested or verified that you did not run.
- When you get something wrong and a human corrects it, propose the rule that
  would have prevented it as an addition to this file.
