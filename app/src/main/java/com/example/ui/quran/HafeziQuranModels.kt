package com.example.ui.quran

import androidx.compose.ui.graphics.Color

/**
 * Models for the Verified 616-Page 15-Line Indo-Pak Hafezi Quran PDF System.
 * Strictly isolated from Messaging, Feed, Auth, and Firestore modules.
 */
data class HafeziSurahInfo(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val meaningBangla: String,
    val isMakki: Boolean,
    val ayahCount: Int,
    val startPage: Int,
    val endPage: Int,
    val startPara: Int,
    val rukuCount: Int = 1
) {
    val revelationTypeBangla: String
        get() = if (isMakki) "মাক্কি" else "মাদানী"
}

data class HafeziParaInfo(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val startPage: Int,
    val endPage: Int,
    val startSurahNumber: Int,
    val startSurahNameBangla: String,
    val startAyahLabel: String,
    val ordinalBanglaTitle: String = "",
    val surahRangeLabelBangla: String = ""
) {
    val totalPages: Int
        get() = (endPage - startPage + 1).coerceAtLeast(1)

    val displayOrdinalBangla: String
        get() = ordinalBanglaTitle.ifBlank { "পারা ${HafeziQuranIndexData.toBanglaDigits(number)}" }

    val displaySurahRangeBangla: String
        get() = surahRangeLabelBangla.ifBlank { "সূরা $startSurahNameBangla ($startAyahLabel)" }
}

data class MushafLineItem(
    val lineNumber: Int, // 1..15
    val lineType: MushafLineType,
    val arabicText: String,
    val surahHeaderInfo: HafeziSurahInfo? = null,
    val ayahRangeLabel: String = "",
    val hasSajdah: Boolean = false,
    val marginalMarker: String? = null
)

enum class MushafLineType {
    SURAH_HEADER_BANNER,
    BISMILLAH_BANNER,
    QURAN_TEXT_LINE
}

data class HafeziPageMetadata(
    val pageNumber: Int, // 1..616 in the verified 15-Line Indo-Pak Quran PDF
    val paraNumber: Int,
    val paraNameArabic: String,
    val paraNameBangla: String,
    val primarySurah: HafeziSurahInfo,
    val surahsOnPage: List<HafeziSurahInfo>,
    val manzilNumber: Int, // 1..7
    val hizbQuarterLabel: String,
    val hasSajdah: Boolean,
    val lines: List<MushafLineItem> // 15-line structural index metadata
)

data class HafeziAyahSearchEntry(
    val surahNumber: Int,
    val surahNameBangla: String,
    val surahNameArabic: String,
    val ayahNumber: Int,
    val paraNumber: Int,
    val pageNumber: Int,
    val lineNumber: Int,
    val arabicText: String,
    val banglaTopicOrTranslation: String
) {
    val referenceKey: String
        get() = "$surahNumber:$ayahNumber"
}

data class QuranBookmark(
    val pageNumber: Int,
    val surahNumber: Int,
    val surahNameBangla: String,
    val paraNumber: Int,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class QuranReadingHistoryEntry(
    val pageNumber: Int,
    val surahNameBangla: String,
    val paraNumber: Int,
    val visitedAtMillis: Long,
    val durationSeconds: Int = 0
)

data class QuranReadingProgress(
    val lastReadPage: Int = 2,
    val lastReadSurahNumber: Int = 1,
    val lastReadSurahNameBangla: String = "আল-ফাতিহা",
    val lastReadParaNumber: Int = 1,
    val lastReadTimestampMillis: Long = 0L,
    val uniquePagesReadCount: Int = 0,
    val totalMushafPages: Int = HafeziQuranIndexData.TOTAL_PDF_PAGES,
    val completedKhatamCount: Int = 0
) {
    val progressFraction: Float
        get() = (uniquePagesReadCount.toFloat() / totalMushafPages.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = ((uniquePagesReadCount * 100f) / totalMushafPages).toInt().coerceIn(0, 100)
}

enum class QuranDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED_VERIFIED,
    ERROR
}

data class QuranDownloadState(
    val status: QuranDownloadStatus = QuranDownloadStatus.COMPLETED_VERIFIED,
    val downloadedPages: Int = HafeziQuranIndexData.TOTAL_PDF_PAGES,
    val totalPages: Int = HafeziQuranIndexData.TOTAL_PDF_PAGES,
    val verifiedPages: Int = HafeziQuranIndexData.TOTAL_PDF_PAGES,
    val integrityPassed: Boolean = true,
    val statusMessageBangla: String = "কোরআন শরীফ সফলভাবে ডাউনলোড হয়েছে • এখন আপনি ইন্টারনেট ছাড়াই পড়তে পারবেন",
    val lastChecksumSummary: String = HafeziQuranIndexData.ORIGINAL_PDF_SHA256.take(12).uppercase(),
    val originalPdfSha256: String = HafeziQuranIndexData.ORIGINAL_PDF_SHA256
) {
    val progressFraction: Float
        get() = if (totalPages > 0) (downloadedPages.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)
}

data class QuranIntegrityReport(
    val isValid: Boolean,
    val verifiedPageCount: Int,
    val expectedPageCount: Int,
    val corruptedOrMissingPages: List<Int>,
    val sha256DigestPrefix: String,
    val messageBangla: String,
    val fullOriginalPdfSha256: String = HafeziQuranIndexData.ORIGINAL_PDF_SHA256
)

enum class SurahFilterType(val labelBangla: String, val tag: String) {
    ALL("সব সূরা", "quran_filter_all_surahs"),
    MAKKI("মাক্কি", "quran_filter_makki_surahs"),
    MADANI("মাদানী", "quran_filter_madani_surahs")
}

enum class MushafThemeMode(
    val id: String,
    val titleBangla: String,
    val pageBackground: Color,
    val pageInnerBackground: Color,
    val textColor: Color,
    val borderColor: Color,
    val goldAccent: Color,
    val headerBackground: Color
) {
    EMDADIA_CREAM(
        id = "emdadia_cream",
        titleBangla = "গাঢ় সবুজ ও শুভ্র পৃষ্ঠা",
        pageBackground = Color(0xFFFFFFFF),
        pageInnerBackground = Color(0xFFFFFFFF),
        textColor = Color(0xFF0B0E0C),
        borderColor = Color(0xFF0F6B4B),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFF0F6B4B)
    ),
    CLASSIC_WHITE(
        id = "classic_white",
        titleBangla = "এমদাদিয়া ক্রিম",
        pageBackground = Color(0xFFFFFDF7),
        pageInnerBackground = Color(0xFFFFFEF9),
        textColor = Color(0xFF0E1310),
        borderColor = Color(0xFF164A35),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFFFFFDF7)
    ),
    SEPIA_HERITAGE(
        id = "sepia_heritage",
        titleBangla = "ক্লাসিক সেপিয়া",
        pageBackground = Color(0xFFF8F1E0),
        pageInnerBackground = Color(0xFFFCF7EB),
        textColor = Color(0xFF1F160D),
        borderColor = Color(0xFF5A4027),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFFF8F1E0)
    ),
    NIGHT_MODE(
        id = "night_mode",
        titleBangla = "রাতের মোড (Night)",
        pageBackground = Color(0xFF0B1612),
        pageInnerBackground = Color(0xFF0E1D17),
        textColor = Color(0xFFF4F0E4),
        borderColor = Color(0xFFD9A82E),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFF13261F)
    )
}
