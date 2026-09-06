package com.aida.s34597506.medtrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aida.s34597506.medtrack.data.entities.MedCoachTipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedCoachTipsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: MedCoachTipEntity)

    // Get all tips for this patient, newest first (reactive, for history dialog)
    @Query("SELECT * FROM medcoach_tips WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getTipsForPatient(patientId: String): Flow<List<MedCoachTipEntity>>
}