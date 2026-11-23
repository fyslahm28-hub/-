package com.example.ui.duas

/**
 * প্রিমিয়াম ইসলামিক দোয়া ও যিকির বিভাগের ডেটা মডেল।
 * সকল দোয়া কুরআন ও গ্রহণযোগ্য হাদিসগ্রন্থ থেকে যাচাইকৃত।
 */
enum class DuaCategory(
    val id: String,
    val titleBangla: String,
    val shortDescriptionBangla: String,
    val order: Int
) {
    DAILY_DUA(
        id = "daily_dua",
        titleBangla = "দৈনন্দিন দোয়া",
        shortDescriptionBangla = "ঘুম, খাওয়া, পোশাক ও দৈনিক জীবনের মাসনুন দোয়া",
        order = 1
    ),
    MORNING_EVENING_AZKAR(
        id = "morning_evening_azkar",
        titleBangla = "সকাল-সন্ধ্যার যিকির",
        shortDescriptionBangla = "ফজর ও আসরের পর পঠিতব্য সুরক্ষা ও বরকতের যিকির",
        order = 2
    ),
    SALAH_DUAS(
        id = "salah_duas",
        titleBangla = "সালাতের দোয়া",
        shortDescriptionBangla = "নামাজের শুরু, রুকু, সিজদা ও সালামের পরের দোয়া",
        order = 3
    ),
    QURANIC_DUAS(
        id = "quranic_duas",
        titleBangla = "কুরআনের দোয়া",
        shortDescriptionBangla = "পবিত্র কুরআনে বর্ণিত রব্বানা ও কল্যাণমূলক দোয়া",
        order = 4
    ),
    PROPHETS_DUAS(
        id = "prophets_duas",
        titleBangla = "নবীদের দোয়া",
        shortDescriptionBangla = "কুরআনে বর্ণিত নবী-রাসূলগণের (আ.) প্রসিদ্ধ দোয়া",
        order = 5
    ),
    HOME_FAMILY_DUAS(
        id = "home_family_duas",
        titleBangla = "ঘর ও পরিবারের দোয়া",
        shortDescriptionBangla = "গৃহে প্রবেশ-প্রস্থান, পিতামাতা ও সন্তানের দোয়া",
        order = 6
    ),
    TRAVEL_DUAS(
        id = "travel_duas",
        titleBangla = "সফরের দোয়া",
        shortDescriptionBangla = "যানবাহনে আরোহণ, ভ্রমণ ও নিরাপদ ফেরার দোয়া",
        order = 7
    ),
    DISTRESS_ILLNESS_DUAS(
        id = "distress_illness_duas",
        titleBangla = "বিপদ ও অসুস্থতার দোয়া",
        shortDescriptionBangla = "দুশ্চিন্তা, ঋণমুক্তি, রোগী দেখা ও শিফার দোয়া",
        order = 8
    ),
    FORGIVENESS_TAWBAH_DUAS(
        id = "forgiveness_tawbah_duas",
        titleBangla = "ক্ষমা ও তওবার দোয়া",
        shortDescriptionBangla = "সাইয়্যিদুল ইস্তিগফার ও গুনাহ মাফের মাসনুন দোয়া",
        order = 9
    ),
    RIZQ_BARAKAH_DUAS(
        id = "rizq_barakah_duas",
        titleBangla = "রিজিক ও বরকতের দোয়া",
        shortDescriptionBangla = "হালাল রিজিক, উপকারী ইলম ও বরকতের দোয়া",
        order = 10
    ),
    MASJID_DUAS(
        id = "masjid_duas",
        titleBangla = "মসজিদে প্রবেশ ও বের হওয়ার দোয়া",
        shortDescriptionBangla = "মসজিদে গমন, প্রবেশ, প্রস্থান ও আজানের দোয়া",
        order = 11
    ),
    MISC_MASNUN_DUAS(
        id = "misc_masnun_duas",
        titleBangla = "বিবিধ মাসনুন দোয়া",
        shortDescriptionBangla = "বৃষ্টি, হাঁচি, মজলিস, কৃতজ্ঞতা ও অন্যান্য দোয়া",
        order = 12
    )
}

enum class DuaSourceType(val labelBangla: String) {
    QURAN("কুরআন"),
    HADITH("হাদিস")
}

enum class DailyAmalSlot(val titleBangla: String, val subtitleBangla: String) {
    MORNING("সকালের যিকির", "ফজরের পর থেকে সূর্যোদয় পর্যন্ত"),
    EVENING("সন্ধ্যার যিকির", "আসরের পর থেকে সূর্যাস্ত পর্যন্ত"),
    AFTER_SALAH("ফরজ সালাতের পর", "প্রতি ওয়াক্ত ফরজ নামাজের সালাম ফিরিয়ে"),
    BEFORE_SLEEP("ঘুমানোর আগে", "রাতে বিছানায় শয়নের সময়"),
    TIMED_MASNUN("নির্দিষ্ট সময়ের মাসনুন দোয়া", "দৈনন্দিন বিশেষ সময়ের সুন্নত আমল")
}

enum class DuaMainTab(val labelBangla: String) {
    ALL_DUAS("সকল দোয়া"),
    DAILY_AMAL("দৈনিক আমল"),
    FAVORITES("প্রিয় দোয়া")
}

enum class DuaSourceFilter(val labelBangla: String) {
    ALL("সব"),
    QURAN("কুরআন"),
    HADITH("হাদিস")
}

/**
 * প্রতিটি যাচাইকৃত দোয়ার পূর্ণাঙ্গ মডেল।
 * - কোনো দুর্বল বা মতভেদপূর্ণ বর্ণনাকে সহীহ হিসেবে দেখানো হয় না।
 * - কুরআনের পূর্ণ আয়াত হলে quranSurahAyah সেট থাকে যাতে শাইখ হুসারীর যাচাইকৃত তিলাওয়াত অডিও চলে।
 */
data class VerifiedDua(
    val id: String,
    val category: DuaCategory,
    val titleBangla: String,
    val occasionBangla: String,
    val arabicText: String,
    val banglaPronunciation: String,
    val banglaMeaning: String,
    val referenceBangla: String,
    val gradingBangla: String,
    val scholarlyNoteBangla: String? = null,
    val sourceType: DuaSourceType,
    val quranSurahAyah: Pair<Int, Int>? = null,
    val recommendedCount: Int = 1,
    val dailyAmalSlot: DailyAmalSlot? = null,
    val hasVerifiedAudio: Boolean = true
)

sealed interface DuaDestination {
    data object Home : DuaDestination
    data class CategoryList(val category: DuaCategory) : DuaDestination
    data class Detail(
        val duaId: String,
        val fromCategory: DuaCategory? = null
    ) : DuaDestination
}
