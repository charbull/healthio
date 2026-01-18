package com.healthio.core.ai

data class FoodAnalysis(
    val foodName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val healthScore: Int, // 1-10
    val feedback: String
)
