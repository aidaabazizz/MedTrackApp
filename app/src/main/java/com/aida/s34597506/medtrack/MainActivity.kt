package com.aida.s34597506.medtrack

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.database.MedTrackDatabase
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.entities.PatientEntity
import com.aida.s34597506.medtrack.data.entities.SymptomEntity
import com.aida.s34597506.medtrack.data.repository.CsvRepository
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.screens.*
import com.aida.s34597506.medtrack.ui.theme.Aidas34597506medtrackTheme
import com.aida.s34597506.medtrack.utils.NotificationHelper
import com.aida.s34597506.medtrack.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var repository: MedTrackRepository

    private var isSeedingComplete by mutableStateOf(false)
    private var seedingError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request permissions
        requestNotificationPermission()
        requestExactAlarmPermission()

        // Build Room database and repository
        val database = MedTrackDatabase.getDatabase(this)
        repository = MedTrackRepository(
            patientDao = database.patientDao(),
            medicationDao = database.medicationDao(),
            symptomDao = database.symptomDao(),
            medCoachTipsDao = database.medCoachTipsDao()
        )
        sessionManager = SessionManager(this)

        // Check if already seeded
        val prefs = getSharedPreferences("medtrack_prefs", MODE_PRIVATE)
        val isSeeded = prefs.getBoolean("db_seeded", false)

        if (!isSeeded) {
            // Show loading screen while seeding
            setContent {
                Aidas34597506medtrackTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LoadingScreen(
                            message = "Loading medical data...",
                            error = seedingError
                        )
                    }
                }
            }

            // Perform seeding in background
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    seedDatabase()
                    withContext(Dispatchers.Main) {
                        isSeedingComplete = true
                        prefs.edit().putBoolean("db_seeded", true).apply()
                        // Reset taken statuses for new day
                        resetTakenStatuses()
                        // Launch main app
                        launchMainApp()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        seedingError = e.message ?: "Failed to load data"
                    }
                }
            }
        } else {
            // Database already seeded, launch directly
            lifecycleScope.launch(Dispatchers.IO) {
                resetTakenStatuses()
            }
            launchMainApp()
        }
    }

    private suspend fun seedDatabase() {
        val csvRepository = CsvRepository(this@MainActivity)

        // Seed patients
        val patients = csvRepository.loadPatients().map { p ->
            PatientEntity(
                patientId = p.patientId,
                phoneNumber = p.phoneNumber,
                name = p.name,
                password = ""
            )
        }
        repository.insertAllPatients(patients)

        // Verify patients were inserted
        val patientCount = repository.getPatientCount()
        android.util.Log.d("MainActivity", "Seeded $patientCount patients")
        android.util.Log.d("MainActivity", "Patient IDs: ${repository.getAllPatientIds()}")

        // Seed medications
        val medications = csvRepository.loadMedications().map { m ->
            MedicationEntity(
                patientId = m.patientId,
                medicationName = m.medicationName,
                dosage = m.dosage,
                frequency = m.frequency,
                scheduledTime = m.scheduledTime,
                medicationType = m.medicationType,
                notes = m.notes,
                isTaken = false,
                takenDate = null
            )
        }
        repository.insertAllMedications(medications)

        // Seed symptoms
        val symptoms = csvRepository.loadSymptoms().map { s ->
            SymptomEntity(
                patientId = s.patientId,
                category = s.category,
                severity = s.severity,
                notes = s.notes,
                dateTime = s.dateTime
            )
        }
        repository.insertAllSymptoms(symptoms)
    }

    private suspend fun resetTakenStatuses() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        repository.resetTakenStatusForNewDay(today)
    }

    private fun launchMainApp() {
        setContent {
            Aidas34597506medtrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedTrackApp(
                        sessionManager = sessionManager,
                        repository = repository
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String, error: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(text = message, style = MaterialTheme.typography.bodyLarge)

            if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { /* Retry logic could go here */ }) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun MedTrackApp(
    sessionManager: SessionManager,
    repository: MedTrackRepository
) {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }

    LaunchedEffect(Unit) {
        isLoggedIn = sessionManager.isLoggedIn()
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "welcome"
    ) {
        // ── Pre-login screens ────────────────────────────────────────────────
        composable("welcome") {
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onSignUpClick = { navController.navigate("signup") }
            )
        }

        composable("login") {
            LoginScreen(
                repository = repository,
                sessionManager = sessionManager,
                onLoginSuccess = {
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onAccountClaimClick = { navController.navigate("account_claim") },
                onSignUpClick = { navController.navigate("signup") }
            )
        }

        composable("account_claim") {
            AccountClaimScreen(
                repository = repository,
                sessionManager = sessionManager,
                onClaimSuccess = {
                    navController.navigate("login") {
                        popUpTo("account_claim") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("signup") {
            SignUpScreen(
                repository = repository,
                sessionManager = sessionManager,
                onSignUpSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Main app screens (bottom nav) ────────────────────────────────────
        composable("home") {
            val patientId = sessionManager.getLoggedInPatientId() ?: ""
            HomeScreen(
                patientId = patientId,
                repository = repository,
                onAddMedicationClick = { navController.navigate("add_medication") },
                onSymptomsClick = { navController.navigate("symptoms") },
                onMedCoachClick = { navController.navigate("medcoach") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("add_medication") {
            AddMedicationScreen(
                patientId = sessionManager.getLoggedInPatientId() ?: "",
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("symptoms") {
            SymptomsScreen(
                patientId = sessionManager.getLoggedInPatientId() ?: "",
                repository = repository,
                onHomeClick = { navController.navigate("home") },
                onMedCoachClick = { navController.navigate("medcoach") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("medcoach") {
            MedCoachScreen(
                patientId = sessionManager.getLoggedInPatientId() ?: "",
                repository = repository,
                onHomeClick = { navController.navigate("home") },
                onSymptomsClick = { navController.navigate("symptoms") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                patientId = sessionManager.getLoggedInPatientId() ?: "",
                repository = repository,
                onLogout = {
                    sessionManager.clearSession()
                    isLoggedIn = false
                    navController.navigate("welcome") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onClinicianLoginClick = { navController.navigate("clinician_login") },
                onHomeClick = { navController.navigate("home") },
                onSymptomsClick = { navController.navigate("symptoms") },
                onMedCoachClick = { navController.navigate("medcoach") }
            )
        }

        composable("clinician_login") {
            ClinicianLoginScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}