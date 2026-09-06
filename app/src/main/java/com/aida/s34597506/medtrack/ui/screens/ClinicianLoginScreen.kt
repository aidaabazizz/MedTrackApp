package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.ClinicianViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.ClinicianViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianLoginScreen(
    repository: MedTrackRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: ClinicianViewModel = viewModel(
        factory = ClinicianViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isAuthenticated) {
        ClinicianDashboardScreen(viewModel = viewModel, onBack = onNavigateBack)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Clinician Login") },
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔐 Clinician Access",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the secure access key to view clinical analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = uiState.accessKey,
                    onValueChange = { viewModel.updateAccessKey(it) },
                    label = { Text("Access Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                uiState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.authenticate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
        }
    }
}