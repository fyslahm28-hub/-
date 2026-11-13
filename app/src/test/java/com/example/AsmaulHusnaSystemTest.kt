package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.asmaulhusna.AsmaulHusnaCatalog
import com.example.ui.asmaulhusna.AsmaulHusnaFilter
import com.example.ui.asmaulhusna.AsmaulHusnaManager
import com.example.ui.asmaulhusna.AsmaulHusnaScreen
import com.example.ui.screens.IslamicAndEssentialServicesSection
import com.example.ui.viewmodel.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AsmaulHusnaSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var manager: AsmaulHusnaManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = AsmaulHusnaManager.getInstance(context)
        manager.resetStateForTesting()
    }

    @Test
    fun test01_homePageAsmaulHusnaButton_navigatesToFullScreenAndPreservesExistingServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            MaterialTheme {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        // Verify existing services are intact
        composeTestRule.onNodeWithTag("home_islamic_service_quran").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_hadith").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_books").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_articles").assertIsDisplayed()

        // Tap "আসমাউল হুসনা"
        composeTestRule.onNodeWithTag("home_islamic_service_asmaul_husna")
            .assertIsDisplayed()
            .performClick()

        assertEquals(MainDestination.ASMAUL_HUSNA, navigatedDestination)
    }

    @Test
    fun test02_catalogContainsAll99NamesWithCompleteAuthenticIslamicData() {
        val allNames = AsmaulHusnaCatalog.allNames
        assertEquals(99, allNames.size)

        allNames.forEachIndexed { index, item ->
            assertEquals(index + 1, item.number)
            assertTrue("Bangla number must not be blank for #${item.number}", item.numberBangla.isNotBlank())
            assertTrue("Arabic name must not be blank for #${item.number}", item.arabicName.isNotBlank())
            assertTrue("Bangla transliteration must not be blank for #${item.number}", item.transliterationBangla.isNotBlank())
            assertTrue("Bangla meaning must not be blank for #${item.number}", item.meaningBangla.isNotBlank())
            assertTrue("Explanation must not be blank for #${item.number}", item.detailedExplanationBangla.isNotBlank())
            assertTrue("Quranic verse Arabic must not be blank for #${item.number}", item.quranicVerseArabic.isNotBlank())
            assertTrue("Quranic verse Bangla must not be blank for #${item.number}", item.quranicVerseBangla.isNotBlank())
            assertTrue("Quranic reference must not be blank for #${item.number}", item.quranicReference.isNotBlank())
            assertTrue("Scholarly note must not be blank for #${item.number}", item.scholarlySourceAndNote.isNotBlank())
            assertTrue("Lesson must not be blank for #${item.number}", item.lessonForBelieverBangla.isNotBlank())
            assertTrue("Authentic Dua must not be blank for #${item.number}", item.authenticDuaArabic.isNotBlank())
            assertTrue("Authentic Dua source must not be blank for #${item.number}", item.authenticDuaSource.isNotBlank())
        }
    }

    @Test
    fun test03_localBookmarkLearnedProgress_dailyName_andSearchFilter() {
        assertFalse(manager.isBookmarked(2))
        assertTrue(manager.toggleBookmark(2))
        assertTrue(manager.isBookmarked(2))

        assertFalse(manager.isLearned(2))
        assertTrue(manager.toggleLearned(2))
        assertTrue(manager.isLearned(2))

        val bookmarkedList = manager.filterAndSearchNames("", AsmaulHusnaFilter.BOOKMARKED)
        assertEquals(1, bookmarkedList.size)
        assertEquals("আর-রাহমান", bookmarkedList.first().transliterationBangla)

        val searchByMeaning = manager.filterAndSearchNames("রিযিকদাতা", AsmaulHusnaFilter.ALL)
        assertTrue(searchByMeaning.any { it.number == 18 })

        val dailyDay1 = manager.getDailyNameToLearn(1)
        assertEquals(1, dailyDay1.number)
        val dailyDay99 = manager.getDailyNameToLearn(99)
        assertEquals(99, dailyDay99.number)
    }

    @Test
    fun test04_asmaulHusnaScreenUI_rendersHeaders_dailyCard_opensFullScreenDetailAndNavigatesPrevNext() {
        composeTestRule.setContent {
            MaterialTheme {
                AsmaulHusnaScreen()
            }
        }

        // Verify Arabic & Bangla headers and intro
        composeTestRule.onNodeWithTag("asmaul_husna_arabic_title")
            .assertIsDisplayed()
            .assertTextContains("أَسْمَاءُ اللَّهِ الْحُسْنَىٰ")
        composeTestRule.onNodeWithTag("asmaul_husna_bangla_title")
            .assertIsDisplayed()
            .assertTextContains("আল্লাহর ৯৯টি সুন্দর নাম")

        composeTestRule.onNodeWithTag("asmaul_husna_home_screen")
            .performScrollToNode(hasTestTag("asmaul_husna_daily_card"))
        composeTestRule.onNodeWithTag("asmaul_husna_daily_card").assertIsDisplayed()

        // Search for #2 "আর-রাহমান" and open its detail screen
        composeTestRule.onNodeWithTag("asmaul_husna_home_screen")
            .performScrollToNode(hasTestTag("asmaul_husna_search_input"))
        composeTestRule.onNodeWithTag("asmaul_husna_search_input")
            .performTextInput("আর-রাহমান")
        composeTestRule.onNodeWithTag("asmaul_husna_home_screen")
            .performScrollToNode(hasTestTag("asmaul_husna_card_2"))
        composeTestRule.onNodeWithTag("asmaul_husna_card_2")
            .assertIsDisplayed()
            .performClick()

        // Verify Full-Screen Detail Page
        composeTestRule.onNodeWithTag("asmaul_husna_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_arabic_name")
            .assertTextContains("الرَّحْمَٰنُ")
        composeTestRule.onNodeWithTag("asmaul_husna_detail_transliteration")
            .assertTextContains("আর-রাহমান")

        // Bookmark & Mark Learned on Detail Page
        composeTestRule.onNodeWithTag("asmaul_husna_detail_bookmark_btn").performClick()
        assertTrue(manager.isBookmarked(2))
        composeTestRule.onNodeWithTag("asmaul_husna_detail_learned_btn").performClick()
        assertTrue(manager.isLearned(2))

        // Verify Explanation, Quranic Verse, Lesson, and Dua cards
        composeTestRule.onNodeWithTag("asmaul_husna_detail_explanation_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_quran_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_lesson_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_dua_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Navigate to Next Name (#3 আর-রাহীম) and Previous Name (#2 আর-রাহমান)
        composeTestRule.onNodeWithTag("asmaul_husna_next_btn")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_transliteration")
            .performScrollTo()
            .assertTextContains("আর-রাহীম")

        composeTestRule.onNodeWithTag("asmaul_husna_prev_btn")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("asmaul_husna_detail_transliteration")
            .performScrollTo()
            .assertTextContains("আর-রাহমান")
    }
}
