package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material3.MaterialTheme
import com.example.ui.islamicarticles.ArticleSubmissionStatus
import com.example.ui.islamicarticles.IslamicArticleCategory
import com.example.ui.islamicarticles.IslamicArticleManager
import com.example.ui.islamicarticles.IslamicArticlePublicationScreen
import com.example.ui.islamicarticles.IslamicArticleVerifiedCatalog
import com.example.ui.screens.IslamicAndEssentialServicesSection
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.viewmodel.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ১২ নং নির্দেশনা অনুযায়ী সম্পূর্ণ এন্ড-টু-এন্ড টেস্ট স্যুট:
 * ১. Home Page -> প্রবন্ধ যাওয়া
 * ২. ১২টি ক্যাটাগরি ও সার্চ
 * ৩. প্রবন্ধ পড়ার পেজ ও বাংলা ফন্ট সাইজ পরিবর্তন
 * ৪. বুকমার্ক ও শেয়ার
 * ৫. ইউজারের প্রবন্ধ জমা দেওয়া (৮টি ফিল্ড + এডিটর ফরম্যাটিং)
 * ৬. ইউনিক Submission ID তৈরি
 * ৭. লেখক ড্যাশবোর্ড ও সংশোধন (Revise & Resubmit)
 * ৮. Admin Review, ৬টি স্ট্যাটাস ও রেফারেন্স যাচাই
 * ৯. অনুমোদনের আগে প্রবন্ধ Public Page-এ না দেখানো
 * ১০. অনুমোদনের পর প্রবন্ধ Public Page-এ দেখানো
 * ১১. এক ইউজারের জমা অন্য ইউজার পরিবর্তন বা দেখতে না পারা
 * ১২. আগের সব ফিচার অক্ষত থাকা ও ইসলামিক তথ্যের নির্ভুলতা
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IslamicArticlePublicationSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var articleManager: IslamicArticleManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        articleManager = IslamicArticleManager.getInstance(context)
        articleManager.resetStateForTesting()
    }

    /**
     * টেস্ট ১ ও ১২: Home Page -> “প্রবন্ধ” বাটনে চাপ দিলে আলাদা Full-Screen প্রবন্ধ বিভাগে নেভিগেট হয়
     * এবং আগের সব সার্ভিস (কুরআন, হাদিস, তিলাওয়াত, ইসলামিক বই) অক্ষত থাকে।
     */
    @Test
    fun test01_homePageArticlesButton_navigatesToFullScreenArticlePublicationAndPreservesExistingServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            MaterialTheme {
                IslamicAndEssentialServicesSection(
                    onNavigate = { dest -> navigatedDestination = dest },
                    onOpenPrayerTimes = {}
                )
            }
        }

        // Verify existing services are intact
        composeTestRule.onNodeWithTag("home_islamic_service_quran").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_hadith").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_tilawat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_books").assertIsDisplayed()

        // Tap "প্রবন্ধ" button
        composeTestRule.onNodeWithTag("home_islamic_service_articles")
            .assertIsDisplayed()
            .performClick()

        assertEquals(MainDestination.ISLAMIC_ARTICLE_PUBLICATION, navigatedDestination)
    }

    /**
     * টেস্ট ২ ও ৮ (ইসলামিক তথ্যের নির্ভুলতা): ১২টি নির্ধারিত ক্যাটাগরি, প্রধান ৩টি বিভাগ, সার্চ বার
     * এবং প্রতিটি প্রবন্ধে যাচাইকৃত কুরআন সূরা-আয়াত, হাদিস নম্বর ও গ্রন্থপঞ্জি থাকা।
     */
    @Test
    fun test02_twelveCategories_searchFilter_andVerifiedIslamicReferencesCatalog() {
        assertEquals(12, IslamicArticleCategory.entries.size)
        val allArticles = articleManager.getPublicPublishedArticles()
        assertEquals(12, allArticles.size)

        // Verify every category has a verified article with non-empty Quran, Hadith, and Bibliography references
        IslamicArticleCategory.entries.forEach { cat ->
            val byCat = articleManager.getArticlesByCategory(cat)
            assertTrue("Category ${cat.titleBangla} must have verified articles", byCat.isNotEmpty())
            byCat.forEach { art ->
                assertTrue("Quran reference must not be blank for ${art.articleId}", art.quranReferences.isNotBlank())
                assertTrue("Hadith reference must not be blank for ${art.articleId}", art.hadithReferences.isNotBlank())
                assertTrue("Bibliography must not be blank for ${art.articleId}", art.otherSourcesAndBibliography.isNotBlank())
                assertTrue("Must be admin approved", art.isAdminApproved)
                assertEquals("PUBLISHED", art.publicationStatus)
            }
        }

        // Verify Search query filtering by title, author, category, and reference
        val searchByTadabbur = articleManager.searchPublicArticles("তাদাব্বুর")
        assertEquals(1, searchByTadabbur.size)
        assertEquals(IslamicArticleCategory.QURAN_TAFSIR, searchByTadabbur.first().category)

        val searchByFiqh = articleManager.searchPublicArticles("ইজারাহ", IslamicArticleCategory.FIQH_MASAIL)
        assertEquals(1, searchByFiqh.size)
        assertEquals("art_pub_fiqh_ijarah_04", searchByFiqh.first().articleId)
    }

    /**
     * টেস্ট ৩ ও ৪: আলাদা Full-Screen প্রবন্ধ পড়ার পেজ, বাংলা ফন্ট ছোট-বড় করার সুবিধা, বুকমার্ক ও শেয়ার।
     */
    @Test
    fun test03_04_fullScreenArticleReadingPage_banglaFontSizeAdjustment_bookmarkAndShare() {
        composeTestRule.setContent {
            MaterialTheme {
                IslamicArticlePublicationScreen(
                    currentUserId = "reader_user_01",
                    currentUserName = "মাওলানা পাঠক",
                    currentUserBio = "শিক্ষক • ঢাকা"
                )
            }
        }

        // Main Magazine Home displays Featured, New, Popular, and Category sections
        composeTestRule.onNodeWithTag("islamic_article_magazine_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_main_tab_featured").assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_main_tab_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_main_tab_my_submissions").assertIsDisplayed()

        // Open first Featured Article into dedicated Full-Screen Reading Page
        val targetArticle = IslamicArticleVerifiedCatalog.verifiedArticles.first()
        composeTestRule.onNodeWithTag("islamic_article_magazine_home")
            .performScrollToNode(hasTestTag("featured_articles_section"))
        composeTestRule.onNodeWithTag("featured_article_card_${targetArticle.articleId}")
            .performClick()

        // Verify Reading Page is displayed
        composeTestRule.onNodeWithTag("article_reading_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_reading_title")
            .assertTextContains(targetArticle.title, substring = true)
        composeTestRule.onNodeWithTag("article_reading_author_card").assertIsDisplayed()

        // Verify Bangla font size increase & decrease
        assertEquals(18, articleManager.getReaderFontSizeSp())
        composeTestRule.onNodeWithTag("article_font_size_label").assertTextContains("18sp")

        composeTestRule.onNodeWithTag("article_font_increase_btn").performClick()
        assertEquals(20, articleManager.getReaderFontSizeSp())
        composeTestRule.onNodeWithTag("article_font_size_label").assertTextContains("20sp")

        composeTestRule.onNodeWithTag("article_font_decrease_btn").performClick()
        assertEquals(18, articleManager.getReaderFontSizeSp())

        // Verify Bookmark toggle on Reading Page
        assertFalse(articleManager.isArticleBookmarked("reader_user_01", targetArticle.articleId))
        composeTestRule.onNodeWithTag("article_reading_bookmark_btn").performClick()
        assertTrue(articleManager.isArticleBookmarked("reader_user_01", targetArticle.articleId))

        // Verify Share button is present and clickable
        composeTestRule.onNodeWithTag("article_reading_share_btn").assertIsDisplayed().performClick()

        // Verify Quran, Hadith, Scholarly Note, and Bibliography cards are rendered
        composeTestRule.onNodeWithTag("article_reading_quran_refs")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_reading_hadith_refs")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_reading_scholarly_note")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("article_reading_bibliography")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * টেস্ট ৫ ও ৬: ইউজারের প্রবন্ধ জমা দেওয়া (“আপনার প্রবন্ধ জমা দিন” Full-Screen Page),
     * ফরম্যাটিং এডিটর (Heading, Bold, Paragraph, List) এবং ইউনিক Submission ID তৈরি।
     */
    @Test
    fun test05_06_userArticleSubmissionWithRichEditor_generatesUniqueSubmissionIdAndRoutesToAuthorDashboard() {
        composeTestRule.setContent {
            MaterialTheme {
                IslamicArticlePublicationScreen(
                    currentUserId = "author_user_101",
                    currentUserName = "মাওলানা তানভীর হাসান",
                    currentUserBio = "ফারেগ: জামিয়া রাহমানিয়া ঢাকা"
                )
            }
        }

        // Open Full-Screen Submission Page
        composeTestRule.onNodeWithTag("open_submit_article_page_btn").performClick()
        composeTestRule.onNodeWithTag("article_submission_screen").assertIsDisplayed()

        // Fill in 1. Title
        composeTestRule.onNodeWithTag("article_input_title")
            .performScrollTo()
            .performTextInput("ইসলামে প্রতিবেশীর হক ও সামাজিক সম্প্রীতির গুরুত্ব")

        // Select 2. Category
        composeTestRule.onNodeWithTag("article_input_category_FAMILY_SOCIAL")
            .performScrollTo()
            .performClick()

        // Use Rich Editor formatting buttons (Heading, Bold, Paragraph, List) and input 3. Content
        composeTestRule.onNodeWithTag("editor_btn_heading").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("editor_btn_bold").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("editor_btn_list").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("article_input_content")
            .performScrollTo()
            .performTextClearance()
        composeTestRule.onNodeWithTag("article_input_content")
            .performTextInput("## প্রতিবেশীর অধিকার\nইসলামে প্রতিবেশীর হক অত্যন্ত গুরুত্বপূর্ণ। **মুসলিম ও অমুসলিম** সকল প্রতিবেশীর সাথে সদাচরণ করা ঈমানের দাবি।\n• খোঁজখবর নেওয়া\n• বিপদে সাহায্য করা")

        // Fill in 7. Quran & Hadith References and 8. Other Sources
        composeTestRule.onNodeWithTag("article_input_quran_refs")
            .performScrollTo()
            .performTextInput("সূরা নিসা (৪), আয়াত: ৩৬")
        composeTestRule.onNodeWithTag("article_input_hadith_refs")
            .performScrollTo()
            .performTextInput("সহীহ বুখারী, হাদিস নং: ৬০১৪; সহীহ মুসলিম, হাদিস নং: ২৬২৫")
        composeTestRule.onNodeWithTag("article_input_other_sources")
            .performScrollTo()
            .performTextInput("আল-আদাবুল মুফরাদ, ইমাম বুখারী (রহ.)")

        // Submit Article
        composeTestRule.onNodeWithTag("article_submit_btn")
            .performScrollTo()
            .performClick()

        // Verify Submission ID was generated and user is taken to "আমার জমা" Author Dashboard
        val authorSubs = articleManager.getSubmissionsForAuthor("author_user_101")
        assertEquals(1, authorSubs.size)
        val createdSub = authorSubs.first()
        assertTrue(createdSub.submissionId.startsWith("ART_SUB_2026_"))
        assertEquals(ArticleSubmissionStatus.SUBMITTED, createdSub.status)

        composeTestRule.onNodeWithTag("submission_success_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_id_text")
            .assertTextContains(createdSub.submissionId, substring = true)
    }

    /**
     * টেস্ট ৭, ৮, ৯, ১০ ও ১১:
     * - অনুমোদনের আগে প্রবন্ধ Public Page-এ কখনো দেখাবে না (Test 9)
     * - এক ইউজারের জমা অন্য ইউজার দেখতে বা পরিবর্তন করতে পারবে না (Test 11)
     * - অ্যাডমিন রিভিউ, সংশোধন প্রয়োজন (More Information Required), লেখকের পুনরায় জমা (Test 7)
     * - অ্যাডমিন রেফারেন্স যাচাই ছাড়া প্রকাশ করতে না পারা এবং রেফারেন্স যাচাই ও অনুমোদনের পর Public Page-এ দেখানো (Test 8 & 10)
     */
    @Test
    fun test07_08_09_10_11_publicationGate_crossAuthorPrivacy_authorRevision_andAdminPublishWorkflow() {
        val initialPublicCount = articleManager.getPublicPublishedArticles().size

        // 1. Author Alice submits an article
        val submitRes = articleManager.submitNewArticle(
            authorId = "author_alice_01",
            authorName = "মাওলানা খালিদ সাইফুল্লাহ",
            authorBio = "ফিকহ গবেষক, চট্টগ্রাম",
            title = "যাকাত আদায়ের শরয়ী নিসাব ও আধুনিক মাসায়েল",
            category = IslamicArticleCategory.FIQH_MASAIL,
            content = "## যাকাতের গুরুত্ব\nইসলামের পঞ্চস্তম্ভের অন্যতম হলো যাকাত। নিসাব পরিমাণ সম্পদের মালিক হলে বছরান্তে আড়াই শতাংশ যাকাত আদায় করা ফরয।",
            coverImageUrl = "preset_cover_fiqh_masail",
            quranReferences = "সূরা বাকারা (২), আয়াত: ৪৩ — তোমরা নামায কায়েম করো এবং যাকাত আদায় করো।",
            hadithReferences = "সহীহ বুখারী, হাদিস নং: ১৩৯৫ (হযরত মুআয রা.-কে ইয়েমেনে প্রেরণের হাদিস)",
            otherSourcesAndBibliography = "ফাতাওয়ায়ে শামী, খণ্ড ২; আল-হিদায়া, কিতাবুয যাকাত",
            isAuthenticityConfirmed = true
        )
        assertTrue(submitRes.isSuccess)
        val aliceSub = submitRes.getOrThrow()

        // TEST 9: Unapproved submission MUST NOT appear in Public Published Articles!
        val publicBeforeApproval = articleManager.getPublicPublishedArticles()
        assertEquals(initialPublicCount, publicBeforeApproval.size)
        assertFalse(publicBeforeApproval.any { it.submissionId == aliceSub.submissionId })

        // TEST 11: Another user (Bob) CANNOT see or modify Alice's private submission!
        val bobSubmissions = articleManager.getSubmissionsForAuthor("author_bob_02")
        assertTrue("Bob must not see Alice's submission", bobSubmissions.isEmpty())

        val unauthorizedEditRes = articleManager.resubmitUpdatedArticle(
            submissionId = aliceSub.submissionId,
            authorId = "author_bob_02",
            title = "বব কর্তৃক পরিবর্তনের অপচেষ্টা",
            category = IslamicArticleCategory.FIQH_MASAIL,
            content = "অন্য লেখকের প্রবন্ধ পরিবর্তন করার অবৈধ চেষ্টা।",
            authorName = "বব",
            authorBio = "লেখক",
            coverImageUrl = "",
            quranReferences = "সূরা বাকারা: ৪৩",
            hadithReferences = "সহীহ বুখারী: ১৩৯৫",
            otherSourcesAndBibliography = "আল-হিদায়া"
        )
        assertTrue("Cross-user edit must fail with SecurityException", unauthorizedEditRes.isFailure)
        assertTrue(unauthorizedEditRes.exceptionOrNull() is SecurityException)

        // Non-admin CANNOT review or publish submissions
        val nonAdminReview = articleManager.adminReviewSubmission(
            adminId = "author_bob_02",
            adminEmail = "bob@example.com",
            adminPhone = "01700000000",
            submissionId = aliceSub.submissionId,
            newStatus = ArticleSubmissionStatus.PUBLISHED,
            adminComment = "অবৈধ অনুমোদন",
            isReferencesVerified = true
        )
        assertTrue("Non-admin review must fail", nonAdminReview.isFailure)

        // TEST 7 & 8: Authorized Admin marks as MORE_INFO_REQUIRED with comment
        val moreInfoRes = articleManager.adminReviewSubmission(
            adminId = "admin_super",
            adminEmail = AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.first(),
            adminPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
            submissionId = aliceSub.submissionId,
            newStatus = ArticleSubmissionStatus.MORE_INFO_REQUIRED,
            adminComment = "অনুগ্রহ করে রুপার নিসাবের পরিমাণ গ্রাম হিসেবে স্পষ্ট উল্লেখ করুন।",
            isReferencesVerified = false
        )
        assertTrue(moreInfoRes.isSuccess)
        assertEquals(ArticleSubmissionStatus.MORE_INFO_REQUIRED, moreInfoRes.getOrThrow().status)
        assertEquals(
            "অনুগ্রহ করে রুপার নিসাবের পরিমাণ গ্রাম হিসেবে স্পষ্ট উল্লেখ করুন।",
            articleManager.getSubmissionsForAuthor("author_alice_01").first().adminComment
        )

        // Author Alice revises and resubmits her article
        val reviseRes = articleManager.resubmitUpdatedArticle(
            submissionId = aliceSub.submissionId,
            authorId = "author_alice_01",
            title = "যাকাত আদায়ের শরয়ী নিসাব (৫২.৫ তোলা রুপা) ও আধুনিক মাসায়েল",
            category = IslamicArticleCategory.FIQH_MASAIL,
            content = "## যাকাতের গুরুত্ব ও নিসাব\nসাড়ে বায়ান্ন তোলা (৬১২.৩৬ গ্রাম) রুপা অথবা সাড়ে সাত তোলা (৮৭.৪৮ গ্রাম) স্বর্ণ থাকলে যাকাত ফরয হয়।",
            authorName = aliceSub.authorName,
            authorBio = aliceSub.authorBio,
            coverImageUrl = aliceSub.coverImageUrl,
            quranReferences = aliceSub.quranReferences,
            hadithReferences = aliceSub.hadithReferences,
            otherSourcesAndBibliography = aliceSub.otherSourcesAndBibliography,
            authorReplyNote = "রুপা ও স্বর্ণের গ্রাম ও তোলার পরিমাণ সংযোজন করা হয়েছে।"
        )
        assertTrue(reviseRes.isSuccess)
        assertEquals(ArticleSubmissionStatus.SUBMITTED, reviseRes.getOrThrow().status)

        // TEST 8: Admin cannot publish without verifying references (`isReferencesVerified == false`)
        val unverifiedPublishAttempt = articleManager.adminReviewSubmission(
            adminId = "admin_super",
            adminEmail = AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.first(),
            adminPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
            submissionId = aliceSub.submissionId,
            newStatus = ArticleSubmissionStatus.PUBLISHED,
            adminComment = "প্রকাশের চেষ্টা",
            isReferencesVerified = false
        )
        assertTrue("Publishing without reference verification must fail", unverifiedPublishAttempt.isFailure)

        // TEST 10: Admin verifies references and publishes the article -> Now it MUST appear in Public Published Articles!
        val publishRes = articleManager.adminReviewSubmission(
            adminId = "admin_super",
            adminEmail = AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.first(),
            adminPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
            submissionId = aliceSub.submissionId,
            newStatus = ArticleSubmissionStatus.PUBLISHED,
            adminComment = "আলহামদুলিল্লাহ, কুরআন ও হাদিসের রেফারেন্স যাচাই শেষে প্রবন্ধটি প্রকাশিত হলো।",
            isReferencesVerified = true,
            scholarlyDifferenceNote = "কাগুজে মুদ্রার নিসাব নির্ধারণে গরিবের কল্যাণার্থে রুপার নিসাব ধর্তব্য হওয়ার বিষয়ে ফুকাহায়ে কেরাম একমত।",
            markAsFeatured = true
        )
        assertTrue(publishRes.isSuccess)
        val publishedSub = publishRes.getOrThrow()
        assertEquals(ArticleSubmissionStatus.PUBLISHED, publishedSub.status)
        assertTrue(publishedSub.publishedArticleId.isNotBlank())

        val publicAfterPublish = articleManager.getPublicPublishedArticles()
        assertEquals(initialPublicCount + 1, publicAfterPublish.size)
        val newlyPublishedPublicArticle = publicAfterPublish.firstOrNull { it.articleId == publishedSub.publishedArticleId }
        assertNotNull("Approved & Published article must appear in public catalog", newlyPublishedPublicArticle)
        assertEquals("যাকাত আদায়ের শরয়ী নিসাব (৫২.৫ তোলা রুপা) ও আধুনিক মাসায়েল", newlyPublishedPublicArticle!!.title)
        assertTrue(newlyPublishedPublicArticle.isFeatured)
    }

    /**
     * টেস্ট ১২ (অ্যাডমিন প্যানেল UI বাগ ফিক্স):
     * অ্যাডমিন প্যানেলে গেলে হেডার কার্ডে কোনো বিকৃত বা সংকুচিত Back বাটন থাকবে না
     * এবং “সকল (12)” ও “প্রকাশিত (12)” ট্যাবে সকল প্রকাশিত প্রবন্ধ সঠিকভাবে প্রদর্শিত হবে।
     */
    @Test
    fun test12_adminReviewDashboardUI_displaysCleanHeaderWithoutVerticalPillAndShowsAllPublishedArticles() {
        composeTestRule.setContent {
            MaterialTheme {
                IslamicArticlePublicationScreen(
                    currentUserId = "admin_super",
                    currentUserName = "মাওলানা এডমিন",
                    currentUserEmail = AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.first(),
                    currentUserPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
                )
            }
        }

        // Open Admin Review Dashboard
        composeTestRule.onNodeWithTag("open_article_admin_dashboard_btn")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("article_admin_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("অ্যাডমিন প্রবন্ধ যাচাই ও প্রকাশনা প্যানেল").assertIsDisplayed()

        // Ensure the redundant squished back button inside the card does NOT exist
        composeTestRule.onNodeWithTag("admin_dashboard_back_btn").assertDoesNotExist()

        // Verify "সকল (12)" and "প্রকাশিত (12)" filter chips show the 12 published articles
        composeTestRule.onNodeWithTag("admin_tab_ALL")
            .assertIsDisplayed()
            .assertTextContains("সকল (12)", substring = true)

        val firstPublishedId = IslamicArticleVerifiedCatalog.verifiedArticles.first().submissionId
        composeTestRule.onNodeWithTag("admin_submission_card_$firstPublishedId").assertIsDisplayed()
    }
}

