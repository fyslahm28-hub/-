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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.quran.HafeziDrivePdfManager
import com.example.ui.quran.HafeziQuranHubScreen
import com.example.ui.quran.HafeziQuranIndexData
import com.example.ui.quran.HafeziQuranStorageManager
import com.example.ui.quran.QuranDownloadStatus
import com.example.ui.screens.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w420dp-h2800dp")
class HafeziQuranAndHomeOrderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        HafeziQuranStorageManager.init(context)
    }

    @Test
    fun verify15LineHafeziPdfIndex_114Surahs_30Paras_616Pages_andSha256() {
        assertEquals(114, HafeziQuranIndexData.all114Surahs.size)
        assertEquals(30, HafeziQuranIndexData.all30Paras.size)
        assertEquals(616, HafeziQuranIndexData.TOTAL_PDF_PAGES)
        assertEquals(616, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        assertEquals(
            "1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828",
            HafeziQuranIndexData.ORIGINAL_PDF_SHA256
        )
        assertEquals(
            HafeziQuranIndexData.ORIGINAL_PDF_SHA256,
            HafeziDrivePdfManager.stateFlow.value.originalSha256
        )

        // Verify Surah Al-Fatiha (Surah 1) -> Page 2, Para 1, 7 Ayahs
        val fatiha = HafeziQuranIndexData.getSurahByNumber(1)
        assertEquals("আল-ফাতিহা", fatiha.nameBangla)
        assertEquals(7, fatiha.ayahCount)
        assertEquals(2, fatiha.startPage)
        assertEquals(1, fatiha.startPara)

        // Verify Surah Al-Baqarah (Surah 2) -> Page 3, Para 1, 286 Ayahs
        val baqarah = HafeziQuranIndexData.getSurahByNumber(2)
        assertEquals("আল-বাকারা", baqarah.nameBangla)
        assertEquals(286, baqarah.ayahCount)
        assertEquals(3, baqarah.startPage)

        // Verify Surah Ya-Sin (Surah 36) -> Page 441, Para 22, 83 Ayahs
        val yasin = HafeziQuranIndexData.getSurahByNumber(36)
        assertEquals("ইয়াসিন", yasin.nameBangla)
        assertEquals(83, yasin.ayahCount)
        assertEquals(441, yasin.startPage)
        assertEquals(22, yasin.startPara)

        // Verify Surah Ar-Rahman (Surah 55) -> Page 532, Para 27, 78 Ayahs (verified from PDF Fehrist)
        val rahman = HafeziQuranIndexData.getSurahByNumber(55)
        assertEquals("আর-রহমান", rahman.nameBangla)
        assertEquals(78, rahman.ayahCount)
        assertEquals(532, rahman.startPage)
        assertEquals(27, rahman.startPara)

        // Verify Surah Al-Mulk (Surah 67) -> Page 563, Para 29, 30 Ayahs (verified from PDF Fehrist)
        val mulk = HafeziQuranIndexData.getSurahByNumber(67)
        assertEquals("আল-মুলক", mulk.nameBangla)
        assertEquals(30, mulk.ayahCount)
        assertEquals(563, mulk.startPage)
        assertEquals(29, mulk.startPara)

        // Verify Surah An-Nas (Surah 114) -> Page 611, Para 30, 6 Ayahs (verified from PDF Fehrist)
        val nas = HafeziQuranIndexData.getSurahByNumber(114)
        assertEquals("আন-নাস", nas.nameBangla)
        assertEquals(6, nas.ayahCount)
        assertEquals(611, nas.startPage)
        assertEquals(611, nas.endPage)
        assertEquals(30, nas.startPara)

        // Verify Para 1, Para 2, Para 29, Para 30 verified start pages
        assertEquals(2, HafeziQuranIndexData.getParaByNumber(1).startPage)
        assertEquals(23, HafeziQuranIndexData.getParaByNumber(2).startPage)
        assertEquals(563, HafeziQuranIndexData.getParaByNumber(29).startPage)
        assertEquals(587, HafeziQuranIndexData.getParaByNumber(30).startPage)
    }

    @Test
    fun verifyUnifiedSearch_Surah_Para_Page_andAyah() {
        // 1. Direct page search (Bangla & English digits)
        val pageSearch = HafeziQuranIndexData.searchAll("৪৪১")
        assertEquals(441, pageSearch.directPageMatch)

        // 2. Surah search by Bangla name
        val surahSearch = HafeziQuranIndexData.searchAll("ইয়াসিন")
        assertTrue(surahSearch.matchingSurahs.any { it.number == 36 && it.startPage == 441 })

        // 3. Para search (Para 29 starts on Page 563 in the verified PDF Fehrist)
        val paraSearch = HafeziQuranIndexData.searchAll("তাবারাকাল্লাযী")
        assertTrue(paraSearch.matchingParas.any { it.number == 29 && it.startPage == 563 })

        // 4. Ayah reference search (2:255 - Ayatul Kursi)
        val ayahSearch = HafeziQuranIndexData.searchAll("2:255")
        assertTrue(ayahSearch.matchingAyahs.any { it.surahNumber == 2 && it.ayahNumber == 255 && it.pageNumber == 43 })
    }

    @Test
    fun verifyBookmarksAndLastReadPagePersistence() {
        // Record reading page 441 (Surah Ya-Sin, Para 22)
        HafeziQuranStorageManager.recordPageVisited(context, 441)
        val progress = HafeziQuranStorageManager.progressFlow.value
        assertEquals(441, progress.lastReadPage)
        assertEquals(36, progress.lastReadSurahNumber)
        assertEquals(22, progress.lastReadParaNumber)
        assertTrue(progress.uniquePagesReadCount >= 1)

        // Bookmark multiple pages (Page 43 and Page 563)
        HafeziQuranStorageManager.addOrUpdateBookmark(context, 43, "আয়াতুল কুরসি")
        HafeziQuranStorageManager.addOrUpdateBookmark(context, 563, "সূরা মুলক তিলাওয়াত")

        assertTrue(HafeziQuranStorageManager.isPageBookmarked(43))
        assertTrue(HafeziQuranStorageManager.isPageBookmarked(563))

        val savedBookmarks = HafeziQuranStorageManager.bookmarksFlow.value
        assertTrue(savedBookmarks.any { it.pageNumber == 43 && it.note == "আয়াতুল কুরসি" })
        assertTrue(savedBookmarks.any { it.pageNumber == 563 && it.note == "সূরা মুলক তিলাওয়াত" })

        // Remove one bookmark
        HafeziQuranStorageManager.removeBookmark(context, 43)
        assertTrue(!HafeziQuranStorageManager.isPageBookmarked(43))
        assertTrue(HafeziQuranStorageManager.isPageBookmarked(563))
    }

    @Test
    fun verifyOfflineDownloadPauseResumeAndSha256FileIntegrity() {
        // Step 1: Partial download (Pages 1..50) simulating Pause at page 50
        val partialReport = HafeziQuranStorageManager.downloadRangeAndVerifySync(context, startPage = 1, endPage = 50)
        assertTrue(partialReport.isValid)
        assertEquals(50, partialReport.verifiedPageCount)
        assertEquals(QuranDownloadStatus.PAUSED, HafeziQuranStorageManager.downloadStateFlow.value.status)

        // Step 2: Resume and complete all 616 pages
        val fullReport = HafeziQuranStorageManager.downloadRangeAndVerifySync(
            context,
            startPage = 51,
            endPage = HafeziQuranIndexData.TOTAL_PDF_PAGES
        )
        assertTrue(fullReport.isValid)
        assertEquals(616, fullReport.verifiedPageCount)
        assertTrue(fullReport.corruptedOrMissingPages.isEmpty())
        assertEquals(QuranDownloadStatus.COMPLETED_VERIFIED, HafeziQuranStorageManager.downloadStateFlow.value.status)
        assertNotNull(fullReport.sha256DigestPrefix)
        assertTrue(fullReport.sha256DigestPrefix.isNotBlank())
    }

    @Test
    fun verifyHomeScreenSectionOrder_Date_PrayerTimes_AjkerSmoron_IslamicServices() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(2600.dp)) {
                HomeScreen(
                    currentUserId = "test_user",
                    onNavigate = {},
                    onOpenFeedFilter = {}
                )
            }
        }

        // Verify top elements above the 4 target sections remain intact
        composeTestRule.onNodeWithTag("home_card_publish_application").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_card_madrasa_directory").assertIsDisplayed()

        val madrasaTop = composeTestRule.onNodeWithTag("home_card_madrasa_directory")
            .fetchSemanticsNode().boundsInRoot.top
        val dateTop = composeTestRule.onNodeWithTag("home_todays_date_card")
            .fetchSemanticsNode().boundsInRoot.top
        val prayerTop = composeTestRule.onNodeWithTag("home_prayer_times_card")
            .fetchSemanticsNode().boundsInRoot.top
        val smoronTop = composeTestRule.onNodeWithTag("home_ajker_smoron_card")
            .fetchSemanticsNode().boundsInRoot.top
        val servicesTop = composeTestRule.onNodeWithTag("home_islamic_essential_services_section")
            .fetchSemanticsNode().boundsInRoot.top

        // Verify exact vertical sequence:
        // মাদরাসা (above) -> ১. আজকের তারিখ -> ২. নামাজের সময়সূচি -> ৩. আজকের স্মরণ -> ৪. ইসলামিক ও প্রয়োজনীয় সেবাসমূহ
        assertTrue("মাদরাসা card must be above ১. আজকের তারিখ", madrasaTop < dateTop)
        assertTrue("১. আজকের তারিখ must appear above ২. নামাজের সময়সূচি", dateTop < prayerTop)
        assertTrue("২. নামাজের সময়সূচি must appear above ৩. আজকের স্মরণ", prayerTop < smoronTop)
        assertTrue("৩. আজকের স্মরণ must appear above ৪. ইসলামিক ও প্রয়োজনীয় সেবাসমূহ", smoronTop < servicesTop)
    }

    @Test
    fun verifyHafeziQuranHubScreen_4Tabs_Filters_andInternalPdfReader() {
        composeTestRule.setContent {
            HafeziQuranHubScreen(onCloseHub = {})
        }

        // 1. Verify Quran Home Screen Dashboard
        composeTestRule.onNodeWithTag("hafezi_quran_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_hero_resume_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_offline_download_card").assertIsDisplayed()

        // 2. Verify 4 Navigation Tabs exist and work
        composeTestRule.onNodeWithTag("quran_hub_lazy_list")
            .performScrollToNode(hasTestTag("quran_hub_tab_surahs"))
        composeTestRule.onNodeWithTag("quran_hub_tab_surahs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_tab_paras").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_tab_bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_tab_history").assertIsDisplayed()

        // 3. Switch to 30 Para tab and verify Para 1 (starts at Page 2) is displayed
        composeTestRule.onNodeWithTag("quran_hub_tab_paras").performClick()
        composeTestRule.onNodeWithTag("quran_hub_lazy_list")
            .performScrollToNode(hasTestTag("quran_para_item_1"))
        composeTestRule.onNodeWithTag("quran_para_item_1").assertIsDisplayed()

        // 4. Switch back to 114 Surah tab and open Surah 1 (Al-Fatiha -> Page 2) in Internal PDF Reader
        composeTestRule.onNodeWithTag("quran_hub_lazy_list")
            .performScrollToNode(hasTestTag("quran_hub_tab_surahs"))
        composeTestRule.onNodeWithTag("quran_hub_tab_surahs").performClick()
        composeTestRule.onNodeWithTag("quran_hub_lazy_list")
            .performScrollToNode(hasTestTag("quran_surah_item_1"))
        composeTestRule.onNodeWithTag("quran_surah_item_1").performClick()

        // 5. Verify Internal PDF Reader opens directly inside the app without Google Login or External App
        composeTestRule.onNodeWithTag("hafezi_quran_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hafezi_mushaf_page_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_prev_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_next_page_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_zoom_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reader_zoom_out_button").assertIsDisplayed()
    }
}
