```mermaid
flowchart TD
    SignIn[Sign in] <--> Register[Register]
    SignIn --> Home
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

    Profile --> EditProfile[Edit profile]
    Profile --> NotificationSettings[Notification settings]
    ...