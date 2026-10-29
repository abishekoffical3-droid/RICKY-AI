package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.identity.AnswerStyle
import com.example.core.identity.RickyIdentity
import com.example.core.identity.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ricky_preferences")

class AppPreferencesManager(private val context: Context) {

    companion object {
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")
        val KEY_SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val KEY_SEARCH_API_KEY = stringPreferencesKey("search_api_key")

        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_REGION = stringPreferencesKey("user_region")
        val KEY_USER_LANG = stringPreferencesKey("user_lang")
        val KEY_USER_INTERESTS = stringPreferencesKey("user_interests")
        val KEY_USER_PROJECTS = stringPreferencesKey("user_projects")
        val KEY_ANSWER_STYLE = stringPreferencesKey("answer_style")
        val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val KEY_VOICE_SPEED = floatPreferencesKey("voice_speed")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

        val KEY_DEV_NAME = stringPreferencesKey("dev_name")
        val KEY_DEV_ORG = stringPreferencesKey("dev_org")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val answerStyleStr = prefs[KEY_ANSWER_STYLE] ?: AnswerStyle.BALANCED.name
        val answerStyle = try {
            AnswerStyle.valueOf(answerStyleStr)
        } catch (_: Exception) {
            AnswerStyle.BALANCED
        }
        val interestsStr = prefs[KEY_USER_INTERESTS] ?: "AI, Coding, Android applications, Personal AI projects"
        val interestsList = interestsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        UserProfile(
            displayName = prefs[KEY_USER_NAME] ?: "Abhishek Bhusal",
            region = prefs[KEY_USER_REGION] ?: "Nepal",
            preferredLanguage = prefs[KEY_USER_LANG] ?: "English",
            interests = interestsList,
            projects = prefs[KEY_USER_PROJECTS] ?: "Building RICKY AI assistant and personal machine learning workflows",
            answerStyle = answerStyle,
            memoryEnabled = prefs[KEY_MEMORY_ENABLED] ?: true,
            voiceSpeed = prefs[KEY_VOICE_SPEED] ?: 1.0f,
            voicePitch = prefs[KEY_VOICE_PITCH] ?: 1.0f
        )
    }

    val identityFlow: Flow<RickyIdentity> = context.dataStore.data.map { prefs ->
        val base = RickyIdentity()
        base.copy(
            developerName = prefs[KEY_DEV_NAME] ?: base.developerName,
            organizationName = prefs[KEY_DEV_ORG] ?: base.organizationName
        )
    }

    val activeAiProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        // Local-first default: no API key is required for offline tools.
        prefs[KEY_AI_PROVIDER] ?: "LOCAL_ASSISTANT"
    }

    val activeAiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODEL] ?: "ricky-char-gpt-v1"
    }

    val customApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_API_KEY] ?: ""
        if (raw == "DEFAULT_CUSTOM_KEY") "" else raw
    }

    val customBaseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_BASE_URL] ?: "https://openrouter.ai/api/v1"
        if (raw.isBlank() || raw == "DEFAULT_CUSTOM_URL") "https://openrouter.ai/api/v1" else raw
    }

    val searchProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEARCH_PROVIDER] ?: "DUCKDUCKGO" // default works out of the box without keys!
    }

    val searchApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SEARCH_API_KEY] ?: ""
        if (raw == "DEFAULT_SEARCH_KEY") "" else raw
    }

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: true
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = profile.displayName
            prefs[KEY_USER_REGION] = profile.region
            prefs[KEY_USER_LANG] = profile.preferredLanguage
            prefs[KEY_USER_INTERESTS] = profile.interests.joinToString(", ")
            prefs[KEY_USER_PROJECTS] = profile.projects
            prefs[KEY_ANSWER_STYLE] = profile.answerStyle.name
            prefs[KEY_MEMORY_ENABLED] = profile.memoryEnabled
            prefs[KEY_VOICE_SPEED] = profile.voiceSpeed
            prefs[KEY_VOICE_PITCH] = profile.voicePitch
        }
    }

    suspend fun saveAiSettings(provider: String, model: String, customApiKey: String, customBaseUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AI_PROVIDER] = provider
            prefs[KEY_AI_MODEL] = model
            prefs[KEY_CUSTOM_API_KEY] = customApiKey
            prefs[KEY_CUSTOM_BASE_URL] = customBaseUrl
        }
    }

    suspend fun saveSearchSettings(provider: String, apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEARCH_PROVIDER] = provider
            prefs[KEY_SEARCH_API_KEY] = apiKey
        }
    }

    suspend fun saveIdentityInfo(devName: String, devOrg: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEV_NAME] = devName
            prefs[KEY_DEV_ORG] = devOrg
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = isDark
        }
    }
}
