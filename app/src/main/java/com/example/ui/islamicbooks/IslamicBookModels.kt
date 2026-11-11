package com.example.ui.islamicbooks

import com.google.firebase.Timestamp

/**
 * ২. ইসলামিক বইয়ের মূল লাইব্রেরির ১৫টি নির্ধারিত বিভাগ
 */
enum class IslamicBookCategory(
    val id: String,
    val titleBangla: String,
    val subtitleBangla: String
) {
    NEW_RELEASE(
        id = "NEW_RELEASE",
        titleBangla = "নতুন প্রকাশিত বই",
        subtitleBangla = "যাচাইকৃত প্রকাশের বছরসহ সাম্প্রতিক প্রকাশিত নির্ভরযোগ্য ইসলামিক বই"
    ),
    POPULAR(
        id = "POPULAR",
        titleBangla = "জনপ্রিয় ইসলামিক বই",
        subtitleBangla = "পাঠকপ্রিয় ও বহুল পঠিত নির্ভরযোগ্য দ্বীনি গ্রন্থসমূহ"
    ),
    QURAN_TAFSIR(
        id = "QURAN_TAFSIR",
        titleBangla = "কুরআন ও তাফসির",
        subtitleBangla = "তাফসীরুল কুরআন, উলুমুল কুরআন ও নির্ভরযোগ্য বাংলা অনুবাদ"
    ),
    HADITH_SHARAH(
        id = "HADITH_SHARAH",
        titleBangla = "হাদিস ও হাদিসের ব্যাখ্যা",
        subtitleBangla = "হাদিস সংকলন, শরাহ ও উসূলুল হাদিস বিষয়ক গ্রন্থ"
    ),
    AQIDAH_IMAN(
        id = "AQIDAH_IMAN",
        titleBangla = "আকিদা ও ঈমান",
        subtitleBangla = "আহলুস সুন্নাত ওয়াল জামাআতের বিশুদ্ধ আকিদা ও ঈমান"
    ),
    FIQH_MASAIL(
        id = "FIQH_MASAIL",
        titleBangla = "ফিকহ ও মাসআলা-মাসায়েল",
        subtitleBangla = "ইবাদত, মুআমালাত ও দৈনন্দিন জীবনের প্রামাণ্য মাসায়েল"
    ),
    SIRAT(
        id = "SIRAT",
        titleBangla = "সিরাত ও নবীজির জীবনী",
        subtitleBangla = "রাসূলুল্লাহ ﷺ-এর পবিত্র জীবনচরিত, শামায়েল ও আদর্শ"
    ),
    SAHABI_BIOGRAPHY(
        id = "SAHABI_BIOGRAPHY",
        titleBangla = "সাহাবিদের জীবনী",
        subtitleBangla = "আশারায়ে মুবাশশারাহ ও সাহাবায়ে কেরামের জীবনাদর্শ"
    ),
    ISLAMIC_HISTORY(
        id = "ISLAMIC_HISTORY",
        titleBangla = "ইসলামের ইতিহাস",
        subtitleBangla = "খিলাফতে রাশেদা, মুসলিম সভ্যতা ও ইতিহাসের প্রামাণ্য গ্রন্থ"
    ),
    TAZKIYAH_AKHLAQ(
        id = "TAZKIYAH_AKHLAQ",
        titleBangla = "আত্মশুদ্ধি ও আখলাক",
        subtitleBangla = "তাসাউফ, আত্মশুদ্ধি, তাকওয়া ও উত্তম চরিত্র গঠন"
    ),
    DAWAH_CONTEMPORARY(
        id = "DAWAH_CONTEMPORARY",
        titleBangla = "দাওয়াহ ও সমসাময়িক বিষয়",
        subtitleBangla = "দ্বীনি দাওয়াত, আধুনিক জিজ্ঞাসা ও সমসাময়িক ফিকহী সমাধান"
    ),
    FAMILY_SOCIAL(
        id = "FAMILY_SOCIAL",
        titleBangla = "পরিবার ও সামাজিক জীবন",
        subtitleBangla = "দাম্পত্য জীবন, পিতামাতা-সন্তানের হক ও ইসলামী সমাজ"
    ),
    WOMEN_CHILDREN(
        id = "WOMEN_CHILDREN",
        titleBangla = "নারী ও শিশু-কিশোর বিষয়ক বই",
        subtitleBangla = "নারীদের দ্বীনি মাসায়েল, আদর্শ পরিবার ও শিশু-কিশোর শিক্ষা"
    ),
    ISLAMIC_RESEARCH(
        id = "ISLAMIC_RESEARCH",
        titleBangla = "ইসলামিক গবেষণা ও প্রবন্ধ",
        subtitleBangla = "উচ্চতর ইসলামী গবেষণা, ইসলামী অর্থনীতি ও প্রবন্ধ সংকলন"
    ),
    ARABIC_BANGLA_EDU(
        id = "ARABIC_BANGLA_EDU",
        titleBangla = "আরবি-বাংলা শিক্ষামূলক বই",
        subtitleBangla = "নাহু-সরফ, আরবি ভাষা শিক্ষা ও কওমি মাদরাসা সহায়ক গ্রন্থ"
    );

    companion object {
        fun fromId(id: String): IslamicBookCategory =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: POPULAR
    }
}

/**
 * ৬.১ লেখক / প্রকাশক অ্যাকাউন্টের পরিচয় ধরন
 */
enum class ContributorRoleType(val id: String, val labelBangla: String) {
    AUTHOR("AUTHOR", "লেখক"),
    TRANSLATOR("TRANSLATOR", "অনুবাদক"),
    PUBLISHER("PUBLISHER", "প্রকাশক"),
    COMPILER("COMPILER", "সংকলক");

    companion object {
        fun fromId(id: String): ContributorRoleType =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: AUTHOR
    }
}

/**
 * ৭. কপিরাইট ও অনুমতি যাচাইয়ের ধরন
 */
enum class CopyrightPermissionType(val id: String, val labelBangla: String, val detailBangla: String) {
    SELF_COPYRIGHT(
        id = "SELF_COPYRIGHT",
        labelBangla = "বইটির স্বত্ব আমার নিজের",
        detailBangla = "লেখক বা মূল স্বত্বাধিকারী হিসেবে নিজস্ব প্রকাশনা"
    ),
    WRITTEN_PERMISSION(
        id = "WRITTEN_PERMISSION",
        labelBangla = "লেখক/প্রকাশকের লিখিত অনুমতি আছে",
        detailBangla = "মূল লেখক বা প্রকাশনীর লিখিত অনুমোদনপত্র যুক্ত"
    ),
    OPEN_LICENSE(
        id = "OPEN_LICENSE",
        labelBangla = "বইটি উন্মুক্ত প্রকাশের অনুমতিপ্রাপ্ত",
        detailBangla = "সর্বসাধারণের দ্বীনি দাওয়াত ও পাঠের জন্য উন্মুক্ত"
    ),
    REFERENCE_CATALOG_ONLY(
        id = "REFERENCE_CATALOG_ONLY",
        labelBangla = "প্রকাশনীর যাচাইকৃত ক্যাটালগ ও উন্মুক্ত পাঠ সংস্করণ",
        detailBangla = "সরকারি/অফিসিয়াল ওয়েবসাইট বা উন্মুক্ত আর্কাইভ থেকে যাচাইকৃত"
    );

    companion object {
        fun fromId(id: String): CopyrightPermissionType =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: OPEN_LICENSE
    }
}

/**
 * ৮. বই জমা দেওয়ার পর ৬টি অনুমোদন অবস্থা
 */
enum class BookSubmissionStatus(
    val id: String,
    val labelEnglish: String,
    val labelBangla: String,
    val descriptionBangla: String
) {
    SUBMITTED(
        id = "SUBMITTED",
        labelEnglish = "Submitted",
        labelBangla = "নতুন জমা দেওয়া আবেদন",
        descriptionBangla = "আপনার বইয়ের আবেদনটি সফলভাবে জমা হয়েছে এবং প্রাথমিক যাচাইয়ের অপেক্ষায় রয়েছে।"
    ),
    UNDER_REVIEW(
        id = "UNDER_REVIEW",
        labelEnglish = "Under Review",
        labelBangla = "যাচাইাধীন আবেদন",
        descriptionBangla = "অ্যাডমিন প্যানেল আপনার বইয়ের তথ্য, পিডিএফ ও কপিরাইট প্রমাণপত্র যাচাই করছেন।"
    ),
    MORE_INFO_REQUIRED(
        id = "MORE_INFO_REQUIRED",
        labelEnglish = "More Information Required",
        labelBangla = "অতিরিক্ত তথ্য প্রয়োজন",
        descriptionBangla = "অ্যাডমিন কিছু অতিরিক্ত তথ্য বা সংশোধনী চেয়েছেন। অনুগ্রহ করে নিচে উত্তর প্রদান করুন।"
    ),
    APPROVED(
        id = "APPROVED",
        labelEnglish = "Approved",
        labelBangla = "অনুমোদিত আবেদন",
        descriptionBangla = "বইটি অনুমোদিত হয়েছে এবং মূল লাইব্রেরিতে প্রকাশের জন্য প্রস্তুত।"
    ),
    REJECTED(
        id = "REJECTED",
        labelEnglish = "Rejected",
        labelBangla = "প্রত্যাখ্যাত আবেদন",
        descriptionBangla = "তথ্য বা কপিরাইট প্রমাণের ঘাটতির কারণে আবেদনটি অনুমোদিত হয়নি।"
    ),
    PUBLISHED(
        id = "PUBLISHED",
        labelEnglish = "Published",
        labelBangla = "প্রকাশিত বই",
        descriptionBangla = "আলহামদুলিল্লাহ! বইটি মূল ইসলামিক বই লাইব্রেরিতে সবার জন্য প্রকাশিত হয়েছে।"
    );

    companion object {
        fun fromId(id: String): BookSubmissionStatus =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: SUBMITTED
    }
}

/**
 * ৯. প্রকাশিত বইয়ের পাবলিক ক্যাটালগ স্ট্যাটাস
 */
enum class BookPublicationState(val id: String, val labelBangla: String) {
    PUBLISHED("PUBLISHED", "প্রকাশিত"),
    SUSPENDED("SUSPENDED", "সাময়িকভাবে স্থগিত (Suspended)"),
    REMOVED("REMOVED", "অপসারিত (Removed)");

    companion object {
        fun fromId(id: String): BookPublicationState =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: PUBLISHED
    }
}

/**
 * ৫. অফলাইন পিডিএফ ডাউনলোড অবস্থা
 */
enum class IslamicBookDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    FAILED
}

data class IslamicBookDownloadProgress(
    val bookId: String,
    val volumeNumber: Int = 1,
    val state: IslamicBookDownloadState = IslamicBookDownloadState.NOT_DOWNLOADED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = 0,
    val localFilePath: String? = null,
    val errorMessageBangla: String? = null
)

/**
 * ৬.২ সেলফ-পাবলিশিং পিডিএফ আপলোড অবস্থা
 */
enum class IslamicBookUploadState {
    IDLE,
    VALIDATING,
    UPLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class IslamicBookUploadProgress(
    val state: IslamicBookUploadState = IslamicBookUploadState.IDLE,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val uploadedBytes: Long = 0L,
    val progressPercent: Int = 0,
    val sha256Hash: String = "",
    val isDuplicateDetected: Boolean = false,
    val duplicateSubmissionOrBookTitle: String? = null,
    val storageDownloadUrl: String = "",
    val errorMessageBangla: String? = null
)

/**
 * একাধিক খণ্ড থাকলে প্রতিটি খণ্ডের তথ্য
 */
data class IslamicBookVolume(
    val volumeNumber: Int,
    val volumeTitleBangla: String,
    val pageCount: Int,
    val fileSizeTextBangla: String,
    val fileSizeBytes: Long,
    val pdfDownloadUrl: String
)

/**
 * ৩ ও ৪. মূল লাইব্রেরির বইয়ের সম্পূর্ণ মডেল
 */
data class IslamicBookCatalogItem(
    val bookId: String,
    val submissionId: String = "",
    val contributorId: String = "",
    val titleBangla: String,
    val titleArabicOrOriginal: String = "",
    val authorName: String,
    val translatorOrEditorName: String = "",
    val publisherName: String,
    val category: IslamicBookCategory,
    val secondaryCategories: List<IslamicBookCategory> = emptyList(),
    val language: String = "বাংলা",
    val editionText: String = "প্রথম সংস্করণ",
    val publicationYear: String = "",
    val isPublicationYearVerified: Boolean = false,
    val pageCount: Int,
    val isbn: String = "",
    val shortIntroduction: String,
    val tableOfContents: List<String> = emptyList(),
    val volumes: List<IslamicBookVolume> = emptyList(),
    val coverBadgeColorHex: Long = 0xFF0E5A3E,
    val coverImageUrl: String = "",
    val pdfUrl: String,
    val pdfFileSizeBytes: Long,
    val fileSizeTextBangla: String,
    val pdfSha256: String = "",
    val copyrightOwnerName: String,
    val copyrightPermissionType: CopyrightPermissionType = CopyrightPermissionType.OPEN_LICENSE,
    val approvalStatus: BookPublicationState = BookPublicationState.PUBLISHED,
    val approvedByAdminId: String = "",
    val approvedAtMillis: Long = 0L,
    val isCuratedReference: Boolean = true
) {
    /**
     * ৩. নির্ভরযোগ্য উৎস ও সততা নীতি:
     * প্রকাশের বছর নিশ্চিতভাবে জানা না থাকলে কোনো বইকে "নতুন প্রকাশিত" হিসেবে দেখানো যাবে না।
     */
    fun belongsToCategory(targetCategory: IslamicBookCategory): Boolean {
        if (targetCategory == IslamicBookCategory.NEW_RELEASE) {
            return isPublicationYearVerified &&
                publicationYear.isNotBlank() &&
                (category == IslamicBookCategory.NEW_RELEASE || secondaryCategories.contains(IslamicBookCategory.NEW_RELEASE))
        }
        return category == targetCategory || secondaryCategories.contains(targetCategory)
    }
}

/**
 * ৬.১ লেখক / অনুবাদক / প্রকাশক প্রোফাইল মডেল (`/book_contributors/{userId}`)
 */
data class BookContributorProfile(
    val userId: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val contributorRole: ContributorRoleType = ContributorRoleType.AUTHOR,
    val organizationOrPublisherName: String = "",
    val shortBio: String = "",
    val contactInfo: String = "",
    val isVerifiedContributor: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * ৬.২ ও ৭ ও ৮. লেখক/প্রকাশকের বই জমা দেওয়ার আবেদন মডেল (`/islamic_book_submissions/{submissionId}`)
 */
data class IslamicBookSubmission(
    val submissionId: String = "",
    val contributorId: String = "",
    val contributorName: String = "",
    val contributorRole: ContributorRoleType = ContributorRoleType.AUTHOR,
    val contributorPhone: String = "",
    val contributorEmail: String = "",
    val bookTitleBangla: String = "",
    val bookTitleArabicOrOriginal: String = "",
    val authorName: String = "",
    val translatorOrEditorName: String = "",
    val publisherName: String = "",
    val category: IslamicBookCategory = IslamicBookCategory.FIQH_MASAIL,
    val language: String = "বাংলা",
    val editionText: String = "প্রথম সংস্করণ",
    val publicationYear: String = "",
    val isPublicationYearVerified: Boolean = false,
    val pageCount: Int = 100,
    val isbn: String = "",
    val shortIntroduction: String = "",
    val tableOfContentsText: String = "",
    val coverImageUrl: String = "",
    val pdfStorageUrl: String = "",
    val pdfFileName: String = "",
    val pdfFileSizeBytes: Long = 0L,
    val pdfSha256: String = "",
    val copyrightOwnerName: String = "",
    val copyrightPermissionType: CopyrightPermissionType = CopyrightPermissionType.SELF_COPYRIGHT,
    val permissionProofUrl: String = "",
    val isTruthConfirmed: Boolean = false,
    val status: BookSubmissionStatus = BookSubmissionStatus.SUBMITTED,
    val adminComment: String = "",
    val contributorReplyNote: String = "",
    val reviewedByAdminId: String = "",
    val publishedBookId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * ৯. অডিট লগ মডেল (`/islamic_book_audit_logs/{logId}`)
 */
data class IslamicBookAuditLog(
    val logId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorRole: String = "CONTRIBUTOR",
    val actionType: String = "",
    val targetSubmissionId: String = "",
    val targetBookId: String = "",
    val detailsBangla: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * বুকমার্ক ও সর্বশেষ পঠিত পৃষ্ঠা সংরক্ষণের মডেল
 */
data class IslamicBookBookmark(
    val bookId: String,
    val volumeNumber: Int,
    val pageNumber: Int,
    val noteBangla: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)
