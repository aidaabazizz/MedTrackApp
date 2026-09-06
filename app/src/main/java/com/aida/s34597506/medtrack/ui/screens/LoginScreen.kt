package com.aida.s34597506.medtrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aida.s34597506.medtrack.data.SessionManager
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModel
import com.aida.s34597506.medtrack.ui.viewmodels.PatientViewModelFactory
import com.fit2081.aida.s34597506.medtrack.R

@Composable
fun LoginScreen(
    repository: MedTrackRepository,
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    onAccountClaimClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PatientViewModel = viewModel(
        factory = PatientViewModelFactory(repository, sessionManager, context)
    )

    var patientId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val uiState by viewModel.loginUiState.collectAsState()

    // Handle success navigation
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetLoginState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.medtrack_logo),
            contentDescription = "MedTrack Logo",
            modifier = Modifier.size(220.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = patientId,
            onValueChange = { patientId = it.uppercase() },
            label = { Text("Patient ID") },
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

        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.login(patientId, password) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onAccountClaimClick) {
                Text("Claim Account")
            }

            TextButton(onClick = onSignUpClick) {
                Text("Sign Up")
            }
        }
    }
}