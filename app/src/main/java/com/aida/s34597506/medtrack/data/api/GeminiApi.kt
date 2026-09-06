package com.aida.s34597506.medtrack.data.api

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlin.random.Random

class GeminiApiService(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "GeminiAPI"
    }

    private val generativeModel: GenerativeModel? = try {
        Log.d(TAG, "Creating GenerativeModel with API key length: ${apiKey.length}")

        if (apiKey.isEmpty()) {
            Log.e(TAG, "API KEY IS EMPTY!")
            null
        } else if (!apiKey.startsWith("AIza")) {
            Log.e(TAG, "API KEY DOES NOT START WITH 'AIza'! Invalid key format.")
            null
        } else {
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.9f  // Increased for more variety
                    topP = 0.95f
                    topK = 40
                    maxOutputTokens = 150
                }
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create GenerativeModel: ${e.message}", e)
        null
    }

    suspend fun generateMedicationTip(
        patientName: String,
        medications: List<String>,
        symptoms: List<String>? = null,
        adherenceHistory: String? = null,
        timeOfDay: String? = null
    ): String {
        Log.d(TAG, "generateMedicationTip called with enhanced personalization")

        if (generativeModel == null) {
            return getPersonalizedFallbackTip(patientName, medications, symptoms, timeOfDay)
        }

        // Create rich context about the patient
        val medicationContext = if (medications.isEmpty()) {
            "currently not taking any medications, but we want to encourage healthy habits"
        } else if (medications.size == 1) {
            "takes ${medications[0]}"
        } else {
            "takes multiple medications: ${medications.joinToString(", ")}"
        }

        // Add symptom context
        val symptomContext = if (!symptoms.isNullOrEmpty()) {
            """
            Recent symptoms reported: ${symptoms.joinToString(", ")}
            Consider how medications might help with these symptoms or if side effects could be related.
            """.trimIndent()
        } else {
            "No recent symptoms reported. Focus on prevention and consistency."
        }

        // Add time of day context
        val timeContext = when (timeOfDay) {
            "morning" -> "It's morning - a great time to establish a medication routine for the day ahead."
            "afternoon" -> "Midday check-in - remind about any medications that might be due."
            "evening" -> "Evening reminder - perfect for bedtime medications and reflecting on the day."
            else -> "Any time is a good time for a medication reminder."
        }

        // Randomize prompt style for variety
        val promptStyles = listOf(
            // Style 1: Encouraging coach
            { name: String, meds: String, symptoms: String, time: String ->
                """
                You are a warm, encouraging medication coach. Create a short (20-30 words) personalized message for $name who is prescribed $meds.
                
                Context:
                $symptoms
                $time
                
                Make it uplifting and specific to their situation. Use emojis naturally. Be conversational like "Hey $name!" or "$name, remember...".
                """.trimIndent()
            },
            // Style 2: Practical tip giver
            { name: String, meds: String, symptoms: String, time: String ->
                """
                You are a practical medication adherence expert. Give ONE specific, actionable tip for $name who is prescribed $meds.
                
                Context:
                $symptoms
                $time
                
                Format: Start with "💡 Tip:" then give practical advice. Be concrete (e.g., "Try taking your medication with breakfast").
                """.trimIndent()
            },
            // Style 3: Motivational speaker
            { name: String, meds: String, symptoms: String, time: String ->
                """
                You are a motivational health coach. Write a short (15-25 words) inspiring message for $name who takes $meds.
                
                Context:
                $symptoms
                $time
                
                Focus on progress, consistency, and health goals. Use encouraging language. End with a positive emoji.
                """.trimIndent()
            },
            // Style 4: Friendly reminder
            { name: String, meds: String, symptoms: String, time: String ->
                """
                You are a friendly reminder system. Create a gentle, helpful reminder for $name who $meds.
                
                Context:
                $symptoms
                $time
                
                Be supportive, not nagging. Acknowledge that managing medications can be hard. Offer a simple strategy.
                """.trimIndent()
            },
            // Style 5: Health educator
            { name: String, meds: String, symptoms: String, time: String ->
                """
                You are a health educator. Share ONE brief, helpful fact or insight related to $name's situation.
                
                Context:
                $symptoms
                $time
                
                Example: "Taking your medication at the same time each day helps maintain consistent levels in your body."
                Keep it simple and supportive.
                """.trimIndent()
            }
        )

        // Select random prompt style for variety
        val selectedStyle = promptStyles[Random.nextInt(promptStyles.size)]
        val prompt = selectedStyle(patientName, medicationContext, symptomContext, timeContext)

        Log.d(TAG, "Using prompt style: ${selectedStyle.hashCode()}")
        Log.d(TAG, "Prompt preview: ${prompt.take(150)}...")

        return try {
            val response = generativeModel.generateContent(prompt)
            val result = response.text?.trim()
            Log.d(TAG, "Generated tip: $result")

            if (result.isNullOrEmpty()) {
                getPersonalizedFallbackTip(patientName, medications, symptoms, timeOfDay)
            } else {
                result.replace("\"", "").trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failed: ${e.message}", e)
            getPersonalizedFallbackTip(patientName, medications, symptoms, timeOfDay)
        }
    }

    suspend fun generateClinicianInsights(
        totalPatients: Int,
        avgMedications: Double,
        mostCommonSymptom: String,
        avgSeverity: Double,
        additionalContext: String = ""
    ): String {
        Log.d(TAG, "generateClinicianInsights called")

        if (generativeModel == null) {
            return getDefaultInsights(totalPatients, avgMedications, mostCommonSymptom, avgSeverity)
        }

        val prompt = """
            You are a clinical data analyst providing insights for healthcare providers.
            
            Data Summary:
            - Total active patients: $totalPatients
            - Average medications per patient: ${String.format("%.1f", avgMedications)}
            - Most common symptom reported: $mostCommonSymptom
            - Average symptom severity (1-10 scale): ${String.format("%.1f", avgSeverity)}
            $additionalContext
            
            Provide 3 distinct, actionable insights based on this data. Each insight should:
            1. Start with a bullet point (•)
            2. Be specific to these numbers
            3. Suggest a clinical action or observation
            4. Be written professionally but concisely
            
            Example insights:
            • With ${String.format("%.1f", avgMedications)} medications per patient on average, consider medication reconciliation reviews for polypharmacy risks.
            • ${mostCommonSymptom} being the top reported symptom suggests potential need for targeted intervention protocols.
            • Severity level of ${String.format("%.1f", avgSeverity)}/10 indicates moderate symptom burden - monthly follow-ups recommended.
            
            Generate your 3 unique insights now:
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text?.trim() ?: getDefaultInsights(totalPatients, avgMedications, mostCommonSymptom, avgSeverity)
        } catch (e: Exception) {
            Log.e(TAG, "Insights failed: ${e.message}")
            getDefaultInsights(totalPatients, avgMedications, mostCommonSymptom, avgSeverity)
        }
    }

    private fun getPersonalizedFallbackTip(
        patientName: String,
        medications: List<String>,
        symptoms: List<String>?,
        timeOfDay: String?
    ): String {
        val medName = medications.firstOrNull() ?: "your medications"
        val time = timeOfDay ?: "daily"

        val fallbackTips = listOf(
            "💡 $patientName, consistency with $medName leads to better health outcomes. You're doing great!",
            "💡 Tip: Try taking your $medName at the same $time activity, like brushing your teeth!",
            "💡 $patientName, every dose of $medName brings you closer to your health goals. Keep going! 💪",
            "💡 Setting a phone alarm for $medName can help build a lasting habit. Try it today! ⏰",
            "💡 $patientName, remember that managing $medName is a journey - celebrate every dose you take! 🎉"
        )
        return fallbackTips.random()
    }

    private fun getDefaultInsights(
        totalPatients: Int,
        avgMedications: Double,
        mostCommonSymptom: String,
        avgSeverity: Double
    ): String = """
        • $totalPatients patients are actively using the platform - consider population health outreach
        • Average of ${String.format("%.1f", avgMedications)} medications per patient suggests regular medication reviews would be beneficial
        • $mostCommonSymptom is the most frequently reported symptom (severity ${String.format("%.1f", avgSeverity)}/10) - consider targeted education materials
    """.trimIndent()
}