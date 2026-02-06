package com.healthio.core.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    suspend fun analyzeImage(bitmap: Bitmap, apiKey: String, userContext: String? = null): Result<FoodAnalysis> = withContext(Dispatchers.IO) {
        // Use Gemini 2.5 Flash Lite
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = apiKey
        )

        val contextString = if (!userContext.isNullOrEmpty()) {
            "IMPORTANT - User Context: \"$userContext\". The user has explicitly identified or described the food. Use this information to override visual assumptions (e.g., 'cauliflower rice' vs 'white rice')."
        } else {
            ""
        }

        val prompt = """
            Analyze this food image.
            $contextString
            
            Identify the main dish.
            Estimate calories and macros (Protein, Carbs, Fat) for the visible portion.
            Estimate dietary Fiber (in grams) for Net Carb calculation.
            Estimate an Insulin Index Score (0-100), where 0 is no impact (water) and 100 is pure glucose/jelly beans.
            Provide a health score from 1 (Unhealthy) to 10 (Very Healthy).
            
            IMPORTANT: Return whole numbers (integers) for all numeric values (calories, protein, carbs, fat, fiber, scores).
            
            Keto Rule: If the dish is high in carbohydrates (generally >15g net carbs) or not keto-friendly, 
            explicitly propose 2-3 keto-friendly alternatives or modifications in the feedback text.
            
            Provide a short feedback (1-3 sentences).
            
            Return ONLY raw JSON with keys: foodName, calories, protein, carbs, fat, fiber, insulinScore, healthScore, feedback.
            Do not use markdown formatting.
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            
            val rawText = response.text ?: ""
            parseResponse(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    internal fun parseResponse(rawText: String): Result<FoodAnalysis> {
        // Extract JSON substring
        val startIndex = rawText.indexOf('{')
        val endIndex = rawText.lastIndexOf('}')

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return try {
                val jsonString = rawText.substring(startIndex, endIndex + 1)
                val analysis = Gson().fromJson(jsonString, FoodAnalysis::class.java)

                if (analysis != null) {
                    Result.success(analysis)
                } else {
                    Result.failure(Exception("Parsed JSON was null"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("JSON Parsing failed: ${e.message}"))
            }
        } else {
            return Result.failure(Exception("No valid JSON found in response: $rawText"))
        }
    }
}