package com.example.ui.quranlearning

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * অফলাইন লোকাল স্টোরেজ (SharedPreferences) ভিত্তিক কুরআন শিক্ষা প্রোগ্রেস, স্ট্রিক, XP,
 * পাঠ আনলক সিস্টেম, ভুল অনুশীলন, রিভিশন হিস্ট্রি ও অ্যাডমিন CMS যাচাই ম্যানেজার।
 * কোনো অপ্রয়োজনীয় ক্লাউড/ফায়ারস্টোর খরচ নেই।
 */
class QuranLearningStorageManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _completedLessonIds = MutableStateFlow(loadStringSet(KEY_COMPLETED_LESSONS))
    val completedLessonIds: StateFlow<Set<String>> = _completedLessonIds.asStateFlow()

    private val _totalXpPoints = MutableStateFlow(prefs.getInt(KEY_TOTAL_XP, 0))
    val totalXpPoints: StateFlow<Int> = _totalXpPoints.asStateFlow()

    private val _dailyGoalLessons = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL, 3).coerceIn(1, 10))
    val dailyGoalLessons: StateFlow<Int> = _dailyGoalLessons.asStateFlow()

    private val _todayCompletedCount = MutableStateFlow(loadTodayCompletedCount())
    val todayCompletedCount: StateFlow<Int> = _todayCompletedCount.asStateFlow()

    private val _currentStreakDays = MutableStateFlow(loadValidatedStreak())
    val currentStreakDays: StateFlow<Int> = _currentStreakDays.asStateFlow()

    private val _longestStreakDays = MutableStateFlow(prefs.getInt(KEY_LONGEST_STREAK, 0))
    val longestStreakDays: StateFlow<Int> = _longestStreakDays.asStateFlow()

    private val _lastReadLessonId = MutableStateFlow(prefs.getString(KEY_LAST_READ_LESSON, null))
    val lastReadLessonId: StateFlow<String?> = _lastReadLessonId.asStateFlow()

    private val _mistakeLessonIds = MutableStateFlow(loadStringSet(KEY_MISTAKE_LESSONS))
    val mistakeLessonIds: StateFlow<Set<String>> = _mistakeLessonIds.asStateFlow()

    private val _mistakeStepsByLesson = MutableStateFlow(loadMistakeStepsMap())
    val mistakeStepsByLesson: StateFlow<Map<String, Set<Int>>> = _mistakeStepsByLesson.asStateFlow()

    private val _revisionHistory = MutableStateFlow(loadRevisionHistory())
    val revisionHistory: StateFlow<List<RevisionHistoryEntry>> = _revisionHistory.asStateFlow()

    private val _adminApprovedLessonIds = MutableStateFlow(loadStringSet(KEY_ADMIN_APPROVED))
    val adminApprovedLessonIds: StateFlow<Set<String>> = _adminApprovedLessonIds.asStateFlow()

    /**
     * ধাপে ধাপে আনলক নীতি:
     * - প্রতিটি স্তরের প্রথম পাঠ (globalOrderInLevel == 1) সর্বদা আনলক থাকে।
     * - পরবর্তী যেকোনো পাঠ আনলক হতে হলে একই স্তরের ঠিক আগের পাঠটি সম্পন্ন হতে হবে।
     */
    fun isLessonUnlocked(
        lesson: QuranLesson,
        completedIds: Set<String> = _completedLessonIds.value,
        approvedIds: Set<String> = _adminApprovedLessonIds.value
    ): Boolean {
        if (completedIds.contains(lesson.id)) return true
        val levelLessons = QuranLearningCatalog.getLessonsForLevel(lesson.level, approvedIds)
        val index = levelLessons.indexOfFirst { it.id == lesson.id }
        if (index <= 0) return true
        val previousLesson = levelLessons[index - 1]
        return completedIds.contains(previousLesson.id)
    }

    fun getNodeState(
        lesson: QuranLesson,
        completedIds: Set<String> = _completedLessonIds.value,
        approvedIds: Set<String> = _adminApprovedLessonIds.value
    ): LessonNodeState {
        return when {
            completedIds.contains(lesson.id) -> LessonNodeState.COMPLETED
            isLessonUnlocked(lesson, completedIds, approvedIds) -> LessonNodeState.CURRENT
            else -> LessonNodeState.LOCKED
        }
    }

    fun recordLastReadLesson(lessonId: String) {
        _lastReadLessonId.value = lessonId
        prefs.edit().putString(KEY_LAST_READ_LESSON, lessonId).apply()
    }

    fun getLastReadLessonOrDefault(): QuranLesson {
        val approved = _adminApprovedLessonIds.value
        val savedId = _lastReadLessonId.value
        if (!savedId.isNullOrBlank()) {
            QuranLearningCatalog.getLessonById(savedId, approved)?.let { return it }
        }
        return QuranLearningCatalog.getStudentVisibleLessons(approved).first()
    }

    fun setDailyGoal(lessonsPerDay: Int) {
        val clamped = lessonsPerDay.coerceIn(1, 10)
        _dailyGoalLessons.value = clamped
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
    }

    /**
     * ইন্টারঅ্যাক্টিভ পাঠ সম্পন্ন হলে পয়েন্ট, স্ট্রিক, দৈনিক অগ্রগতি, ভুল অনুশীলন তালিকা ও রিভিশন হিস্ট্রি আপডেট করে।
     */
    fun completeLesson(
        lesson: QuranLesson,
        correctAnswers: Int,
        totalQuestions: Int,
        mistakeStepIndices: List<Int>
    ): LessonCompletionResult {
        val wasAlreadyCompleted = _completedLessonIds.value.contains(lesson.id)
        val updatedCompleted = _completedLessonIds.value + lesson.id
        _completedLessonIds.value = updatedCompleted

        // XP পয়েন্ট হিসাব
        val baseXp = if (wasAlreadyCompleted) 10 else 20
        val accuracyXp = correctAnswers.coerceAtLeast(0) * 5
        val specialBonus = when {
            !wasAlreadyCompleted && lesson.isFinalLevelAssessment -> 40
            !wasAlreadyCompleted && lesson.isRevisionLesson -> 20
            else -> 0
        }
        val perfectBonus = if (mistakeStepIndices.isEmpty() && totalQuestions > 0) 10 else 0
        val earnedXp = baseXp + accuracyXp + specialBonus + perfectBonus

        val newTotalXp = _totalXpPoints.value + earnedXp
        _totalXpPoints.value = newTotalXp

        // দৈনিক প্রোগ্রেস ও স্ট্রিক আপডেট
        val todayKey = getDateKey(0)
        val yesterdayKey = getDateKey(-1)
        val lastActiveDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)
        val updatedTodayCount = if (lastActiveDate == todayKey) {
            _todayCompletedCount.value + 1
        } else {
            1
        }
        _todayCompletedCount.value = updatedTodayCount

        val updatedStreak = when (lastActiveDate) {
            todayKey -> _currentStreakDays.value.coerceAtLeast(1)
            yesterdayKey -> _currentStreakDays.value + 1
            else -> 1
        }
        _currentStreakDays.value = updatedStreak

        val updatedLongest = maxOf(_longestStreakDays.value, updatedStreak)
        _longestStreakDays.value = updatedLongest

        // ভুল অনুশীলন তালিকা আপডেট (ভুল থাকলে যোগ হবে, নিখুঁত সম্পন্ন করলে মুছে যাবে)
        val updatedMistakeLessons = _mistakeLessonIds.value.toMutableSet()
        val updatedMistakeMap = _mistakeStepsByLesson.value.toMutableMap()
        if (mistakeStepIndices.isNotEmpty()) {
            updatedMistakeLessons.add(lesson.id)
            updatedMistakeMap[lesson.id] = mistakeStepIndices.toSet()
        } else {
            updatedMistakeLessons.remove(lesson.id)
            updatedMistakeMap.remove(lesson.id)
        }
        _mistakeLessonIds.value = updatedMistakeLessons
        _mistakeStepsByLesson.value = updatedMistakeMap

        // রিভিশন হিস্ট্রি আপডেট (সর্বোচ্চ ৪০টি সাম্প্রতিক রেকর্ড)
        val newEntry = RevisionHistoryEntry(
            lessonId = lesson.id,
            lessonTitle = lesson.titleBangla,
            scoreText = "${toAcademyBanglaDigits(correctAnswers)}/${toAcademyBanglaDigits(totalQuestions)}",
            earnedPoints = earnedXp,
            dateLabel = formatBanglaDateLabel()
        )
        val updatedHistory = (listOf(newEntry) + _revisionHistory.value).take(40)
        _revisionHistory.value = updatedHistory

        // সর্বশেষ পড়া পাঠ আপডেট
        _lastReadLessonId.value = lesson.id

        prefs.edit()
            .putStringSet(KEY_COMPLETED_LESSONS, updatedCompleted)
            .putInt(KEY_TOTAL_XP, newTotalXp)
            .putString(KEY_LAST_ACTIVE_DATE, todayKey)
            .putInt(KEY_TODAY_COMPLETED_COUNT, updatedTodayCount)
            .putInt(KEY_CURRENT_STREAK, updatedStreak)
            .putInt(KEY_LONGEST_STREAK, updatedLongest)
            .putString(KEY_LAST_READ_LESSON, lesson.id)
            .putStringSet(KEY_MISTAKE_LESSONS, updatedMistakeLessons)
            .putString(KEY_MISTAKE_STEPS_SERIALIZED, serializeMistakeSteps(updatedMistakeMap))
            .putString(KEY_REVISION_HISTORY, serializeRevisionHistory(updatedHistory))
            .apply()

        val nextLesson = QuranLearningCatalog.getNextLessonInLevel(lesson, _adminApprovedLessonIds.value)
        return LessonCompletionResult(
            lesson = lesson,
            earnedPoints = earnedXp,
            correctAnswers = correctAnswers,
            totalQuestions = totalQuestions,
            mistakeStepIndices = mistakeStepIndices,
            nextLessonId = nextLesson?.id
        )
    }

    /**
     * অনুমোদিত অ্যাডমিনের মাধ্যমে অপেক্ষমাণ CMS পাঠ যাচাই ও প্রকাশ/অপ্রকাশ টগল করা।
     */
    fun toggleAdminVerifyAndPublish(lessonId: String) {
        val current = _adminApprovedLessonIds.value.toMutableSet()
        if (current.contains(lessonId)) {
            current.remove(lessonId)
        } else {
            current.add(lessonId)
        }
        _adminApprovedLessonIds.value = current
        prefs.edit().putStringSet(KEY_ADMIN_APPROVED, current).apply()
    }

    fun getCurrentActiveLevelAndModule(): Pair<QuranLearningLevel, QuranLearningModule> {
        val approved = _adminApprovedLessonIds.value
        val completed = _completedLessonIds.value
        val lastLesson = getLastReadLessonOrDefault()
        val module = QuranLearningCatalog.getModuleById(lastLesson.level, lastLesson.moduleId)
            ?: QuranLearningCatalog.getModulesForLevel(lastLesson.level).first()
        if (completed.isNotEmpty() || _lastReadLessonId.value != null) {
            return lastLesson.level to module
        }
        return QuranLearningLevel.PRIMARY to QuranLearningCatalog.getModulesForLevel(QuranLearningLevel.PRIMARY).first()
    }

    fun isModuleCompleted(
        module: QuranLearningModule,
        completedIds: Set<String> = _completedLessonIds.value,
        approvedIds: Set<String> = _adminApprovedLessonIds.value
    ): Boolean {
        val modLessons = QuranLearningCatalog.getLessonsForModule(module.level, module.moduleId, approvedIds)
        return modLessons.isNotEmpty() && modLessons.all { completedIds.contains(it.id) }
    }

    fun getCompletedModulesCount(
        completedIds: Set<String> = _completedLessonIds.value,
        approvedIds: Set<String> = _adminApprovedLessonIds.value
    ): Int = QuranLearningCatalog.allModules.count { isModuleCompleted(it, completedIds, approvedIds) }

    fun getAchievementBadges(
        completedIds: Set<String> = _completedLessonIds.value,
        streakDays: Int = _currentStreakDays.value,
        totalXp: Int = _totalXpPoints.value,
        approvedIds: Set<String> = _adminApprovedLessonIds.value
    ): List<AcademyAchievementBadge> {
        val completedModules = getCompletedModulesCount(completedIds, approvedIds)
        val primaryDone = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.PRIMARY, approvedIds)
            .let { list -> list.isNotEmpty() && list.all { completedIds.contains(it.id) } }
        val intermediateDone = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.INTERMEDIATE, approvedIds)
            .let { list -> list.isNotEmpty() && list.all { completedIds.contains(it.id) } }
        val advancedDone = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.ADVANCED, approvedIds)
            .let { list -> list.isNotEmpty() && list.all { completedIds.contains(it.id) } }

        return listOf(
            AcademyAchievementBadge(
                id = "first_step",
                titleBangla = "প্রথম পদক্ষেপ",
                descriptionBangla = "সর্বপ্রথম ইন্টারঅ্যাক্টিভ পাঠ সম্পন্ন করা",
                isUnlocked = completedIds.isNotEmpty()
            ),
            AcademyAchievementBadge(
                id = "ten_lessons",
                titleBangla = "মডিউল বিজয়ী",
                descriptionBangla = "কমপক্ষে ১টি পূর্ণ মডিউল (১০+ পাঠ) সম্পন্ন করা",
                isUnlocked = completedModules >= 1 || completedIds.size >= 10
            ),
            AcademyAchievementBadge(
                id = "streak_3",
                titleBangla = "ধারাবাহিক শিক্ষার্থী",
                descriptionBangla = "টানা ৩ দিন কুরআন শিক্ষা অব্যাহত রাখা",
                isUnlocked = streakDays >= 3 || _longestStreakDays.value >= 3
            ),
            AcademyAchievementBadge(
                id = "xp_500",
                titleBangla = "তাজবীদ সাধক",
                descriptionBangla = "৫০০+ শিক্ষণ পয়েন্ট (XP) অর্জন করা",
                isUnlocked = totalXp >= 500
            ),
            AcademyAchievementBadge(
                id = "hundred_lessons",
                titleBangla = "শতপাঠ অর্জন",
                descriptionBangla = "১০০টি স্বতন্ত্র কুরআন শিক্ষার পাঠ সম্পন্ন করা",
                isUnlocked = completedIds.size >= 100
            ),
            AcademyAchievementBadge(
                id = "primary_master",
                titleBangla = "প্রাথমিক স্তর বিজয়ী",
                descriptionBangla = "প্রাথমিক শিক্ষার ১০টি মডিউল ও চূড়ান্ত মূল্যায়ন সম্পন্ন",
                isUnlocked = primaryDone
            ),
            AcademyAchievementBadge(
                id = "intermediate_master",
                titleBangla = "তাজবীদ বিশেষজ্ঞ",
                descriptionBangla = "মধ্যবর্তী শিক্ষার ১০টি মডিউল ও তাজবীদ মূল্যায়ন সম্পন্ন",
                isUnlocked = intermediateDone
            ),
            AcademyAchievementBadge(
                id = "advanced_master",
                titleBangla = "উচ্চ তিলাওয়াত সনদ-প্রস্তুতি",
                descriptionBangla = "উচ্চ শিক্ষার ১০টি মডিউল ও চূড়ান্ত মূল্যায়ন সম্পন্ন",
                isUnlocked = advancedDone
            )
        )
    }

    internal fun resetForTesting() {
        prefs.edit().clear().commit()
        _completedLessonIds.value = emptySet()
        _totalXpPoints.value = 0
        _dailyGoalLessons.value = 3
        _todayCompletedCount.value = 0
        _currentStreakDays.value = 0
        _longestStreakDays.value = 0
        _lastReadLessonId.value = null
        _mistakeLessonIds.value = emptySet()
        _mistakeStepsByLesson.value = emptyMap()
        _revisionHistory.value = emptyList()
        _adminApprovedLessonIds.value = emptySet()
    }

    private fun loadStringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()

    private fun loadTodayCompletedCount(): Int {
        val savedDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)
        return if (savedDate == getDateKey(0)) {
            prefs.getInt(KEY_TODAY_COMPLETED_COUNT, 0)
        } else {
            0
        }
    }

    private fun loadValidatedStreak(): Int {
        val savedDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null) ?: return 0
        return if (savedDate == getDateKey(0) || savedDate == getDateKey(-1)) {
            prefs.getInt(KEY_CURRENT_STREAK, 0)
        } else {
            0
        }
    }

    private fun getDateKey(dayOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun formatBanglaDateLabel(): String {
        val raw = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        return raw.map { ch ->
            if (ch in '0'..'9') "০১২৩৪৫৬৭৮৯"[ch - '0'] else ch
        }.joinToString("")
    }

    private fun serializeMistakeSteps(map: Map<String, Set<Int>>): String =
        map.entries.joinToString(";") { (k, v) -> "$k:${v.joinToString(",")}" }

    private fun loadMistakeStepsMap(): Map<String, Set<Int>> {
        val raw = prefs.getString(KEY_MISTAKE_STEPS_SERIALIZED, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val indices = parts[1].split(",").mapNotNull { it.toIntOrNull() }.toSet()
            if (indices.isEmpty()) null else parts[0] to indices
        }.toMap()
    }

    private fun serializeRevisionHistory(list: List<RevisionHistoryEntry>): String =
        list.joinToString("\n") {
            listOf(
                it.lessonId.replace("|", ""),
                it.lessonTitle.replace("|", ""),
                it.scoreText.replace("|", ""),
                it.earnedPoints.toString(),
                it.dateLabel.replace("|", "")
            ).joinToString("|")
        }

    private fun loadRevisionHistory(): List<RevisionHistoryEntry> {
        val raw = prefs.getString(KEY_REVISION_HISTORY, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 5) return@mapNotNull null
            RevisionHistoryEntry(
                lessonId = p[0],
                lessonTitle = p[1],
                scoreText = p[2],
                earnedPoints = p[3].toIntOrNull() ?: 0,
                dateLabel = p[4]
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "quran_learning_academy_v2_prefs"
        private const val KEY_COMPLETED_LESSONS = "completed_lesson_ids"
        private const val KEY_TOTAL_XP = "total_xp_points"
        private const val KEY_DAILY_GOAL = "daily_goal_lessons"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        private const val KEY_TODAY_COMPLETED_COUNT = "today_completed_count"
        private const val KEY_CURRENT_STREAK = "current_streak_days"
        private const val KEY_LONGEST_STREAK = "longest_streak_days"
        private const val KEY_LAST_READ_LESSON = "last_read_lesson_id"
        private const val KEY_MISTAKE_LESSONS = "mistake_lesson_ids"
        private const val KEY_MISTAKE_STEPS_SERIALIZED = "mistake_steps_map"
        private const val KEY_REVISION_HISTORY = "revision_history"
        private const val KEY_ADMIN_APPROVED = "admin_approved_lesson_ids"

        @Volatile
        private var INSTANCE: QuranLearningStorageManager? = null

        fun getInstance(context: Context): QuranLearningStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuranLearningStorageManager(context).also { INSTANCE = it }
            }
        }
    }
}
