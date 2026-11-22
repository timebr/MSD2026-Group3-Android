# Frontend (UI) workflow

Canonical loop for anyone, human or agent, changing the Compose UI. It turns
the tool notes in `.context/frontend_agent_tools.md` into a workflow that
works with the tools the repository already has. Read
[AGENTS.md](../AGENTS.md), [requirements.md](requirements.md) and
[architecture.md](architecture.md) first; this file only describes *how* to
build a UI change.

## When this applies

Use this workflow for any change to:

- `app/src/main/java/com/example/oweowe/ui/**` (screens, components, theme)
- Compose navigation and bottom-bar destinations
- `@Composable` functions and `@Preview` functions
- a screen's `UiState` or ViewModel presentation logic
- `app/src/main/res/values/strings.xml` for UI text

Pure domain or repository changes follow [AGENTS.md](../AGENTS.md) instead.
If they change what a screen displays, still use this workflow for the UI
part.

## The loop

### 1. Frame the change

- Name the screen ID (S1–S11) and the FR/NFR it serves.
- Read the closest existing screen or component and copy its patterns.
- If the docs do not decide something (for example a missing state or a new
  interaction), stop and ask, or record the assumption in the PR. Do not
  invent requirements.

### 2. Define the state first

- One immutable `UiState` per screen, exposed from the ViewModel as
  `StateFlow`.
- Model loading, content, empty and error explicitly. One-off effects
  (snackbar, navigation) are separate from the persistent state.
- The error state is retryable, not terminal: use
  `flow.retryOnReadError(retryTrigger) { LoadError }` from
  `ui/components/ReadErrorRetry.kt`, expose `retry()`, and pass it to
  `ErrorState(onRetry = …)`. Never swallow a read error in the repository, and
  never let one escape a `viewModelScope` launch. See the read path in
  [architecture.md](architecture.md).
- The ViewModel never holds a `Context` and never calls the remote data
  source.

### 3. Use fake data, never the network

- If a `Fake…` repository exists under
  `app/src/main/java/com/example/oweowe/data/repository/fake/`, use it. Its
  `FakeBackend` has delay and failure knobs (`delayMs`, `failNextWith` for
  writes, `failReadsWith` for reads), so loading and load-error states can be
  reproduced on purpose.
- If no fake exists yet, keep previews self-contained with local sample data.
- Never call Firebase, a DAO or a repository from a `@Composable`.

### 4. Keep composables stateless

- The screen-level composable may take a ViewModel; everything below it takes
  state and lambdas only.
- Give every reusable composable a `modifier: Modifier = Modifier` parameter.
- Put shared UI in `ui/components/`, feature UI in `ui/<feature>/`.

### 5. Add previews for the states

Every screen-level composable and every new shared component gets at least:

- `Default` (normal content)
- `Loading`
- `Empty`
- `Error`

Skip a state only when it genuinely cannot occur, and say why in the PR. Use
fake state objects, `showBackground = true`, and `@PreviewScreenSizes` for
full screens. Previews must compile without Firebase or an emulator.

### 6. Strings, money and accessibility

- All user-facing text goes into `res/values/strings.xml`; no hardcoded UI
  text.
- Amounts stay `Long` in øre. Format with `formatKr` from `domain/` in the UI.
- Add content descriptions for icons and images, keep touch targets large
  enough, and do not rely on colour alone to show a state.

### 7. Verify before handing off

Run from the repository root:

```bash
./gradlew assembleDebug
./gradlew lint
./gradlew testDebugUnitTest
```

Then, when available:

- Open the previews in Android Studio and check every state.
- Run the app on an emulator and check the flow, including airplane mode for
  anything that reads or writes data.
- If Paparazzi or Roborazzi is already configured in `libs.versions.toml` and
  the app module, run its verify task. If it is not configured, do **not** add
  it in a UI feature PR; propose it in a separate `chore/` PR.

Report exactly what you ran. If a command or an emulator was unavailable, say
so in the PR instead of claiming it passed.

### 8. Hand off

- Fill in the PR template, including the screen IDs, FR/NFR, what was tested
  and the AI disclosure.
- Include screenshots or recordings for visible changes when possible.
- Keep the PR to one screen or component where practical.

## Tool map

The table comes from `.context/frontend_agent_tools.md`. The repository does
not install all of these yet, so the workflow degrades gracefully.

| Layer | Tool | Role | Status in this repo |
| --- | --- | --- | --- |
| Component spec | `@Preview` (Showkase optional) | Ground agents in existing UI components | Use `@Preview` now; Showkase only if already configured |
| Visual testing | Paparazzi / Roborazzi | Headless image diffing in the build loop | Not configured; propose a separate chore PR |
| Token sync | Style Dictionary / Figma API | Generate theme scales and tokens | Not configured; use Material 3 theme in `ui/theme/` |
| Architecture | Compose previews + fake repositories | Decouple UI from network and data | Fake repositories live in `data/repository/fake/` |
| Harness | Gradle CLI / Android Studio | Compile, lint, test, preview | Use the commands above |

## Guardrails

- Kotlin + Jetpack Compose (Material 3) only. No XML layouts, no Java.
- No new dependency without a reason in the PR, and declare it through
  `gradle/libs.versions.toml`.
- Never block a screen on a network call; no network failure may dead-end the
  UI.
- Do not disable, skip or weaken a test to make the build pass.
- Do not reformat or rename unrelated code.
- Do not mark something tested that you did not run.

## Prompt template

Give the agent the screen, the requirement IDs, the data source and the states
you expect. For example:

```text
Implement S4 Groups (FR3, FR5, FR6) following docs/frontend-workflow.md.
Use FakeGroupRepository and FakeExpenseRepository.
Show loading, empty, list and error states.
Add previews for all four states and keep all text in strings.xml.
```

Agents that support skills load the `android-compose-ui` skill automatically
for UI requests. The skill points back to this file, so this document stays
the single source of truth.

## Definition of done for a UI PR

- [ ] Screen ID and FR/NFR are named in the PR.
- [ ] State is exposed as one immutable `UiState` and covers loading, empty
      and error.
- [ ] The UI uses a fake repository or local preview data; no network call in
      a composable.
- [ ] Previews exist for the states that can occur.
- [ ] All user-facing text is in `strings.xml`; amounts use `formatKr`.
- [ ] `assembleDebug`, `lint` and `testDebugUnitTest` were run, or the PR
      says why not.
- [ ] The change was checked on an emulator, including offline behaviour when
      relevant.
- [ ] AI use is disclosed and logged as required by AGENTS.md.
