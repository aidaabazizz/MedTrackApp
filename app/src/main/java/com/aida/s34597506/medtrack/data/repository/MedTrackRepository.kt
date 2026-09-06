package com.aida.s34597506.medtrack.data.repository

import com.aida.s34597506.medtrack.data.dao.MedCoachTipsDao
import com.aida.s34597506.medtrack.data.dao.MedicationDao
import com.aida.s34597506.medtrack.data.dao.PatientDao
import com.aida.s34597506.medtrack.data.dao.SymptomDao
import com.aida.s34597506.medtrack.data.entities.MedCoachTipEntity
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.entities.PatientEntity
import com.aida.s34597506.medtrack.data.entities.SymptomEntity
import kotlinx.coroutines.flow.Flow

class MedTrackRepository(
    private val patientDao: PatientDao,
    private val medicationDao: MedicationDao,
    private val symptomDao: SymptomDao,
    private val medCoachTipsDao: MedCoachTipsDao
) {

    // ========== PATIENT ==========

    suspend fun insertPatient(patient: PatientEntity) =
        patientDao.insertPatient(patient)

    suspend fun insertAllPatients(patients: List<PatientEntity>) =
        patientDao.insertAll(patients)

    // Account claiming: check patientId + phoneNumber
    suspend fun findPatientByIdAndPhone(patientId: String, phoneNumber: String): PatientEntity? =
        patientDao.findByIdAndPhone(patientId, phoneNumber)

    // Set password after account claiming
    suspend fun setPatientPassword(patientId: String, password: String) =
        patientDao.setPassword(patientId, password)

    // Login: validate patientId + password
    suspend fun login(patientId: String, password: String): PatientEntity? =
        patientDao.login(patientId, password)

    // Check phone uniqueness (sign up)
    suspend fun findPatientByPhone(phoneNumber: String): PatientEntity? =
        patientDao.findByPhone(phoneNumber)

    suspend fun getPatientById(patientId: String): PatientEntity? =
        patientDao.getPatientById(patientId)

    suspend fun getPatientCount(): Int =
        patientDao.getPatientCount()

    suspend fun getAllPatientIds(): List<String> = patientDao.getAllPatientIds()

    // ========== MEDICATION ==========

    suspend fun insertMedication(medication: MedicationEntity) =
        medicationDao.insertMedication(medication)

    suspend fun insertAllMedications(medications: List<MedicationEntity>) =
        medicationDao.insertAll(medications)

    fun getMedicationsForPatient(patientId: String): Flow<List<MedicationEntity>> =
        medicationDao.getMedicationsForPatient(patientId)

    suspend fun getMedicationsForPatientOnce(patientId: String): List<MedicationEntity> =
        medicationDao.getMedicationsForPatientOnce(patientId)

    suspend fun getAverageMedicationsPerPatient(): Double? =
        medicationDao.getAverageMedicationsPerPatient()

    suspend fun getMedicationNamesForPatient(patientId: String): List<String> =
        medicationDao.getMedicationNamesForPatient(patientId)

    // ========== SYMPTOM ==========

    suspend fun insertSymptom(symptom: SymptomEntity) =
        symptomDao.insertSymptom(symptom)

    suspend fun insertAllSymptoms(symptoms: List<SymptomEntity>) =
        symptomDao.insertAll(symptoms)

    fun getSymptomsForPatient(patientId: String): Flow<List<SymptomEntity>> =
        symptomDao.getSymptomsForPatient(patientId)

    suspend fun getMostCommonSymptomCategory(): String? =
        symptomDao.getMostCommonCategory()

    suspend fun getAverageSeverity(): Double? =
        symptomDao.getAverageSeverity()

    suspend fun updateTakenStatus(medicationId: Int, isTaken: Boolean, takenDate: String?) =
        medicationDao.updateTakenStatus(medicationId, isTaken, takenDate)

    suspend fun resetTakenStatusForNewDay(today: String) =
        medicationDao.resetTakenStatusForNewDay(today)

    suspend fun getMaxPatientIdNumber(): Int? = patientDao.getMaxPatientIdNumber()

    suspend fun generateNextPatientId(): String {
        val maxNum = getMaxPatientIdNumber()
        val nextNum = (maxNum ?: 1000) + 1
        return "P$nextNum"
    }

    // ========== MEDCOACH TIPS ==========

    suspend fun insertTip(tip: MedCoachTipEntity) =
        medCoachTipsDao.insertTip(tip)

    fun getTipsForPatient(patientId: String): Flow<List<MedCoachTipEntity>> =
        medCoachTipsDao.getTipsForPatient(patientId)


    suspend fun deleteMedication(medicationId: Int) =
        medicationDao.deleteMedicationById(medicationId)

    suspend fun updateMedication(medication: MedicationEntity) =
        medicationDao.updateMedication(medication)



    suspend fun getSymptomsForPatientOnce(patientId: String): List<SymptomEntity> =
        symptomDao.getSymptomsForPatientOnce(patientId)

    suspend fun getAllSymptomsForInsights(): List<SymptomEntity> =
        symptomDao.getAllSymptoms()
}
