package com.example.ui.duas

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * দোয়া ও যিকির বিভাগের অফলাইন লোকাল স্টোরেজ (SharedPreferences)।
 * - কোনো অপ্রয়োজনীয় Firebase Read/Write ছাড়াই প্রিয় দোয়া, শেখা দোয়া ও দৈনিক আমলের অগ্রগতি সংরক্ষণ করে।
 * - কোনো আমল ব্যবহারকারী নিজে চিহ্নিত না করা পর্যন্ত কখনো স্বয়ংক্রিয়ভাবে সম্পন্ন দেখানো হয় না।
 */
class DuaStorageManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteDuaIds = MutableStateFlow(loadStringSet(KEY_FAVORITES))
    val favoriteDuaIds: StateFlow<Set<String>> = _favoriteDuaIds.asStateFlow()

    private val _learnedDuaIds = MutableStateFlow(loadStringSet(KEY_LEARNED))
    val learnedDuaIds: StateFlow<Set<String>> = _learnedDuaIds.asStateFlow()

    private val _dailyCompletedIds = MutableStateFlow(loadTodayCompletedSet())
    val dailyCompletedIds: StateFlow<Set<String>> = _dailyCompletedIds.asStateFlow()

    private val _arabicFontScale = MutableStateFlow(
        prefs.getFloat(KEY_ARABIC_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.40f)
    )
    val arabicFontScale: StateFlow<Float> = _arabicFontScale.asStateFlow()

    fun isFavorite(duaId: String): Boolean = _favoriteDuaIds.value.contains(duaId)

    fun toggleFavorite(duaId: String): Boolean {
        val current = _favoriteDuaIds.value.toMutableSet()
        val nowFavorite = if (current.contains(duaId)) {
            current.remove(duaId)
            false
        } else {
            current.add(duaId)
            true
        }
        _favoriteDuaIds.value = current.toSet()
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return nowFavorite
    }

    fun isLearned(duaId: String): Boolean = _learnedDuaIds.value.contains(duaId)

    fun toggleLearned(duaId: String): Boolean {
        val current = _learnedDuaIds.value.toMutableSet()
        val nowLearned = if (current.contains(duaId)) {
            current.remove(duaId)
            false
        } else {
            current.add(duaId)
            true
        }
        _learnedDuaIds.value = current.toSet()
        prefs.edit().putStringSet(KEY_LEARNED, current).apply()
        return nowLearned
    }

    /**
     * ব্যবহারকারী নিজে "সম্পন্ন করেছি" (Mark as Completed) চাপলেই কেবল দৈনিক আমল সম্পন্ন হিসেবে সংরক্ষিত হয়।
     */
    fun toggleDailyAmalCompleted(duaId: String): Boolean {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_DAILY_DATE, null)
        val base = if (savedDate == today) {
            _dailyCompletedIds.value.toMutableSet()
        } else {
            mutableSetOf()
        }
        val nowCompleted = if (base.contains(duaId)) {
            base.remove(duaId)
            false
        } else {
            base.add(duaId)
            true
        }
        _dailyCompletedIds.value = base.toSet()
        prefs.edit()
            .putString(KEY_DAILY_DATE, today)
            .putStringSet(KEY_DAILY_COMPLETED, base)
            .apply()
        return nowCompleted
    }

    fun refreshDailyDateIfChanged() {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_DAILY_DATE, null)
        if (savedDate != today && _dailyCompletedIds.value.isNotEmpty()) {
            _dailyCompletedIds.value = emptySet()
            prefs.edit()
                .putString(KEY_DAILY_DATE, today)
                .putStringSet(KEY_DAILY_COMPLETED, emptySet())
                .apply()
        }
    }

    fun updateArabicFontScale(newScale: Float) {
        val clamped = newScale.coerceIn(0.85f, 1.40f)
        _arabicFontScale.value = clamped
        prefs.edit().putFloat(KEY_ARABIC_FONT_SCALE, clamped).apply()
    }

    fun resetForTesting() {
        prefs.edit().clear().apply()
        _favoriteDuaIds.value = emptySet()
        _learnedDuaIds.value = emptySet()
        _dailyCompletedIds.value = emptySet()
        _arabicFontScale.value = 1.0f
    }

    private fun loadStringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet().orEmpty()

    private fun loadTodayCompletedSet(): Set<String> {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_DAILY_DATE, null)
        return if (savedDate == today) {
            loadStringSet(KEY_DAILY_COMPLETED)
        } else {
            emptySet()
        }
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        private const val PREFS_NAME = "qawmi_duas_azkar_local_v1"
        private const val KEY_FAVORITES = "favorite_dua_ids"
        private const val KEY_LEARNED = "learned_dua_ids"
        private const val KEY_DAILY_DATE = "daily_amal_date"
        private const val KEY_DAILY_COMPLETED = "daily_amal_completed_ids"
        private const val KEY_ARABIC_FONT_SCALE = "arabic_font_scale"

        @Volatile
        private var INSTANCE: DuaStorageManager? = null

        fun getInstance(context: Context): DuaStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DuaStorageManager(context).also { INSTANCE = it }
            }
        }
    }
}
