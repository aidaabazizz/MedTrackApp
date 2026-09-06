package com.aida.s34597506.medtrack.data.models

data class Symptom(
    val patientId: String,
    val category: String,
    val severity: Int,
    val notes: String,
    val dateTime: String
)