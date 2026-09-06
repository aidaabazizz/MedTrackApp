package com.aida.s34597506.medtrack.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.aida.s34597506.medtrack.data.models.Medication
import com.aida.s34597506.medtrack.data.models.Patient
import com.aida.s34597506.medtrack.data.models.Symptom
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("medtrack_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ========== SESSION MANAGEMENT ==========

    fun saveLoggedInPatientId(patientId: String) {
        prefs.edit { putString("logged_in_patient_id", patientId) }
    }

    fun getLoggedInPatientId(): String? {
        return prefs.getString("logged_in_patient_id", null)
    }

    fun clearSession() {
        prefs.edit { remove("logged_in_patient_id") }
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInPatientId() != null
    }

    // ========== FIRST LAUNCH DETECTION (for CSV seeding) ==========

    fun isDbSeeded(): Boolean {
        return prefs.getBoolean("db_seeded", false)
    }

    fun setDbSeeded(seeded: Boolean) {
        prefs.edit { putBoolean("db_seeded", seeded) }
    }

    // ========== USER MANAGEMENT (Gson - for CSV patients + signups) ==========

    fun saveUser(user: Patient) {
        val users = getUsers().toMutableList()
        // Check if user already exists (by patientId)
        val existingIndex = users.indexOfFirst { it.patientId == user.patientId }
        if (existingIndex >= 0) {
            users[existingIndex] = user
        } else {
            users.add(user)
        }
        val json = gson.toJson(users)
        prefs.edit { putString("users", json) }
    }

    fun getUsers(): List<Patient> {
        val json = prefs.getString("users", null) ?: return emptyList()
        val type = object : TypeToken<List<Patient>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUserById(patientId: String): Patient? {
        return getUsers().find { it.patientId == patientId }
    }

    fun getUserByPhone(phoneNumber: String): Patient? {
        return getUsers().find { it.phoneNumber == phoneNumber }
    }

    // Get all patients (CSV + SharedPreferences users)
    fun getAllPatients(csvPatients: List<Patient>): List<Patient> {
        val allPatients = csvPatients.toMutableList()
        val sharedPrefUsers = getUsers()
        // Add users from SharedPreferences that aren't already in CSV
        for (user in sharedPrefUsers) {
            if (allPatients.none { it.patientId == user.patientId }) {
                allPatients.add(user)
            }
        }
        return allPatients
    }

    // Get max Patient ID for auto-generation (scans both CSV and SharedPreferences)
    fun getMaxPatientId(csvPatients: List<Patient>): Int {
        val allIds = mutableListOf<Int>()

        // Get from CSV
        allIds.addAll(csvPatients.mapNotNull {
            it.patientId.removePrefix("P").toIntOrNull()
        })

        // Get from SharedPreferences users
        allIds.addAll(getUsers().mapNotNull {
            it.patientId.removePrefix("P").toIntOrNull()
        })

        return allIds.maxOrNull() ?: 1000
    }

    // ========== MEDICATION MANAGEMENT (Gson - for user-added meds) ==========

    fun saveMedication(medication: Medication) {
        val medications = getAllMedications().toMutableList()
        medications.add(medication)
        val json = gson.toJson(medications)
        prefs.edit { putString("medications", json) }
    }

    fun getAllMedications(): List<Medication> {
        val json = prefs.getString("medications", null) ?: return emptyList()
        val type = object : TypeToken<List<Medication>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMedicationsForPatient(patientId: String): List<Medication> {
        return getAllMedications().filter { it.patientId == patientId }
    }

    // Get all medications (CSV + SharedPreferences user-added)
    fun getAllMedicationsCombined(csvMedications: List<Medication>): List<Medication> {
        val allMeds = csvMedications.toMutableList()
        allMeds.addAll(getAllMedications())
        return allMeds
    }

    fun getMedicationsForPatientCombined(patientId: String, csvMedications: List<Medication>): List<Medication> {
        return getAllMedicationsCombined(csvMedications).filter { it.patientId == patientId }
    }

    // ========== SYMPTOM MANAGEMENT (Gson - for user-logged symptoms) ==========

    fun saveSymptom(symptom: Symptom) {
        val symptomId = "sym_${System.currentTimeMillis()}_${symptom.patientId}"
        val symptomData = "${symptom.patientId}|${symptom.category}|${symptom.severity}|${symptom.notes}|${symptom.dateTime}"
        prefs.edit { putString(symptomId, symptomData) }

        val patientSymIds = prefs.getStringSet("patient_symptoms_${symptom.patientId}", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        patientSymIds.add(symptomId)
        prefs.edit { putStringSet("patient_symptoms_${symptom.patientId}", patientSymIds) }

        val allSymIds = prefs.getStringSet("all_symptom_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        allSymIds.add(symptomId)
        prefs.edit { putStringSet("all_symptom_ids", allSymIds) }
    }

    fun getSymptomsForPatient(patientId: String): List<Symptom> {
        val patientSymIds = prefs.getStringSet("patient_symptoms_$patientId", emptySet()) ?: return emptyList()
        val symptoms = mutableListOf<Symptom>()

        for (symId in patientSymIds) {
            val symptomData = prefs.getString(symId, null)
            if (symptomData != null) {
                val parts = symptomData.split("|")
                if (parts.size == 5) {
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
        }
        return symptoms.sortedByDescending { it.dateTime }
    }

    // Get all symptoms (CSV + SharedPreferences user-logged)
    fun getAllSymptomsCombined(csvSymptoms: List<Symptom>): List<Symptom> {
        val allSymptoms = csvSymptoms.toMutableList()
        // Get unique patient IDs from SharedPreferences
        val allKeys = prefs.all.keys
        val symptomKeys = allKeys.filter { it.startsWith("sym_") }

        for (key in symptomKeys) {
            val symptomData = prefs.getString(key, null)
            if (symptomData != null) {
                val parts = symptomData.split("|")
                if (parts.size == 5) {
                    allSymptoms.add(
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
        }
        return allSymptoms.sortedByDescending { it.dateTime }
    }

    fun getSymptomsForPatientCombined(patientId: String, csvSymptoms: List<Symptom>): List<Symptom> {
        return getAllSymptomsCombined(csvSymptoms).filter { it.patientId == patientId }
    }

    // ========== HELPER METHODS ==========

    fun clearAllUserData() {
        // Clear all SharedPreferences data (for testing)
        val users = getUsers()
        for (user in users) {
            prefs.edit { remove("patient_symptoms_${user.patientId}") }
        }
        prefs.edit {
            remove("users")
            remove("medications")
            remove("all_symptom_ids")
            remove("logged_in_patient_id")
        }

        // Clear individual symptom entries
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.startsWith("sym_")) {
                prefs.edit { remove(key) }
            }
        }
    }

    // Check if phone number is unique (across CSV + SharedPreferences)
    fun isPhoneNumberUnique(phoneNumber: String, csvPatients: List<Patient>): Boolean {
        // Check CSV
        if (csvPatients.any { it.phoneNumber == phoneNumber }) {
            return false
        }
        // Check SharedPreferences users
        if (getUsers().any { it.phoneNumber == phoneNumber }) {
            return false
        }
        return true
    }
}