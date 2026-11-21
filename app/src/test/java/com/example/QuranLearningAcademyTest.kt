package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.example.ui.quranlearning.QuranLearningAcademyScreen
import com.example.ui.quranlearning.QuranLearningCatalog
import com.example.ui.quranlearning.QuranLearningLevel
import com.example.ui.quranlearning.QuranLearningStorageManager
import com.example.ui.screens.IslamicAndEssentialServicesSection
import com.example.ui.viewmodel.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranLearningAcademyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var storage: QuranLearningStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = QuranLearningStorageManager.getInstance(context)
        storage.resetForTesting()
    }

    @Test
    fun test01_homeScreenQuranLearningTile_navigatesToAcademyAndPreservesAllOtherServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            MaterialTheme {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        // Verify untouched existing services remain intact
        composeTestRule.onNodeWithTag("home_islamic_service_quran").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_hadith").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_books").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_articles").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_asmaul_husna").assertIsDisplayed()

        // Tap "কুরআন শিক্ষা"
        composeTestRule.onNodeWithTag("home_islamic_service_quran_edu")
            .assertIsDisplayed()
            .performClick()

        assertEquals(MainDestination.QURAN_LEARNING, navigatedDestination)
    }

    @Test
    fun test02_academyHeaderProportions_threePrimaryLevels_andSecondaryToolsRenderCleanlyOnRedmiNote14ProPlusAndCompactScreens() {
        // Test on Redmi Note 14 Pro+ portrait viewport (412 x 915 dp)
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(412.dp).height(915.dp)) {
                    QuranLearningAcademyScreen()
                }
            }
        }

        // Verify Header texts & medium proportion (18-20% of 915dp => ~173.8dp)
        composeTestRule.onNodeWithTag("quran_academy_header").assertIsDisplayed()
        val headerHeight = composeTestRule.onNodeWithTag("quran_academy_header")
            .getUnclippedBoundsInRoot().height
        assertTrue(
            "Header height ($headerHeight) should be medium (~138..180dp)",
            headerHeight in 135.dp..182.dp
        )

        composeTestRule.onNodeWithTag("quran_academy_header_arabic")
            .assertIsDisplayed()
            .assertTextContains("تَعْلِيمُ الْقُرْآنِ")
        composeTestRule.onNodeWithTag("quran_academy_header_bangla")
            .assertIsDisplayed()
            .assertTextContains("কুরআন শিক্ষা")
        composeTestRule.onNodeWithTag("quran_academy_header_subtitle")
            .assertIsDisplayed()
            .assertTextContains("শিখুন ধাপে ধাপে")

        // Verify all 3 primary learning level cards in exact order with exact Arabic & Bangla strings
        composeTestRule.onNodeWithTag("quran_level_card_primary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_level_arabic_primary", useUnmergedTree = true).assertTextContains("أ ب ت ث")
        composeTestRule.onNodeWithTag("quran_level_title_primary", useUnmergedTree = true).assertTextContains("প্রাথমিক শিক্ষা")
        composeTestRule.onNodeWithTag("quran_level_desc_primary", useUnmergedTree = true).assertTextContains("আরবি হরফ, হরকত ও প্রাথমিক পাঠ।")

        composeTestRule.onNodeWithTag("quran_level_card_intermediate").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_level_arabic_intermediate", useUnmergedTree = true).assertTextContains("تَجْوِيد")
        composeTestRule.onNodeWithTag("quran_level_title_intermediate", useUnmergedTree = true).assertTextContains("মধ্যবর্তী শিক্ষা")
        composeTestRule.onNodeWithTag("quran_level_desc_intermediate", useUnmergedTree = true).assertTextContains("মাখরাজ, সিফাত ও তাজবিদের নিয়ম।")

        composeTestRule.onNodeWithTag("quran_level_card_advanced").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_level_arabic_advanced", useUnmergedTree = true).assertTextContains("التِّلَاوَة")
        composeTestRule.onNodeWithTag("quran_level_title_advanced", useUnmergedTree = true).assertTextContains("উচ্চ শিক্ষা")
        composeTestRule.onNodeWithTag("quran_level_desc_advanced", useUnmergedTree = true).assertTextContains("উন্নত তাজবিদ ও শুদ্ধ তিলাওয়াত।")

        // Verify all 4 secondary tools exist and are visible
        composeTestRule.onNodeWithTag("quran_tool_listen_learn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_practice_quiz").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_learning_progress").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_last_read").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test03_primaryLevelNavigation_lessonCompletion_lastReadAndBackInterceptorWorkEndToEnd() {
        var backInterceptor: (() -> Boolean)? = null

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(360.dp).height(760.dp)) {
                    QuranLearningAcademyScreen(
                        registerBackInterceptor = { backInterceptor = it }
                    )
                }
            }
        }

        // Open প্রাথমিক শিক্ষা -> Lesson 1 -> Mark Completed
        composeTestRule.onNodeWithTag("quran_level_card_primary").performClick()
        composeTestRule.onNodeWithTag("quran_level_lessons_screen_primary").assertIsDisplayed()

        val firstLesson = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.PRIMARY).first()
        composeTestRule.onNodeWithTag("quran_lesson_item_${firstLesson.id}").performClick()
        composeTestRule.onNodeWithTag("quran_lesson_reader_screen").assertIsDisplayed()

        assertEquals(firstLesson.id, storage.lastReadLessonId.value)

        composeTestRule.onNodeWithTag("quran_lesson_mark_complete_btn")
            .performScrollTo()
            .performClick()
        assertTrue(storage.isLessonCompleted(firstLesson.id))

        // Step back from LessonReader -> LevelLessons -> AcademyHome via TopBar Back Interceptor
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_level_lessons_screen_primary").assertIsDisplayed()

        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_learning_academy_home").assertIsDisplayed()

        // Verify secondary tools navigate cleanly (Practice & Quiz, Listen & Learn, Progress)
        composeTestRule.onNodeWithTag("quran_tool_practice_quiz").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_practice_quiz_screen").assertIsDisplayed()
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_tool_listen_learn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_listen_learn_screen").assertIsDisplayed()
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_tool_learning_progress").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_learning_progress_screen").assertIsDisplayed()
    }
}
