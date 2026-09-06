package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.MedicationViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.MedicationViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable


fun AddMedicationScreen(
    patientId: String,
    repository: MedTrackRepository,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current

    val medicationViewModel: MedicationViewModel = viewModel(
        factory = MedicationViewModelFactory(repository, context)
    )
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Once daily") }
    var scheduledTime by remember { mutableStateOf("") }
    var medicationType by remember { mutableStateOf("Tablet") }
    var notes by remember { mutableStateOf("") }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val uiState by medicationViewModel.uiState.collectAsState()

    val frequencies = listOf("Once daily", "Twice daily", "Three times daily", "As needed")
    val medicationTypes = listOf("Tablet", "Capsule", "Liquid", "Injection", "Cream", "Other")

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            medicationViewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Medication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = medicationName,
                onValueChange = { medicationName = it },
                label = { Text("Medication Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage * (e.g., 500mg, 10ml)") },
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
                    label = { Text("Frequency *") },
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
                label = { Text("Scheduled Time * (e.g., 08:00, 14:30)") },
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
                    label = { Text("Medication Type *") },
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

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (medicationName.isNotBlank() && dosage.isNotBlank() && scheduledTime.isNotBlank()) {
                        val newMedication = MedicationEntity(
                            patientId = patientId,
                            medicationName = medicationName,
                            dosage = dosage,
                            frequency = frequency,
                            scheduledTime = scheduledTime,
                            medicationType = medicationType,
                            notes = notes
                        )
                        medicationViewModel.addMedication(newMedication) { }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Medication")
                }
            }
        }
    }
}