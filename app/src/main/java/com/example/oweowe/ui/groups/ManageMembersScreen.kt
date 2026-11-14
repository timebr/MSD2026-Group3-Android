package com.example.oweowe.ui.groups

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppBottomSheet
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SheetHeader
import com.example.oweowe.ui.components.SheetScoped
import com.example.oweowe.ui.components.SignedAmount
import com.example.oweowe.ui.components.asString
import com.example.oweowe.ui.groups.contacts.ContactSuggestion
import com.example.oweowe.ui.groups.contacts.DEMO_CONTACTS
import com.example.oweowe.ui.groups.contacts.hasContactsPermission
import com.example.oweowe.ui.groups.contacts.readContactsWithEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** S8 Group members (FR3) as a bottom sheet, with S10 Add member on top. */
@Composable
fun ManageMembersSheet(
    container: AppContainer,
    groupId: String,
    onDismiss: () -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "manage-members-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    var showAddMember by rememberSaveable { mutableStateOf(false) }

    AppBottomSheet(onDismiss = onDismiss) {
        SheetHeader(stringResource(R.string.group_overview_members))
        MembersSection(
            state = state,
            onRemoveMember = viewModel::removeMember,
            onAddMember = { showAddMember = true },
        )
        SnackbarHost(snackbarHostState)
    }

    if (showAddMember) {
        SheetScoped {
            AddMemberSheet(
                container = container,
                groupId = groupId,
                onDismiss = { showAddMember = false },
            )
        }
    }
}

/** S10 Add member (FR3) as its own bottom sheet, opened from S8. */
@Composable
fun AddMemberSheet(
    container: AppContainer,
    groupId: String,
    onDismiss: () -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "add-member-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    // Contacts are read on the device and only used to suggest members (FR3).
    val appContext = context.applicationContext
    var contactsAllowed by remember { mutableStateOf(hasContactsPermission(appContext)) }
    var contacts by remember { mutableStateOf<List<ContactSuggestion>>(emptyList()) }
    var contactsDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        contactsAllowed = granted
        contactsDenied = !granted
    }
    LaunchedEffect(contactsAllowed) {
        contacts = if (contactsAllowed) {
            withContext(Dispatchers.IO) { readContactsWithEmail(appContext.contentResolver) }
        } else {
            emptyList()
        }
    }
    // A clean emulator has no email contacts, so show demo people instead (D3).
    val shownSuggestions = if (contacts.isEmpty()) DEMO_CONTACTS else contacts
    val isDemoData = contactsAllowed && contacts.isEmpty()

    AppBottomSheet(onDismiss = onDismiss) {
        SheetHeader(stringResource(R.string.manage_members_add_title))
        AddMemberSection(
            state = state,
            onAddMemberByEmail = viewModel::addMemberByEmail,
            onAddMemberByPhone = viewModel::addMemberByPhone,
            suggestions = shownSuggestions,
            demoData = isDemoData,
            contactsAllowed = contactsAllowed,
            contactsDenied = contactsDenied,
            onRequestContacts = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
        )
        SnackbarHost(snackbarHostState)
    }
}

/** How the user identifies the person to add (S10). */
enum class AddMemberMode { EMAIL, PHONE }

/** The members list and the "Add member" action shown in the S8 sheet. */
@Composable
private fun MembersSection(
    state: GroupOverviewUiState,
    onRemoveMember: (MemberRow) -> Unit,
    onAddMember: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.offline) OfflineBanner()
        state.members.forEach { member ->
            ManageMemberRow(member, enabled = !state.busy, onRemoveMember = onRemoveMember)
        }
        OutlinedButton(
            onClick = onAddMember,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.manage_members_add_title))
        }
    }
}

/** The S10 add form plus contact suggestions, reused by the sheet and the fallback screen. */
@Composable
fun AddMemberSection(
    state: GroupOverviewUiState,
    onAddMemberByEmail: (String) -> Unit,
    onAddMemberByPhone: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<ContactSuggestion> = emptyList(),
    demoData: Boolean = false,
    contactsAllowed: Boolean = false,
    contactsDenied: Boolean = false,
    onRequestContacts: () -> Unit = {},
) {
    var mode by rememberSaveable { mutableStateOf(AddMemberMode.EMAIL) }
    var input by rememberSaveable { mutableStateOf("") }
    var showAllSuggestions by rememberSaveable { mutableStateOf(false) }

    val memberEmails = remember(state.members) {
        state.members.map { it.user.email.lowercase() }.toSet()
    }
    // A contact that is already in the group (or has no email) is not suggested again.
    val candidates = remember(suggestions, memberEmails) {
        suggestions.filter { it.email.lowercase() !in memberEmails }
    }
    val visibleSuggestions =
        if (showAllSuggestions) candidates else candidates.take(SUGGESTIONS_PREVIEW_COUNT)

    fun submit() {
        val value = input.trim()
        when (mode) {
            AddMemberMode.EMAIL -> onAddMemberByEmail(value)
            AddMemberMode.PHONE -> onAddMemberByPhone(value)
        }
        input = ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AddMemberForm(
            mode = mode,
            input = input,
            busy = state.busy,
            onModeChange = { mode = it },
            onInputChange = { input = it },
            onSubmit = { submit() },
        )
        if (!contactsAllowed) {
            ContactsAccessCard(
                denied = contactsDenied,
                onRequest = onRequestContacts,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    text = stringResource(R.string.manage_members_contacts_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (demoData) {
                    Text(
                        text = stringResource(R.string.manage_members_contacts_demo_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (candidates.isEmpty()) {
                Text(
                    text = stringResource(R.string.manage_members_contacts_all_added),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                visibleSuggestions.forEach { contact ->
                    ContactSuggestionRow(
                        contact = contact,
                        enabled = !state.busy,
                        onAdd = { onAddMemberByEmail(contact.email) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (candidates.size > SUGGESTIONS_PREVIEW_COUNT) {
                    TextButton(onClick = { showAllSuggestions = !showAllSuggestions }) {
                        Text(
                            if (showAllSuggestions) {
                                stringResource(R.string.manage_members_contacts_show_less)
                            } else {
                                stringResource(
                                    R.string.manage_members_contacts_show_all,
                                    candidates.size,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** How many contact suggestions are shown before "Show all". */
private const val SUGGESTIONS_PREVIEW_COUNT = 5

/** Explains why the app wants the contacts permission before the system prompt. */
@Composable
private fun ContactsAccessCard(
    denied: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.manage_members_contacts_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (denied) {
                        R.string.manage_members_contacts_denied
                    } else {
                        R.string.manage_members_contacts_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = onRequest, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.manage_members_contacts_allow))
            }
        }
    }
}

@Composable
private fun ContactSuggestionRow(
    contact: ContactSuggestion,
    enabled: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        InitialsAvatar(name = contact.name)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(contact.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = contact.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onAdd, enabled = enabled) {
            Text(stringResource(R.string.manage_members_contacts_add))
        }
    }
}

/** S10 Add member form: the person is found by email or by phone number (FR3). */
@Composable
private fun AddMemberForm(
    mode: AddMemberMode,
    input: String,
    busy: Boolean,
    onModeChange: (AddMemberMode) -> Unit,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == AddMemberMode.EMAIL,
                onClick = { onModeChange(AddMemberMode.EMAIL) },
                label = { Text(stringResource(R.string.manage_members_email)) },
                enabled = !busy,
            )
            FilterChip(
                selected = mode == AddMemberMode.PHONE,
                onClick = { onModeChange(AddMemberMode.PHONE) },
                label = { Text(stringResource(R.string.manage_members_phone)) },
                enabled = !busy,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = {
                    Text(
                        stringResource(
                            if (mode == AddMemberMode.EMAIL) {
                                R.string.manage_members_email_label
                            } else {
                                R.string.manage_members_phone_label
                            },
                        ),
                    )
                },
                supportingText = if (mode == AddMemberMode.PHONE) {
                    { Text(stringResource(R.string.manage_members_phone_hint)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (mode == AddMemberMode.EMAIL) {
                        KeyboardType.Email
                    } else {
                        KeyboardType.Phone
                    },
                ),
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onSubmit,
                enabled = !busy,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(stringResource(R.string.manage_members_add_button))
                }
            }
        }
    }
}

@Composable
private fun ManageMemberRow(
    member: MemberRow,
    enabled: Boolean,
    onRemoveMember: (MemberRow) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        InitialsAvatar(name = member.user.fullName, photoPath = member.user.photoUri)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.user.fullName, style = MaterialTheme.typography.bodyLarge)
                if (member.isMe) {
                    Text(
                        text = stringResource(R.string.common_you_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                text = member.user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // FR5: everyone's net balance, so the group can see who owes or gets
        // money and how much, next to Remove.
        SignedAmount(
            ore = member.balanceOre,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 12.dp),
        )
        OutlinedButton(
            onClick = { onRemoveMember(member) },
            enabled = enabled && member.canRemove,
        ) {
            Text(stringResource(R.string.manage_members_remove))
        }
    }
}

@Preview(showBackground = true, name = "Add member by phone")
@Composable
private fun AddMemberByPhonePreview() {
    MaterialTheme {
        AddMemberForm(
            mode = AddMemberMode.PHONE,
            input = "+45 22 22 22 22",
            busy = false,
            onModeChange = {},
            onInputChange = {},
            onSubmit = {},
        )
    }
}
