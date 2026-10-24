package com.colombiaguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.repository.FilterConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación de FilterConfigRepository usando SharedPreferences.
 * Principio SRP: Solo persiste configuración.
 */
class FilterConfigRepositoryImpl(
    context: Context
) : FilterConfigRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val configFlow = MutableStateFlow(loadConfig())

    override fun getConfig(): Flow<FilterConfig> = configFlow.asStateFlow()

    override suspend fun getConfigOnce(): FilterConfig = loadConfig()

    override suspend fun updateConfig(config: FilterConfig) {
        prefs.edit()
            .putString(KEY_FILTER_LEVEL, config.level.name)
            .putBoolean(KEY_BLOCK_FOREIGN, config.blockForeignCalls)
            .putBoolean(KEY_SILENCE_UNKNOWN, config.silenceUnknownColombian)
            .putBoolean(KEY_PATTERN_DETECTION, config.enablePatternDetection)
            .putBoolean(KEY_WHITELIST_CONTACTS, config.whitelistAllContacts)
            .apply()

        configFlow.value = config
    }

    private fun loadConfig(): FilterConfig {
        val levelName = prefs.getString(KEY_FILTER_LEVEL, FilterLevel.NORMAL.name)
        val level = try {
            FilterLevel.valueOf(levelName ?: FilterLevel.NORMAL.name)
        } catch (e: Exception) {
            FilterLevel.NORMAL
        }

        return FilterConfig(
            level = level,
            blockForeignCalls = prefs.getBoolean(KEY_BLOCK_FOREIGN, true),
            silenceUnknownColombian = prefs.getBoolean(KEY_SILENCE_UNKNOWN, false),
            enablePatternDetection = prefs.getBoolean(KEY_PATTERN_DETECTION, true),
            whitelistAllContacts = prefs.getBoolean(KEY_WHITELIST_CONTACTS, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "colombia_guard_config"
        private const val KEY_FILTER_LEVEL = "filter_level"
        private const val KEY_BLOCK_FOREIGN = "block_foreign"
        private const val KEY_SILENCE_UNKNOWN = "silence_unknown"
        private const val KEY_PATTERN_DETECTION = "pattern_detection"
        private const val KEY_WHITELIST_CONTACTS = "whitelist_contacts"
    }
}
