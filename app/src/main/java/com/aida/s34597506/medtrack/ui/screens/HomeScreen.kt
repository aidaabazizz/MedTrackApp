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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.MedicationViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.MedicationViewModelFactory
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    patientId: String,
    repository: MedTrackRepository,
    onAddMedicationClick: () -> Unit,
    onSymptomsClick: () -> Unit,
    onMedCoachClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    val medicationViewModel: MedicationViewModel = viewModel(
        factory = MedicationViewModelFactory(repository, context)
    )
    val patientViewModel: PatientViewModel = viewModel(
        factory = PatientViewModelFactory(repository, sessionManager, context)
    )

    val medicationUiState by medicationViewModel.uiState.collectAsState()
    val currentPatient by patientViewModel.currentPatient.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var medicationToDelete by remember { mutableStateOf<MedicationEntity?>(null) }
    var medicationToEdit by remember { mutableStateOf<MedicationEntity?>(null) }

    LaunchedEffect(patientId) {
        medicationViewModel.loadMedications(patientId)
        patientViewModel.loadCurrentPatient(patientId)
    }

    val totalMedications = medicationUiState.medications.size
    val takenCount = medicationUiState.medications.count { it.isTaken }

    // Show edit dialog if medicationToEdit is not null
    if (medicationToEdit != null) {
        EditMedicationDialog(
            medication = medicationToEdit!!,
            onSave = { updatedMedication ->
                medicationViewModel.updateMedication(updatedMedication)
                medicationToEdit = null
            },
            onDismiss = { medicationToEdit = null }
        )
    }

    // Show delete confirmation dialog
    if (showDeleteDialog && medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Medication") },
            text = { Text("Are you sure you want to delete ${medicationToDelete?.medicationName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        medicationViewModel.deleteMedication(medicationToDelete!!.id)
                        showDeleteDialog = false
                        medicationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MedTrack Pro") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Hello, ${currentPatient?.name ?: "Patient"}!",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$takenCount of $totalMedications taken",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddMedicationClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add New Medication")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (medicationUiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (medicationUiState.medications.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No medications scheduled",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(medicationUiState.medications) { medication ->
                        MedicationCardWithMenu(
                            medication = medication,
                            isTaken = medication.isTaken,
                            onTakenToggle = { isChecked ->
                                medicationViewModel.updateTakenStatus(medication.id, isChecked)
                                medicationViewModel.loadMedications(patientId)
                            },
                            onEditClick = { medicationToEdit = medication },
                            onDeleteClick = {
                                medicationToDelete = medication
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            medicationUiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun MedicationCardWithMenu(
    medication: MedicationEntity,
    isTaken: Boolean,
    onTakenToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isTaken) TextDecoration.LineThrough else null
                )
                Text(
                    text = "${medication.dosage} • ${medication.frequency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Time: ${medication.scheduledTime} • Type: ${medication.medicationType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (medication.notes.isNotBlank()) {
                    Text(
                        text = medication.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Menu button (three dots)
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            expanded = false
                            onEditClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = isTaken,
                onCheckedChange = onTakenToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationDialog(
    medication: MedicationEntity,
    onSave: (MedicationEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var medicationName by remember { mutableStateOf(medication.medicationName) }
    var dosage by remember { mutableStateOf(medication.dosage) }
    var frequency by remember { mutableStateOf(medication.frequency) }
    var scheduledTime by remember { mutableStateOf(medication.scheduledTime) }
    var medicationType by remember { mutableStateOf(medication.medicationType) }
    var notes by remember { mutableStateOf(medication.notes) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val frequencies = listOf("Once daily", "Twice daily", "Three times daily", "As needed")
    val medicationTypes = listOf("Tablet", "Capsule", "Liquid", "Injection", "Cream", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Medication") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("Medication Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = { expandedFrequency = it }
                ) {
                    TextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        frequencies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    frequency = option
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = { Text("Scheduled Time") },
                    placeholder = { Text("HH:MM") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    TextField(
                        value = medicationType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Medication Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        medicationTypes.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    medicationType = option
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedMedication = medication.copy(
                        medicationName = medicationName,
                        dosage = dosage,
                        frequency = frequency,
                        scheduledTime = scheduledTime,
                        medicationType = medicationType,
                        notes = notes
                    )
                    onSave(updatedMedication)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}