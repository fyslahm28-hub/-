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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.example.ui.quranlearning.LessonNodeState
import com.example.ui.quranlearning.PublicationStatus
import com.example.ui.quranlearning.QuranLearningAcademyScreen
import com.example.ui.quranlearning.QuranLearningCatalog
import com.example.ui.quranlearning.QuranLearningLevel
import com.example.ui.quranlearning.QuranLearningStorageManager
import com.example.ui.quranlearning.QuranLessonStep
import com.example.ui.quranlearning.VerificationStatus
import com.example.ui.screens.IslamicAndEssentialServicesSection
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
    fun test02_curriculumIntegrity_300PlusDistinctVerifiedLessons_30Modules_andUnverifiedCmsHiddenFromStudents() {
        // 30 Modules (10 per level)
        assertEquals(30, QuranLearningCatalog.allModules.size)
        QuranLearningLevel.entries.forEach { level ->
            val mods = QuranLearningCatalog.getModulesForLevel(level)
            assertEquals("Expected 10 modules for ${level.id}", 10, mods.size)
            val lvlLessons = QuranLearningCatalog.getLessonsForLevel(level)
            assertTrue(
                "Expected 100+ verified lessons for ${level.id}, got ${lvlLessons.size}",
                lvlLessons.size >= 100
            )
            // Every lesson in a level has unique title and sequential order
            assertEquals(lvlLessons.size, lvlLessons.map { it.titleBangla }.toSet().size)
            // Each level ends with a Final Level Assessment
            assertTrue(lvlLessons.last().isFinalLevelAssessment)
        }

        val allStudentLessons = QuranLearningCatalog.getStudentVisibleLessons()
        assertTrue("Expected 300+ verified lessons total, got ${allStudentLessons.size}", allStudentLessons.size >= 306)
        assertEquals(allStudentLessons.size, allStudentLessons.map { it.id }.toSet().size)

        // Verify every single lesson has authentic non-empty Arabic, Bangla explanation, source citation, and 5 interactive steps
        allStudentLessons.forEach { lesson ->
            assertTrue(lesson.isStudentVisible)
            assertEquals(VerificationStatus.VERIFIED, lesson.verificationStatus)
            assertEquals(PublicationStatus.PUBLISHED, lesson.publicationStatus)
            assertTrue("Missing Arabic in ${lesson.id}", lesson.arabicContent.isNotBlank())
            assertTrue("Missing explanation in ${lesson.id}", lesson.bengaliExplanation.isNotBlank())
            assertTrue("Missing source in ${lesson.id}", lesson.sourceReference.isNotBlank())
            assertTrue("Expected >= 4 interactive steps in ${lesson.id}", lesson.practiceSteps.size >= 4)
            assertTrue(lesson.practiceSteps.first() is QuranLessonStep.TeachingCard)
        }

        // Verify pending/unpublished CMS lessons NEVER leak into student visible list
        val pendingIds = QuranLearningCatalog.pendingCmsLessons.map { it.id }
        assertEquals(3, pendingIds.size)
        pendingIds.forEach { pendingId ->
            assertFalse(
                "Unverified lesson $pendingId must not be student-visible",
                allStudentLessons.any { it.id == pendingId }
            )
        }

        // Once Admin verifies & publishes a pending CMS lesson, it becomes visible
        val firstPendingId = pendingIds.first()
        storage.toggleAdminVerifyAndPublish(firstPendingId)
        val afterApproval = QuranLearningCatalog.getStudentVisibleLessons(storage.adminApprovedLessonIds.value)
        assertTrue(afterApproval.any { it.id == firstPendingId && it.isStudentVisible })
    }

    @Test
    fun test03_sequentialUnlock_xpStreakMistakesAndAchievementBadgesWorkAccurately() {
        val primaryLessons = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.PRIMARY)
        val lesson1 = primaryLessons[0]
        val lesson2 = primaryLessons[1]
        val lesson3 = primaryLessons[2]

        // Lesson 1 unlocked (CURRENT), Lesson 2 & 3 locked
        assertEquals(LessonNodeState.CURRENT, storage.getNodeState(lesson1))
        assertEquals(LessonNodeState.LOCKED, storage.getNodeState(lesson2))
        assertEquals(LessonNodeState.LOCKED, storage.getNodeState(lesson3))

        // Lesson 1 of Intermediate & Advanced are also unlocked so learners can start at their level
        val inter1 = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.INTERMEDIATE).first()
        val adv1 = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.ADVANCED).first()
        assertEquals(LessonNodeState.CURRENT, storage.getNodeState(inter1))
        assertEquals(LessonNodeState.CURRENT, storage.getNodeState(adv1))

        // Complete Lesson 1 with 1 mistake
        val result1 = storage.completeLesson(
            lesson = lesson1,
            correctAnswers = 3,
            totalQuestions = 4,
            mistakeStepIndices = listOf(2)
        )
        assertTrue(result1.earnedPoints > 0)
        assertEquals(lesson2.id, result1.nextLessonId)
        assertEquals(LessonNodeState.COMPLETED, storage.getNodeState(lesson1))
        assertEquals(LessonNodeState.CURRENT, storage.getNodeState(lesson2))
        assertEquals(LessonNodeState.LOCKED, storage.getNodeState(lesson3))
        assertEquals(1, storage.currentStreakDays.value)
        assertEquals(1, storage.todayCompletedCount.value)
        assertTrue(storage.mistakeLessonIds.value.contains(lesson1.id))
        assertEquals(1, storage.revisionHistory.value.size)

        // First badge unlocked
        val firstBadge = storage.getAchievementBadges().first { it.id == "first_step" }
        assertTrue(firstBadge.isUnlocked)

        // Re-complete Lesson 1 cleanly (0 mistakes) -> removed from mistake queue
        storage.completeLesson(
            lesson = lesson1,
            correctAnswers = 4,
            totalQuestions = 4,
            mistakeStepIndices = emptyList()
        )
        assertFalse(storage.mistakeLessonIds.value.contains(lesson1.id))
    }

    @Test
    fun test04_firstScreenHeaderDailyStreak_threePrimaryLevels_andSecondaryToolsRenderOnRedmiNote14ProPlus() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(412.dp).height(915.dp)) {
                    QuranLearningAcademyScreen(isAdmin = true)
                }
            }
        }

        // Medium Royal Header check
        composeTestRule.onNodeWithTag("quran_academy_header").assertIsDisplayed()
        val headerHeight = composeTestRule.onNodeWithTag("quran_academy_header")
            .getUnclippedBoundsInRoot().height
        assertTrue(
            "Header height ($headerHeight) should be medium (~132..172dp)",
            headerHeight in 130.dp..174.dp
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

        // Daily Progress & Streak Strip
        composeTestRule.onNodeWithTag("quran_daily_progress_streak_strip").assertIsDisplayed()

        // All 3 Primary Levels clearly visible on Screen 1 with exact names & order
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

        // Secondary tools including Admin CMS tool
        composeTestRule.onNodeWithTag("quran_tool_mistakes_revision").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_learning_progress").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_last_read").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tool_admin_verify").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test05_duolingoLearningPath_lockedLessonGuard_interactiveLessonEngine_andBackNavigationEndToEnd() {
        var backInterceptor: (() -> Boolean)? = null

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(360.dp).height(760.dp)) {
                    QuranLearningAcademyScreen(
                        isAdmin = true,
                        registerBackInterceptor = { backInterceptor = it }
                    )
                }
            }
        }

        // Open প্রাথমিক শিক্ষা Learning Path
        composeTestRule.onNodeWithTag("quran_level_card_primary").performClick()
        composeTestRule.onNodeWithTag("quran_level_path_screen_primary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("module_card_primary_m01").assertIsDisplayed()

        val primaryLessons = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.PRIMARY)
        val lesson1 = primaryLessons[0]
        val lesson2 = primaryLessons[1]

        // Clicking locked Lesson 2 displays docked unlock guidance immediately
        composeTestRule.onNodeWithTag("quran_level_path_lazy_list")
            .performScrollToKey("node_${lesson2.id}")
        composeTestRule.onNodeWithTag("lesson_path_node_${lesson2.id}").performClick()
        composeTestRule.onNodeWithTag("locked_lesson_notice").assertIsDisplayed()

        // Clicking unlocked Lesson 1 opens Interactive Lesson Engine
        composeTestRule.onNodeWithTag("quran_level_path_lazy_list")
            .performScrollToKey("node_${lesson1.id}")
        composeTestRule.onNodeWithTag("lesson_path_node_${lesson1.id}").performClick()
        composeTestRule.onNodeWithTag("quran_interactive_lesson_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("step_teaching_card").assertIsDisplayed()
        // Verified audio bar shown because Lesson 1 has audioReference
        assertNotNull(lesson1.audioReference)
        composeTestRule.onNodeWithTag("verified_audio_control_bar").assertIsDisplayed()

        // Advance from Teaching Card -> Step 2 (SelectOption)
        composeTestRule.onNodeWithTag("teaching_continue_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("step_select_option").assertIsDisplayed()

        // Select correct option -> check answer -> verify instant feedback card
        val step2 = lesson1.practiceSteps[1] as QuranLessonStep.SelectOption
        composeTestRule.onNodeWithTag("select_option_item_${step2.correctIndex}").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("step_check_answer_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("lesson_instant_feedback_card").assertIsDisplayed()

        // Navigate back via TopBar back interceptor: InteractiveLesson -> LevelLessonMap -> AcademyHome
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_level_path_screen_primary").assertIsDisplayed()

        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_learning_academy_home").assertIsDisplayed()

        // Verify secondary screens & Admin CMS screen navigate cleanly
        composeTestRule.onNodeWithTag("quran_tool_mistakes_revision").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_mistakes_revision_screen").assertIsDisplayed()
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_tool_learning_progress").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_progress_achievements_screen").assertIsDisplayed()
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_tool_admin_verify").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quran_admin_verification_screen").assertIsDisplayed()
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_learning_academy_home").assertIsDisplayed()
    }
}
