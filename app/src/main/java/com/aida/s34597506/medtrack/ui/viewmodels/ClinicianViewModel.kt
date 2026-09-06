package com.aida.s34597506.medtrack.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit2081.aida.s34597506.medtrack.BuildConfig
import com.aida.s34597506.medtrack.data.api.GeminiApiService
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ClinicianUiState(
    val isAuthenticated: Boolean = false,
    val accessKey: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val totalPatients: Int = 0,
    val avgMedicationsPerPatient: Double = 0.0,
    val mostCommonSymptom: String = "N/A",
    val avgSymptomSeverity: Double = 0.0,
    val aiInsights: String? = null,
    val isLoadingInsights: Boolean = false
)

class ClinicianViewModel(
    private val repository: MedTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClinicianUiState())
    val uiState: StateFlow<ClinicianUiState> = _uiState.asStateFlow()

    private val ACCESS_KEY = "dollar-entry-apples"

    private val geminiService by lazy {
        GeminiApiService(BuildConfig.GEMINI_API_KEY)
    }

    fun updateAccessKey(key: String) {
        _uiState.update { it.copy(accessKey = key, errorMessage = null) }
    }

    fun authenticate() {
        val currentKey = _uiState.value.accessKey
        if (currentKey == ACCESS_KEY) {
            _uiState.update { it.copy(isAuthenticated = true, errorMessage = null) }
            loadDashboardData()
        } else {
            _uiState.update { it.copy(errorMessage = "Invalid access key") }
        }
    }

    fun logout() {
        _uiState.update { it.copy(isAuthenticated = false, accessKey = "", aiInsights = null) }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val totalPatients = repository.getPatientCount()
                val avgMedications = repository.getAverageMedicationsPerPatient() ?: 0.0
                val mostCommonSymptom = repository.getMostCommonSymptomCategory() ?: "N/A"
                val avgSeverity = repository.getAverageSeverity() ?: 0.0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalPatients = totalPatients,
                        avgMedicationsPerPatient = avgMedications,
                        mostCommonSymptom = mostCommonSymptom,
                        avgSymptomSeverity = avgSeverity
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateAiInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInsights = true, aiInsights = null) }

            try {
                val state = _uiState.value

                // Get additional context from database for better insights
                val allSymptoms = repository.getAllSymptomsForInsights()
                val symptomDistribution = allSymptoms.groupBy { it.category }
                    .map { it.key to it.value.size }
                    .sortedByDescending { it.second }
                    .take(3)
                    .joinToString(", ") { "${it.first} (${it.second} reports)" }

                val additionalContext = if (symptomDistribution.isNotBlank()) {
                    "\nSymptom distribution: $symptomDistribution"
                } else ""

                // Call REAL Gemini API
                val insights = geminiService.generateClinicianInsights(
                    totalPatients = state.totalPatients,
                    avgMedications = state.avgMedicationsPerPatient,
                    mostCommonSymptom = state.mostCommonSymptom,
                    avgSeverity = state.avgSymptomSeverity,
                    additionalContext = additionalContext
                )

                _uiState.update {
                    it.copy(
                        isLoadingInsights = false,
                        aiInsights = insights
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingInsights = false,
                        errorMessage = "Failed to generate insights: ${e.message}"
                    )
                }
            }
        }
    }
}

class ClinicianViewModelFactory(
    private val repository: MedTrackRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClinicianViewModel::class.java)) {
            return ClinicianViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}