---
name: android-compose-ui
description: Use when creating or changing Jetpack Compose screens, components, @Preview previews, UI state, ViewModels, navigation, theme, or strings in this Android app (app/src/main/java/**/ui/**, screen IDs S1-S11, FR1-FR10). Follows the repository frontend workflow and verifies with Gradle before hand-off.
---

# Android Compose UI workflow

Read `AGENTS.md`, `docs/requirements.md`, `docs/architecture.md` and
`docs/frontend-workflow.md` before editing. `docs/frontend-workflow.md` is the
canonical loop; this skill is the short version used when a request touches
the UI.

## Steps

1. Name the screen ID (S1-S11) and the FR/NFR. Read the closest existing
   screen or component and copy its patterns. If the docs do not decide a
   detail, ask or record the assumption in the PR; do not invent requirements.
2. Define one immutable `UiState` per screen with explicit loading, content,
   empty and error states. Expose it from the ViewModel as `StateFlow`. No
   `Context` in the ViewModel, no remote data source call.
3. Use a fake repository from `data/repository/fake/` or local sample data.
   Never call a repository, DAO or Firebase from a `@Composable`.
4. Keep composables stateless: the screen-level composable may take a
   ViewModel, nested composables take state and lambdas. Add
   `modifier: Modifier = Modifier`.
5. Add `@Preview` functions for Default, Loading, Empty and Error (skip only
   states that cannot occur, and say why). Use `showBackground = true` and
   `@PreviewScreenSizes` for full screens. Previews must compile without
   Firebase or an emulator.
6. Put all user-facing text in `res/values/strings.xml`. Keep money as `Long`
   in øre and format with `formatKr`. Add content descriptions and check
   contrast/touch targets.
7. Verify and report honestly:

   ```bash
   ./gradlew assembleDebug
   ./gradlew lint
   ./gradlew testDebugUnitTest
   ```

   Open the previews and run on an emulator when available, including
   airplane mode if the screen reads or writes data. Use Paparazzi/Roborazzi
   only if already configured; otherwise propose it in a separate `chore/` PR.

## Guardrails

- Kotlin + Compose Material 3 only; no XML layouts, no Java.
- No new dependency without a reason in the PR; declare it through
  `gradle/libs.versions.toml`.
- No screen may block on a network call; no network failure may dead-end the
  UI.
- Do not weaken tests, reformat unrelated code, or claim verification you did
  not run.
- Fill in the PR template, including screen IDs, FR/NFR, verification and AI
  disclosure.

For the full checklist, tool map and prompt template, see
`docs/frontend-workflow.md`.
