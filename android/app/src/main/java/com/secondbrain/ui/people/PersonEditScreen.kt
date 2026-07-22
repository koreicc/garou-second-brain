package com.secondbrain.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.TagInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    personId: String?,
    onNavigateBack: () -> Unit
) {
    val viewModel: PersonEditViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PersonEditViewModel(
                    personRepository = AppModule.personRepository,
                    personId = personId
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (personId != null) "Edit Person" else "New Person",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to people")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(PersonEditEvent.Save) },
                        enabled = state.name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Save person")
                    }
                },
                colors = transparentTopAppBarColors()
            )
        }
    ) { padding ->
        if (state.isLoading && personId != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Contact Information
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.onEvent(PersonEditEvent.UpdateName(it)) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = { viewModel.onEvent(PersonEditEvent.UpdatePhone(it)) },
                                label = { Text("Phone") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = { viewModel.onEvent(PersonEditEvent.UpdateEmail(it)) },
                                label = { Text("Email") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                        }

                        TagInput(
                            tags = state.tags,
                            onTagsChanged = { viewModel.onEvent(PersonEditEvent.SetTags(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2: Social Links
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Social Links",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        state.socialLinks.forEachIndexed { index, link ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = "${link.platform} link",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.platform,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = link.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onEvent(PersonEditEvent.RemoveSocialLink(index)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove ${link.platform} link",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.newPlatform,
                                onValueChange = { viewModel.onEvent(PersonEditEvent.UpdateNewPlatform(it)) },
                                placeholder = { Text("Platform") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            OutlinedTextField(
                                value = state.newUrl,
                                onValueChange = { viewModel.onEvent(PersonEditEvent.UpdateNewUrl(it)) },
                                placeholder = { Text("URL") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.onEvent(PersonEditEvent.AddSocialLink) }
                                )
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(PersonEditEvent.AddSocialLink) },
                                enabled = state.newPlatform.isNotBlank() && state.newUrl.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add social link")
                            }
                        }
                    }
                }

                // Section 3: Notes
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { viewModel.onEvent(PersonEditEvent.UpdateNotes(it)) },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }
    }
}
}
