package com.example.ui.asmaulhusna

/**
 * আসমাউল হুসনা (أَسْمَاءُ اللَّهِ الْحُسْنَىٰ — আল্লাহর ৯৯টি সুন্দর নাম) ডেটা মডেল।
 * কুরআনুল কারীম, সহীহ হাদিস ও আহলে সুন্নাত ওয়াল জামাআতের নির্ভরযোগ্য আলেমগণের ব্যাখ্যার আলোকে প্রণীত।
 */
data class AsmaulHusnaName(
    val number: Int,
    val numberBangla: String,
    val arabicName: String,
    val transliterationBangla: String,
    val pronunciationGuideBangla: String,
    val meaningBangla: String,
    val detailedExplanationBangla: String,
    val quranicVerseArabic: String,
    val quranicVerseBangla: String,
    val quranicReference: String,
    val scholarlySourceAndNote: String,
    val lessonForBelieverBangla: String,
    val authenticDuaArabic: String,
    val authenticDuaPronunciation: String,
    val authenticDuaMeaning: String,
    val authenticDuaSource: String,
    val verifiedAudioUrl: String? = null
)

enum class AsmaulHusnaFilter(val id: String, val labelBangla: String) {
    ALL("all", "সব নাম"),
    BOOKMARKED("bookmarked", "পছন্দের নাম"),
    LEARNED("learned", "শেখা হয়েছে"),
    UNLEARNED("unlearned", "শেখা বাকি")
}

enum class AsmaulHusnaSubScreen {
    LIST_HOME,
    NAME_DETAIL
}
