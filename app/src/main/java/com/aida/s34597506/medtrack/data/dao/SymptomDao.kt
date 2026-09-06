package com.aida.s34597506.medtrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aida.s34597506.medtrack.data.entities.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(symptoms: List<SymptomEntity>)

    @Query("SELECT * FROM symptoms WHERE patientId = :patientId ORDER BY dateTime DESC")
    fun getSymptomsForPatient(patientId: String): Flow<List<SymptomEntity>>

    @Query("""
        SELECT category FROM symptoms 
        GROUP BY category 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostCommonCategory(): String?

    @Query("SELECT AVG(severity) FROM symptoms")
    suspend fun getAverageSeverity(): Double?

    // ADD THESE TWO METHODS:
    @Query("SELECT * FROM symptoms")
    suspend fun getAllSymptoms(): List<SymptomEntity>

    @Query("SELECT * FROM symptoms WHERE patientId = :patientId ORDER BY dateTime DESC LIMIT 10")
    suspend fun getSymptomsForPatientOnce(patientId: String): List<SymptomEntity>
}