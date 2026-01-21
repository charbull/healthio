package com.healthio.core.ai

data class FoodAnalysis(
    val foodName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int, // Added for Net Carbs
    val insulinScore: Int, // 0-100 scale (Insulin Index)
    val healthScore: Int, // 1-10
    val feedback: String
)
