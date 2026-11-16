# MSD2026-Group3-Android

Shared-expenses Android app (Kotlin, Jetpack Compose) for the second project of
Mobile Software Development, Fall 2026, Group 3.

- Requirements and screens: [docs/requirements.md](docs/requirements.md)
- Architecture and Reliability QA: [docs/architecture.md](docs/architecture.md)
- Conventions and definition of done for humans and AI agents: [AGENTS.md](AGENTS.md)
- Compose UI workflow for humans and agents: [docs/frontend-workflow.md](docs/frontend-workflow.md)

## Firebase configuration

`app/google-services.json` is git-ignored. If it is present, the app builds
with the Google Services plugin and `AppContainer` uses the Firebase Auth and
Firestore repositories. Without it the app still builds and runs on the
in-memory fake repositories, so UI work and unit tests do not need Firebase.

The Firestore rules and indexes are in `firestore.rules` and
`firestore.indexes.json`; `firebase.json` points the Firebase Emulator Suite
at both files. To run the fake backend while `google-services.json` is present,
temporarily rename that file; `OweOweApplication` then chooses the fakes.

All changes go into `main` through a pull request with green CI and a review
from a teammate who didn't write the code.
