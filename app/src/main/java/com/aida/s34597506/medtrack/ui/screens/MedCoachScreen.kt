package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.MedCoachViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.MedCoachViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.Button

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposeNonConstantLocale")
@Composable
fun MedCoachScreen(
    patientId: String,
    repository: MedTrackRepository,
    onHomeClick: () -> Unit,
    onSymptomsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MedCoachViewModel = viewModel(
        factory = MedCoachViewModelFactory(repository, context)
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedMedication by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }
    var manualSearch by remember { mutableStateOf("") }

    LaunchedEffect(patientId) {
        viewModel.loadPatientMedications(patientId)
        viewModel.loadTipHistory(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MedCoach") },
                actions = {
                    IconButton(onClick = { viewModel.setShowHistoryDialog(true) }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSymptomsClick,
                    icon = { Icon(Icons.Default.List, contentDescription = "Symptoms") },
                    label = { Text("Symptoms") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Star, contentDescription = "MedCoach") },
                    label = { Text("MedCoach") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Drug Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔍 Drug Information",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Medication dropdown (if patient has medications)
                        if (uiState.patientMedications.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = it }
                            ) {
                                TextField(
                                    value = selectedMedication,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Your Medication") },
                                    placeholder = { Text("Choose a medication...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    uiState.patientMedications.forEach { med ->
                                        DropdownMenuItem(
                                            text = { Text(med) },
                                            onClick = {
                                                selectedMedication = med
                                                manualSearch = med
                                                expandedDropdown = false
                                                viewModel.searchDrug(med)
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— OR —",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Manual search field
                        Row {
                            OutlinedTextField(
                                value = manualSearch,
                                onValueChange = { manualSearch = it },
                                label = { Text("Search any medication") },
                                placeholder = { Text("e.g., ibuprofen, metformin") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualSearch.isNotBlank()) {
                                        selectedMedication = manualSearch
                                        viewModel.searchDrug(manualSearch)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }

                        // Loading state
                        if (uiState.isLoadingDrug) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }

                        // Error state
                        uiState.drugError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Drug info display
                        uiState.drugInfo?.let { drug ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "📋 Information for: $selectedMedication",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Brand name
                            drug.openfda?.brand_name?.firstOrNull()?.let { value ->
                                DrugInfoRow("Brand Name:", value)
                            }

                            // Generic name
                            drug.openfda?.generic_name?.firstOrNull()?.let { value ->
                                DrugInfoRow("Generic Name:", value)
                            }

                            // Purpose
                            drug.purpose?.firstOrNull()?.let { value ->
                                DrugInfoRow("Purpose:", value)
                            }

                            // Warnings
                            drug.warnings?.firstOrNull()?.let { value ->
                                DrugInfoRow("Warnings:", value.take(300) + if (value.length > 300) "..." else "")
                            }

                            // Dosage
                            drug.dosage_and_administration?.firstOrNull()?.let { value ->
                                DrugInfoRow("Dosage & Administration:", value.take(200) + if (value.length > 200) "..." else "")
                            }

                            // Active ingredient
                            drug.active_ingredient?.firstOrNull()?.let { value ->
                                DrugInfoRow("Active Ingredient:", value)
                            }

                            // Indications
                            drug.indications_and_usage?.firstOrNull()?.let { value ->
                                DrugInfoRow("Indications:", value.take(200) + if (value.length > 200) "..." else "")
                            }
                        }
                    }
                }
            }

            // Section 2: GenAI Tips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🤖 AI Medication Tips",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Get personalized medication adherence tips powered by AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isLoadingTip) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        } else if (uiState.currentTip != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = uiState.currentTip!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        uiState.tipError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.generateTip(patientId) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoadingTip
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Tip")
                        }
                    }
                }
            }
        }
    }

    // Tip History Dialog
    if (uiState.showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowHistoryDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tip History")
                }
            },
            text = {
                if (uiState.tipHistory.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tips generated yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'Generate AI Tip' to get your first personalized tip!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.tipHistory) { tip ->
                            TipHistoryCard(tip = tip)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowHistoryDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DrugInfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TipHistoryCard(tip: com.aida.s34597506.medtrack.data.entities.MedCoachTipEntity) {
    val dateString = remember(tip.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tip.timestamp))
    }
    val timeString = remember(tip.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tip.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tip.tipText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}