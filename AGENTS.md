# AGENTS.md

Guidance for anyone (human or AI agent) changing this repository. Keep it
current: when an agent gets something wrong, add the rule here in the same PR
that fixes it.

> DRAFT: sections marked `TODO(design-doc)` are filled in once the assignment
> and the design document are available.

## Project

Native Android app for the Mobile Software Development course (MSD2026,
Group 3), second project. Requirements (FR#, NFR#) and the navigation model
(screen IDs S1, S2, …) live in the group's design document; reference those
IDs in PRs, commit messages and code comments.

TODO(design-doc): one paragraph on what the app does and for whom.

## Hard constraints

- TODO(design-doc): language/UI toolkit (assumed: Kotlin + Jetpack Compose),
  `minSdk` / `targetSdk`, required libraries or ones the course forbids.
- Never bump the Android Gradle Plugin, Kotlin or Compose BOM versions as a
  side effect of another change; version upgrades get their own PR.
- Dependencies are declared in `gradle/libs.versions.toml` (version catalog),
  never inline in `build.gradle.kts`.
- Never commit secrets, keystores or `local.properties`. Configuration goes in
  `local.properties` / environment variables and is documented in the README.

## Architecture

TODO(design-doc): confirm layers and module layout. Default proposal:

```
app/src/main/java/<package>/
  ui/           Compose screens + ViewModels, one package per screen
  domain/       Pure Kotlin business rules, no Android imports
  data/         Repository interfaces and implementations
  model/        Data classes
```

- Screens observe `StateFlow` from a ViewModel; they never call a repository
  directly.
- The data source is chosen in one place (DI module); replacing it must not
  require changes above `data/`.
- Business rules live in `domain/` as pure functions so they can be unit
  tested without an emulator.

## Conventions

- Kotlin official code style, enforced by TODO(ktlint/detekt).
- Composables in PascalCase, previews for every screen-level composable.
- Strings in `res/values/strings.xml`, no hardcoded UI text.
- Unit tests in `src/test`, instrumented/UI tests in `src/androidTest`.

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew lint                 # Android lint
./gradlew testDebugUnitTest    # unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (emulator needed)
```

## Definition of done

A change is done when:

1. It is on a branch and merged into `main` through a pull request.
2. CI is green: build, lint, unit tests.
3. New logic (domain rules, repositories, ViewModels) has tests.
4. A teammate who did not write or generate the code has reviewed and approved
   it, and can explain it.
5. It runs on an emulator or a real device.
6. AI use is logged in the design document's AI appendix (tool, task, prompt or
   link, how it was verified, decision), and wrong AI output in the failure log.
