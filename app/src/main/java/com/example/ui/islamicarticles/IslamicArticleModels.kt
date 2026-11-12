package com.example.ui.islamicarticles

import androidx.compose.ui.graphics.Color

/**
 * ৩. প্রবন্ধের ১২টি নির্ধারিত ক্যাটাগরি
 */
enum class IslamicArticleCategory(
    val id: String,
    val titleBangla: String,
    val subtitleBangla: String,
    val badgeColorHex: Long
) {
    QURAN_TAFSIR(
        id = "QURAN_TAFSIR",
        titleBangla = "কুরআন ও তাফসির",
        subtitleBangla = "কুরআনুল কারীমের আয়াতের তাফসির, শানে নুযুল ও তাদাব্বুর",
        badgeColorHex = 0xFF0A4A35
    ),
    HADITH_SUNNAH(
        id = "HADITH_SUNNAH",
        titleBangla = "হাদিস ও সুন্নাহ",
        subtitleBangla = "রাসূলুল্লাহ ﷺ-এর সহীহ হাদিস, সুন্নাহ ও মুহাদ্দিসগণের ব্যাখ্যা",
        badgeColorHex = 0xFF0E5E46
    ),
    AQIDAH_IMAN(
        id = "AQIDAH_IMAN",
        titleBangla = "আকিদা ও ঈমান",
        subtitleBangla = "আহলুস সুন্নাহ ওয়াল জামাআতের বিশুদ্ধ আকিদা ও ঈমানি আলোচনা",
        badgeColorHex = 0xFF1B4332
    ),
    FIQH_MASAIL(
        id = "FIQH_MASAIL",
        titleBangla = "ফিকহ ও মাসআলা-মাসায়েল",
        subtitleBangla = "ইবাদত, মুআমালাত ও জীবনঘনিষ্ঠ ফিকহি মাসায়েলের দালিলিক সমাধান",
        badgeColorHex = 0xFF7C5800
    ),
    TAZKIYAH_AKHLAQ(
        id = "TAZKIYAH_AKHLAQ",
        titleBangla = "আত্মশুদ্ধি ও আখলাক",
        subtitleBangla = "তাযকিয়ায়ে নফস, তাকওয়া ও উত্তম চরিত্র গঠন",
        badgeColorHex = 0xFF2D6A4F
    ),
    SEERAH_NABI(
        id = "SEERAH_NABI",
        titleBangla = "নবীজির সিরাত",
        subtitleBangla = "মহানবী হযরত মুহাম্মাদ ﷺ-এর পবিত্র জীবনাদর্শ ও সিরাতুন্নবী",
        badgeColorHex = 0xFF0F5132
    ),
    SAHABA_LIVES(
        id = "SAHABA_LIVES",
        titleBangla = "সাহাবিদের জীবন",
        subtitleBangla = "সাহাবায়ে কেরাম রাদিয়াল্লাহু আনহুমের ঈমানদীপ্ত জীবন ও ত্যাগ",
        badgeColorHex = 0xFF8D5B04
    ),
    ISLAMIC_HISTORY(
        id = "ISLAMIC_HISTORY",
        titleBangla = "ইসলামি ইতিহাস",
        subtitleBangla = "মুসলিম উম্মাহর গৌরবোজ্জ্বল ইতিহাস, সভ্যতা ও মনীষীদের অবদান",
        badgeColorHex = 0xFF3E4E50
    ),
    FAMILY_SOCIAL(
        id = "FAMILY_SOCIAL",
        titleBangla = "পরিবার ও সামাজিক জীবন",
        subtitleBangla = "ইসলামি পারিবারিক ব্যবস্থা, হক্কুল ইবাদ ও আদর্শ সমাজ গঠন",
        badgeColorHex = 0xFF1E5631
    ),
    CONTEMPORARY_ISSUES(
        id = "CONTEMPORARY_ISSUES",
        titleBangla = "সমসাময়িক ইসলামিক বিষয়",
        subtitleBangla = "আধুনিক যুগের জিজ্ঞাসা, অর্থনীতি ও প্রযুক্তির শরয়ী মূল্যায়ন",
        badgeColorHex = 0xFF6B4E00
    ),
    QAWMI_EDUCATION_RESEARCH(
        id = "QAWMI_EDUCATION_RESEARCH",
        titleBangla = "কওমি শিক্ষা ও গবেষণা",
        subtitleBangla = "দরসে নিযামী, কওমি মাদরাসা শিক্ষা ও উচ্চতর ইসলামি গবেষণা",
        badgeColorHex = 0xFF0A4A35
    ),
    DAWAH_DEENI_KHEDMOT(
        id = "DAWAH_DEENI_KHEDMOT",
        titleBangla = "দাওয়াহ ও দ্বীনি খেদমত",
        subtitleBangla = "দাওয়াত ইলাল্লাহ, সৎকাজের আদেশ ও দ্বীনি খেদমতের কর্মপন্থা",
        badgeColorHex = 0xFF14532D
    );

    companion object {
        fun fromId(id: String?): IslamicArticleCategory {
            if (id.isNullOrBlank()) return QURAN_TAFSIR
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
                ?: entries.firstOrNull { it.titleBangla == id.trim() }
                ?: QURAN_TAFSIR
        }
    }
}

/**
 * ৬ ও ৭. লেখক ও অ্যাডমিন ড্যাশবোর্ডের প্রবন্ধের অবস্থা (Status)
 */
enum class ArticleSubmissionStatus(
    val id: String,
    val englishLabel: String,
    val banglaLabel: String,
    val adminTabLabelBangla: String,
    val badgeColor: Color,
    val badgeBgColor: Color
) {
    SUBMITTED(
        id = "SUBMITTED",
        englishLabel = "Submitted",
        banglaLabel = "দাখিলকৃত (Submitted)",
        adminTabLabelBangla = "নতুন জমা",
        badgeColor = Color(0xFF1565C0),
        badgeBgColor = Color(0xFFE3F2FD)
    ),
    UNDER_REVIEW(
        id = "UNDER_REVIEW",
        englishLabel = "Under Review",
        banglaLabel = "যাচাইাধীন (Under Review)",
        adminTabLabelBangla = "যাচাইাধীন",
        badgeColor = Color(0xFFB45309),
        badgeBgColor = Color(0xFFFFF8E1)
    ),
    MORE_INFO_REQUIRED(
        id = "MORE_INFO_REQUIRED",
        englishLabel = "More Information Required",
        banglaLabel = "সংশোধন প্রয়োজন (More Information Required)",
        adminTabLabelBangla = "সংশোধন প্রয়োজন",
        badgeColor = Color(0xFFD97706),
        badgeBgColor = Color(0xFFFEF3C7)
    ),
    APPROVED(
        id = "APPROVED",
        englishLabel = "Approved",
        banglaLabel = "অনুমোদিত (Approved)",
        adminTabLabelBangla = "অনুমোদিত",
        badgeColor = Color(0xFF0E5E46),
        badgeBgColor = Color(0xFFE8F5E9)
    ),
    PUBLISHED(
        id = "PUBLISHED",
        englishLabel = "Published",
        banglaLabel = "প্রকাশিত (Published)",
        adminTabLabelBangla = "প্রকাশিত",
        badgeColor = Color(0xFF0A4A35),
        badgeBgColor = Color(0xFFDDF3EC)
    ),
    REJECTED(
        id = "REJECTED",
        englishLabel = "Rejected",
        banglaLabel = "বাতিল (Rejected)",
        adminTabLabelBangla = "বাতিল",
        badgeColor = Color(0xFFC62828),
        badgeBgColor = Color(0xFFFFEBEE)
    );

    companion object {
        fun fromId(id: String?): ArticleSubmissionStatus {
            if (id.isNullOrBlank()) return SUBMITTED
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
                ?: entries.firstOrNull { it.englishLabel.equals(id.trim(), ignoreCase = true) }
                ?: SUBMITTED
        }
    }
}

/**
 * ২. প্রবন্ধের মূল পেজের প্রধান তিনটি বিভাগ
 */
enum class ArticleMainTab(
    val id: String,
    val titleBangla: String
) {
    FEATURED("featured", "নির্বাচিত"),
    ALL_ARTICLES("all_articles", "সকল প্রবন্ধ"),
    MY_SUBMISSIONS("my_submissions", "আমার জমা")
}

/**
 * প্রবন্ধ বিভাগের ফুল-স্ক্রিন সাব-পেজ নেভিগেশন (কোনো Modal বা Popup নয়)
 */
enum class ArticleSubView {
    MAGAZINE_HOME,
    READING_PAGE,
    SUBMISSION_PAGE,
    ADMIN_REVIEW_DASHBOARD
}

/**
 * লেখক প্রোফাইল (`/article_authors/{userId}`)
 */
data class ArticleAuthorProfile(
    val userId: String = "",
    val authorName: String = "",
    val shortBio: String = "",
    val designationOrQualification: String = "",
    val institutionName: String = "",
    val isVerifiedAuthor: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * ব্যবহারকারীর জমা দেওয়া প্রবন্ধ (`/islamic_article_submissions/{submissionId}`)
 */
data class IslamicArticleSubmission(
    val submissionId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorBio: String = "",
    val title: String = "",
    val category: IslamicArticleCategory = IslamicArticleCategory.QURAN_TAFSIR,
    val content: String = "",
    val coverImageUrl: String = "",
    val quranReferences: String = "",
    val hadithReferences: String = "",
    val otherSourcesAndBibliography: String = "",
    val isAuthenticityConfirmed: Boolean = true,
    val status: ArticleSubmissionStatus = ArticleSubmissionStatus.SUBMITTED,
    val adminComment: String = "",
    val authorReplyNote: String = "",
    val reviewedByAdminId: String = "",
    val isReferencesVerifiedByAdmin: Boolean = false,
    val publishedArticleId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * পাবলিক প্রকাশিত প্রবন্ধ (`/islamic_articles_published/{articleId}`)
 * শুধুমাত্র যাচাইকৃত ও Admin কর্তৃক অনুমোদিত (`isAdminApproved && publicationStatus == "PUBLISHED"`) প্রবন্ধ
 */
data class PublishedIslamicArticle(
    val articleId: String = "",
    val submissionId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorBio: String = "",
    val title: String = "",
    val category: IslamicArticleCategory = IslamicArticleCategory.QURAN_TAFSIR,
    val summary: String = "",
    val content: String = "",
    val coverImageUrl: String = "",
    val quranReferences: String = "",
    val hadithReferences: String = "",
    val otherSourcesAndBibliography: String = "",
    val scholarlyDifferenceNote: String = "",
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val isNewRelease: Boolean = false,
    val isReferencesVerified: Boolean = true,
    val isAdminApproved: Boolean = true,
    val publicationStatus: String = "PUBLISHED",
    val approvedByAdminId: String = "",
    val publishedDateTextBangla: String = "",
    val estimatedReadMinutesBangla: String = "৫ মিনিট পাঠ",
    val publishedAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
