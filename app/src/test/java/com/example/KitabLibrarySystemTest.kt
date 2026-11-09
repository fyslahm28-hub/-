package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.kitab.KitabCategory
import com.example.ui.kitab.KitabDownloadAndStorageManager
import com.example.ui.kitab.KitabDownloadStatus
import com.example.ui.kitab.KitabFormatUtils
import com.example.ui.kitab.KitabLibraryDatabase
import com.example.ui.kitab.KitabLibraryHubScreen
import com.example.ui.kitab.KitabMetadataEntity
import com.example.ui.kitab.KitabNetworkStreamResponse
import com.example.ui.kitab.KitabNetworkTransport
import com.example.ui.kitab.KitabVerifiedCatalogData
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w420dp-h1800dp")
class KitabLibrarySystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var testDb: KitabLibraryDatabase
    private lateinit var deviceAStorageDir: File
    private lateinit var deviceBStorageDir: File

    private val syntheticPdfMap = mutableMapOf<String, ByteArray>()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        deviceAStorageDir = File(context.cacheDir, "test_kitab_device_A_${System.nanoTime()}").apply { mkdirs() }
        deviceBStorageDir = File(context.cacheDir, "test_kitab_device_B_${System.nanoTime()}").apply { mkdirs() }

        testDb = Room.inMemoryDatabaseBuilder(context, KitabLibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        KitabLibraryDatabase.overrideInstanceForTesting(testDb)

        KitabDownloadAndStorageManager.isNetworkConnectedOverride = true
        KitabDownloadAndStorageManager.setCustomStorageDirectoryForTesting(deviceAStorageDir)
        KitabDownloadAndStorageManager.initAndAwaitSync(context, deviceAStorageDir)
    }

    @After
    fun tearDown() {
        KitabDownloadAndStorageManager.isNetworkConnectedOverride = null
        KitabDownloadAndStorageManager.setCustomStorageDirectoryForTesting(null)
        runCatching { testDb.close() }
        runCatching { deviceAStorageDir.deleteRecursively() }
        runCatching { deviceBStorageDir.deleteRecursively() }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private suspend fun installSyntheticVerifiedBook(
        bookId: String,
        titleBangla: String,
        pageCount: Int = 8,
        sizeBytesTarget: Int = 32 * 1024,
        corruptPayloadOnServer: Boolean = false
    ): KitabMetadataEntity {
        val validBytes = KitabDownloadAndStorageManager.buildValidTestPdfBytes(
            title = titleBangla,
            pageCount = pageCount,
            minTargetSizeBytes = sizeBytesTarget
        )
        val expectedSha256 = sha256Hex(validBytes)
        val url = "https://archive.org/download/test_verified/$bookId.pdf"

        val serverBytes = if (corruptPayloadOnServer) {
            validBytes.clone().also { arr ->
                // Corrupt bytes in the middle so SHA-256 fails while size remains identical
                arr[arr.size / 2] = (arr[arr.size / 2].toInt() xor 0xFF).toByte()
            }
        } else {
            validBytes
        }
        syntheticPdfMap[url] = serverBytes

        KitabDownloadAndStorageManager.networkTransport = object : KitabNetworkTransport {
            override fun openStream(downloadUrl: String, rangeStartBytes: Long): KitabNetworkStreamResponse {
                val payload = syntheticPdfMap[downloadUrl]
                    ?: throw IllegalStateException("No test payload registered for $downloadUrl")
                val safeOffset = rangeStartBytes.toInt().coerceIn(0, payload.size)
                val sliced = payload.copyOfRange(safeOffset, payload.size)
                val isPartial = safeOffset > 0
                return KitabNetworkStreamResponse(
                    httpStatusCode = if (isPartial) HttpURLConnection.HTTP_PARTIAL else HttpURLConnection.HTTP_OK,
                    isPartialContent = isPartial,
                    contentLengthBytes = sliced.size.toLong(),
                    inputStream = ByteArrayInputStream(sliced)
                )
            }
        }

        val entity = KitabMetadataEntity(
            bookId = bookId,
            seriesId = "series_$bookId",
            titleBangla = titleBangla,
            arabicTitle = "شرح الحديث",
            authorBangla = "শাইখুল হাদিস আল্লামা আজিজুল হক (রহ.)",
            translatorOrCompilerBangla = "শাইখুল হাদিস আল্লামা আজিজুল হক (রহ.)",
            publisherBangla = "মাকতাবাতুল আশরাফ ঢাকা",
            categoryId = KitabCategory.BUKHARI_SHARAH.id,
            volumeNumber = 1,
            totalVolumes = 2,
            fileSizeBytes = validBytes.size.toLong(),
            languageBangla = "বাংলা ও আরবি",
            shortDescriptionBangla = "দাওরায়ে হাদিসের শিক্ষার্থীদের জন্য সহিহ বুখারির প্রামাণ্য বাংলা শরাহ।",
            marhalaBangla = "দাওরায়ে হাদিস",
            downloadUrl = url,
            sha256Checksum = expectedSha256,
            isOpenLicenseVerified = true,
            isAdminApproved = true,
            licenseNoteBangla = "ওপেন-অ্যাক্সেস ভেরিফায়েড সংস্করণ",
            coverPrimaryHex = 0xFF074E35,
            coverAccentHex = 0xFFDDA83A
        )
        val dao = testDb.kitabLibraryDao()
        dao.upsertCatalogItem(entity)
        KitabDownloadAndStorageManager.refreshSnapshotFromDao(dao)
        return entity
    }

    @Test
    fun test01_catalogLoadsAll8Categories_andValidatesAuthenticMetadataAndSha256() {
        val defaultCatalog = KitabVerifiedCatalogData.defaultCatalogEntities
        assertTrue("Catalog must contain at least 15 verified items", defaultCatalog.size >= 15)

        // All 8 required Hadith Sharah & Qawmi categories must be represented
        val requiredCategories = listOf(
            KitabCategory.BUKHARI_SHARAH,
            KitabCategory.MUSLIM_SHARAH,
            KitabCategory.ABU_DAWOOD_SHARAH,
            KitabCategory.TIRMIZI_SHARAH,
            KitabCategory.NASAI_SHARAH,
            KitabCategory.IBN_MAJAH_SHARAH,
            KitabCategory.MISHKAT_SHARAH,
            KitabCategory.QAWMI_TEXTBOOK_SHARAH
        )
        for (cat in requiredCategories) {
            assertTrue(
                "Category ${cat.titleBangla} must have at least 1 book in catalog",
                defaultCatalog.any { it.categoryId == cat.id }
            )
        }

        // Every open-license downloadable book must have HTTPS URL, >0 fileSizeBytes, and 64-char SHA-256
        val downloadableBooks = defaultCatalog.filter { it.isOpenLicenseVerified }
        assertTrue(downloadableBooks.size >= 13)
        for (book in downloadableBooks) {
            assertTrue(book.downloadUrl.startsWith("https://archive.org/download/"))
            assertTrue(book.fileSizeBytes > 100_000L)
            assertEquals(64, book.sha256Checksum.length)
        }

        // Before user downloads anything, 0 PDF files are saved on the phone
        val localFilesBeforeDownload = deviceAStorageDir.listFiles()?.filter { it.name.endsWith(".pdf") }.orEmpty()
        assertTrue("No PDF file may be stored before user initiates download", localFilesBeforeDownload.isEmpty())
    }

    @Test
    fun test02_03_08_09_downloadWithPauseResume_progressPercentAndMb_persistsAcrossRestart_andReadsWithoutRedownload() = runBlocking {
        val entity = installSyntheticVerifiedBook(
            bookId = "test_bukhari_vol_1",
            titleBangla = "সহীহ বুখারী বাংলা শরাহ — ১ম খণ্ড",
            pageCount = 10,
            sizeBytesTarget = 40 * 1024
        )

        // Step 1: Start download and pause halfway (at 16 KB) -> verifies PAUSED status & partial progress (% and MB)
        val pausedModel = KitabDownloadAndStorageManager.downloadBookSynchronously(
            context = context,
            bookId = entity.bookId,
            pauseAfterBytes = 16 * 1024L
        )
        assertNotNull(pausedModel)
        assertEquals(KitabDownloadStatus.PAUSED, pausedModel!!.downloadStatus)
        assertTrue("Partial bytes must be saved in .part file", pausedModel.downloadedBytes >= 16 * 1024L)
        assertTrue("Progress percent must be between 30% and 90%", pausedModel.progressPercent in 30..90)
        assertTrue(pausedModel.formattedDownloadedMb.endsWith("MB"))
        assertFalse("Partial download must NOT be marked ready to read", pausedModel.isReadyToReadOffline)

        // Step 2: Resume download via HTTP 206 Partial Content until 100% completion + SHA-256 verification
        val completedModel = KitabDownloadAndStorageManager.downloadBookSynchronously(
            context = context,
            bookId = entity.bookId,
            pauseAfterBytes = -1L
        )
        assertNotNull(completedModel)
        assertEquals(KitabDownloadStatus.DOWNLOADED_VERIFIED, completedModel!!.downloadStatus)
        assertTrue(completedModel.sha256Verified)
        assertTrue(completedModel.isReadyToReadOffline)
        assertEquals(100, completedModel.progressPercent)
        assertEquals(10, completedModel.totalPageCount)

        // Step 3: Read up to page 7 and record progress locally
        KitabDownloadAndStorageManager.recordPageReadSynchronously(
            context = context,
            bookId = entity.bookId,
            pageNumber = 7,
            totalPages = 10
        )

        // Step 4: Simulate App Restart (re-initializing storage manager & reconciling with disk)
        KitabDownloadAndStorageManager.initAndAwaitSync(context, deviceAStorageDir)
        val afterRestartModel = KitabDownloadAndStorageManager.catalogItemsFlow.value
            .first { it.bookId == entity.bookId }

        assertEquals(KitabDownloadStatus.DOWNLOADED_VERIFIED, afterRestartModel.downloadStatus)
        assertEquals("Last read page (7) must persist after app restart", 7, afterRestartModel.lastReadPage)

        // Step 5: Open book again without re-downloading -> opens directly at lastReadPage = 7
        val openCheck = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(context, entity.bookId)
        assertTrue(openCheck.canOpen)
        assertEquals(7, openCheck.initialPage)
        assertEquals(10, openCheck.totalPages)
    }

    @Test
    fun test05_06_offlineModeAllowsReadingDownloadedBook_andBlocksUndownloadedBookWithClearMessage() = runBlocking {
        val downloadedBook = installSyntheticVerifiedBook(
            bookId = "offline_ready_muslim_v1",
            titleBangla = "সহীহ মুসলিম বাংলা শরাহ — ১ম খণ্ড",
            pageCount = 6
        )
        val undownloadedBook = installSyntheticVerifiedBook(
            bookId = "undownloaded_tirmizi_v1",
            titleBangla = "জামে তিরমিযী বাংলা শরাহ — ১ম খণ্ড",
            pageCount = 6
        )

        // Download only `offline_ready_muslim_v1` while online
        KitabDownloadAndStorageManager.downloadBookSynchronously(context, downloadedBook.bookId)

        // Turn OFF internet connection (Offline Mode)
        KitabDownloadAndStorageManager.isNetworkConnectedOverride = false

        // Test 5: Downloaded book opens and renders pages 100% offline without internet
        val offlineReadResult = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(
            context = context,
            bookId = downloadedBook.bookId
        )
        assertTrue("Downloaded book must open when internet is OFF", offlineReadResult.canOpen)
        val renderedPageBitmap = KitabDownloadAndStorageManager.renderKitabPageBitmap(
            context = context,
            bookId = downloadedBook.bookId,
            pageNumber = 2,
            targetWidthPx = 600
        )
        assertNotNull("Native PDF Reader must render page bitmap while offline", renderedPageBitmap)

        // Test 6: Un-downloaded book cannot open when offline and returns clear Bengali offline message
        val blockedResult = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(
            context = context,
            bookId = undownloadedBook.bookId
        )
        assertFalse("Un-downloaded book must NOT open when offline", blockedResult.canOpen)
        assertTrue(
            "Error message must mention offline/download requirement",
            blockedResult.errorMessageBangla.contains("ডাউনলোড করা হয়নি") &&
                blockedResult.errorMessageBangla.contains("ইন্টারনেট")
        )
    }

    @Test
    fun test07_deletingOneDownloadedBookRemovesOnlyItsFile_whileKeepingOtherDownloadedBooksIntact() = runBlocking {
        val bookA = installSyntheticVerifiedBook(
            bookId = "delete_test_book_A",
            titleBangla = "সুনানে আবু দাউদ শরাহ — ১ম খণ্ড"
        )
        val bookB = installSyntheticVerifiedBook(
            bookId = "delete_test_book_B",
            titleBangla = "সুনানে নাসায়ী শরাহ — ১ম খণ্ড"
        )

        KitabDownloadAndStorageManager.downloadBookSynchronously(context, bookA.bookId)
        KitabDownloadAndStorageManager.downloadBookSynchronously(context, bookB.bookId)

        assertTrue(KitabDownloadAndStorageManager.getFinalPdfFile(context, bookA.bookId).exists())
        assertTrue(KitabDownloadAndStorageManager.getFinalPdfFile(context, bookB.bookId).exists())

        // Delete ONLY bookA
        KitabDownloadAndStorageManager.deleteDownloadedBookSynchronously(context, bookA.bookId)

        // Verify bookA's local PDF file is deleted, but bookA remains in the catalog as NOT_DOWNLOADED
        assertFalse(KitabDownloadAndStorageManager.getFinalPdfFile(context, bookA.bookId).exists())
        val updatedBookA = KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == bookA.bookId }
        assertEquals(KitabDownloadStatus.NOT_DOWNLOADED, updatedBookA.downloadStatus)

        // Verify bookB's local PDF file and DOWNLOADED_VERIFIED state remain 100% intact
        assertTrue(KitabDownloadAndStorageManager.getFinalPdfFile(context, bookB.bookId).exists())
        val updatedBookB = KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == bookB.bookId }
        assertEquals(KitabDownloadStatus.DOWNLOADED_VERIFIED, updatedBookB.downloadStatus)
        assertTrue(updatedBookB.isReadyToReadOffline)
    }

    @Test
    fun test10_corruptedOrBadChecksumOrInvalidHeaderPdf_isBlockedAndQuarantined() = runBlocking {
        val corruptedBook = installSyntheticVerifiedBook(
            bookId = "corrupted_sha256_book",
            titleBangla = "ক্ষতিগ্রস্ত ফাইল টেস্ট কিতাব",
            corruptPayloadOnServer = true
        )

        val resultAfterDownload = KitabDownloadAndStorageManager.downloadBookSynchronously(
            context = context,
            bookId = corruptedBook.bookId
        )
        assertNotNull(resultAfterDownload)
        assertEquals(
            "Corrupted SHA-256 download must be flagged as FAILED_CORRUPTED",
            KitabDownloadStatus.FAILED_CORRUPTED,
            resultAfterDownload!!.downloadStatus
        )
        assertFalse(resultAfterDownload.isReadyToReadOffline)
        assertFalse(
            "Corrupted PDF must never be promoted to final .pdf file",
            KitabDownloadAndStorageManager.getFinalPdfFile(context, corruptedBook.bookId).exists()
        )

        val openAttempt = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(
            context = context,
            bookId = corruptedBook.bookId
        )
        assertFalse("Corrupted book must be blocked from opening in reader", openAttempt.canOpen)
    }

    @Test
    fun test11_perDeviceIsolation_userADownloadDoesNotAffectUserBPhone() = runBlocking {
        val book = installSyntheticVerifiedBook(
            bookId = "device_isolation_mishkat_v1",
            titleBangla = "মাজাহেরে হক্ব (মিশকাত শরাহ) — ১ম খণ্ড"
        )

        // User A downloads the book on Device A (`deviceAStorageDir`)
        KitabDownloadAndStorageManager.setCustomStorageDirectoryForTesting(deviceAStorageDir)
        val userAState = KitabDownloadAndStorageManager.downloadBookSynchronously(context, book.bookId)
        assertEquals(KitabDownloadStatus.DOWNLOADED_VERIFIED, userAState!!.downloadStatus)
        assertTrue(File(deviceAStorageDir, "${book.bookId}.pdf").exists())

        // Switch to User B's phone (`deviceBStorageDir` + fresh device database reconciliation)
        KitabDownloadAndStorageManager.initAndAwaitSync(context, deviceBStorageDir)
        val userBState = KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == book.bookId }

        assertFalse("User B's phone must NOT have the PDF file", File(deviceBStorageDir, "${book.bookId}.pdf").exists())
        assertEquals(
            "On User B's phone, the book must remain NOT_DOWNLOADED",
            KitabDownloadStatus.NOT_DOWNLOADED,
            userBState.downloadStatus
        )
    }

    @Test
    fun test04_12_uiEndToEnd_catalogToDownloadToInternalPdfReader_andPageNavigation() = runBlocking {
        val testBook = installSyntheticVerifiedBook(
            bookId = "ui_test_bukhari_1",
            titleBangla = "বুখারী শরীফ বাংলা শরাহ (UI টেস্ট)",
            pageCount = 6
        )
        // Pre-download so it can be opened immediately in the UI test
        KitabDownloadAndStorageManager.downloadBookSynchronously(context, testBook.bookId)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1600.dp)) {
                    KitabLibraryHubScreen(
                        isAdminUser = false,
                        onCloseLibrary = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Kitab Library Hub Screen, tabs, and category filters are displayed
        composeTestRule.onNodeWithTag("kitab_library_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_tab_online_catalog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_tab_my_books").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_tab_bookmarks").assertIsDisplayed()

        // 2. Switch to "আমার কিতাব (My Books)" tab -> verify the downloaded book card is shown with Read & Delete buttons
        composeTestRule.onNodeWithTag("kitab_tab_my_books").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_my_books_card_${testBook.bookId}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_my_books_read_btn_${testBook.bookId}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_my_books_delete_btn_${testBook.bookId}").assertIsDisplayed()

        // 3. Click "পড়া চালিয়ে যান (Read)" -> opens the Internal Native PDF Reader directly inside the app
        composeTestRule.onNodeWithTag("kitab_my_books_read_btn_${testBook.bookId}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("kitab_internal_pdf_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("বুখারী শরীফ বাংলা শরাহ (UI টেস্ট)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_next_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_prev_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_zoom_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_zoom_out_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitab_reader_page_jump_button").assertIsDisplayed()

        // 4. Navigate to Page 2 using the Next Page button and verify lastReadPage updates to 2
        composeTestRule.onNodeWithTag("kitab_reader_next_page_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitab_pdf_page_canvas_2").assertIsDisplayed()

        val updatedModel = KitabDownloadAndStorageManager.catalogItemsFlow.value.first { it.bookId == testBook.bookId }
        assertEquals(2, updatedModel.lastReadPage)
    }
}
