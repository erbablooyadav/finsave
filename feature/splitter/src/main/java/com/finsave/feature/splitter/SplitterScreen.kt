package com.finsave.feature.splitter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.SplitterGroup

@Composable
fun SplitterScreen(
    modifier: Modifier = Modifier,
    viewModel: SplitterViewModel = hiltViewModel(),
    onNavigateToGroupDetail: (Long) -> Unit = {}
) {
    val groups by viewModel.groups.collectAsState()
    val spacing = LocalSpacing.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.large),
            contentPadding = PaddingValues(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Text(
                    text = "Splitter Groups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(spacing.medium))
            }

            if (groups.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.huge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active groups",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap the + button to create a group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onNavigateToGroupDetail(group.id) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, emoji ->
                    viewModel.createGroup(name, emoji)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun GroupCard(group: SplitterGroup, onClick: () -> Unit = {}) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = group.emoji, style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(modifier = Modifier.width(spacing.medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (group.isSettled) "Settled" else "Active",
                style = MaterialTheme.typography.bodySmall,
                color = if (group.isSettled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏠") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, emoji) },
                enabled = name.isNotBlank() && emoji.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
