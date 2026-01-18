package com.healthio.core.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    suspend fun analyzeImage(bitmap: Bitmap, apiKey: String): FoodAnalysis? = withContext(Dispatchers.IO) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            Analyze this food image. Identify the main dish.
            Estimate calories and macros (Protein, Carbs, Fat) for the visible portion.
            Provide a health score from 1 (Unhealthy) to 10 (Very Healthy).
            Provide a short 1-sentence feedback.
            
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
            Gson().fromJson(json, FoodAnalysis::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
