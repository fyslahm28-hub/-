package com.example.ui.kitab

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class KitabCategory(
    val id: String,
    val titleBangla: String,
    val shortBadgeBangla: String,
    val tag: String
) {
    ALL("ALL", "সব কিতাব", "সকল কিতাব", "kitab_cat_all"),
    BUKHARI_SHARAH("BUKHARI_SHARAH", "সহিহ বুখারির বাংলা শরাহ", "বুখারি শরাহ", "kitab_cat_bukhari"),
    MUSLIM_SHARAH("MUSLIM_SHARAH", "সহিহ মুসলিমের বাংলা শরাহ", "মুসলিম শরাহ", "kitab_cat_muslim"),
    ABU_DAWOOD_SHARAH("ABU_DAWOOD_SHARAH", "সুনানে আবু দাউদের বাংলা শরাহ", "আবু দাউদ শরাহ", "kitab_cat_abudawood"),
    TIRMIZI_SHARAH("TIRMIZI_SHARAH", "জামে তিরমিজির বাংলা শরাহ", "তিরমিজি শরাহ", "kitab_cat_tirmizi"),
    NASAI_SHARAH("NASAI_SHARAH", "সুনানে নাসায়ির বাংলা শরাহ", "নাসায়ি শরাহ", "kitab_cat_nasai"),
    IBN_MAJAH_SHARAH("IBN_MAJAH_SHARAH", "সুনানে ইবনে মাজাহর বাংলা শরাহ", "ইবনে মাজাহ শরাহ", "kitab_cat_ibnmajah"),
    MISHKAT_SHARAH("MISHKAT_SHARAH", "মিশকাতুল মাসাবিহের বাংলা ব্যাখ্যা", "মিশকাত শরাহ", "kitab_cat_mishkat"),
    QAWMI_TEXTBOOK_SHARAH("QAWMI_TEXTBOOK_SHARAH", "অন্যান্য কওমি মাদরাসায় পাঠ্য বাংলা শরাহগ্রন্থ", "কওমি নেসাব শরাহ", "kitab_cat_qawmi_others");

    companion object {
        fun fromId(id: String): KitabCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: QAWMI_TEXTBOOK_SHARAH
        }
    }
}

enum class KitabDownloadStatus(val labelBangla: String) {
    NOT_DOWNLOADED("ডাউনলোড করা হয়নি"),
    DOWNLOADING("ডাউনলোড হচ্ছে..."),
    PAUSED("বিরতি দেওয়া হয়েছে (Resume প্রস্তুত)"),
    VERIFYING_CHECKSUM("SHA-256 Checksum যাচাই হচ্ছে..."),
    DOWNLOADED_VERIFIED("ডাউনলোড সম্পন্ন • অফলাইনে প্রস্তুত"),
    FAILED_CORRUPTED("ত্রুটি বা অসম্পূর্ণ ফাইল (পুনরায় চেষ্টা করুন)"),
    REFERENCE_ONLY_NO_LICENSE("প্রকাশকের কপিরাইট সংরক্ষিত (শুধু পরিচিতি)")
}

enum class KitabReaderThemeMode(
    val id: String,
    val titleBangla: String
) {
    MUSHAF_CREAM("cream", "মাদরাসা ক্রিম"),
    CLASSIC_WHITE("white", "সাদা পৃষ্ঠা"),
    SEPIA_WARM("sepia", "সেপিয়া মোড"),
    NIGHT_DARK("night", "রাতের মোড")
}

/**
 * UI & Domain combined model representing a Kitab in the Catalog or My Books (আমার কিতাব) section.
 */
data class KitabItemUiModel(
    val bookId: String,
    val seriesId: String,
    val titleBangla: String,
    val arabicTitle: String,
    val authorBangla: String,
    val translatorOrCompilerBangla: String,
    val publisherBangla: String,
    val category: KitabCategory,
    val volumeNumber: Int,
    val totalVolumes: Int,
    val fileSizeBytes: Long,
    val languageBangla: String,
    val shortDescriptionBangla: String,
    val marhalaBangla: String,
    val downloadUrl: String,
    val sha256Checksum: String,
    val isOpenLicenseVerified: Boolean,
    val isAdminApproved: Boolean,
    val licenseNoteBangla: String,
    val coverPrimaryHex: Long,
    val coverAccentHex: Long,
    // Local Device State
    val downloadStatus: KitabDownloadStatus = KitabDownloadStatus.NOT_DOWNLOADED,
    val downloadedBytes: Long = 0L,
    val localFilePath: String = "",
    val sha256Verified: Boolean = false,
    val downloadedAtMillis: Long = 0L,
    val lastReadPage: Int = 1,
    val totalPageCount: Int = 0,
    val errorMessageBangla: String = ""
) {
    val canDownloadFromServer: Boolean
        get() = isAdminApproved &&
            isOpenLicenseVerified &&
            downloadUrl.startsWith("https://") &&
            sha256Checksum.length == 64 &&
            fileSizeBytes > 0L

    val isReadyToReadOffline: Boolean
        get() = downloadStatus == KitabDownloadStatus.DOWNLOADED_VERIFIED &&
            sha256Verified &&
            localFilePath.isNotBlank()

    val progressFraction: Float
        get() = if (fileSizeBytes > 0L) {
            (downloadedBytes.toFloat() / fileSizeBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val readingProgressFraction: Float
        get() = if (totalPageCount > 0) {
            (lastReadPage.toFloat() / totalPageCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val readingProgressPercent: Int
        get() = if (totalPageCount > 0) {
            ((lastReadPage * 100) / totalPageCount).coerceIn(1, 100)
        } else {
            0
        }

    val formattedFileSizeMb: String
        get() = KitabFormatUtils.formatSizeMbBangla(fileSizeBytes)

    val formattedDownloadedMb: String
        get() = KitabFormatUtils.formatSizeMbBangla(downloadedBytes)

    val formattedVolumeBadge: String
        get() = if (totalVolumes > 1) {
            "${KitabFormatUtils.toBanglaDigits(volumeNumber)}ম খণ্ড (মোট ${KitabFormatUtils.toBanglaDigits(totalVolumes)} খণ্ড)"
        } else {
            "পূর্ণাঙ্গ এক খণ্ডে সমাপ্ত"
        }
}

data class KitabBookmarkModel(
    val id: Long = 0L,
    val bookId: String,
    val bookTitleBangla: String,
    val volumeNumber: Int,
    val pageNumber: Int,
    val note: String,
    val createdAtMillis: Long
)

object KitabFormatUtils {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(value: Int): String = toBanglaDigits(value.toString())
    fun toBanglaDigits(value: Long): String = toBanglaDigits(value.toString())

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parseBanglaOrEnglishInt(raw: String): Int? {
        val normalized = buildString {
            for (ch in raw.trim()) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '০'..'৯' -> append(('0'.code + (ch.code - '০'.code)).toChar())
                }
            }
        }
        return normalized.toIntOrNull()
    }

    fun formatSizeMbBangla(bytes: Long): String {
        if (bytes <= 0L) return "০.০ MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val df = DecimalFormat("0.00")
        return "${toBanglaDigits(df.format(mb))} MB"
    }

    fun formatDateBangla(epochMillis: Long): String {
        if (epochMillis <= 0L) return "অজানা তারিখ"
        return runCatching {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            toBanglaDigits(sdf.format(Date(epochMillis)))
        }.getOrDefault("সংরক্ষিত")
    }
}
