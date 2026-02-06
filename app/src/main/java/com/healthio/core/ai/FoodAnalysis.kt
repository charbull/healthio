package com.healthio.core.ai

data class FoodAnalysis(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val insulinScore: Double,
    val healthScore: Double,
    val feedback: String
)
