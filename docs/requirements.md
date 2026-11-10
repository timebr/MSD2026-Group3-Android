# Requirements and screens

Working copy of the design document's requirements, so agents and reviewers
can look up IDs. The design document is the original; update this file in the
same PR when the design document changes.

## Functional requirements

### Must have

| ID  | Requirement | How (from the design document) | Screens |
| --- | --- | --- | --- |
| FR1 | Create a profile with full name, phone, email, password and a default avatar / initials. | Unauthenticated users see Sign in / Register before reaching Home. | S1, S2 |
| FR2 | Update profile data (optional photo) and manage notification preferences. | Profile tab: edit name, email, phone, Change photo, Push notifications toggle, save. | S6 |
| FR3 | Create groups, add / remove participants, set group name and description. | "+ Group" action, Edit details, Members / Add member by full name and email. | S4, S7, S8, S9, S10 |
| FR4 | Add expenses with payer and amount, split among all or selected members. | Central + button: group, purpose / category, amount, Paid by, Split with (avatars). | S11 |
| FR5 | Calculate how much each person owes or is owed in a group. | Calculation engine over all group expenses; results shown on Home, Groups, Activity. | S3, S4, S5 |
| FR6 | Summary of all transactions and current balance (owed / owing) per group. | Balances on Home, Group overview, Activity with total spent, balance and chronological list. | S3, S4, S5 |
| FR7 | Notify group members of new expenses and updates. | In-app or push notification, respecting the user's push preference. | all |
| FR8 | Send reminders to members who owe you money. | Remind button next to owing members on Home (Balances) and Group overview. | S3, S4 |

### Should have

| ID  | Requirement | How |
| --- | --- | --- |
| FR9 | Authentication and security via a third-party provider (Firebase / Supabase) linked to a cloud database. | Provider stores credentials and returns a UID; the database stores profile and notification settings under that UID. Passwords are never stored locally. |

### Could have

| ID   | Requirement | How |
| ---- | --- | --- |
| FR10 | Attach receipts or invoices to expenses. | "Add receipt +" / camera upload in Add expense, linked to the expense record. |

## Non-functional requirements

The design document's NFR section is still empty. These are the Reliability QA
requirements from the assignment, numbered so we can reference them. Replace
with the design document's numbering once it exists.

| ID   | Quality attribute | Requirement |
| ---- | --- | --- |
| NFR1 | Fault tolerance | A request that fails because the network is temporarily unavailable is retried in the background. |
| NFR2 | Fault tolerance | Failed requests never stop the user from continuing to use the app. |
| NFR3 | Offline availability | Previously retrieved data stays accessible when the device is offline. |
| NFR4 | User feedback | The app shows whether a request is pending, successful or failed. |
| NFR5 | User feedback | The user is notified when a previously failed request succeeds in the background. |

## Screens (from the low-fi wireframe)

Bottom navigation: Home, Groups, + (Add expense), Activity, Profile.
S7–S11 are bottom sheets over the blurred previous screen.

| ID  | Screen | Contents |
| --- | --- | --- |
| S1  | Sign in | Sign in / Register toggle, email, password, Sign in. |
| S2  | Register (New user) | Full name, email, password, phone, Create user. |
| S3  | Home | Greeting, current group card, your balance with Add expense / Remind, Balances list per member. |
| S4  | Groups | Your groups list, "+ Group", selected group overview (members, balance, Edit details, Members). |
| S5  | Activity | Total spent, balance, list of all transactions. |
| S6  | Profile | Avatar, full name, email, phone, Push notifications toggle, save. |
| S7  | Edit group details (sheet) | Group name, description, Save. |
| S8  | Group members (sheet) | Member list with Remove, "+ Add member". |
| S9  | New group (sheet) | Group name, description, Create group. |
| S10 | Add member (sheet) | Full name, email, Add to group. |
| S11 | Add expense (sheet) | Group, purpose, category, amount, Paid by, Split with, Add receipt, Split expense. |

## Open questions

- Is the app name final? The wireframe and design document still say
  "App name" / `APP_TITLE`.
- FR7 / FR8: real push (FCM plus a server or Cloud Function) or in-app
  notifications from database listeners plus local notifications?
- Can a member be added who has no account yet (invite by email), or only
  registered users?
- Single currency (DKK) only?
