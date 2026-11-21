package com.example.ui.quranlearning

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * কুরআন শিক্ষা বিভাগের সম্পূর্ণ অফলাইন Local Storage ম্যানেজার।
 * - কোনো Firestore Read/Write করে না।
 * - সম্পন্ন পাঠ, সর্বশেষ পড়া পাঠ এবং পরীক্ষার ফলাফল SharedPreferences-এ সংরক্ষণ করে।
 */
class QuranLearningStorageManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _completedLessonIds = MutableStateFlow(loadStringSet(KEY_COMPLETED_LESSONS))
    val completedLessonIds: StateFlow<Set<String>> = _completedLessonIds.asStateFlow()

    private val _lastReadLessonId = MutableStateFlow(
        prefs.getString(KEY_LAST_READ_LESSON, null)?.takeIf { it.isNotBlank() }
    )
    val lastReadLessonId: StateFlow<String?> = _lastReadLessonId.asStateFlow()

    private val _bestQuizScore = MutableStateFlow(prefs.getInt(KEY_BEST_QUIZ_SCORE, 0))
    val bestQuizScore: StateFlow<Int> = _bestQuizScore.asStateFlow()

    private val _totalQuizAttempts = MutableStateFlow(prefs.getInt(KEY_TOTAL_QUIZ_ATTEMPTS, 0))
    val totalQuizAttempts: StateFlow<Int> = _totalQuizAttempts.asStateFlow()

    fun recordLastReadLesson(lessonId: String) {
        _lastReadLessonId.value = lessonId
        prefs.edit().putString(KEY_LAST_READ_LESSON, lessonId).apply()
    }

    fun toggleLessonCompleted(lessonId: String): Boolean {
        val updated = _completedLessonIds.value.toMutableSet()
        val nowCompleted = if (updated.contains(lessonId)) {
            updated.remove(lessonId)
            false
        } else {
            updated.add(lessonId)
            true
        }
        _completedLessonIds.value = updated
        prefs.edit().putString(KEY_COMPLETED_LESSONS, updated.joinToString(",")).apply()
        return nowCompleted
    }

    fun isLessonCompleted(lessonId: String): Boolean = _completedLessonIds.value.contains(lessonId)

    fun recordQuizScore(score: Int) {
        val attempts = _totalQuizAttempts.value + 1
        _totalQuizAttempts.value = attempts
        val best = maxOf(_bestQuizScore.value, score)
        _bestQuizScore.value = best
        prefs.edit()
            .putInt(KEY_BEST_QUIZ_SCORE, best)
            .putInt(KEY_TOTAL_QUIZ_ATTEMPTS, attempts)
            .apply()
    }

    fun getCompletedCountForLevel(level: QuranLearningLevel): Int {
        val completed = _completedLessonIds.value
        return QuranLearningCatalog.getLessonsForLevel(level).count { completed.contains(it.id) }
    }

    fun getLastReadLessonOrDefault(): QuranLesson {
        val savedId = _lastReadLessonId.value
        return if (savedId != null) {
            QuranLearningCatalog.getLessonById(savedId) ?: QuranLearningCatalog.allLessons.first()
        } else {
            QuranLearningCatalog.allLessons.first()
        }
    }

    fun resetForTesting() {
        prefs.edit().clear().commit()
        _completedLessonIds.value = emptySet()
        _lastReadLessonId.value = null
        _bestQuizScore.value = 0
        _totalQuizAttempts.value = 0
    }

    private fun loadStringSet(key: String): Set<String> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    companion object {
        private const val PREFS_NAME = "qawmi_quran_learning_academy_prefs"
        private const val KEY_COMPLETED_LESSONS = "completed_lesson_ids"
        private const val KEY_LAST_READ_LESSON = "last_read_lesson_id"
        private const val KEY_BEST_QUIZ_SCORE = "best_quiz_score"
        private const val KEY_TOTAL_QUIZ_ATTEMPTS = "total_quiz_attempts"

        @Volatile
        private var instance: QuranLearningStorageManager? = null

        fun getInstance(context: Context): QuranLearningStorageManager {
            return instance ?: synchronized(this) {
                instance ?: QuranLearningStorageManager(context).also { instance = it }
            }
        }
    }
}
