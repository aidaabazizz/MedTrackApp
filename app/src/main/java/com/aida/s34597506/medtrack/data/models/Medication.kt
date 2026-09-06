package com.aida.s34597506.medtrack.data.models

data class Medication(
    val patientId: String,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val scheduledTime: String,
    val medicationType: String,
    val notes: String = ""
)