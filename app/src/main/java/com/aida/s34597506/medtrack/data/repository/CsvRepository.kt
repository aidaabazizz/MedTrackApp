package com.aida.s34597506.medtrack.data.repository

import android.content.Context
import com.aida.s34597506.medtrack.data.models.Medication
import com.aida.s34597506.medtrack.data.models.Patient
import com.aida.s34597506.medtrack.data.models.Symptom

class CsvRepository(private val context: Context) {

    fun loadPatients(): List<Patient> {
        val patients = mutableListOf<Patient>()
        val csvString = readCsvFromAssets("patients.csv")

        csvString?.lines()?.drop(1)?.forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 4) {
                patients.add(
                    Patient(
                        patientId = parts[0],
                        phoneNumber = parts[1],
                        name = parts[2],
                        password = parts[3]
                    )
                )
            }
        }
        return patients
    }

    fun loadMedications(): List<Medication> {
        val medications = mutableListOf<Medication>()
        val csvString = readCsvFromAssets("medications.csv")

        csvString?.lines()?.drop(1)?.forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 7) {
                medications.add(
                    Medication(
                        patientId = parts[0],
                        medicationName = parts[1],
                        dosage = parts[2],
                        frequency = parts[3],
                        scheduledTime = parts[4],
                        medicationType = parts[5],
                        notes = if (parts.size > 6) parts[6] else ""
                    )
                )
            }
        }
        return medications
    }

    fun loadSymptoms(): List<Symptom> {
        val symptoms = mutableListOf<Symptom>()
        val csvString = readCsvFromAssets("symptoms.csv")

        csvString?.lines()?.drop(1)?.forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 5) {
                symptoms.add(
                    Symptom(
                        patientId = parts[0],
                        category = parts[1],
                        severity = parts[2].toIntOrNull() ?: 0,
                        notes = parts[3],
                        dateTime = parts[4]
                    )
                )
            }
        }
        return symptoms
    }

    private fun readCsvFromAssets(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}