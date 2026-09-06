package com.aida.s34597506.medtrack.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aida.s34597506.medtrack.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val medicationName = inputData.getString("medication_name") ?: "Your medication"
            val dosage = inputData.getString("dosage") ?: ""
            val scheduledTime = inputData.getString("scheduled_time") ?: ""

            val title = "💊 Medication Reminder"
            val content = "Time to take $medicationName $dosage (scheduled for $scheduledTime)"

            NotificationHelper.showNotification(applicationContext, title, content)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}