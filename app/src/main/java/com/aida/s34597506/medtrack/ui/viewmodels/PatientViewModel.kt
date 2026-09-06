package com.aida.s34597506.medtrack.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.entities.PatientEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class ClaimUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val generatedPatientId: String? = null
)

class PatientViewModel(
    private val repository: MedTrackRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModel() {

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _claimUiState = MutableStateFlow(ClaimUiState())
    val claimUiState: StateFlow<ClaimUiState> = _claimUiState.asStateFlow()

    private val _signUpUiState = MutableStateFlow(SignUpUiState())
    val signUpUiState: StateFlow<SignUpUiState> = _signUpUiState.asStateFlow()

    // FIXED: Correct syntax for MutableStateFlow
    private val _currentPatient = MutableStateFlow<PatientEntity?>(null)
    val currentPatient: StateFlow<PatientEntity?> = _currentPatient.asStateFlow()

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage = _errorMessage

    fun login(patientId: String, password: String) {
        viewModelScope.launch {
            _loginUiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val patient = repository.login(patientId, password)
                if (patient != null) {
                    sessionManager.saveLoggedInPatientId(patientId)
                    _loginUiState.update { it.copy(isSuccess = true, isLoading = false) }
                } else {
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid Patient ID or Password"
                        )
                    }
                }
            } catch (e: Exception) {
                _loginUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun claimAccount(patientId: String, phoneNumber: String, password: String) {
        viewModelScope.launch {
            _claimUiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val patient = repository.findPatientByIdAndPhone(patientId, phoneNumber)
                if (patient != null) {
                    // Check if password is already set (account already claimed)
                    if (patient.password.isNotEmpty()) {
                        _claimUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Account already claimed. Please login."
                            )
                        }
                        return@launch
                    }

                    repository.setPatientPassword(patientId, password)
                    _claimUiState.update { it.copy(isSuccess = true, isLoading = false) }
                } else {
                    _claimUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No patient found with that ID and phone number"
                        )
                    }
                }
            } catch (e: Exception) {
                _claimUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Claim failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun signUp(name: String, phoneNumber: String, password: String) {
        viewModelScope.launch {
            _signUpUiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                android.util.Log.d("PatientViewModel", "=== SIGN UP STARTED ===")
                android.util.Log.d("PatientViewModel", "Name: $name, Phone: $phoneNumber")

                // Check if phone number already exists
                val existingPatient = repository.findPatientByPhone(phoneNumber)
                if (existingPatient != null) {
                    android.util.Log.d("PatientViewModel", "Phone already exists: $phoneNumber")
                    _signUpUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Phone number already registered"
                        )
                    }
                    return@launch
                }

                // Get all existing patient IDs
                val allPatientIds = repository.getAllPatientIds()
                android.util.Log.d("PatientViewModel", "All existing IDs: $allPatientIds")

                // Get max number
                val maxNum = repository.getMaxPatientIdNumber()
                android.util.Log.d("PatientViewModel", "Max ID number: $maxNum")

                val nextNum = (maxNum ?: 1000) + 1
                val newPatientId = "P$nextNum"
                android.util.Log.d("PatientViewModel", "Generated new ID: $newPatientId")

                // Create new patient
                val newPatient = PatientEntity(
                    patientId = newPatientId,
                    phoneNumber = phoneNumber,
                    name = name,
                    password = password
                )

                repository.insertPatient(newPatient)
                android.util.Log.d("PatientViewModel", "Patient inserted successfully")

                _signUpUiState.update {
                    it.copy(
                        isSuccess = true,
                        isLoading = false,
                        generatedPatientId = newPatientId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PatientViewModel", "Sign up failed", e)
                _signUpUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sign up failed: ${e.message}"
                    )
                }
            }
        }
    }
    fun loadCurrentPatient(patientId: String) {
        viewModelScope.launch {
            val patient = repository.getPatientById(patientId)
            _currentPatient.value = patient
        }
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }


    fun resetLoginState() {
        _loginUiState.value = LoginUiState()
    }

    fun resetSignUpState() {
        _signUpUiState.value = SignUpUiState()
    }

}

class PatientViewModelFactory(
    private val repository: MedTrackRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            return PatientViewModel(repository, sessionManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}