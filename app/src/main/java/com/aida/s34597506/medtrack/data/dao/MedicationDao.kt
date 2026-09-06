package com.aida.s34597506.medtrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :medicationId")
    suspend fun deleteMedicationById(medicationId: Int)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE patientId = :patientId")
    fun getMedicationsForPatient(patientId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE patientId = :patientId")
    suspend fun getMedicationsForPatientOnce(patientId: String): List<MedicationEntity>

    @Query("UPDATE medications SET isTaken = :isTaken, takenDate = :takenDate WHERE id = :medicationId")
    suspend fun updateTakenStatus(medicationId: Int, isTaken: Boolean, takenDate: String?)

    @Query("UPDATE medications SET isTaken = 0, takenDate = NULL WHERE takenDate != :today OR takenDate IS NULL")
    suspend fun resetTakenStatusForNewDay(today: String)

    @Query("SELECT AVG(med_count) FROM (SELECT COUNT(*) as med_count FROM medications GROUP BY patientId)")
    suspend fun getAverageMedicationsPerPatient(): Double?

    @Query("SELECT medicationName FROM medications WHERE patientId = :patientId")
    suspend fun getMedicationNamesForPatient(patientId: String): List<String>
}