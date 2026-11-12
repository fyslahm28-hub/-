package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
import java.io.File

/**
 * ইসলামিক বই লাইব্রেরি — Ultra Compact UI Redesign & 3 Specific PDF Problems Verification Tests
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
            pageCount = 12
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

        val rect1 = card1.fetchSemanticsNode().boundsInRoot
        val rect2 = card2.fetchSemanticsNode().boundsInRoot
        val rect3 = card3.fetchSemanticsNode().boundsInRoot
        val rect4 = card4.fetchSemanticsNode().boundsInRoot

        assertEquals("Card 1 and Card 2 must be in the same row", rect1.top, rect2.top, 2f)
        assertEquals("Card 2 and Card 3 must be in the same row", rect2.top, rect3.top, 2f)
        assertEquals("Card 3 and Card 4 must be in the same row", rect3.top, rect4.top, 2f)

        assertEquals(rect1.height, rect2.height, 2f)
        assertEquals(rect2.height, rect3.height, 2f)
        assertEquals(rect3.height, rect4.height, 2f)

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
    fun test04_problem1_pdfSubmissionWithImmediateInFormPdfPreview_and3x4Portrait600x800Thumbnail() {
        // 1. Verify exact 600 × 800 (3:4 Portrait) Thumbnail formatting without distortion or cropping
        val wideSourceBitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(20, 120, 60))
        }
        val formattedThumb = bookManager.formatBitmapTo3x4Portrait600x800(wideSourceBitmap)
        assertEquals("Thumbnail width must be strictly 600px", 600, formattedThumb.width)
        assertEquals("Thumbnail height must be strictly 800px", 800, formattedThumb.height)

        val savedThumbUrl = bookManager.prepareCoverImageFromBitmap(wideSourceBitmap)
        assertTrue(
            "Saved 600x800 thumbnail must be base64 data URI or file URI",
            savedThumbUrl.startsWith("data:image/jpeg;base64,") || savedThumbUrl.startsWith("file://")
        )

        // 2. Select PDF for upload and verify stagedLocalFilePath and detectedPageCount are populated immediately
        val dummyBook = IslamicBookVerifiedCatalog.curatedBooks.first()
        val multiPagePdfBytes = IslamicBookVerifiedCatalog.generateAuthenticBookPdfBytes(dummyBook, 1)
        val uploadProgress = bookManager.validateAndPreparePdfForUpload(
            fileName = "selected_user_book.pdf",
            pdfBytes = multiPagePdfBytes,
            contributorId = "author_user_1"
        ).getOrThrow()

        assertTrue("Staged local PDF path must exist for immediate in-form preview", File(uploadProgress.stagedLocalFilePath).exists())
        assertTrue("Detected page count must be >= 2", uploadProgress.detectedPageCount >= 2)

        // 3. Render the Author/Publisher Self-Publishing Portal and verify in-form PDF Preview & 3:4 Thumbnail Preview
        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(2400.dp)) {
                AuthorPublisherSelfPublishingPortalScreen(
                    bookManager = bookManager,
                    currentUserId = "author_user_1",
                    currentUserName = "মাওলানা মাহমুদ",
                    currentUserPhone = "01711000000",
                    currentUserEmail = "mahmud@example.com",
                    onBackToLibrary = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Switch to the New Book Submission tab
        composeTestRule.onNodeWithTag("new_book_submission_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Because validateAndPreparePdfForUpload updated uploadProgress, the in-form PDF Preview box must be visible immediately!
        composeTestRule.onNodeWithTag("submission_pdf_preview_box").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_pdf_preview_page_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_pdf_preview_next_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("submission_pdf_preview_prev_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Verify the 3:4 Portrait Thumbnail Preview Box (600x800) & Change / Full Preview / Remove controls
        composeTestRule.onNodeWithTag("submission_thumbnail_preview_box").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_thumbnail_change_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_thumbnail_full_preview_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("submission_thumbnail_remove_btn").assertIsDisplayed()
    }

    @Test
    fun test05_problem2_firstTimeDownloadBeforeReading_offlineReuseWithoutReDownload_andPreservesOldDownloads() = runBlocking {
        val book1 = seedAdminUploadedBook(
            titleBangla = "রিয়াদুস সালেহীন",
            category = IslamicBookCategory.HADITH_SHARAH
        )
        val book2 = seedAdminUploadedBook(
            titleBangla = "আল-ফিকহুল আকবার",
            category = IslamicBookCategory.AQIDAH_IMAN
        )

        // Initially neither book is downloaded locally in offline storage
        assertFalse("Book 1 must not be downloaded before user initiates download", bookManager.isBookDownloaded(book1.bookId, 1))
        assertFalse("Book 2 must not be downloaded before user initiates download", bookManager.isBookDownloaded(book2.bookId, 1))

        // First-time download of Book 1
        val downloadedFile1 = bookManager.downloadBookSynchronously(book1, 1)
        val firstModifiedTime = downloadedFile1.lastModified()
        assertTrue(downloadedFile1.exists())
        assertTrue(bookManager.isValidPdfFile(downloadedFile1))
        assertTrue(bookManager.isBookDownloaded(book1.bookId, 1))

        // Verify strict association between bookId and its downloaded PDF file
        assertEquals(
            downloadedFile1.absolutePath,
            bookManager.getAssociatedPdfFilePathForBook(book1.bookId, 1)
        )

        // Subsequent read / download request for Book 1 must reuse the existing offline file without re-downloading
        Thread.sleep(20)
        val reusedFile1 = bookManager.downloadBookSynchronously(book1, 1)
        assertEquals("Same offline PDF file must be reused without re-downloading", downloadedFile1.absolutePath, reusedFile1.absolutePath)
        assertEquals("File timestamp must not change when already downloaded", firstModifiedTime, reusedFile1.lastModified())

        // Downloading Book 2 must keep Book 1's downloaded PDF completely intact (never delete old downloaded PDFs)
        val downloadedFile2 = bookManager.downloadBookSynchronously(book2, 1)
        assertTrue(downloadedFile2.exists())
        assertTrue("Old downloaded PDF (Book 1) must remain intact after downloading Book 2", downloadedFile1.exists())
        assertTrue(bookManager.isBookDownloaded(book1.bookId, 1))
        assertTrue(bookManager.isBookDownloaded(book2.bookId, 1))
        assertNotEquals(
            "Each book must be bound to its own distinct PDF file",
            bookManager.getAssociatedPdfFilePathForBook(book1.bookId, 1),
            bookManager.getAssociatedPdfFilePathForBook(book2.bookId, 1)
        )
    }

    @Test
    fun test06_problem3_pdfReaderSwipeNavigation_fullScreenMode_andZeroSwipeZoomConflict() = runBlocking {
        val sampleBook = seedAdminUploadedBook(
            titleBangla = "সহিহ বুখারী",
            category = IslamicBookCategory.HADITH_SHARAH
        )
        // Ensure book is downloaded so PDF Reader opens directly
        val pdfFile = bookManager.downloadBookSynchronously(sampleBook, 1)
        assertTrue(pdfFile.exists())

        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                IslamicBookNativePdfReaderScreen(
                    book = sampleBook,
                    volumeNumber = 1,
                    bookManager = bookManager,
                    onBackToDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Wait until PDF page is rendered
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("pdf_reader_rendered_page").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("pdf_reader_rendered_page").assertIsDisplayed()
        assertEquals(1, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 1. Right-to-Left Swipe (swipeLeft) -> Next Page (Page 2)
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            swipeLeft(startX = right * 0.85f, endX = left + width * 0.15f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        assertEquals("Right-to-Left swipe must advance to Page 2", 2, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 2. Left-to-Right Swipe (swipeRight) -> Previous Page (Page 1)
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            swipeRight(startX = left + width * 0.15f, endX = right * 0.85f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        assertEquals("Left-to-Right swipe must return to Page 1", 1, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 2b. Bottom-to-Top Vertical Scroll/Swipe (swipeUp) -> Next Page (Page 2) naturally without mode toggle
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + height * 0.15f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        assertEquals("Vertical Upward scroll/swipe must advance to Page 2", 2, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 2c. Top-to-Bottom Vertical Scroll/Swipe (swipeDown) -> Previous Page (Page 1) naturally without mode toggle
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            swipeDown(startY = top + height * 0.15f, endY = bottom * 0.85f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        assertEquals("Vertical Downward scroll/swipe must return to Page 1", 1, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 3. Previous and Next Page buttons also remain active and work
        composeTestRule.onNodeWithTag("pdf_reader_next_page_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Next button must advance to Page 2", 2, bookManager.getLastReadPage(sampleBook.bookId, 1))

        composeTestRule.onNodeWithTag("pdf_reader_prev_page_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Previous button must return to Page 1", 1, bookManager.getLastReadPage(sampleBook.bookId, 1))

        // 4. Double-Tap Zoom -> zooms in to 225%, and while zoomed in, swiping pans the page WITHOUT flipping to Page 2!
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            doubleClick()
        }
        composeTestRule.waitForIdle()

        // Swipe left while zoomed in -> must pan, NOT flip to page 2
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            swipeLeft(startX = right * 0.85f, endX = left + width * 0.15f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        assertEquals(
            "Swiping while zoomed in must pan the page and NEVER conflict with page navigation",
            1,
            bookManager.getLastReadPage(sampleBook.bookId, 1)
        )

        // Double-tap again to reset zoom to 100%
        composeTestRule.onNodeWithTag("pdf_reader_page_viewport").performTouchInput {
            doubleClick()
        }
        composeTestRule.waitForIdle()

        // 5. Fit Mode (Fit to Width <-> Fit to Page), Reading Mode (Horizontal <-> Vertical), and Full Screen Mode toggles
        composeTestRule.onNodeWithTag("pdf_reader_fit_mode_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pdf_reader_reading_mode_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Enter Full Screen Mode and verify exit button appears
        composeTestRule.onNodeWithTag("pdf_reader_fullscreen_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pdf_reader_exit_fullscreen_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test07_urgentFix1_bookDetailsPdfPreviewAndLastReadPage_noVerticalTextBreak_preservesAspectRatio() = runBlocking {
        // 1. Verify symmetric whitespace trimming preserves exact PDF page aspect ratio even when ink is only on one side
        val asymmetricPage = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            // Put dark text pixels only in a narrow strip on the right side
            for (y in 100..200) {
                for (x in 450..500) {
                    setPixel(x, y, Color.BLACK)
                }
            }
        }
        val trimmedPage = trimPdfOuterWhitespaceSymmetrically(asymmetricPage, Color.WHITE)
        val originalRatio = asymmetricPage.width.toFloat() / asymmetricPage.height.toFloat()
        val trimmedRatio = trimmedPage.width.toFloat() / trimmedPage.height.toFloat()
        assertEquals(
            "Symmetric PDF trimming must preserve the exact original page aspect ratio without shifting or breaking text",
            originalRatio,
            trimmedRatio,
            0.01f
        )

        // 2. Verify Book Details page displays last read page badge horizontally (never broken vertically) and renders full PDF preview
        val sampleBook = seedAdminUploadedBook(
            titleBangla = "ফাজায়েলে আমল",
            category = IslamicBookCategory.TAZKIYAH_AKHLAQ
        )
        bookManager.downloadBookSynchronously(sampleBook, 1)

        composeTestRule.setContent {
            Box(modifier = Modifier.width(412.dp).height(1800.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "reader_user_1",
                    isAdmin = false
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("islamic_book_card_${sampleBook.bookId}").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        val badgeNode = composeTestRule.onNodeWithTag("book_details_last_read_page_badge").assertIsDisplayed()
        val badgeBounds = badgeNode.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Last read page badge must be rendered horizontally on a single line (width > height * 2), never broken vertically",
            badgeBounds.width > badgeBounds.height * 2f
        )

        // Wait until Book Details PDF Page Preview renders
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("book_details_pdf_preview_image").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("book_details_pdf_preview_card").assertIsDisplayed()
        val previewImageBounds = composeTestRule.onNodeWithTag("book_details_pdf_preview_image")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val previewAspect = previewImageBounds.width / previewImageBounds.height
        assertEquals(
            "Book details PDF preview must preserve natural portrait page aspect ratio (~0.707)",
            595f / 842f,
            previewAspect,
            0.05f
        )

        val pageIndicatorBounds = composeTestRule.onNodeWithTag("book_details_pdf_preview_page_indicator")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "PDF preview page number indicator must be horizontal (width > height), never vertical",
            pageIndicatorBounds.width > pageIndicatorBounds.height
        )
    }

    @Test
    fun test08_urgentFix2_noDoubleBackButtonOnBookDetailsOrLibrary_andSingleTopBarBackNavigatesCorrectly() {
        val sampleBook = seedAdminUploadedBook(
            titleBangla = "কিতাবুত তাওহীদ",
            category = IslamicBookCategory.AQIDAH_IMAN
        )
        var currentSubView = LibrarySubView.CATALOG_HOME
        var registeredBackHandler: (() -> Boolean)? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.width(412.dp).height(1000.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "reader_user_1",
                    isAdmin = false,
                    onSubViewChanged = { currentSubView = it },
                    onRegisterBackInterceptor = { registeredBackHandler = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // On CATALOG_HOME, redundant secondary back button must NOT exist
        composeTestRule.onNodeWithTag("islamic_book_library_back_btn").assertDoesNotExist()
        assertEquals(LibrarySubView.CATALOG_HOME, currentSubView)

        // Open Book Details
        composeTestRule.onNodeWithTag("islamic_book_card_${sampleBook.bookId}").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Verify we are on Book Details screen and the redundant second Back Button ('book_details_back_btn') does NOT exist
        composeTestRule.onNodeWithTag("islamic_book_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_details_back_btn").assertDoesNotExist()
        assertEquals(LibrarySubView.BOOK_DETAILS, currentSubView)

        // Verify the single Top App Bar back interceptor navigates cleanly back from BOOK_DETAILS to CATALOG_HOME
        assertNotNull("Back interceptor must be registered for the main Top App Bar Back Button", registeredBackHandler)
        val consumed = registeredBackHandler!!.invoke()
        composeTestRule.waitForIdle()

        assertTrue("Back interceptor must consume back click on BOOK_DETAILS", consumed)
        assertEquals("Back interceptor must return to CATALOG_HOME", LibrarySubView.CATALOG_HOME, currentSubView)
        composeTestRule.onNodeWithTag("islamic_book_library_root").assertIsDisplayed()
    }
}

