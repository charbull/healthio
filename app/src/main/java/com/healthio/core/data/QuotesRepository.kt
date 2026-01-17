package com.healthio.core.data

object QuotesRepository {
    val quotes = listOf(
        "Fasting gives your digestive system a rest.",
        "Intermittent fasting can improve insulin sensitivity.",
        "Your body initiates cellular repair processes during fasting.",
        "Fasting may help reduce inflammation in the body.",
        "Mental clarity often increases during a fasted state.",
        "You are taking control of your health, one fast at a time.",
        "Fasting promotes autophagy, the body's way of cleaning out damaged cells.",
        "Consistency is key. Great job on completing this session!",
        "Discipline is the bridge between goals and accomplishment.",
        "Your future self will thank you for the health choices you make today."
    )

    fun getRandomQuote(): String {
        return quotes.random()
    }
}
