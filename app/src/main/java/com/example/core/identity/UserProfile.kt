package com.example.core.identity

enum class AnswerStyle(val label: String, val description: String) {
    BALANCED("Balanced", "Direct, well-rounded answers with natural context and examples when helpful."),
    CONCISE("Concise", "Short, fast, high-density answers without preamble or filler."),
    DETAILED("Detailed", "Comprehensive, step-by-step explanations with in-depth analysis and background."),
    LEARNING("Learning-focused", "Patient, educational explanations that explain core concepts, terms, and analogies.")
}

/**
 * User profile configuration for personalizing interactions with user consent.
 * Default starter values represent known, non-sensitive initial details.
 */
data class UserProfile(
    val displayName: String = "Abhishek Bhusal",
    val region: String = "Nepal",
    val preferredLanguage: String = "English",
    val languagesOfInterest: List<String> = listOf("Nepali", "English", "Hindi"),
    val interests: List<String> = listOf("AI", "Coding", "Android applications", "Personal AI projects"),
    val projects: String = "Building RICKY AI assistant and personal machine learning workflows",
    val answerStyle: AnswerStyle = AnswerStyle.BALANCED,
    val email: String? = null,
    val isGoogleSignedIn: Boolean = false,
    val memoryEnabled: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f
)
