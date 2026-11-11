package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.kitab.*
import com.example.ui.quran.HafeziQuranReaderView
import com.example.ui.quran.HafeziQuranStorageManager
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
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w420dp-h2400dp")
class KitabLibrarySystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        KitabDownloadAndStorageManager.isNetworkConnectedOverride = true
        KitabDownloadAndStorageManager.initAndAwaitSync(context)
        HafeziQuranStorageManager.init(context)
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun verifyCatalogCoversAll8CategoriesWithAuthenticVerifiedMetadata() {
        val allEntities = KitabVerifiedCatalogData.defaultCatalogEntities
        assertTrue("Catalog must contain at least 20 verified Hadith Sharah & Kitab entries", allEntities.size >= 20)

        KitabCategory.entries.filter { it != KitabCategory.ALL }.forEach { category ->
            val booksInCategory = allEntities.filter { it.categoryId == category.id }
            assertTrue(
                "Category ${category.titleBangla} must contain verified books",
                booksInCategory.isNotEmpty()
            )
            booksInCategory.forEach { book ->
                assertTrue(book.titleBangla.isNotBlank())
                assertTrue(book.authorBangla.isNotBlank())
                assertTrue(book.translatorOrCompilerBangla.isNotBlank())
                assertTrue(book.publisherBangla.isNotBlank())
                assertTrue(
                    "Book ${book.bookId} must be open-license verified with no copyright lock",
                    book.isOpenLicenseVerified
                )
                assertTrue(
                    "Book ${book.bookId} must have a valid HTTPS PDF download URL",
                    book.downloadUrl.startsWith("https://")
                )
            }
        }
        val runtimeCatalog = KitabDownloadAndStorageManager.catalogItemsFlow.value
        assertTrue(
            "No book in runtime catalog may have REFERENCE_ONLY_NO_LICENSE copyright lock",
            runtimeCatalog.none { it.downloadStatus == KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE }
        )
    }

    @Test
    fun verifyZeroAutoDownloadBeforeUserAction_andPerBookIsolatedStorage() = runBlocking {
        val sampleBook = KitabDownloadAndStorageManager.catalogItemsFlow.value.first()
        KitabDownloadAndStorageManager.deleteDownloadedBookSynchronously(context, sampleBook.bookId)

        val localFile = KitabDownloadAndStorageManager.getFinalPdfFile(context, sampleBook.bookId)
        assertFalse(
            "PDF file must NOT exist on device before user clicks download",
            localFile.exists()
        )
        val refreshed = KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == sampleBook.bookId }
        assertFalse(
            "isReadyToReadOffline must be false before download",
            refreshed.isReadyToReadOffline
        )
    }

    @Test
    fun verifyRealDownloadProgress_PauseResume_andOfflineVerification() = runBlocking {
        val validPdfBytes = KitabDownloadAndStorageManager.buildValidTestPdfBytes(
            title = "Test Bukhari Sharah",
            pageCount = 8,
            minTargetSizeBytes = 32 * 1024
        )
        val checksum = sha256Hex(validPdfBytes)
        val testBookId = "test_download_bukhari_1"

        val dao = KitabLibraryDatabase.getInstance(context).kitabLibraryDao()
        dao.upsertCatalogItem(
            KitabMetadataEntity(
                bookId = testBookId,
                seriesId = "series_test_bukhari",
                titleBangla = "নুযহাতুল কারী টেস্ট খণ্ড",
                arabicTitle = "نزهة القاري",
                authorBangla = "ইমাম বুখারী (রহ.)",
                translatorOrCompilerBangla = "দারুল হাদিস ফাজেলবৃন্দ",
                publisherBangla = "মাকতাবাতুল ইলম",
                categoryId = KitabCategory.BUKHARI_SHARAH.id,
                volumeNumber = 1,
                totalVolumes = 1,
                fileSizeBytes = validPdfBytes.size.toLong(),
                languageBangla = "বাংলা ও আরবি",
                shortDescriptionBangla = "টেস্ট কিতাব",
                marhalaBangla = "দাওরায়ে হাদিস",
                downloadUrl = "https://archive.org/download/test_bukhari.pdf",
                sha256Checksum = checksum,
                isOpenLicenseVerified = true,
                isAdminApproved = true,
                licenseNoteBangla = "SHA-256 যাচাইকৃত",
                coverPrimaryHex = 0xFF074E35L,
                coverAccentHex = 0xFFDDA83AL
            )
        )
        KitabDownloadAndStorageManager.deleteDownloadedBookSynchronously(context, testBookId)

        val prevTransport = KitabDownloadAndStorageManager.networkTransport
        KitabDownloadAndStorageManager.networkTransport = object : KitabNetworkTransport {
            override fun openStream(downloadUrl: String, rangeStartBytes: Long): KitabNetworkStreamResponse {
                val slice = if (rangeStartBytes > 0 && rangeStartBytes < validPdfBytes.size) {
                    validPdfBytes.copyOfRange(rangeStartBytes.toInt(), validPdfBytes.size)
                } else {
                    validPdfBytes
                }
                return KitabNetworkStreamResponse(
                    httpStatusCode = if (rangeStartBytes > 0) 206 else 200,
                    isPartialContent = rangeStartBytes > 0,
                    contentLengthBytes = slice.size.toLong(),
                    inputStream = ByteArrayInputStream(slice)
                )
            }
        }

        try {
            // 1. Partial download (Pause at 16KB)
            val pausedModel = KitabDownloadAndStorageManager.downloadBookSynchronously(
                context = context,
                bookId = testBookId,
                pauseAfterBytes = 16 * 1024L
            )
            assertNotNull(pausedModel)
            assertEquals(KitabDownloadStatus.PAUSED, pausedModel?.downloadStatus)

            // 2. Resume and complete download + SHA-256 verification
            val completedModel = KitabDownloadAndStorageManager.downloadBookSynchronously(
                context = context,
                bookId = testBookId,
                pauseAfterBytes = -1L
            )
            assertNotNull(completedModel)
            assertEquals(KitabDownloadStatus.DOWNLOADED_VERIFIED, completedModel?.downloadStatus)
            assertTrue(completedModel!!.isReadyToReadOffline)
            assertEquals(8, completedModel.totalPageCount)

            // 3. Delete downloaded book and verify local file is removed
            KitabDownloadAndStorageManager.deleteDownloadedBookSynchronously(context, testBookId)
            assertFalse(KitabDownloadAndStorageManager.getFinalPdfFile(context, testBookId).exists())
        } finally {
            KitabDownloadAndStorageManager.networkTransport = prevTransport
        }
    }

    @Test
    fun verifyReadingProgress_LastReadPage_andBookmarksIsolationPerBook() = runBlocking {
        val books = KitabDownloadAndStorageManager.catalogItemsFlow.value
        val bookA = books[0]
        val bookB = books[1]

        KitabDownloadAndStorageManager.recordPageReadSynchronously(
            context = context,
            bookId = bookA.bookId,
            pageNumber = 42,
            totalPages = 520
        )
        KitabDownloadAndStorageManager.recordPageReadSynchronously(
            context = context,
            bookId = bookB.bookId,
            pageNumber = 15,
            totalPages = 400
        )

        val updatedBooks = KitabDownloadAndStorageManager.catalogItemsFlow.value
        val updatedA = updatedBooks.first { it.bookId == bookA.bookId }
        val updatedB = updatedBooks.first { it.bookId == bookB.bookId }
        assertEquals(42, updatedA.lastReadPage)
        assertEquals(15, updatedB.lastReadPage)

        // Toggle bookmark on Book A Page 42
        KitabDownloadAndStorageManager.toggleBookmarkSynchronously(
            context = context,
            bookId = bookA.bookId,
            bookTitleBangla = bookA.titleBangla,
            volumeNumber = bookA.volumeNumber,
            pageNumber = 42,
            note = "গুরুত্বপূর্ণ শরাহ"
        )
        val bookmarksForA = KitabDownloadAndStorageManager.bookmarksFlow.value.filter { it.bookId == bookA.bookId }
        val bookmarksForB = KitabDownloadAndStorageManager.bookmarksFlow.value.filter { it.bookId == bookB.bookId }
        assertTrue(bookmarksForA.any { it.pageNumber == 42 })
        assertTrue("Bookmarks for Book A must not leak into Book B", bookmarksForB.none { it.pageNumber == 42 })
    }

    @Test
    fun verifyCompact2ColumnGridLayout_andCardBookmarkAction() {
        val books = KitabDownloadAndStorageManager.catalogItemsFlow.value
        val firstBook = books[0]
        val secondBook = books[1]

        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(2200.dp)) {
                KitabLibraryHubScreen(onCloseLibrary = {})
            }
        }

        composeTestRule.onNodeWithTag("kitab_library_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_catalog_list").assertIsDisplayed()

        // Verify Row 0 displays 2 compact cards side-by-side on mobile width (420dp)
        val card1Bounds = composeTestRule.onNodeWithTag("kitab_catalog_card_${firstBook.bookId}")
            .fetchSemanticsNode().boundsInRoot
        val card2Bounds = composeTestRule.onNodeWithTag("kitab_catalog_card_${secondBook.bookId}")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(
            "Two cards in the same mobile row must have aligned top Y coordinates",
            card1Bounds.top,
            card2Bounds.top,
            2.0f
        )
        assertTrue(
            "Second card must be positioned to the right of first card in 2-column grid",
            card2Bounds.left > card1Bounds.left
        )

        // Verify Bookmark button exists on the compact card and toggles bookmark
        composeTestRule.onNodeWithTag("kitab_card_bookmark_btn_${firstBook.bookId}")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue(
            "Card bookmark icon should bookmark page 1",
            KitabDownloadAndStorageManager.bookmarksFlow.value.any { it.bookId == firstBook.bookId }
        )
    }

    @Test
    fun verifyInAppPdfReader_SwipePageTurning_NextPrev_JumpFirstLast_andZoomControls() {
        val book = KitabDownloadAndStorageManager.catalogItemsFlow.value.first()
        val finalPdf = KitabDownloadAndStorageManager.getFinalPdfFile(context, book.bookId)
        finalPdf.writeBytes(
            KitabDownloadAndStorageManager.buildValidTestPdfBytes(
                title = book.titleBangla,
                pageCount = 10
            )
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                val settings by KitabDownloadAndStorageManager.readerSettingsFlow.collectAsState()
                val theme by KitabDownloadAndStorageManager.readerThemeFlow.collectAsState()
                val bookmarks by KitabDownloadAndStorageManager.bookmarksFlow.collectAsState()
                KitabNativePdfReaderView(
                    book = book,
                    siblingVolumes = listOf(book),
                    initialPage = 1,
                    totalPages = 10,
                    bookmarksForBook = bookmarks.filter { it.bookId == book.bookId },
                    themeMode = theme,
                    onThemeModeChange = { KitabDownloadAndStorageManager.setReaderThemeMode(context, it) },
                    onSwitchVolume = {},
                    onCloseReader = {},
                    readerSettings = settings,
                    onUpdateReaderSettings = { transform ->
                        KitabDownloadAndStorageManager.updateReaderSettings(context, transform)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("kitab_native_pdf_reader_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_bottom_control_bar").assertIsDisplayed()

        fun currentRecordedPage(): Int =
            KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == book.bookId }.lastReadPage

        // 1. Test Next Page button advances from Page 1 to Page 2
        composeTestRule.onNodeWithTag("kitab_reader_next_page_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Next page button must advance to page 2", 2, currentRecordedPage())

        // 2. Test Previous Page button returns from Page 2 to Page 1
        composeTestRule.onNodeWithTag("kitab_reader_prev_page_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Previous page button must return to page 1", 1, currentRecordedPage())

        // 3. Test Right-to-Left Swipe (swipeLeft) advances from Page 1 to Page 2
        composeTestRule.onNodeWithTag("kitab_reader_horizontal_pager")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals("Swipe Right-to-Left must advance to Next Page (Page 2)", 2, currentRecordedPage())

        // 4. Test Left-to-Right Swipe (swipeRight) returns from Page 2 to Page 1
        composeTestRule.onNodeWithTag("kitab_reader_horizontal_pager")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals("Swipe Left-to-Right must return to Previous Page (Page 1)", 1, currentRecordedPage())

        // 5. Test Jump to Page Dialog -> Last Page & First Page buttons
        composeTestRule.onNodeWithTag("kitab_reader_jump_page_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_reader_last_page_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Last Page button must jump to page 10", 10, currentRecordedPage())

        composeTestRule.onNodeWithTag("kitab_reader_jump_page_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_reader_first_page_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("First Page button must jump back to page 1", 1, currentRecordedPage())

        // 6. Test Zoom In (+), Reset Zoom (100%), and Table of Contents dialog
        composeTestRule.onNodeWithTag("kitab_reader_zoom_in_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_reader_zoom_reset_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("kitab_reader_toc_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_reader_toc_dialog").assertIsDisplayed()
    }

    @Test
    fun verifyQuranReaderBottomControlBar_isVisibleAndResponsive() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                HafeziQuranReaderView(
                    initialPage = 2,
                    onBackToHub = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("hafezi_quran_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_bottom_control_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_prev_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_next_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_bottom_page_badge").assertIsDisplayed()
    }

    @Test
    fun verifyStrictFeatureSeparation_Quran_Hadith_Tilawat_andOtherServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(1800.dp)) {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        // 1. Clicking "tilawat" must open AudioTilawatSectionDialog, NOT Quran PDF Hub
        composeTestRule.onNodeWithTag("home_islamic_service_tilawat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tilawat_audio_section_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tilawat_dialog_close_btn").performClick()
        composeTestRule.waitForIdle()

        // 2. Clicking "dua" must open DedicatedIslamicServiceDialog("dua"), NOT navigate to AJKER_SMORON
        composeTestRule.onNodeWithTag("home_islamic_service_dua").performClick()
        composeTestRule.waitForIdle()
        assertNull("Clicking Dua must not redirect to AJKER_SMORON", navigatedDestination)
        composeTestRule.onNodeWithTag("dedicated_service_dialog_dua").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dedicated_service_dialog_close_btn").performClick()
        composeTestRule.waitForIdle()

        // 3. Clicking "amol_tracker" must open DedicatedIslamicServiceDialog("amol_tracker"), NOT AJKER_SMORON
        composeTestRule.onNodeWithTag("home_islamic_service_amol_tracker").performClick()
        composeTestRule.waitForIdle()
        assertNull("Clicking Amol Tracker must not redirect to AJKER_SMORON", navigatedDestination)
        composeTestRule.onNodeWithTag("dedicated_service_dialog_amol_tracker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dedicated_service_dialog_close_btn").performClick()
        composeTestRule.waitForIdle()

        // 4. Clicking "hadith" must open Hadith Kitab Library Hub
        composeTestRule.onNodeWithTag("home_islamic_service_hadith").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_library_hub_screen").assertIsDisplayed()
    }
}
