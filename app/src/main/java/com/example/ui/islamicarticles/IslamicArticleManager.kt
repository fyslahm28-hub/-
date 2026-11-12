package com.example.ui.islamicarticles

import android.content.Context
import android.util.Log
import com.example.R
import com.example.ui.util.AdminSecurityHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ইসলামিক প্রবন্ধ ও জ্ঞানচর্চা বিভাগ — মূল ইঞ্জিন ও রিপোজিটরি (IslamicArticleManager)
 * - পাবলিক প্রকাশিত প্রবন্ধ, নির্বাচিত, নতুন প্রকাশিত, জনপ্রিয় ও ১২টি ক্যাটাগরি ফিল্টার
 * - লেখকের প্রবন্ধ জমা (Submission ID সহ), লেখক ড্যাশবোর্ড ও সংশোধন (Resubmit)
 * - অ্যাডমিন রিভিউ, রেফারেন্স যাচাই, সম্পাদনা ও প্রকাশনা সিস্টেম
 * - বুকমার্ক ও বাংলা ফন্ট ছোট-বড় করার সুবিধা
 * - ক্রস-ইউজার গোপনীয়তা ও শূন্য অপ্রয়োজনীয় ডাটাবেস রিড/রাইট নীতি
 */
class IslamicArticleManager(private val context: Context) {

    companion object {
        private const val TAG = "IslamicArticleManager"
        private const val PREFS_NAME = "qawmi_islamic_articles_prefs"
        private const val KEY_SUBMISSIONS_JSON = "article_submissions_json"
        private const val KEY_DYNAMIC_PUBLISHED_JSON = "dynamic_published_articles_json"
        private const val KEY_AUTHOR_PROFILES_JSON = "article_author_profiles_json"
        private const val KEY_BOOKMARK_PREFIX = "article_bookmarks_user_"
        private const val KEY_READER_FONT_SIZE_SP = "article_reader_font_size_sp"

        const val MIN_FONT_SIZE_SP = 14
        const val DEFAULT_FONT_SIZE_SP = 18
        const val MAX_FONT_SIZE_SP = 28

        @Volatile
        private var instance: IslamicArticleManager? = null

        fun getInstance(context: Context): IslamicArticleManager {
            return instance ?: synchronized(this) {
                instance ?: IslamicArticleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _allSubmissions = MutableStateFlow<List<IslamicArticleSubmission>>(emptyList())
    val allSubmissions: StateFlow<List<IslamicArticleSubmission>> = _allSubmissions.asStateFlow()

    private val _dynamicPublishedArticles = MutableStateFlow<List<PublishedIslamicArticle>>(emptyList())
    val dynamicPublishedArticles: StateFlow<List<PublishedIslamicArticle>> = _dynamicPublishedArticles.asStateFlow()

    private val _authorProfiles = MutableStateFlow<Map<String, ArticleAuthorProfile>>(emptyMap())
    val authorProfiles: StateFlow<Map<String, ArticleAuthorProfile>> = _authorProfiles.asStateFlow()

    private val _bookmarkedIdsByUser = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val bookmarkedIdsByUser: StateFlow<Map<String, Set<String>>> = _bookmarkedIdsByUser.asStateFlow()

    private val _readerFontSizeSp = MutableStateFlow(DEFAULT_FONT_SIZE_SP)
    val readerFontSizeSp: StateFlow<Int> = _readerFontSizeSp.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (ex: Exception) {
                null
            }
        }
    }

    // =========================================================================
    // ১. পাবলিক প্রকাশিত প্রবন্ধ অনুসন্ধান ও ফিল্টার (শুধুমাত্র Admin অনুমোদিত ও PUBLISHED)
    // =========================================================================

    /**
     * শুধুমাত্র যাচাইকৃত ও Admin কর্তৃক অনুমোদিত এবং প্রকাশিত (`isAdminApproved && publicationStatus == "PUBLISHED"`)
     * প্রবন্ধগুলো পাবলিক তালিকায় পাওয়া যাবে। কোনো অবস্থাতেই অনুমোদনহীন ইউজার প্রবন্ধ এখানে আসবে না।
     */
    fun getPublicPublishedArticles(): List<PublishedIslamicArticle> {
        val approvedDynamic = _dynamicPublishedArticles.value.filter {
            it.isAdminApproved && it.publicationStatus == "PUBLISHED" && it.isReferencesVerified
        }
        val dynamicIds = approvedDynamic.map { it.articleId }.toSet()
        val staticCatalog = IslamicArticleVerifiedCatalog.verifiedArticles.filter {
            it.articleId !in dynamicIds && it.isAdminApproved && it.publicationStatus == "PUBLISHED"
        }
        return (approvedDynamic + staticCatalog).sortedByDescending { it.publishedAtMillis }
    }

    fun getFeaturedArticles(): List<PublishedIslamicArticle> {
        return getPublicPublishedArticles().filter { it.isFeatured }
    }

    fun getNewlyPublishedArticles(): List<PublishedIslamicArticle> {
        val all = getPublicPublishedArticles()
        val flagged = all.filter { it.isNewRelease }
        return if (flagged.isNotEmpty()) flagged else all.take(4)
    }

    fun getPopularArticles(): List<PublishedIslamicArticle> {
        val all = getPublicPublishedArticles()
        val flagged = all.filter { it.isPopular }
        return if (flagged.isNotEmpty()) flagged else all.take(4)
    }

    fun getArticlesByCategory(category: IslamicArticleCategory?): List<PublishedIslamicArticle> {
        val all = getPublicPublishedArticles()
        if (category == null) return all
        return all.filter { it.category == category }
    }

    fun searchPublicArticles(
        query: String,
        selectedCategory: IslamicArticleCategory? = null
    ): List<PublishedIslamicArticle> {
        val baseList = getArticlesByCategory(selectedCategory)
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return baseList
        return baseList.filter { article ->
            article.title.contains(trimmed, ignoreCase = true) ||
                article.authorName.contains(trimmed, ignoreCase = true) ||
                article.authorBio.contains(trimmed, ignoreCase = true) ||
                article.category.titleBangla.contains(trimmed, ignoreCase = true) ||
                article.summary.contains(trimmed, ignoreCase = true) ||
                article.content.contains(trimmed, ignoreCase = true) ||
                article.quranReferences.contains(trimmed, ignoreCase = true) ||
                article.hadithReferences.contains(trimmed, ignoreCase = true)
        }
    }

    fun getPublicArticleById(articleId: String): PublishedIslamicArticle? {
        return getPublicPublishedArticles().firstOrNull { it.articleId == articleId }
    }

    // =========================================================================
    // ২. লেখক প্রোফাইল ও প্রবন্ধ জমা দেওয়ার সিস্টেম (User Article Submission)
    // =========================================================================

    fun saveOrUpdateAuthorProfile(
        userId: String,
        authorName: String,
        shortBio: String,
        designationOrQualification: String = "",
        institutionName: String = ""
    ): Result<ArticleAuthorProfile> {
        val cleanUserId = userId.trim()
        val cleanName = authorName.trim()
        val cleanBio = shortBio.trim()
        if (cleanUserId.isEmpty()) {
            return Result.failure(IllegalArgumentException("লগইন ছাড়া লেখক প্রোফাইল সংরক্ষণ করা যাবে না।"))
        }
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("লেখকের নাম কমপক্ষে ২ অক্ষরের হতে হবে।"))
        }
        if (cleanBio.length < 4) {
            return Result.failure(IllegalArgumentException("লেখকের সংক্ষিপ্ত পরিচিতি উল্লেখ করুন।"))
        }

        val now = System.currentTimeMillis()
        val existing = _authorProfiles.value[cleanUserId]
        val profile = ArticleAuthorProfile(
            userId = cleanUserId,
            authorName = cleanName,
            shortBio = cleanBio,
            designationOrQualification = designationOrQualification.trim(),
            institutionName = institutionName.trim(),
            isVerifiedAuthor = existing?.isVerifiedAuthor ?: false,
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now
        )

        _authorProfiles.update { it + (cleanUserId to profile) }
        persistAuthorProfiles()
        syncAuthorProfileToFirestore(profile)
        return Result.success(profile)
    }

    fun getAuthorProfile(userId: String): ArticleAuthorProfile? {
        return _authorProfiles.value[userId.trim()]
    }

    /**
     * ৫. “আপনার প্রবন্ধ জমা দিন” — নতুন প্রবন্ধ দাখিল
     * জমা দেওয়ার পর ইউনিক Submission ID তৈরি হবে এবং স্ট্যাটাস হবে `SUBMITTED`।
     * অ্যাডমিন অনুমোদন ও প্রকাশের আগে এটি কখনোই পাবলিক পেজে প্রদর্শিত হবে না।
     */
    fun submitNewArticle(
        authorId: String,
        authorName: String,
        authorBio: String,
        title: String,
        category: IslamicArticleCategory,
        content: String,
        coverImageUrl: String = "",
        quranReferences: String,
        hadithReferences: String,
        otherSourcesAndBibliography: String,
        isAuthenticityConfirmed: Boolean
    ): Result<IslamicArticleSubmission> {
        val cleanAuthorId = authorId.trim()
        val cleanAuthorName = authorName.trim()
        val cleanAuthorBio = authorBio.trim()
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        val cleanQuranRefs = quranReferences.trim()
        val cleanHadithRefs = hadithReferences.trim()
        val cleanOtherSources = otherSourcesAndBibliography.trim()

        if (cleanAuthorId.isEmpty()) {
            return Result.failure(IllegalArgumentException("প্রবন্ধ জমা দিতে অনুগ্রহ করে লগইন করুন।"))
        }
        if (cleanTitle.length < 4) {
            return Result.failure(IllegalArgumentException("প্রবন্ধের শিরোনাম কমপক্ষে ৪ অক্ষরের হতে হবে।"))
        }
        if (cleanContent.length < 20) {
            return Result.failure(IllegalArgumentException("সম্পূর্ণ প্রবন্ধের লেখা কমপক্ষে ২০ অক্ষরের হতে হবে।"))
        }
        if (cleanAuthorName.length < 2) {
            return Result.failure(IllegalArgumentException("লেখকের নাম উল্লেখ করা আবশ্যক।"))
        }
        if (cleanAuthorBio.length < 4) {
            return Result.failure(IllegalArgumentException("লেখকের সংক্ষিপ্ত পরিচিতি উল্লেখ করা আবশ্যক।"))
        }
        if (cleanQuranRefs.isEmpty() && cleanHadithRefs.isEmpty() && cleanOtherSources.isEmpty()) {
            return Result.failure(IllegalArgumentException("প্রবন্ধের নির্ভরযোগ্যতার জন্য কুরআন-হাদিসের রেফারেন্স অথবা তথ্যসূত্র উল্লেখ করা আবশ্যক।"))
        }
        if (!isAuthenticityConfirmed) {
            return Result.failure(IllegalArgumentException("ইসলামিক তথ্যের বিশুদ্ধতা ও রেফারেন্সের সত্যতা নিশ্চিতকরণে টিক দিন।"))
        }

        val now = System.currentTimeMillis()
        val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        val submissionId = "ART_SUB_2026_$uniqueSuffix"

        val submission = IslamicArticleSubmission(
            submissionId = submissionId,
            authorId = cleanAuthorId,
            authorName = cleanAuthorName,
            authorBio = cleanAuthorBio,
            title = cleanTitle,
            category = category,
            content = cleanContent,
            coverImageUrl = coverImageUrl.trim(),
            quranReferences = cleanQuranRefs,
            hadithReferences = cleanHadithRefs,
            otherSourcesAndBibliography = cleanOtherSources,
            isAuthenticityConfirmed = true,
            status = ArticleSubmissionStatus.SUBMITTED,
            adminComment = "",
            authorReplyNote = "",
            reviewedByAdminId = "",
            isReferencesVerifiedByAdmin = false,
            publishedArticleId = "",
            createdAtMillis = now,
            updatedAtMillis = now
        )

        saveOrUpdateAuthorProfile(cleanAuthorId, cleanAuthorName, cleanAuthorBio)

        _allSubmissions.update { listOf(submission) + it }
        persistSubmissions()
        syncSubmissionToFirestore(submission, isNewCreate = true)

        return Result.success(submission)
    }

    // =========================================================================
    // ৩. আমার জমা / লেখক Dashboard ও সংশোধন (Strict Cross-Author Privacy)
    // =========================================================================

    /**
     * ৬ ও ৯. একজন লেখক শুধুমাত্র নিজের জমা দেওয়া প্রবন্ধ দেখতে পারবেন।
     * অন্য কোনো ইউজারের ব্যক্তিগত সাবমিশন কখনোই দেখা যাবে না।
     */
    fun getSubmissionsForAuthor(authorId: String): List<IslamicArticleSubmission> {
        val cleanAuthorId = authorId.trim()
        if (cleanAuthorId.isEmpty()) return emptyList()
        return _allSubmissions.value
            .filter { it.authorId == cleanAuthorId }
            .sortedByDescending { it.updatedAtMillis }
    }

    /**
     * ৬. লেখক ড্যাশবোর্ড থেকে প্রবন্ধ সংশোধন করে পুনরায় জমা দেওয়া (Revise & Resubmit)
     * শুধুমাত্র মূল লেখক (`submission.authorId == authorId`) নিজের প্রবন্ধ সংশোধন করতে পারবেন।
     */
    fun resubmitUpdatedArticle(
        submissionId: String,
        authorId: String,
        title: String,
        category: IslamicArticleCategory,
        content: String,
        authorName: String,
        authorBio: String,
        coverImageUrl: String,
        quranReferences: String,
        hadithReferences: String,
        otherSourcesAndBibliography: String,
        authorReplyNote: String = ""
    ): Result<IslamicArticleSubmission> {
        val cleanAuthorId = authorId.trim()
        val existing = _allSubmissions.value.firstOrNull { it.submissionId == submissionId }
            ?: return Result.failure(IllegalArgumentException("সাবমিশনটি খুঁজে পাওয়া যায়নি।"))

        if (existing.authorId != cleanAuthorId) {
            return Result.failure(SecurityException("অন্য লেখকের ব্যক্তিগত প্রবন্ধ পরিবর্তন বা সম্পাদনা করার অনুমতি নেই।"))
        }

        if (existing.status != ArticleSubmissionStatus.SUBMITTED &&
            existing.status != ArticleSubmissionStatus.MORE_INFO_REQUIRED &&
            existing.status != ArticleSubmissionStatus.UNDER_REVIEW
        ) {
            return Result.failure(IllegalStateException("শুধুমাত্র দাখিলকৃত বা সংশোধন প্রয়োজন এমন প্রবন্ধ পুনরায় সম্পাদনা করা যাবে।"))
        }

        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        if (cleanTitle.length < 4 || cleanContent.length < 20) {
            return Result.failure(IllegalArgumentException("শিরোনাম ও মূল প্রবন্ধ যথাযথভাবে পূরণ করুন।"))
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = cleanTitle,
            category = category,
            content = cleanContent,
            authorName = authorName.trim().ifEmpty { existing.authorName },
            authorBio = authorBio.trim().ifEmpty { existing.authorBio },
            coverImageUrl = coverImageUrl.trim(),
            quranReferences = quranReferences.trim(),
            hadithReferences = hadithReferences.trim(),
            otherSourcesAndBibliography = otherSourcesAndBibliography.trim(),
            authorReplyNote = authorReplyNote.trim(),
            status = ArticleSubmissionStatus.SUBMITTED,
            updatedAtMillis = now
        )

        _allSubmissions.update { list ->
            list.map { if (it.submissionId == submissionId) updated else it }
        }
        persistSubmissions()
        syncSubmissionToFirestore(updated, isNewCreate = false)
        return Result.success(updated)
    }

    // =========================================================================
    // ৪. Admin Review, Reference Verification & Publication System
    // =========================================================================

    fun isAuthorizedAdmin(email: String?, phone: String?): Boolean {
        return AdminSecurityHelper.isAuthorizedAdminIdentity(email, phone)
    }

    fun getAllSubmissionsForAdmin(
        adminEmail: String?,
        adminPhone: String?,
        filterStatus: ArticleSubmissionStatus? = null
    ): Result<List<IslamicArticleSubmission>> {
        if (!isAuthorizedAdmin(adminEmail, adminPhone)) {
            return Result.failure(SecurityException("শুধুমাত্র অনুমোদিত অ্যাডমিন সকল সাবমিশন পর্যালোচনা করতে পারবেন।"))
        }
        val list = _allSubmissions.value.let { all ->
            if (filterStatus == null) all else all.filter { it.status == filterStatus }
        }.sortedByDescending { it.updatedAtMillis }
        return Result.success(list)
    }

    /**
     * ৭. অ্যাডমিন কর্তৃক প্রবন্ধের বিষয়বস্তু বা রেফারেন্স সম্পাদনা করার সুবিধা
     */
    fun adminEditSubmissionContent(
        adminId: String,
        adminEmail: String?,
        adminPhone: String?,
        submissionId: String,
        updatedTitle: String,
        updatedCategory: IslamicArticleCategory,
        updatedContent: String,
        updatedQuranRefs: String,
        updatedHadithRefs: String,
        updatedOtherSources: String,
        adminComment: String
    ): Result<IslamicArticleSubmission> {
        if (!isAuthorizedAdmin(adminEmail, adminPhone)) {
            return Result.failure(SecurityException("শুধুমাত্র অনুমোদিত অ্যাডমিন প্রবন্ধ সম্পাদনা করতে পারবেন।"))
        }
        val existing = _allSubmissions.value.firstOrNull { it.submissionId == submissionId }
            ?: return Result.failure(IllegalArgumentException("সাবমিশনটি পাওয়া যায়নি।"))

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = updatedTitle.trim().ifEmpty { existing.title },
            category = updatedCategory,
            content = updatedContent.trim().ifEmpty { existing.content },
            quranReferences = updatedQuranRefs.trim(),
            hadithReferences = updatedHadithRefs.trim(),
            otherSourcesAndBibliography = updatedOtherSources.trim(),
            adminComment = adminComment.trim().ifEmpty { existing.adminComment },
            reviewedByAdminId = adminId.trim().ifEmpty { "admin_verified" },
            updatedAtMillis = now
        )

        _allSubmissions.update { list ->
            list.map { if (it.submissionId == submissionId) updated else it }
        }
        persistSubmissions()
        syncSubmissionToFirestore(updated, isNewCreate = false)

        // যদি প্রবন্ধটি ইতিমধ্যে প্রকাশিত হয়ে থাকে, তবে পাবলিক ক্যাটালগেও আপডেট হবে
        if (updated.status == ArticleSubmissionStatus.PUBLISHED && updated.publishedArticleId.isNotBlank()) {
            _dynamicPublishedArticles.update { pubList ->
                pubList.map { pub ->
                    if (pub.articleId == updated.publishedArticleId) {
                        pub.copy(
                            title = updated.title,
                            category = updated.category,
                            summary = buildSummaryFromContent(updated.content),
                            content = updated.content,
                            quranReferences = updated.quranReferences,
                            hadithReferences = updated.hadithReferences,
                            otherSourcesAndBibliography = updated.otherSourcesAndBibliography,
                            updatedAtMillis = now
                        )
                    } else pub
                }
            }
            persistDynamicPublishedArticles()
        }

        return Result.success(updated)
    }

    /**
     * ৭. অ্যাডমিন রিভিউ ও প্রকাশনা (৬টি স্ট্যাটাস ও রেফারেন্স যাচাই)
     * - `PUBLISHED` করতে হলে অবশ্যই `isReferencesVerified = true` হতে হবে।
     * - `PUBLISHED` হওয়ার সাথে সাথে পাবলিক প্রবন্ধ তালিকায় যুক্ত হবে।
     * - `REJECTED` বা `MORE_INFO_REQUIRED` করা হলে পাবলিক তালিকা থেকে সরিয়ে নেওয়া হবে।
     */
    fun adminReviewSubmission(
        adminId: String,
        adminEmail: String?,
        adminPhone: String?,
        submissionId: String,
        newStatus: ArticleSubmissionStatus,
        adminComment: String,
        isReferencesVerified: Boolean,
        scholarlyDifferenceNote: String = "",
        markAsFeatured: Boolean = false
    ): Result<IslamicArticleSubmission> {
        if (!isAuthorizedAdmin(adminEmail, adminPhone)) {
            return Result.failure(SecurityException("শুধুমাত্র অনুমোদিত অ্যাডমিন প্রবন্ধ যাচাই ও প্রকাশ করতে পারবেন।"))
        }

        val existing = _allSubmissions.value.firstOrNull { it.submissionId == submissionId }
            ?: return Result.failure(IllegalArgumentException("সাবমিশনটি খুঁজে পাওয়া যায়নি।"))

        if ((newStatus == ArticleSubmissionStatus.APPROVED || newStatus == ArticleSubmissionStatus.PUBLISHED) &&
            !isReferencesVerified
        ) {
            return Result.failure(
                IllegalArgumentException("প্রবন্ধ অনুমোদন বা প্রকাশের পূর্বে কুরআন-হাদিস ও তথ্যসূত্রের নির্ভুলতা যাচাই (Reference Verification) নিশ্চিত করা বাধ্যতামূলক।")
            )
        }

        val now = System.currentTimeMillis()
        val pubArticleId = if (newStatus == ArticleSubmissionStatus.PUBLISHED) {
            existing.publishedArticleId.ifBlank { "art_pub_${existing.submissionId.lowercase(Locale.US)}" }
        } else {
            existing.publishedArticleId
        }

        val updatedSubmission = existing.copy(
            status = newStatus,
            adminComment = adminComment.trim(),
            reviewedByAdminId = adminId.trim().ifEmpty { "admin_verified" },
            isReferencesVerifiedByAdmin = isReferencesVerified,
            publishedArticleId = pubArticleId,
            updatedAtMillis = now
        )

        _allSubmissions.update { list ->
            list.map { if (it.submissionId == submissionId) updatedSubmission else it }
        }
        persistSubmissions()
        syncSubmissionToFirestore(updatedSubmission, isNewCreate = false)

        if (newStatus == ArticleSubmissionStatus.PUBLISHED) {
            val banglaDate = formatBanglaDate(now)
            val publishedArticle = PublishedIslamicArticle(
                articleId = pubArticleId,
                submissionId = updatedSubmission.submissionId,
                authorId = updatedSubmission.authorId,
                authorName = updatedSubmission.authorName,
                authorBio = updatedSubmission.authorBio,
                title = updatedSubmission.title,
                category = updatedSubmission.category,
                summary = buildSummaryFromContent(updatedSubmission.content),
                content = updatedSubmission.content,
                coverImageUrl = updatedSubmission.coverImageUrl.ifBlank { "preset_cover_quran_tadabbur" },
                quranReferences = updatedSubmission.quranReferences,
                hadithReferences = updatedSubmission.hadithReferences,
                otherSourcesAndBibliography = updatedSubmission.otherSourcesAndBibliography,
                scholarlyDifferenceNote = scholarlyDifferenceNote.trim(),
                isFeatured = markAsFeatured,
                isPopular = false,
                isNewRelease = true,
                isReferencesVerified = true,
                isAdminApproved = true,
                publicationStatus = "PUBLISHED",
                approvedByAdminId = updatedSubmission.reviewedByAdminId,
                publishedDateTextBangla = banglaDate,
                estimatedReadMinutesBangla = estimateReadTimeBangla(updatedSubmission.content),
                publishedAtMillis = now,
                updatedAtMillis = now
            )

            _dynamicPublishedArticles.update { current ->
                listOf(publishedArticle) + current.filterNot { it.articleId == pubArticleId }
            }
            persistDynamicPublishedArticles()
            syncPublishedArticleToFirestore(publishedArticle)
        } else if (pubArticleId.isNotBlank()) {
            // যদি পূর্বে প্রকাশিত প্রবন্ধ পরবর্তীতে বাতিল বা সংশোধনের জন্য পাঠানো হয়, তবে পাবলিক তালিকা থেকে অপসারণ করা হবে
            _dynamicPublishedArticles.update { current ->
                current.filterNot { it.articleId == pubArticleId }
            }
            persistDynamicPublishedArticles()
        }

        return Result.success(updatedSubmission)
    }

    // =========================================================================
    // ৫. বুকমার্ক ও ফন্ট সাইজ ম্যানেজমেন্ট (Bookmark & Font Size Adjustment)
    // =========================================================================

    fun isArticleBookmarked(userId: String, articleId: String): Boolean {
        val key = userId.trim().ifEmpty { "guest_local" }
        val set = getOrLoadBookmarksForUser(key)
        return articleId in set
    }

    fun getBookmarkedArticleIds(userId: String): Set<String> {
        val key = userId.trim().ifEmpty { "guest_local" }
        return getOrLoadBookmarksForUser(key)
    }

    fun getBookmarkedArticles(userId: String): List<PublishedIslamicArticle> {
        val ids = getBookmarkedArticleIds(userId)
        if (ids.isEmpty()) return emptyList()
        return getPublicPublishedArticles().filter { it.articleId in ids }
    }

    fun toggleBookmark(userId: String, article: PublishedIslamicArticle): Boolean {
        val key = userId.trim().ifEmpty { "guest_local" }
        val currentSet = getOrLoadBookmarksForUser(key).toMutableSet()
        val isNowBookmarked = if (article.articleId in currentSet) {
            currentSet.remove(article.articleId)
            false
        } else {
            currentSet.add(article.articleId)
            true
        }
        _bookmarkedIdsByUser.update { it + (key to currentSet) }
        prefs.edit().putStringSet(KEY_BOOKMARK_PREFIX + key, currentSet).apply()

        if (userId.isNotBlank() && userId != "guest_local") {
            syncBookmarkToFirestore(userId.trim(), article, isNowBookmarked)
        }
        return isNowBookmarked
    }

    private fun getOrLoadBookmarksForUser(userKey: String): Set<String> {
        _bookmarkedIdsByUser.value[userKey]?.let { return it }
        val stored = prefs.getStringSet(KEY_BOOKMARK_PREFIX + userKey, emptySet()) ?: emptySet()
        _bookmarkedIdsByUser.update { it + (userKey to stored) }
        return stored
    }

    fun getReaderFontSizeSp(): Int = _readerFontSizeSp.value

    fun setReaderFontSizeSp(sizeSp: Int): Int {
        val clamped = sizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
        _readerFontSizeSp.value = clamped
        prefs.edit().putInt(KEY_READER_FONT_SIZE_SP, clamped).apply()
        return clamped
    }

    fun increaseReaderFontSize(): Int {
        return setReaderFontSizeSp(_readerFontSizeSp.value + 2)
    }

    fun decreaseReaderFontSize(): Int {
        return setReaderFontSizeSp(_readerFontSizeSp.value - 2)
    }

    // =========================================================================
    // ৬. সহায়ক মেথড ও লোকাল/ক্লাউড সিঙ্ক (Zero-Unnecessary-Write Policy)
    // =========================================================================

    private fun buildSummaryFromContent(content: String): String {
        val plain = content
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .joinToString(" ")
            .replace("**", "")
            .replace("•", "")
            .trim()
        return if (plain.length <= 160) plain else plain.take(157) + "..."
    }

    private fun estimateReadTimeBangla(content: String): String {
        val words = content.split(Regex("\\s+")).size
        val minutes = (words / 120).coerceAtLeast(3)
        val banglaDigits = minutes.toString().map { ch ->
            if (ch in '0'..'9') '০' + (ch - '0') else ch
        }.joinToString("")
        return "$banglaDigits মিনিট পাঠ"
    }

    private fun formatBanglaDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("bn-BD"))
        return try {
            sdf.format(Date(millis))
        } catch (e: Exception) {
            "১২ নভেম্বর ২০২৬"
        }
    }

    private fun syncAuthorProfileToFirestore(profile: ArticleAuthorProfile) {
        val db = getFirestoreOrNull() ?: return
        val data = mapOf(
            "userId" to profile.userId,
            "authorName" to profile.authorName,
            "shortBio" to profile.shortBio,
            "designationOrQualification" to profile.designationOrQualification,
            "institutionName" to profile.institutionName,
            "isVerifiedAuthor" to profile.isVerifiedAuthor,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("article_authors")
            .document(profile.userId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.w(TAG, "Author profile sync skipped/deferred: ${e.message}")
            }
    }

    private fun syncSubmissionToFirestore(submission: IslamicArticleSubmission, isNewCreate: Boolean) {
        val db = getFirestoreOrNull() ?: return
        val data = mutableMapOf<String, Any>(
            "submissionId" to submission.submissionId,
            "authorId" to submission.authorId,
            "authorName" to submission.authorName,
            "authorBio" to submission.authorBio,
            "title" to submission.title,
            "category" to submission.category.id,
            "content" to submission.content,
            "coverImageUrl" to submission.coverImageUrl,
            "quranReferences" to submission.quranReferences,
            "hadithReferences" to submission.hadithReferences,
            "otherSourcesAndBibliography" to submission.otherSourcesAndBibliography,
            "isAuthenticityConfirmed" to submission.isAuthenticityConfirmed,
            "status" to submission.status.id,
            "adminComment" to submission.adminComment,
            "authorReplyNote" to submission.authorReplyNote,
            "reviewedByAdminId" to submission.reviewedByAdminId,
            "isReferencesVerifiedByAdmin" to submission.isReferencesVerifiedByAdmin,
            "publishedArticleId" to submission.publishedArticleId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isNewCreate) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        db.collection("islamic_article_submissions")
            .document(submission.submissionId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.w(TAG, "Article submission sync skipped/deferred: ${e.message}")
            }
    }

    private fun syncPublishedArticleToFirestore(article: PublishedIslamicArticle) {
        val db = getFirestoreOrNull() ?: return
        val data = mapOf(
            "articleId" to article.articleId,
            "submissionId" to article.submissionId,
            "authorId" to article.authorId,
            "authorName" to article.authorName,
            "authorBio" to article.authorBio,
            "title" to article.title,
            "category" to article.category.id,
            "summary" to article.summary,
            "content" to article.content,
            "coverImageUrl" to article.coverImageUrl,
            "quranReferences" to article.quranReferences,
            "hadithReferences" to article.hadithReferences,
            "otherSourcesAndBibliography" to article.otherSourcesAndBibliography,
            "scholarlyDifferenceNote" to article.scholarlyDifferenceNote,
            "isFeatured" to article.isFeatured,
            "isPopular" to article.isPopular,
            "isNewRelease" to article.isNewRelease,
            "isReferencesVerified" to article.isReferencesVerified,
            "isAdminApproved" to article.isAdminApproved,
            "publicationStatus" to article.publicationStatus,
            "approvedByAdminId" to article.approvedByAdminId,
            "publishedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("islamic_articles_published")
            .document(article.articleId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.w(TAG, "Published article sync skipped/deferred: ${e.message}")
            }
    }

    private fun syncBookmarkToFirestore(userId: String, article: PublishedIslamicArticle, isBookmarked: Boolean) {
        val db = getFirestoreOrNull() ?: return
        val docRef = db.collection("users")
            .document(userId)
            .collection("article_bookmarks")
            .document(article.articleId)
        if (isBookmarked) {
            val data = mapOf(
                "userId" to userId,
                "articleId" to article.articleId,
                "title" to article.title,
                "category" to article.category.id,
                "authorName" to article.authorName,
                "savedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data, SetOptions.merge())
        } else {
            docRef.delete()
        }
    }

    private fun loadPersistedState() {
        _readerFontSizeSp.value = prefs.getInt(KEY_READER_FONT_SIZE_SP, DEFAULT_FONT_SIZE_SP)
            .coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)

        // Load Submissions
        val subsJson = prefs.getString(KEY_SUBMISSIONS_JSON, null)
        if (!subsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(subsJson)
                val list = mutableListOf<IslamicArticleSubmission>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        IslamicArticleSubmission(
                            submissionId = obj.optString("submissionId"),
                            authorId = obj.optString("authorId"),
                            authorName = obj.optString("authorName"),
                            authorBio = obj.optString("authorBio"),
                            title = obj.optString("title"),
                            category = IslamicArticleCategory.fromId(obj.optString("category")),
                            content = obj.optString("content"),
                            coverImageUrl = obj.optString("coverImageUrl"),
                            quranReferences = obj.optString("quranReferences"),
                            hadithReferences = obj.optString("hadithReferences"),
                            otherSourcesAndBibliography = obj.optString("otherSourcesAndBibliography"),
                            isAuthenticityConfirmed = obj.optBoolean("isAuthenticityConfirmed", true),
                            status = ArticleSubmissionStatus.fromId(obj.optString("status")),
                            adminComment = obj.optString("adminComment"),
                            authorReplyNote = obj.optString("authorReplyNote"),
                            reviewedByAdminId = obj.optString("reviewedByAdminId"),
                            isReferencesVerifiedByAdmin = obj.optBoolean("isReferencesVerifiedByAdmin", false),
                            publishedArticleId = obj.optString("publishedArticleId"),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
                _allSubmissions.value = list
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse persisted article submissions", e)
            }
        }

        // Load Dynamic Published Articles
        val pubJson = prefs.getString(KEY_DYNAMIC_PUBLISHED_JSON, null)
        if (!pubJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(pubJson)
                val list = mutableListOf<PublishedIslamicArticle>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        PublishedIslamicArticle(
                            articleId = obj.optString("articleId"),
                            submissionId = obj.optString("submissionId"),
                            authorId = obj.optString("authorId"),
                            authorName = obj.optString("authorName"),
                            authorBio = obj.optString("authorBio"),
                            title = obj.optString("title"),
                            category = IslamicArticleCategory.fromId(obj.optString("category")),
                            summary = obj.optString("summary"),
                            content = obj.optString("content"),
                            coverImageUrl = obj.optString("coverImageUrl"),
                            quranReferences = obj.optString("quranReferences"),
                            hadithReferences = obj.optString("hadithReferences"),
                            otherSourcesAndBibliography = obj.optString("otherSourcesAndBibliography"),
                            scholarlyDifferenceNote = obj.optString("scholarlyDifferenceNote"),
                            isFeatured = obj.optBoolean("isFeatured", false),
                            isPopular = obj.optBoolean("isPopular", false),
                            isNewRelease = obj.optBoolean("isNewRelease", true),
                            isReferencesVerified = obj.optBoolean("isReferencesVerified", true),
                            isAdminApproved = obj.optBoolean("isAdminApproved", true),
                            publicationStatus = obj.optString("publicationStatus", "PUBLISHED"),
                            approvedByAdminId = obj.optString("approvedByAdminId"),
                            publishedDateTextBangla = obj.optString("publishedDateTextBangla", "১২ নভেম্বর ২০২৬"),
                            estimatedReadMinutesBangla = obj.optString("estimatedReadMinutesBangla", "৫ মিনিট পাঠ"),
                            publishedAtMillis = obj.optLong("publishedAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
                _dynamicPublishedArticles.value = list
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse dynamic published articles", e)
            }
        }

        // Load Author Profiles
        val profJson = prefs.getString(KEY_AUTHOR_PROFILES_JSON, null)
        if (!profJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(profJson)
                val map = mutableMapOf<String, ArticleAuthorProfile>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val uid = obj.optString("userId")
                    if (uid.isNotBlank()) {
                        map[uid] = ArticleAuthorProfile(
                            userId = uid,
                            authorName = obj.optString("authorName"),
                            shortBio = obj.optString("shortBio"),
                            designationOrQualification = obj.optString("designationOrQualification"),
                            institutionName = obj.optString("institutionName"),
                            isVerifiedAuthor = obj.optBoolean("isVerifiedAuthor", false),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    }
                }
                _authorProfiles.value = map
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse author profiles", e)
            }
        }
    }

    private fun persistSubmissions() {
        val arr = JSONArray()
        _allSubmissions.value.forEach { sub ->
            val obj = JSONObject().apply {
                put("submissionId", sub.submissionId)
                put("authorId", sub.authorId)
                put("authorName", sub.authorName)
                put("authorBio", sub.authorBio)
                put("title", sub.title)
                put("category", sub.category.id)
                put("content", sub.content)
                put("coverImageUrl", sub.coverImageUrl)
                put("quranReferences", sub.quranReferences)
                put("hadithReferences", sub.hadithReferences)
                put("otherSourcesAndBibliography", sub.otherSourcesAndBibliography)
                put("isAuthenticityConfirmed", sub.isAuthenticityConfirmed)
                put("status", sub.status.id)
                put("adminComment", sub.adminComment)
                put("authorReplyNote", sub.authorReplyNote)
                put("reviewedByAdminId", sub.reviewedByAdminId)
                put("isReferencesVerifiedByAdmin", sub.isReferencesVerifiedByAdmin)
                put("publishedArticleId", sub.publishedArticleId)
                put("createdAtMillis", sub.createdAtMillis)
                put("updatedAtMillis", sub.updatedAtMillis)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SUBMISSIONS_JSON, arr.toString()).apply()
    }

    private fun persistDynamicPublishedArticles() {
        val arr = JSONArray()
        _dynamicPublishedArticles.value.forEach { art ->
            val obj = JSONObject().apply {
                put("articleId", art.articleId)
                put("submissionId", art.submissionId)
                put("authorId", art.authorId)
                put("authorName", art.authorName)
                put("authorBio", art.authorBio)
                put("title", art.title)
                put("category", art.category.id)
                put("summary", art.summary)
                put("content", art.content)
                put("coverImageUrl", art.coverImageUrl)
                put("quranReferences", art.quranReferences)
                put("hadithReferences", art.hadithReferences)
                put("otherSourcesAndBibliography", art.otherSourcesAndBibliography)
                put("scholarlyDifferenceNote", art.scholarlyDifferenceNote)
                put("isFeatured", art.isFeatured)
                put("isPopular", art.isPopular)
                put("isNewRelease", art.isNewRelease)
                put("isReferencesVerified", art.isReferencesVerified)
                put("isAdminApproved", art.isAdminApproved)
                put("publicationStatus", art.publicationStatus)
                put("approvedByAdminId", art.approvedByAdminId)
                put("publishedDateTextBangla", art.publishedDateTextBangla)
                put("estimatedReadMinutesBangla", art.estimatedReadMinutesBangla)
                put("publishedAtMillis", art.publishedAtMillis)
                put("updatedAtMillis", art.updatedAtMillis)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DYNAMIC_PUBLISHED_JSON, arr.toString()).apply()
    }

    private fun persistAuthorProfiles() {
        val arr = JSONArray()
        _authorProfiles.value.values.forEach { prof ->
            val obj = JSONObject().apply {
                put("userId", prof.userId)
                put("authorName", prof.authorName)
                put("shortBio", prof.shortBio)
                put("designationOrQualification", prof.designationOrQualification)
                put("institutionName", prof.institutionName)
                put("isVerifiedAuthor", prof.isVerifiedAuthor)
                put("createdAtMillis", prof.createdAtMillis)
                put("updatedAtMillis", prof.updatedAtMillis)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_AUTHOR_PROFILES_JSON, arr.toString()).apply()
    }

    fun resetStateForTesting() {
        _allSubmissions.value = emptyList()
        _dynamicPublishedArticles.value = emptyList()
        _authorProfiles.value = emptyMap()
        _bookmarkedIdsByUser.value = emptyMap()
        _readerFontSizeSp.value = DEFAULT_FONT_SIZE_SP
        prefs.edit().clear().apply()
    }
}
