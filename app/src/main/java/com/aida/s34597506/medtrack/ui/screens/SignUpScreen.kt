package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    repository: MedTrackRepository,
    sessionManager: SessionManager,
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PatientViewModel = viewModel(
        factory = PatientViewModelFactory(repository, sessionManager, context)
    )

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val uiState by viewModel.signUpUiState.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedId by remember { mutableStateOf("") }

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            generatedId = uiState.generatedPatientId ?: ""
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.resetSignUpState()
                onSignUpSuccess()
            },
            title = { Text("Account Created!") },
            text = {
                Column {
                    Text("Your account has been created successfully.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Patient ID is: $generatedId",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please save this ID - you'll need it to log in.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    viewModel.resetSignUpState()
                    onSignUpSuccess()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create New Account",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g., 0412345678") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
            )

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        name.isBlank() -> viewModel.setErrorMessage("Please enter your name")
                        phoneNumber.isBlank() -> viewModel.setErrorMessage("Please enter phone number")
                        phoneNumber.length < 10 -> viewModel.setErrorMessage("Please enter valid phone number")
                        password.isBlank() -> viewModel.setErrorMessage("Please enter a password")
                        password.length < 4 -> viewModel.setErrorMessage("Password must be at least 4 characters")
                        password != confirmPassword -> viewModel.setErrorMessage("Passwords do not match")
                        else -> viewModel.signUp(name, phoneNumber, password)
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Sign Up")
                }
            }
        }
    }
}