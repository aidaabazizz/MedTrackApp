package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aida.s34597506.medtrack.ui.viewmodels.ClinicianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianDashboardScreen(
    viewModel: ClinicianViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinician Dashboard") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout(); onBack() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("📊 Population Health Metrics", style = MaterialTheme.typography.titleLarge)
            }

            item {
                StatsCard(
                    title = "Total Patients",
                    value = "${uiState.totalPatients}",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                StatsCard(
                    title = "Average Medications per Patient",
                    value = String.format("%.1f", uiState.avgMedicationsPerPatient),
                    icon = Icons.Default.Medication,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                StatsCard(
                    title = "Most Common Symptom",
                    value = uiState.mostCommonSymptom,
                    icon = Icons.Default.List,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            item {
                StatsCard(
                    title = "Average Symptom Severity",
                    value = "${String.format("%.1f", uiState.avgSymptomSeverity)}/10",
                    icon = Icons.Default.Warning,
                    color = when {
                        uiState.avgSymptomSeverity > 7 -> MaterialTheme.colorScheme.error
                        uiState.avgSymptomSeverity > 4 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🤖 AI-Powered Insights",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.generateAiInsights() },
                            enabled = !uiState.isLoadingInsights,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoadingInsights) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing data...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find Patterns")
                            }
                        }

                        uiState.aiInsights?.let { insights ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = insights,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = color
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}