package com.aida.s34597506.medtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aida.s34597506.medtrack.data.dao.MedCoachTipsDao
import com.aida.s34597506.medtrack.data.dao.MedicationDao
import com.aida.s34597506.medtrack.data.dao.PatientDao
import com.aida.s34597506.medtrack.data.dao.SymptomDao
import com.aida.s34597506.medtrack.data.entities.MedCoachTipEntity
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.data.entities.PatientEntity
import com.aida.s34597506.medtrack.data.entities.SymptomEntity

@Database(
    entities = [
        PatientEntity::class,
        MedicationEntity::class,
        SymptomEntity::class,
        MedCoachTipEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MedTrackDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun medicationDao(): MedicationDao
    abstract fun symptomDao(): SymptomDao
    abstract fun medCoachTipsDao(): MedCoachTipsDao

    companion object {
        @Volatile
        private var INSTANCE: MedTrackDatabase? = null

        fun getDatabase(context: Context): MedTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackDatabase::class.java,
                    "medtrack_database"
                )
                    .fallbackToDestructiveMigration()  // ADD THIS for development (will clear data)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}