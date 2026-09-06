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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.entities.SymptomEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.SymptomViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.SymptomViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomsScreen(
    patientId: String,
    repository: MedTrackRepository,
    onHomeClick: () -> Unit,
    onMedCoachClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val symptomViewModel: SymptomViewModel = viewModel(
        factory = SymptomViewModelFactory(repository)
    )

    var category by remember { mutableStateOf("Headache") }
    var severity by remember { mutableStateOf(5f) }
    var notes by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(true) }
    var expandedCategory by remember { mutableStateOf(false) }

    val uiState by symptomViewModel.uiState.collectAsState()

    val categories = listOf(
        "Headache", "Nausea", "Fatigue", "Pain", "Dizziness",
        "Fever", "Cough", "Shortness of breath", "Other"
    )

    LaunchedEffect(patientId) {
        symptomViewModel.loadSymptoms(patientId)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            symptomViewModel.resetSaveSuccess()
            symptomViewModel.loadSymptoms(patientId)
            category = "Headache"
            severity = 5f
            notes = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Symptoms") }
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
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.List, contentDescription = "Symptoms") },
                    label = { Text("Symptoms") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onMedCoachClick,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextButton(
                onClick = { showForm = !showForm },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(if (showForm) "Hide Form ▲" else "Log New Symptom ▼")
            }

            if (showForm) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Log New Symptom",
                            style = MaterialTheme.typography.titleLarge
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = it }
                        ) {
                            TextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Symptom Category *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            category = option
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Severity: ${severity.toInt()} - ${getSeverityLabel(severity.toInt())}")
                        Slider(
                            value = severity,
                            onValueChange = { severity = it },
                            valueRange = 1f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = getSeverityColor(severity.toInt()),
                                activeTrackColor = getSeverityColor(severity.toInt())
                            )
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { if (it.length <= 200) notes = it },
                            label = { Text("Additional Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text("${notes.length}/200 characters")
                            }
                        )

                        uiState.errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val newSymptom = SymptomEntity(
                                    patientId = patientId,
                                    category = category,
                                    severity = severity.toInt(),
                                    notes = notes,
                                    dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        .format(Date())
                                )
                                symptomViewModel.addSymptom(newSymptom) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text("Save Symptom")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Symptom History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (uiState.isLoading && uiState.symptoms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.symptoms.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No symptoms logged yet.",
                        modifier = Modifier.padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.symptoms) { symptom ->
                        SymptomHistoryCard(symptom)
                    }
                }
            }
        }
    }
}

@Composable
fun SymptomHistoryCard(symptom: SymptomEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getSeverityColor(symptom.severity).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = symptom.category,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Severity: ${symptom.severity}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getSeverityColor(symptom.severity)
                )
            }
            Text(
                text = symptom.dateTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (symptom.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = symptom.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

fun getSeverityLabel(severity: Int): String {
    return when (severity) {
        in 1..3 -> "Mild"
        in 4..6 -> "Moderate"
        in 7..10 -> "Severe"
        else -> "Unknown"
    }
}

fun getSeverityColor(severity: Int): Color {
    return when (severity) {
        in 1..3 -> Color(0xFF4CAF50)
        in 4..6 -> Color(0xFFFF9800)
        in 7..10 -> Color(0xFFF44336)
        else -> Color.Gray
    }
}