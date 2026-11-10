---
description: Implement or change a Jetpack Compose UI feature using the repository frontend workflow.
agent: build
---

Read `AGENTS.md`, `docs/frontend-workflow.md`, `docs/requirements.md` and
`docs/architecture.md` first. Then implement this UI request following the
workflow:

$ARGUMENTS

Requirements for the result:

- Name the screen ID (S1-S11) and FR/NFR in your summary.
- Keep one immutable `UiState` per screen with loading, content, empty and
  error states.
- Use fake repositories or local preview data; never call a repository or
  Firebase from a composable.
- Add `@Preview` functions for the states that can occur.
- Keep all user-facing text in `res/values/strings.xml` and money as `Long` in
  øre.
- Run `./gradlew assembleDebug`, `./gradlew lint` and
  `./gradlew testDebugUnitTest`, and report exactly what you ran.
