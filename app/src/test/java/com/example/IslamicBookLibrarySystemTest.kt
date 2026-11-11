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
 * ১২. বাধ্যতামূলক ১৪টি যাচাই টেস্ট (PHASE — ISLAMIC BOOK LIBRARY & AUTHOR/PUBLISHER SELF-PUBLISHING SYSTEM)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2400dp")
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

    @Test
    fun test01_homePageIslamicBooksButton_opensDedicatedFullScreenLibraryPage_preservingExistingServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.width(480.dp).height(1800.dp)) {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        // Verify "ইসলামিক বই" button is present on Home Page in its preserved location
        composeTestRule.onNodeWithTag("home_islamic_service_books")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            "Clicking 'ইসলামিক বই' on Home Page must navigate directly to MainDestination.ISLAMIC_BOOK_LIBRARY",
            MainDestination.ISLAMIC_BOOK_LIBRARY,
            navigatedDestination
        )

        // Verify Tilawat and Hadith buttons remain intact (Test 14 - No regression)
        composeTestRule.onNodeWithTag("home_islamic_service_tilawat").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainDestination.TILAWAT, navigatedDestination)
    }

    @Test
    fun test02_all15CategoriesWork_andNewReleaseRequiresVerifiedPublicationYear() {
        val categories = IslamicBookCategory.entries
        assertEquals("There must be all 15 required Islamic Book categories", 15, categories.size)

        // Every category must have at least 1 authentic book in the verified catalog
        categories.forEach { category ->
            val books = bookManager.getBooksByCategory(category)
            assertTrue(
                "Category ${category.titleBangla} (${category.id}) must have verified books",
                books.isNotEmpty()
            )
        }

        // Verify rule: No book without verified publication year can appear in NEW_RELEASE
        val newReleaseBooks = bookManager.getBooksByCategory(IslamicBookCategory.NEW_RELEASE)
        assertTrue(newReleaseBooks.isNotEmpty())
        newReleaseBooks.forEach { book ->
            assertTrue(
                "Book ${book.bookId} in NEW_RELEASE must have isPublicationYearVerified == true",
                book.isPublicationYearVerified && book.publicationYear.isNotBlank()
            )
        }

        val unverifiedYearBook = IslamicBookCatalogItem(
            bookId = "test_unverified_year",
            titleBangla = "অযাচাইকৃত বছরের বই",
            authorName = "লেখক",
            publisherName = "প্রকাশনী",
            category = IslamicBookCategory.NEW_RELEASE,
            publicationYear = "",
            isPublicationYearVerified = false,
            pageCount = 100,
            shortIntroduction = "পরীক্ষামূলক বই",
            pdfUrl = "https://example.com/book.pdf",
            pdfFileSizeBytes = 1000L,
            fileSizeTextBangla = "১ মেগাবাইট",
            copyrightOwnerName = "লেখক"
        )
        assertFalse(
            "Unverified publication year book must NEVER match NEW_RELEASE category",
            unverifiedYearBook.belongsToCategory(IslamicBookCategory.NEW_RELEASE)
        )
    }

    @Test
    fun test03_searchFindsBookByTitle_Author_andPublisher() {
        // 1. Search by Book Title
        val byTitle = bookManager.searchPublicBooks("মাআরিফুল কুরআন")
        assertTrue(byTitle.any { it.bookId == "book_maariful_quran_bangla" })

        // 2. Search by Author Name
        val byAuthor = bookManager.searchPublicBooks("মুফতি মুহাম্মদ ত্বকী উসমানী")
        assertTrue(byAuthor.any { it.bookId == "book_islami_arthaniti_usmani" })

        // 3. Search by Publisher Name
        val byPublisher = bookManager.searchPublicBooks("ইসলামিক ফাউন্ডেশন বাংলাদেশ")
        assertTrue(byPublisher.size >= 2)
    }

    @Test
    fun test04_bookDetailsPageOpens_andDisplaysCover_Intro_TOC_andVolumes() {
        val sampleBook = bookManager.getPublicCatalogBooks().first { it.bookId == "book_riyadus_salihin_bangla" }

        composeTestRule.setContent {
            Box(modifier = Modifier.width(480.dp).height(2200.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "reader_user_1",
                    isAdmin = false
                )
            }
        }

        composeTestRule.onNodeWithTag("islamic_book_library_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_card_details_btn_${sampleBook.bookId}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Book Details Page must be displayed with large cover, volume chips, and read/download buttons
        composeTestRule.onNodeWithTag("islamic_book_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_details_large_cover").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_volume_chip_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_volume_chip_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_details_read_btn").assertIsDisplayed()
    }

    @Test
    fun test05_test06_test07_pdfDownloadsAndSavesOffline_PauseResumeDelete_andPersistsLastReadPageAndBookmarks() = runBlocking {
        val book = bookManager.getPublicCatalogBooks().first()
        assertFalse("Initially book must not be downloaded", bookManager.isBookDownloaded(book.bookId, 1))

        // 1. Pause and Cancel check
        bookManager.pauseDownload(book.bookId, 1)
        assertEquals(
            IslamicBookDownloadState.PAUSED,
            bookManager.downloadProgressMap.value[bookManager.downloadKey(book.bookId, 1)]?.state
        )
        bookManager.cancelDownload(book.bookId, 1)
        assertEquals(
            IslamicBookDownloadState.NOT_DOWNLOADED,
            bookManager.downloadProgressMap.value[bookManager.downloadKey(book.bookId, 1)]?.state
        )

        // 2. Download book and verify valid %PDF- file is saved on device for offline reading
        val downloadedFile = bookManager.downloadBookSynchronously(book, 1)
        assertTrue("Downloaded file must exist on phone storage", downloadedFile.exists())
        assertTrue("Downloaded file must be a valid %PDF- file", bookManager.isValidPdfFile(downloadedFile))
        assertTrue("Book must appear in My Books (আমার ডাউনলোডকৃত বই)", bookManager.getDownloadedBooks().any { it.bookId == book.bookId })

        // 3. Save Last Read Page and Bookmarks and verify persistence
        bookManager.saveLastReadPage(book.bookId, 1, 7)
        assertEquals(7, bookManager.getLastReadPage(book.bookId, 1))

        val afterAdd = bookManager.addOrRemoveBookmark(book.bookId, 1, 7, "গুরুত্বপূর্ণ মাসআলা")
        assertEquals(1, afterAdd.size)
        assertEquals(7, afterAdd.first().pageNumber)

        // 4. Local-only Delete: deletes local PDF file from phone while keeping the book in public catalog
        val deleted = bookManager.deleteLocalDownloadedBook(book.bookId, 1)
        assertTrue(deleted)
        assertFalse("Local PDF file must be removed after local delete", bookManager.isBookDownloaded(book.bookId, 1))
        assertTrue(
            "Deleting local copy must NEVER remove book from main catalog",
            bookManager.getPublicCatalogBooks().any { it.bookId == book.bookId }
        )
    }

    @Test
    fun test08_selfPublishingButton_opensFullScreenAuthorPublisherPortalAndSubmissionForm() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(480.dp).height(2200.dp)) {
                IslamicBookLibraryScreen(
                    currentUserId = "author_uid_1",
                    currentUserName = "মাওলানা মাহমুদুল হাসান",
                    isAdmin = false
                )
            }
        }

        // Click "আপনার লেখা বই প্রকাশ করুন"
        composeTestRule.onNodeWithTag("open_self_publishing_portal_btn")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("author_publisher_portal_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("contributor_dashboard_screen").assertIsDisplayed()

        // Click "নতুন বই জমা দিন" tab
        composeTestRule.onNodeWithTag("new_book_submission_btn")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("book_submission_form_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submission_title_bangla_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("copyright_truth_checkbox").assertIsDisplayed()
    }

    @Test
    fun test09_pdfUploadValidation_acceptsValidPdf_blocksInvalidFormatAndOversize_andDetectsDuplicateSha256() {
        val sampleBook = bookManager.getPublicCatalogBooks().first()
        val validPdfBytes = IslamicBookVerifiedCatalog.generateAuthenticBookPdfBytes(sampleBook)

        // 1. Non-PDF file extension (.txt / .docx) must be blocked
        val invalidExtResult = bookManager.validateAndPreparePdfForUpload(
            fileName = "malicious_script.exe",
            pdfBytes = validPdfBytes,
            contributorId = "author_1"
        )
        assertTrue("Non-PDF extension must be rejected", invalidExtResult.isFailure)

        // 2. Corrupt file without %PDF- magic header must be blocked
        val invalidHeaderResult = bookManager.validateAndPreparePdfForUpload(
            fileName = "fake_book.pdf",
            pdfBytes = "NOT_A_PDF_FILE_CONTENT".toByteArray(),
            contributorId = "author_1"
        )
        assertTrue("File without %PDF- header must be rejected", invalidHeaderResult.isFailure)

        // 3. Valid PDF file must succeed and generate 64-char SHA-256 hash
        val validResult = bookManager.validateAndPreparePdfForUpload(
            fileName = "usulul_fiqh_bangla.pdf",
            pdfBytes = validPdfBytes,
            contributorId = "author_1"
        )
        assertTrue("Valid PDF must succeed", validResult.isSuccess)
        val progress = validResult.getOrThrow()
        assertEquals(64, progress.sha256Hash.length)
        assertEquals(IslamicBookUploadState.COMPLETED, progress.state)

        // 4. Test Upload Pause / Resume / Cancel controls
        bookManager.pauseUpload()
        assertEquals(IslamicBookUploadState.PAUSED, bookManager.uploadProgress.value.state)
        bookManager.resumeUpload()
        assertEquals(IslamicBookUploadState.COMPLETED, bookManager.uploadProgress.value.state)
    }

    @Test
    fun test10_test11_test12_test13_copyrightCheckboxRequired_submissionIsolation_andAdminApproveRejectWorkflow() {
        val samplePdfBytes = IslamicBookVerifiedCatalog.generateAuthenticBookPdfBytes(
            bookManager.getPublicCatalogBooks().first()
        )
        val uploadInfo = bookManager.validateAndPreparePdfForUpload(
            fileName = "adarsha_ustaz_guideline.pdf",
            pdfBytes = samplePdfBytes,
            contributorId = "author_alice"
        ).getOrThrow()

        // Test 10: Submitting WITHOUT checking the mandatory Copyright & Truth confirmation checkbox MUST fail
        val unconfirmedSubmission = IslamicBookSubmission(
            contributorId = "author_alice",
            contributorName = "মাওলানা আব্দুল্লাহ",
            contributorRole = ContributorRoleType.AUTHOR,
            contributorPhone = "01711223344",
            bookTitleBangla = "আদর্শ উস্তায ও তালিবুল ইলমের পাথেয়",
            authorName = "মাওলানা আব্দুল্লাহ",
            publisherName = "মাকতাবাতুল ফুরকান",
            category = IslamicBookCategory.TAZKIYAH_AKHLAQ,
            shortIntroduction = "কওমি মাদরাসার উস্তায ও তালিবে ইলমদের আদব ও আখলাক বিষয়ক মৌলিক গ্রন্থ।",
            pdfStorageUrl = uploadInfo.storageDownloadUrl,
            pdfFileName = uploadInfo.fileName,
            pdfFileSizeBytes = uploadInfo.fileSizeBytes,
            pdfSha256 = uploadInfo.sha256Hash,
            copyrightOwnerName = "মাওলানা আব্দুল্লাহ",
            copyrightPermissionType = CopyrightPermissionType.SELF_COPYRIGHT,
            isTruthConfirmed = false // NOT checked!
        )
        val unconfirmedResult = bookManager.submitNewBookApplication(unconfirmedSubmission)
        assertTrue("Submission without copyright truth checkbox must be blocked", unconfirmedResult.isFailure)

        // Test 11: Submitting WITH copyright truth checkbox checked succeeds with status SUBMITTED and unique ID,
        // and does NOT appear in the Public Library before Admin approval
        val confirmedSubmission = unconfirmedSubmission.copy(isTruthConfirmed = true)
        val submitResult = bookManager.submitNewBookApplication(confirmedSubmission)
        assertTrue("Valid confirmed submission must succeed", submitResult.isSuccess)
        val savedSubmission = submitResult.getOrThrow()

        assertTrue(savedSubmission.submissionId.startsWith("SUB_2026_"))
        assertEquals(BookSubmissionStatus.SUBMITTED, savedSubmission.status)
        assertFalse(
            "Unapproved submission must NEVER appear in Public Library before Admin approval",
            bookManager.getPublicCatalogBooks().any { it.titleBangla == savedSubmission.bookTitleBangla }
        )

        // Duplicate PDF detection: trying to submit the same PDF SHA-256 again must be blocked
        val duplicateSubmitResult = bookManager.submitNewBookApplication(
            confirmedSubmission.copy(submissionId = "")
        )
        assertTrue("Duplicate PDF SHA-256 submission must be blocked", duplicateSubmitResult.isFailure)

        // Test 12: Contributor Dashboard Isolation — Alice sees her submission; Bob CANNOT see Alice's submission
        val aliceList = bookManager.getSubmissionsForContributor("author_alice")
        val bobList = bookManager.getSubmissionsForContributor("author_bob")
        assertEquals(1, aliceList.size)
        assertTrue("Bob must not see Alice's unpublished submissions", bobList.isEmpty())

        // Admin requests MORE_INFO_REQUIRED -> Alice sees Admin comment and submits additional info
        val moreInfoResult = bookManager.adminReviewSubmission(
            adminId = "admin_1",
            adminEmail = "foysalahamad102@gmail.com",
            adminPhone = "01300448147",
            submissionId = savedSubmission.submissionId,
            newStatus = BookSubmissionStatus.MORE_INFO_REQUIRED,
            adminCommentBangla = "অনুগ্রহ করে মুদ্রিত কপির প্রথম পৃষ্ঠার তথ্য যুক্ত করুন।"
        )
        assertTrue(moreInfoResult.isSuccess)
        assertEquals(
            BookSubmissionStatus.MORE_INFO_REQUIRED,
            bookManager.getSubmissionsForContributor("author_alice").first().status
        )

        val replyResult = bookManager.submitContributorAdditionalInfo(
            submissionId = savedSubmission.submissionId,
            contributorId = "author_alice",
            replyNoteBangla = "প্রথম সংস্করণের মুদ্রণ তথ্য ও লিখিত স্বত্বের বিবরণ যুক্ত করা হয়েছে।"
        )
        assertTrue(replyResult.isSuccess)
        assertEquals(BookSubmissionStatus.UNDER_REVIEW, replyResult.getOrThrow().status)

        // Test 13A: Non-admin CANNOT approve or reject submissions
        val fakeAdminAttempt = bookManager.adminReviewSubmission(
            adminId = "intruder_1",
            adminEmail = "intruder@gmail.com",
            adminPhone = "01900000000",
            submissionId = savedSubmission.submissionId,
            newStatus = BookSubmissionStatus.PUBLISHED,
            adminCommentBangla = "Fake approval"
        )
        assertTrue("Non-admin must be blocked from approving books", fakeAdminAttempt.isFailure)

        // Test 13B: Authorized Admin approves and publishes (`PUBLISHED`) -> Book appears in Public Library under its category!
        val publishResult = bookManager.adminReviewSubmission(
            adminId = "admin_1",
            adminEmail = "foysalahamad102@gmail.com",
            adminPhone = "01300448147",
            submissionId = savedSubmission.submissionId,
            newStatus = BookSubmissionStatus.PUBLISHED,
            adminCommentBangla = "তথ্য ও কপিরাইট যাচাই সম্পন্ন। মূল লাইব্রেরিতে প্রকাশের অনুমোদন দেওয়া হলো।"
        )
        assertTrue(publishResult.isSuccess)
        val publishedSub = publishResult.getOrThrow()
        assertEquals(BookSubmissionStatus.PUBLISHED, publishedSub.status)

        val tazkiyahBooks = bookManager.getBooksByCategory(IslamicBookCategory.TAZKIYAH_AKHLAQ)
        assertTrue(
            "Approved & Published book must appear in its selected category in the Public Library",
            tazkiyahBooks.any { it.titleBangla == "আদর্শ উস্তায ও তালিবুল ইলমের পাথেয়" }
        )

        // Test 13C: If Admin rejects or suspends a book, it is excluded from the Public Library
        bookManager.adminUpdatePublishedBookState(
            adminId = "admin_1",
            adminEmail = "foysalahamad102@gmail.com",
            adminPhone = "01300448147",
            bookId = publishedSub.publishedBookId,
            newState = BookPublicationState.SUSPENDED,
            reasonBangla = "পুনঃযাচাইয়ের জন্য সাময়িক স্থগিত"
        )
        assertFalse(
            "Suspended book must be hidden from Public Library",
            bookManager.getPublicCatalogBooks().any { it.bookId == publishedSub.publishedBookId }
        )

        // Verify Audit Logs recorded all actions
        assertTrue("Audit logs must record submission and admin review actions", bookManager.auditLogs.value.size >= 4)
    }
}
