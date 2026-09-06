package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    patientId: String,
    repository: MedTrackRepository,
    onLogout: () -> Unit,
    onClinicianLoginClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSymptomsClick: () -> Unit,
    onMedCoachClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val patientViewModel: PatientViewModel = viewModel(
        factory = PatientViewModelFactory(repository, sessionManager, context)
    )

    val currentPatient by patientViewModel.currentPatient.collectAsState()

    LaunchedEffect(patientId) {
        patientViewModel.loadCurrentPatient(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
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
                    selected = false,
                    onClick = onMedCoachClick,
                    icon = { Icon(Icons.Default.Star, contentDescription = "MedCoach") },
                    label = { Text("MedCoach") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        text = "Account Information",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow("Name:", currentPatient?.name ?: "Loading...")
                    InfoRow("Patient ID:", currentPatient?.patientId ?: "Loading...")
                    InfoRow("Phone Number:", currentPatient?.phoneNumber ?: "Loading...")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onClinicianLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clinician Login")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}