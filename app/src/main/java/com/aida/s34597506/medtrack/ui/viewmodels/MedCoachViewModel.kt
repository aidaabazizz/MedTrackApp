package com.aida.s34597506.medtrack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit2081.aida.s34597506.medtrack.BuildConfig
import com.aida.s34597506.medtrack.data.api.DrugLabel
import com.aida.s34597506.medtrack.data.api.GeminiApiService
import com.aida.s34597506.medtrack.data.api.RetrofitInstance
import com.aida.s34597506.medtrack.data.entities.MedCoachTipEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

data class MedCoachUiState(
    val isLoadingDrug: Boolean = false,
    val isLoadingTip: Boolean = false,
    val drugInfo: DrugLabel? = null,
    val drugError: String? = null,
    val currentTip: String? = null,
    val tipError: String? = null,
    val showHistoryDialog: Boolean = false,
    val tipHistory: List<MedCoachTipEntity> = emptyList(),
    val patientMedications: List<String> = emptyList(),
    val patientName: String = ""
)

class MedCoachViewModel(
    private val repository: MedTrackRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedCoachUiState())
    val uiState: StateFlow<MedCoachUiState> = _uiState.asStateFlow()


    fun loadPatientMedications(patientId: String) {
        viewModelScope.launch {
            try {
                val medications = repository.getMedicationNamesForPatient(patientId)
                val patient = repository.getPatientById(patientId)
                _uiState.update {
                    it.copy(
                        patientMedications = medications,
                        patientName = patient?.name?.split(" ")?.firstOrNull() ?: "there"
                    )
                }
            } catch (e: Exception) {

            }
        }
    }

    fun searchDrug(medicationName: String) {
        if (medicationName.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingDrug = true, drugError = null, drugInfo = null)
            }

            try {
                val response = RetrofitInstance.api.searchDrug(
                    searchQuery = "openfda.brand_name:\"$medicationName\""
                )

                if (response.results.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingDrug = false,
                            drugError = "No information found for '$medicationName'"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingDrug = false,
                            drugInfo = response.results.first()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDrug = false,
                        drugError = "Network error: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateTip(patientId: String) {
        viewModelScope.launch {
            Log.d("MedCoachVM", "=== generateTip CALLED with enhanced personalization ===")

            _uiState.update { it.copy(isLoadingTip = true, tipError = null, currentTip = null) }

            try {
                // Get medications
                val medications = repository.getMedicationNamesForPatient(patientId)
                Log.d("MedCoachVM", "Medications: ${medications.joinToString()}")

                // Get patient details
                val patient = repository.getPatientById(patientId)
                val patientName = patient?.name?.split(" ")?.firstOrNull() ?: "there"
                Log.d("MedCoachVM", "Patient: $patientName")

                // Get recent symptoms (last 7 days for better context)
                val recentSymptoms = try {
                    repository.getSymptomsForPatientOnce(patientId)
                        .take(5)  // Get last 5 symptoms for better context
                        .map { it.category }
                        .distinct()
                } catch (e: Exception) {
                    emptyList()
                }
                Log.d("MedCoachVM", "Recent symptoms: ${recentSymptoms.joinToString()}")

                // Get adherence history from taken medications
                val adherenceHistory = try {
                    val allMeds = repository.getMedicationsForPatientOnce(patientId)
                    val takenCount = allMeds.count { it.isTaken }
                    val totalCount = allMeds.size
                    if (totalCount > 0) {
                        val adherenceRate = (takenCount.toDouble() / totalCount) * 100
                        when {
                            adherenceRate >= 80 -> "Great adherence! $patientName has been taking ${String.format("%.0f", adherenceRate)}% of medications as scheduled."
                            adherenceRate >= 50 -> "Moderate adherence (${String.format("%.0f", adherenceRate)}%). $patientName sometimes misses doses."
                            else -> "Low adherence (${String.format("%.0f", adherenceRate)}%). $patientName needs encouragement to build consistency."
                        }
                    } else {
                        "No adherence data available yet - this is a great time to start building good habits!"
                    }
                } catch (e: Exception) {
                    "No adherence data available"
                }
                Log.d("MedCoachVM", "Adherence: $adherenceHistory")

                // Get time of day for contextual tips
                val calendar = java.util.Calendar.getInstance()
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val timeOfDay = when (hour) {
                    in 0..11 -> "morning"
                    in 12..16 -> "afternoon"
                    else -> "evening"
                }
                Log.d("MedCoachVM", "Time of day: $timeOfDay")

                // Get medication types for more specific tips
                val medicationTypes = try {
                    repository.getMedicationsForPatientOnce(patientId)
                        .map { it.medicationType }
                        .distinct()
                } catch (e: Exception) {
                    emptyList()
                }

                // Build comprehensive context string
                val fullContext = buildString {
                    append("Patient: $patientName\n")
                    append("Medications: ${medications.joinToString()}\n")
                    if (medicationTypes.isNotEmpty()) {
                        append("Medication types: ${medicationTypes.joinToString()}\n")
                    }
                    if (recentSymptoms.isNotEmpty()) {
                        append("Recent symptoms: ${recentSymptoms.joinToString()}\n")
                    }
                    append("Adherence pattern: $adherenceHistory\n")
                    append("Time: $timeOfDay\n")
                }

                Log.d("MedCoachVM", "Full context length: ${fullContext.length}")

                // Get API key
                val apiKey = try {
                    BuildConfig.GEMINI_API_KEY
                } catch (e: Exception) {
                    Log.e("MedCoachVM", "Could not read BuildConfig", e)
                    ""
                }

                if (apiKey.isEmpty() || !apiKey.startsWith("AIza")) {
                    Log.e("MedCoachVM", "Invalid API key! Using fallback.")
                    val fallbackTip = getEnhancedFallbackTip(patientName, medications, recentSymptoms, timeOfDay)
                    saveAndDisplayTip(patientId, fallbackTip)
                    return@launch
                }

                // Call enhanced Gemini API with all context
                val geminiService = GeminiApiService(apiKey)
                val tip = geminiService.generateMedicationTip(
                    patientName = patientName,
                    medications = medications,
                    symptoms = recentSymptoms.ifEmpty { null },
                    adherenceHistory = adherenceHistory,
                    timeOfDay = timeOfDay
                )

                Log.d("MedCoachVM", "Generated personalized tip: $tip")
                saveAndDisplayTip(patientId, tip)

            } catch (e: Exception) {
                Log.e("MedCoachVM", "ERROR in generateTip: ${e.message}", e)
                val fallbackTip = getEnhancedFallbackTip("there", emptyList(), emptyList(), null)
                saveAndDisplayTip(patientId, fallbackTip)
                _uiState.update {
                    it.copy(
                        tipError = "Using enhanced fallback tip. Error: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun saveAndDisplayTip(patientId: String, tip: String) {
        val tipEntity = MedCoachTipEntity(
            patientId = patientId,
            tipText = tip,
            timestamp = System.currentTimeMillis()
        )
        repository.insertTip(tipEntity)

        _uiState.update {
            it.copy(
                isLoadingTip = false,
                currentTip = tip
            )
        }

        loadTipHistory(patientId)
    }

    private fun getEnhancedFallbackTip(
        patientName: String,
        medications: List<String>,
        symptoms: List<String>,
        timeOfDay: String?
    ): String {
        val medName = medications.firstOrNull() ?: "your medications"
        val time = timeOfDay ?: "daily"

        // Create personalized fallback tips based on available data
        val personalizedTips = mutableListOf<String>()

        if (medications.isNotEmpty()) {
            personalizedTips.add("💡 $patientName, taking $medName at the same $time each day can help make it a habit!")
            personalizedTips.add("💡 Great job managing your $medName, $patientName! Every dose brings you closer to your health goals. 💪")
        }

        if (symptoms.isNotEmpty()) {
            personalizedTips.add("💡 $patientName, since you've been experiencing ${symptoms.first()}, staying consistent with $medName is especially important. You've got this!")
        }

        personalizedTips.add("💡 $patientName, try pairing your $time medication with a daily activity like making coffee or brushing your teeth!")
        personalizedTips.add("💡 Setting a recurring alarm for your $time medications can help build consistency, $patientName! ⏰")

        return personalizedTips.random()
    }


    fun loadTipHistory(patientId: String) {
        viewModelScope.launch {
            try {
                repository.getTipsForPatient(patientId).collect { tips ->
                    _uiState.update { it.copy(tipHistory = tips) }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun setShowHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showHistoryDialog = show) }
    }


    init {
        // Log API key status
        try {
            val key = BuildConfig.GEMINI_API_KEY
            Log.d("MedCoachVM", "API Key from BuildConfig: ${if (key.isNotEmpty()) "Loaded (${key.length} chars)" else "EMPTY!"}")
            Log.d("MedCoachVM", "API Key starts with AIza: ${key.startsWith("AIza")}")

            if (key.isEmpty() || !key.startsWith("AIza")) {
                Log.e("MedCoachVM", "WARNING: API key is invalid or missing!")
            }
        } catch (e: Exception) {
            Log.e("MedCoachVM", "Could not read BuildConfig: ${e.message}")
        }
    }
}

class MedCoachViewModelFactory(
    private val repository: MedTrackRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedCoachViewModel::class.java)) {
            return MedCoachViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}