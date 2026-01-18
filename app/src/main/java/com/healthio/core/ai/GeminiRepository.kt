package com.healthio.core.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    suspend fun analyzeImage(bitmap: Bitmap, apiKey: String, userContext: String? = null): Result<FoodAnalysis> = withContext(Dispatchers.IO) {
        // Use Gemini 2.0 Flash Lite for higher free-tier quota
        val model = GenerativeModel(
            modelName = "gemini-2.0-flash-lite-preview-02-05",
            apiKey = apiKey
        )

        val contextString = if (!userContext.isNullOrEmpty()) "User provided context: \"$userContext\"" else ""

        val prompt = """
            Analyze this food image. $contextString
            Identify the main dish.
            Estimate calories and macros (Protein, Carbs, Fat) for the visible portion.
            Provide a health score from 1 (Unhealthy) to 10 (Very Healthy).
            
            Keto Rule: If the dish is high in carbohydrates (generally >15g net carbs) or not keto-friendly, 
            explicitly propose 2-3 keto-friendly alternatives or modifications in the feedback text.
            
            Provide a short feedback (1-3 sentences).
            
            Return ONLY raw JSON with keys: foodName, calories, protein, carbs, fat, healthScore, feedback.
            Do not use markdown formatting.
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            
            val json = response.text?.replace("```json", "")?.replace("```", "")?.trim()
            val analysis = Gson().fromJson(json, FoodAnalysis::class.java)
            
            if (analysis != null) {
                Result.success(analysis)
            } else {
                Result.failure(Exception("Failed to parse AI response"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}