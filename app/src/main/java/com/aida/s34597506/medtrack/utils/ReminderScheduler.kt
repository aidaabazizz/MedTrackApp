package com.aida.s34597506.medtrack.utils

import android.content.Context
import androidx.work.*
import com.aida.s34597506.medtrack.data.entities.MedicationEntity
import com.aida.s34597506.medtrack.workers.MedicationReminderWorker
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleReminder(context: Context, medication: MedicationEntity) {
        cancelReminder(context, medication.id)

        val timeParts = medication.scheduledTime.split(":")
        if (timeParts.size != 2) return

        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val inputData = Data.Builder()
            .putString("medication_name", medication.medicationName)
            .putString("dosage", medication.dosage)
            .putString("scheduled_time", medication.scheduledTime)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("medication_${medication.id}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun scheduleDailyReminder(context: Context, medication: MedicationEntity) {
        val timeParts = medication.scheduledTime.split(":")
        if (timeParts.size != 2) return

        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

        val inputData = Data.Builder()
            .putString("medication_name", medication.medicationName)
            .putString("dosage", medication.dosage)
            .putString("scheduled_time", medication.scheduledTime)
            .build()

        val now = Calendar.getInstance()
        val scheduledCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        var initialDelay = scheduledCal.timeInMillis - now.timeInMillis
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1)
        }

        val workRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            1, TimeUnit.DAYS
        ).apply {
            setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            setInputData(inputData)
            addTag("medication_daily_${medication.id}")
        }.build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "medication_reminder_${medication.id}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, medicationId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag("medication_${medicationId}")
        WorkManager.getInstance(context).cancelUniqueWork("medication_reminder_${medicationId}")
    }

    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    fun rescheduleAllReminders(context: Context, medications: List<MedicationEntity>) {
        cancelAllReminders(context)
        medications.forEach { medication ->
            scheduleDailyReminder(context, medication)
        }
    }
}