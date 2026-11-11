package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.islamicbooks.*
import com.example.ui.screens.IslamicAndEssentialServicesSection
import com.example.ui.viewmodel.MainDestination
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ইসলামিক বই লাইব্রেরি — Ultra Compact UI Redesign & Admin-Uploaded Book Verification Tests
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w420dp-h2400dp")
class IslamicBookLibrarySystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var bookManager: IslamicBookManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bookManager = IslamicBookManager.getInstance(context)
        bookManager.clearAllTestState()
    }

    private fun seedAdminUploadedBook(
        titleBangla: String,
        category: IslamicBookCategory = IslamicBookCategory.AQIDAH_IMAN,
        coverImageUrl: String = "https://example.com/covers/${titleBangla.hashCode()}.jpg",
        authorName: String = "মাওলানা আব্দুল্লাহ",
        publisherName: String = "মাকতাবাতুল ফুরকান"
    ): IslamicBookCatalogItem {
        val dummyForPdf = IslamicBookVerifiedCatalog.curatedBooks.first().copy(
            bookId = "seed_${titleBangla.hashCode()}",
            titleBangla = titleBangla,
            authorName = authorName,
            publisherName = publisherName,
            category = category
        )
        val pdfBytes = IslamicBookVerifiedCatalog.generateAuthenticBookPdfBytes(dummyForPdf, 1)
        bookManager.validateAndPreparePdfForUpload(
            fileName = "admin_book_${System.nanoTime()}.pdf",
            pdfBytes = pdfBytes,
            contributorId = "admin_1"
        ).getOrThrow()

        return bookManager.adminDirectPublishBook(
            adminId = "admin_1",
            adminEmail = "foysalahamad102@gmail.com",
            adminPhone = "01300448147",
            bookTitleBangla = titleBangla,
            category = category,
            coverImageUrl = coverImageUrl,
            authorName = authorName,
            publisherName = publisherName,
            pageCount = 120
        ).getOrThrow()
    }

    @Test
    fun test01_homePageIslamicBooksButton_opensDedicatedFullScreenLibraryPage_preservingExistingServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(1800.dp)) {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        composeTestRule.onNodeWithTag("home_islamic_service_books")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            "Clicking 'ইসলামিক বই' on Home Page must navigate directly to MainDestination.ISLAMIC_BOOK_LIBRARY",
            MainDestination.ISLAMIC_BOOK_LIBRARY,
            navigatedDestination
        )

        // Verify Tilawat button remains intact (No regression)
        composeTestRule.onNodeWithTag("home_islamic_service_tilawat").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainDestination.TILAWAT, navigatedDestination)
    }

    @Test
    fun test02_noDemoOrFakeBooksWhenNoAdminBookIsUploaded_andShowsOnlyAdminUploadedBooks() {
        // Initially no Admin book has been uploaded -> catalog MUST be empty (zero demo/fake books)
        assertTrue(
            "No Demo/Fake/Sample book may appear when Admin has not uploaded any books",
            bookManager.getPublicCatalogBooks().isEmpty()
        )

        val uploadedBook = seedAdminUploadedBook(
            titleBangla = "দরসে তিবরী",
            category = IslamicBookCategory.QURAN_TAFSIR,
            coverImageUrl = "https://example.com/covers/darse_tibari.jpg"
        )

        val catalog = bookManager.getPublicCatalogBooks()
        assertEquals(1, catalog.size)
        assertEquals("দরসে তিবরী", catalog.first().titleBangla)
        assertEquals("https://example.com/covers/darse_tibari.jpg", catalog.first().coverImageUrl)
        assertEquals(uploadedBook.bookId, catalog.first().bookId)
    }

    @Test
    fun test03_ultraCompactMobileGrid_displays4SmallCardsInSingleRow_withCoverTitleAndTinyBookmark() {
        val b1 = seedAdminUploadedBook("দরসে তিবরী", IslamicBookCategory.QURAN_TAFSIR)
        val b2 = seedAdminUploadedBook("সহিহ বুখারী", IslamicBookCategory.HADITH_SHARAH)
        val b3 = seedAdminUploadedBook("মুসলিম শরীফ", IslamicBookCategory.HADITH_SHARAH)
        val b4 = seedAdminUploadedBook("নাসাঈ", IslamicBookCategory.HADITH_SHARAH)

        composeTestRule.setContent {
            Box(modifier = Modifier.width(412.dp).height(900.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "reader_user_1",
                    isAdmin = false
                )
            }
        }
        composeTestRule.waitForIdle()

        val card1 = composeTestRule.onNodeWithTag("islamic_book_card_${b1.bookId}").assertIsDisplayed()
        val card2 = composeTestRule.onNodeWithTag("islamic_book_card_${b2.bookId}").assertIsDisplayed()
        val card3 = composeTestRule.onNodeWithTag("islamic_book_card_${b3.bookId}").assertIsDisplayed()
        val card4 = composeTestRule.onNodeWithTag("islamic_book_card_${b4.bookId}").assertIsDisplayed()

        // Verify all 4 cards are aligned on the exact same horizontal row on mobile screen (412dp width)
        val rect1 = card1.fetchSemanticsNode().boundsInRoot
        val rect2 = card2.fetchSemanticsNode().boundsInRoot
        val rect3 = card3.fetchSemanticsNode().boundsInRoot
        val rect4 = card4.fetchSemanticsNode().boundsInRoot

        assertEquals("Card 1 and Card 2 must be in the same row", rect1.top, rect2.top, 2f)
        assertEquals("Card 2 and Card 3 must be in the same row", rect2.top, rect3.top, 2f)
        assertEquals("Card 3 and Card 4 must be in the same row", rect3.top, rect4.top, 2f)

        // Verify uniform height across all 4 cards
        assertEquals(rect1.height, rect2.height, 2f)
        assertEquals(rect2.height, rect3.height, 2f)
        assertEquals(rect3.height, rect4.height, 2f)

        // Verify cover thumbnail, book title, and tiny bookmark button are displayed on each card
        composeTestRule.onNodeWithTag("islamic_book_cover_${b1.bookId}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("islamic_book_title_${b1.bookId}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("islamic_book_bookmark_btn_${b1.bookId}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "Clicking bookmark icon on compact card must bookmark the book",
            bookManager.getBookmarks(b1.bookId, 1).isNotEmpty()
        )
    }

    @Test
    fun test04_compactCardClick_opensBookDetailsPage_andPreservesPdfDownloadAndOfflineReading() = runBlocking {
        val sampleBook = seedAdminUploadedBook(
            titleBangla = "রিয়াদুস সালেহীন",
            category = IslamicBookCategory.HADITH_SHARAH
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.width(412.dp).height(1800.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "reader_user_1",
                    isAdmin = false
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap the compact book card directly to open Book Details
        composeTestRule.onNodeWithTag("islamic_book_card_${sampleBook.bookId}")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("islamic_book_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_details_read_btn").assertIsDisplayed()

        // Verify offline PDF download & local delete work intact
        val downloadedFile = bookManager.downloadBookSynchronously(sampleBook, 1)
        assertTrue(downloadedFile.exists())
        assertTrue(bookManager.isValidPdfFile(downloadedFile))
        assertTrue(bookManager.getDownloadedBooks().any { it.bookId == sampleBook.bookId })

        bookManager.deleteLocalDownloadedBook(sampleBook.bookId, 1)
        assertFalse(bookManager.isBookDownloaded(sampleBook.bookId, 1))
        assertTrue(bookManager.getPublicCatalogBooks().any { it.bookId == sampleBook.bookId })
    }

    @Test
    fun test05_selfPublishingAndAdminApprovalWorkflow_remainsIntact() {
        val dummyBook = IslamicBookVerifiedCatalog.curatedBooks.first()
        val samplePdfBytes = IslamicBookVerifiedCatalog.generateAuthenticBookPdfBytes(dummyBook)
        val uploadInfo = bookManager.validateAndPreparePdfForUpload(
            fileName = "adarsha_ustaz_guideline.pdf",
            pdfBytes = samplePdfBytes,
            contributorId = "author_alice"
        ).getOrThrow()

        val confirmedSubmission = IslamicBookSubmission(
            contributorId = "author_alice",
            contributorName = "মাওলানা আব্দুল্লাহ",
            contributorRole = ContributorRoleType.AUTHOR,
            contributorPhone = "01711223344",
            bookTitleBangla = "আদর্শ উস্তায ও তালিবুল ইলমের পাথেয়",
            authorName = "মাওলানা আব্দুল্লাহ",
            publisherName = "মাকতাবাতুল ফুরকান",
            category = IslamicBookCategory.TAZKIYAH_AKHLAQ,
            shortIntroduction = "কওমি মাদরাসার উস্তায ও তালিবে ইলমদের আদব ও আখলাক বিষয়ক মৌলিক গ্রন্থ।",
            coverImageUrl = "https://example.com/covers/adarsha_ustaz.jpg",
            pdfStorageUrl = uploadInfo.storageDownloadUrl,
            pdfFileName = uploadInfo.fileName,
            pdfFileSizeBytes = uploadInfo.fileSizeBytes,
            pdfSha256 = uploadInfo.sha256Hash,
            copyrightOwnerName = "মাওলানা আব্দুল্লাহ",
            copyrightPermissionType = CopyrightPermissionType.SELF_COPYRIGHT,
            isTruthConfirmed = true
        )
        val savedSubmission = bookManager.submitNewBookApplication(confirmedSubmission).getOrThrow()

        // Before Admin approval, book must NOT appear in Public Library
        assertFalse(bookManager.getPublicCatalogBooks().any { it.titleBangla == savedSubmission.bookTitleBangla })

        // Admin approves & publishes -> Book appears with exact Admin/Contributor cover & title
        val publishedSub = bookManager.adminReviewSubmission(
            adminId = "admin_1",
            adminEmail = "foysalahamad102@gmail.com",
            adminPhone = "01300448147",
            submissionId = savedSubmission.submissionId,
            newStatus = BookSubmissionStatus.PUBLISHED,
            adminCommentBangla = "অনুমোদিত।"
        ).getOrThrow()

        val publishedCatalogBook = bookManager.getPublicCatalogBooks().firstOrNull {
            it.bookId == publishedSub.publishedBookId
        }
        assertNotNull(publishedCatalogBook)
        assertEquals("আদর্শ উস্তায ও তালিবুল ইলমের পাথেয়", publishedCatalogBook!!.titleBangla)
        assertEquals("https://example.com/covers/adarsha_ustaz.jpg", publishedCatalogBook.coverImageUrl)
    }
}
