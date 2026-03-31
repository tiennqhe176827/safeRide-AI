package com.example.saferideai.data.settings

import android.content.Context

class AppSettingsRepository(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getEmergencyKeyword(): String {
        return preferences.getString(KEY_EMERGENCY_KEYWORD, "dung lai") ?: "dung lai"
    }

    fun setEmergencyKeyword(keyword: String) {
        preferences.edit().putString(KEY_EMERGENCY_KEYWORD, keyword.trim()).apply()
    }

    fun getFamilyPhones(): List<String> {
        return preferences.getString(KEY_FAMILY_PHONES, "")
            .orEmpty()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun setFamilyPhones(phones: List<String>) {
        val normalizedPhones = phones
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

        preferences.edit().putString(KEY_FAMILY_PHONES, normalizedPhones).apply()
    }

    companion object {
        private const val PREF_NAME = "safe_ride_config"
        private const val KEY_EMERGENCY_KEYWORD = "emergency_keyword"
        private const val KEY_FAMILY_PHONES = "family_phones"
    }
}
