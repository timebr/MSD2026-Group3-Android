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
app/src/main/java/dk/sdu/msd/group3/   (package TBD with the first PR)
  ui/<feature>/   Compose screens + ViewModel per feature (auth, home, groups,
                  activity, profile, expense)
  ui/components/  Shared composables
  ui/navigation/  NavHost, routes, bottom bar
  domain/         Pure Kotlin: balance/split calculation, validation. No
                  Android imports.
  data/local/     Room database, DAOs, entities (source of truth)
  data/remote/    RemoteDataSource interface + Firebase and Fake implementations
  data/repo/      Repositories combining local + remote
  sync/           Outbox, WorkManager workers, sync status
  notifications/  Notification channels and posting
  di/             Dependency wiring (one place to swap implementations)
```

Data flow: Composable → ViewModel (`StateFlow<UiState>`) → Repository →
Room (read) / Outbox + WorkManager (write) → RemoteDataSource.

- Composables are stateless where possible: they take state and lambdas. Only
  screen-level composables get a ViewModel.
- Screens never touch a repository, DAO or Firebase directly.
- ViewModels expose one immutable `UiState` per screen and never hold a
  `Context`.
- Reads come from Room as `Flow`. Remote data is written into Room and the UI
  updates from there; the UI never renders a network response directly.
- Every write goes through the outbox (see below). Never call the remote data
  source from a ViewModel.
- Balances and splits are computed only by the functions in `domain/`, so the
  home, groups and activity screens can't disagree (FR5, FR6).
- The remote implementation is chosen in `di/` only. `FakeRemoteDataSource`
  (in-memory, with a switch to simulate failures) must keep working so the app
  and tests run without Firebase.

## Reliability (NFR, graded)

The assignment requires fault tolerance, offline availability and user
feedback. Every feature that writes data must follow this pattern:

1. Write the change to Room and an `outbox` row with status `PENDING` in one
   transaction. The UI updates immediately.
2. Enqueue a unique `WorkManager` job with a `NetworkType.CONNECTED`
   constraint and exponential backoff. Retries happen in the background.
3. The worker sends outbox rows in order; on success mark `SYNCED`, on a
   permanent error mark `FAILED`, on a temporary error return `Result.retry()`.
4. The UI shows the status of each pending item (pending / synced / failed)
   and offers Retry on failed ones.
5. When a request that had failed before succeeds in the background, post a
   notification (or a snackbar if the app is in the foreground).

Do not rely on Firestore's built-in offline cache as the only mechanism: we
must be able to show and test the states ourselves.

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
- Times are `Instant` in UTC in data and domain; convert in the UI.
- Coroutines: use `viewModelScope`, inject dispatchers, never `GlobalScope`
  or `runBlocking` in app code.
- Unit tests in `app/src/test` (JUnit, kotlinx-coroutines-test, Turbine for
  flows); Room and Compose UI tests in `app/src/androidTest`.

## Commands

```bash
./gradlew assembleDebug              # build
./gradlew lint                       # Android lint
./gradlew testDebugUnitTest          # unit tests (domain, ViewModels, sync)
./gradlew connectedDebugAndroidTest  # instrumented tests (needs emulator)
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
