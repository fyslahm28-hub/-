package com.example.ui.asmaulhusna

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * আসমাউল হুসনা লোকাল স্টোরেজ ও স্টেট ম্যানেজার।
 * - কোনো অপ্রয়োজনীয় Firestore Read/Write করে না।
 * - বুকমার্ক (পছন্দের নাম) ও শেখা নামের অগ্রগতি সম্পূর্ণ অফলাইনে SharedPreferences-এ সংরক্ষণ করে।
 * - ইন্টারনেট ছাড়াই ৯৯টি নাম, অর্থ, দলিল, দোয়া, সার্চ ও ডেইলি লার্নিং সম্পূর্ণ কার্যকর থাকে।
 */
class AsmaulHusnaManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _bookmarkedNumbers = MutableStateFlow(loadIntSet(KEY_BOOKMARKED_NUMBERS))
    val bookmarkedNumbers: StateFlow<Set<Int>> = _bookmarkedNumbers.asStateFlow()

    private val _learnedNumbers = MutableStateFlow(loadIntSet(KEY_LEARNED_NUMBERS))
    val learnedNumbers: StateFlow<Set<Int>> = _learnedNumbers.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean(KEY_IS_GRID_VIEW, true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun isBookmarked(number: Int): Boolean = _bookmarkedNumbers.value.contains(number)

    fun isLearned(number: Int): Boolean = _learnedNumbers.value.contains(number)

    fun toggleBookmark(number: Int): Boolean {
        val updated = _bookmarkedNumbers.value.toMutableSet()
        val nowBookmarked = if (updated.contains(number)) {
            updated.remove(number)
            false
        } else {
            updated.add(number)
            true
        }
        _bookmarkedNumbers.value = updated
        saveIntSet(KEY_BOOKMARKED_NUMBERS, updated)
        return nowBookmarked
    }

    fun toggleLearned(number: Int): Boolean {
        val updated = _learnedNumbers.value.toMutableSet()
        val nowLearned = if (updated.contains(number)) {
            updated.remove(number)
            false
        } else {
            updated.add(number)
            true
        }
        _learnedNumbers.value = updated
        saveIntSet(KEY_LEARNED_NUMBERS, updated)
        return nowLearned
    }

    fun setGridView(grid: Boolean) {
        _isGridView.value = grid
        prefs.edit().putBoolean(KEY_IS_GRID_VIEW, grid).apply()
    }

    /**
     * দৈনিক একটি নাম শেখার বিভাগ (Daily Name to Learn):
     * বছরের দিন অনুযায়ী ১ থেকে ৯৯-এর মধ্যে একটি নির্ধারিত নাম প্রদান করে।
     */
    fun getDailyNameToLearn(dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)): AsmaulHusnaName {
        val safeDay = if (dayOfYear <= 0) 1 else dayOfYear
        val targetNumber = ((safeDay - 1) % 99) + 1
        return AsmaulHusnaCatalog.getNameByNumber(targetNumber)
    }

    /**
     * নাম, বাংলা উচ্চারণ, বাংলা অর্থ, আরবি বানান অথবা ক্রমিক নম্বর দিয়ে সার্চ ও ফিল্টার।
     */
    fun filterAndSearchNames(
        query: String,
        filter: AsmaulHusnaFilter = AsmaulHusnaFilter.ALL,
        bookmarks: Set<Int> = _bookmarkedNumbers.value,
        learned: Set<Int> = _learnedNumbers.value
    ): List<AsmaulHusnaName> {
        val baseList = when (filter) {
            AsmaulHusnaFilter.ALL -> AsmaulHusnaCatalog.allNames
            AsmaulHusnaFilter.BOOKMARKED -> AsmaulHusnaCatalog.allNames.filter { bookmarks.contains(it.number) }
            AsmaulHusnaFilter.LEARNED -> AsmaulHusnaCatalog.allNames.filter { learned.contains(it.number) }
            AsmaulHusnaFilter.UNLEARNED -> AsmaulHusnaCatalog.allNames.filter { !learned.contains(it.number) }
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) return baseList

        val normalizedQuery = trimmed.lowercase()
        return baseList.filter { item ->
            item.transliterationBangla.lowercase().contains(normalizedQuery) ||
                item.meaningBangla.lowercase().contains(normalizedQuery) ||
                item.arabicName.contains(trimmed) ||
                item.numberBangla == trimmed ||
                item.number.toString() == trimmed ||
                item.detailedExplanationBangla.lowercase().contains(normalizedQuery) ||
                item.quranicReference.lowercase().contains(normalizedQuery)
        }
    }

    fun resetStateForTesting() {
        prefs.edit().clear().commit()
        _bookmarkedNumbers.value = emptySet()
        _learnedNumbers.value = emptySet()
        _isGridView.value = true
    }

    private fun loadIntSet(key: String): Set<Int> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..99 }
            .toSet()
    }

    private fun saveIntSet(key: String, values: Set<Int>) {
        val serialized = values.sorted().joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    companion object {
        private const val PREFS_NAME = "qawmi_asmaul_husna_local_prefs"
        private const val KEY_BOOKMARKED_NUMBERS = "bookmarked_name_numbers"
        private const val KEY_LEARNED_NUMBERS = "learned_name_numbers"
        private const val KEY_IS_GRID_VIEW = "is_grid_view_enabled"

        @Volatile
        private var instance: AsmaulHusnaManager? = null

        fun getInstance(context: Context): AsmaulHusnaManager {
            return instance ?: synchronized(this) {
                instance ?: AsmaulHusnaManager(context).also { instance = it }
            }
        }
    }
}
