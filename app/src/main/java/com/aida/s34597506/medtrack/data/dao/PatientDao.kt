package com.aida.s34597506.medtrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aida.s34597506.medtrack.data.entities.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(patients: List<PatientEntity>)

    @Query("SELECT * FROM patients WHERE patientId = :patientId AND password = :password LIMIT 1")
    suspend fun login(patientId: String, password: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE patientId = :patientId AND phoneNumber = :phoneNumber LIMIT 1")
    suspend fun findByIdAndPhone(patientId: String, phoneNumber: String): PatientEntity?

    @Query("UPDATE patients SET password = :password WHERE patientId = :patientId")
    suspend fun setPassword(patientId: String, password: String)

    @Query("SELECT * FROM patients WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun findByPhone(phoneNumber: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE patientId = :patientId LIMIT 1")
    suspend fun getPatientById(patientId: String): PatientEntity?

    @Query("SELECT * FROM patients")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCount(): Int

    // FIXED: Make sure this returns all patient IDs
    @Query("SELECT patientId FROM patients")
    suspend fun getAllPatientIds(): List<String>

    // ADD THIS - Get max patient ID number directly
    @Query("SELECT MAX(CAST(SUBSTR(patientId, 2) AS INTEGER)) FROM patients")
    suspend fun getMaxPatientIdNumber(): Int?
}