```mermaid
---
id: b7975f0d-fdc1-499e-8a5a-a3d4746ee932
---
flowchart TD
    SignIn[Sign in] <--> Register[Register]
    SignIn <--> Home
    Register --> Home

    subgraph Navigation["Primary navigation"]
        Home[Home dashboard]
        Groups[Groups]
        AddExpense[Add expense]
        Activity[Activity]
        Profile[Profile]
    end

    Home --> Groups
    Home --> AddExpense
    Home --> Activity
    Home --> Profile

    Groups --> CreateGroup[Create group]
    Groups --> GroupOverview[Group overview]
    GroupOverview --> EditGroup[Edit group]
    GroupOverview --> ManageMembers[Manage members]
    GroupOverview --> AddExpense[Add expense]
    GroupOverview --> Home[Home dashboard]

    CreateGroup --> GroupOverview[Group overview]



    Profile --> EditProfile[Edit profile]
    Profile --> NotificationSettings[Notification settings]
    ...
```