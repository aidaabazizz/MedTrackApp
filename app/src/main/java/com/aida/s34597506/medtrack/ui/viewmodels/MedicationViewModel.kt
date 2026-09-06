package com.aida.s34597506.medtrack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.utils.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MedicationUiState(
    val isLoading: Boolean = false,
    val medications: List<MedicationEntity> = emptyList(),
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class MedicationViewModel(
    private val repository: MedTrackRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    fun loadMedications(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getMedicationsForPatient(patientId).collect { medications ->
                    _uiState.update { it.copy(medications = medications, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load medications: ${e.message}"
                    )
                }
            }
        }
    }

    suspend fun getMedicationsOnce(patientId: String): List<MedicationEntity> {
        return repository.getMedicationsForPatientOnce(patientId)
    }

    fun addMedication(medication: MedicationEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveSuccess = false, errorMessage = null) }
            try {
                repository.insertMedication(medication)
                ReminderScheduler.scheduleDailyReminder(context, medication)
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save medication: ${e.message}"
                    )
                }
            }
        }
    }

    // Add these methods to MedicationViewModel class

    fun deleteMedication(medicationId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.deleteMedication(medicationId)
                // Cancel reminder for this medication
                ReminderScheduler.cancelReminder(context, medicationId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete medication: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.updateMedication(medication)
                // Update reminder for this medication
                ReminderScheduler.cancelReminder(context, medication.id)
                ReminderScheduler.scheduleDailyReminder(context, medication)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to update medication: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateTakenStatus(medicationId: Int, isTaken: Boolean) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.updateTakenStatus(medicationId, isTaken, if (isTaken) today else null)
        }
    }

    fun rescheduleAllReminders(patientId: String) {
        viewModelScope.launch {
            val medications = repository.getMedicationsForPatientOnce(patientId)
            ReminderScheduler.rescheduleAllReminders(context, medications)
        }
    }
}

class MedicationViewModelFactory(
    private val repository: MedTrackRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationViewModel::class.java)) {
            return MedicationViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}