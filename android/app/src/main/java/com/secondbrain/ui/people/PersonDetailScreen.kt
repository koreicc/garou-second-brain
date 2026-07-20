package com.secondbrain.ui.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Contact
import com.secondbrain.domain.model.SocialLink
import com.secondbrain.ui.util.RefreshOnResume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: PersonDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PersonDetailViewModel(
                    personRepository = AppModule.personRepository,
                    personId = personId
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reload person data when navigating back from edit screen
    RefreshOnResume {
        viewModel.reload()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name ?: "Person") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.person != null -> {
                val person = state.person!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (person.contacts.isNotEmpty()) {
                        Text("Contacts", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        person.contacts.forEach { contact ->
                            ContactRow(contact = contact)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (person.socialLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Social Links", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        person.socialLinks.forEach { link ->
                            SocialLinkRow(link = link)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (person.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            person.tags.joinToString(", ") { "#$it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (person.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(person.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when (contact.type) {
                "phone" -> Icons.Default.Phone
                "email" -> Icons.Default.Email
                else -> Icons.Default.Link
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = contact.value, style = MaterialTheme.typography.bodyMedium)
            if (contact.label.isNotEmpty()) {
                Text(text = contact.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SocialLinkRow(link: SocialLink) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "${link.platform}: ${link.url}", style = MaterialTheme.typography.bodyMedium)
    }
}
