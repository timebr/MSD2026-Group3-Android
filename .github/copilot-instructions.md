# Copilot instructions

All project rules live in [AGENTS.md](../AGENTS.md). Read it, together with
`docs/requirements.md`, `docs/architecture.md` and `docs/frontend-workflow.md`,
before making changes.

For any UI change, follow [docs/frontend-workflow.md](../docs/frontend-workflow.md):
one immutable `UiState` per screen, fake repositories or local preview data,
`@Preview` for loading/content/empty/error, strings in `strings.xml`, money as
`Long` in øre, and `./gradlew assembleDebug lint testDebugUnitTest` before
hand-off.
