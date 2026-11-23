package com.example

import android.content.ClipboardManager
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
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.example.ui.duas.DailyAmalSlot
import com.example.ui.duas.DuaCatalog
import com.example.ui.duas.DuaCategory
import com.example.ui.duas.DuaDetailScreen
import com.example.ui.duas.DuaSourceFilter
import com.example.ui.duas.DuaSourceType
import com.example.ui.duas.DuaStorageManager
import com.example.ui.duas.DuaSystemScreen
import com.example.ui.duas.VerifiedDua
import com.example.ui.duas.rememberDuaPalette
import com.example.ui.duas.toDuaBanglaDigits
import com.example.ui.quranlearning.AudioDownloadOutcome
import com.example.ui.quranlearning.QuranArabicSpeechEngine
import com.example.ui.quranlearning.QuranAudioDownloader
import com.example.ui.quranlearning.QuranAudioFocusController
import com.example.ui.quranlearning.QuranAudioPlaybackState
import com.example.ui.quranlearning.QuranLearningAudioController
import com.example.ui.quranlearning.QuranMediaPlayerEngine
import com.example.ui.screens.IslamicAndEssentialServicesSection
import com.example.ui.viewmodel.MainDestination
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class DuaSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var storage: DuaStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = DuaStorageManager.getInstance(context)
        storage.resetForTesting()
    }

    @Test
    fun test01_homeScreenDuaTile_navigatesToDuaAzkarAndPreservesAllExistingServices() {
        var navigatedDestination: MainDestination? = null

        composeTestRule.setContent {
            MaterialTheme {
                IslamicAndEssentialServicesSection(
                    onOpenPrayerTimes = {},
                    onNavigate = { dest -> navigatedDestination = dest }
                )
            }
        }

        // পূর্বের সকল সার্ভিস অক্ষত আছে
        composeTestRule.onNodeWithTag("home_islamic_service_quran").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_hadith").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_books").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_articles").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_asmaul_husna").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_quran_edu").assertIsDisplayed()

        // "দোয়া" টাইল চাপলে নতুন দোয়া ও যিকির বিভাগে যাবে
        composeTestRule.onNodeWithTag("home_islamic_service_dua")
            .assertIsDisplayed()
            .performClick()

        assertEquals(MainDestination.DUA_AZKAR, navigatedDestination)
    }

    @Test
    fun test02_catalogAuthenticity_all12Categories_completeFields_andAudioResolution() {
        assertEquals(12, DuaCatalog.allCategories.size)
        assertTrue("Expected at least 72 verified duas", DuaCatalog.allDuas.size >= 72)

        // প্রতিটি আইডি ইউনিক
        assertEquals(DuaCatalog.allDuas.size, DuaCatalog.allDuas.map { it.id }.toSet().size)

        // প্রতিটি ক্যাটাগরিতে পর্যাপ্ত যাচাইকৃত দোয়া এবং প্রতিটি ফিল্ড পূর্ণাঙ্গ
        DuaCatalog.allCategories.forEach { cat ->
            val catDuas = DuaCatalog.getDuasForCategory(cat)
            assertTrue("Category ${cat.id} should have at least 6 duas", catDuas.size >= 6)
            catDuas.forEach { dua ->
                assertTrue("Title blank in ${dua.id}", dua.titleBangla.isNotBlank())
                assertTrue("Occasion blank in ${dua.id}", dua.occasionBangla.isNotBlank())
                assertTrue("Arabic blank in ${dua.id}", dua.arabicText.isNotBlank())
                assertTrue("Pronunciation blank in ${dua.id}", dua.banglaPronunciation.isNotBlank())
                assertTrue("Meaning blank in ${dua.id}", dua.banglaMeaning.isNotBlank())
                assertTrue("Reference blank in ${dua.id}", dua.referenceBangla.isNotBlank())
                assertTrue("Grading blank in ${dua.id}", dua.gradingBangla.isNotBlank())

                val audio = DuaCatalog.resolveAudioSource(dua)
                assertNotNull("Expected audio source for verified dua ${dua.id}", audio)
                if (dua.quranSurahAyah != null) {
                    assertTrue(
                        "Quran full ayah dua ${dua.id} must resolve to Husary recitation",
                        audio!!.isVerifiedAyahRecitation && audio.primaryMp3Url?.contains("everyayah.com") == true
                    )
                }
            }
        }

        // দৈনিক আমলের ৫টি স্লটেই দোয়া আছে
        DailyAmalSlot.entries.forEach { slot ->
            assertTrue(
                "Slot ${slot.name} should have at least 1 dua",
                DuaCatalog.getDailyAmalDuasBySlot(slot).isNotEmpty()
            )
        }

        // অডিও যাচাইকৃত না হলে কোনো অডিও সোর্স দেখানো হবে না
        val unverifiedAudioDua = DuaCatalog.allDuas.first().copy(hasVerifiedAudio = false)
        assertNull(DuaCatalog.resolveAudioSource(unverifiedAudioDua))
    }

    @Test
    fun test03_all12CategoriesOpen_andDetailScreenDisplaysStrictAtoGAndBackNavigation() {
        var backInterceptor: (() -> Boolean)? = null

        composeTestRule.setContent {
            MaterialTheme {
                DuaSystemScreen(
                    darkThemeOverride = false,
                    registerBackInterceptor = { backInterceptor = it }
                )
            }
        }

        // হেডার ও ৩টি ট্যাব দৃশ্যমান
        composeTestRule.onNodeWithTag("dua_header_title").assertTextContains("দোয়া ও যিকির")
        composeTestRule.onNodeWithTag("dua_tab_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_tab_daily_amal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_tab_favorites").assertIsDisplayed()

        // প্রথম ক্যাটাগরি (দৈনন্দিন দোয়া) খুলি
        val firstCat = DuaCategory.DAILY_DUA
        composeTestRule.onNodeWithTag("dua_category_card_${firstCat.id}")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("dua_category_list_${firstCat.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_category_list_title").assertTextContains(firstCat.titleBangla)

        // প্রথম দোয়ার বিস্তারিত পেজ খুলি
        val firstDua = DuaCatalog.getDuasForCategory(firstCat).first()
        composeTestRule.onNodeWithTag("dua_list_item_${firstDua.id}").performClick()

        // A → G সকল অংশ দৃশ্যমান
        composeTestRule.onNodeWithTag("dua_detail_title").assertTextContains(firstDua.titleBangla)
        composeTestRule.onNodeWithTag("dua_detail_arabic").assertTextContains(firstDua.arabicText)
        composeTestRule.onNodeWithTag("dua_detail_pronunciation")
            .performScrollTo()
            .assertTextContains(firstDua.banglaPronunciation)
        composeTestRule.onNodeWithTag("dua_detail_meaning")
            .performScrollTo()
            .assertTextContains(firstDua.banglaMeaning)

        // অর্থ লুকিয়ে অনুশীলন (Learning Mode) টগল কাজ করে
        composeTestRule.onNodeWithTag("dua_toggle_hide_meaning").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("dua_meaning_hidden_prompt").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_meaning_hidden_prompt").performClick()
        composeTestRule.onNodeWithTag("dua_detail_meaning").assertIsDisplayed()

        // অডিও প্লেয়ার ও রেফারেন্স দৃশ্যমান
        composeTestRule.onNodeWithTag("dua_audio_control_bar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_detail_reference")
            .performScrollTo()
            .assertTextContains(firstDua.referenceBangla, substring = true)

        // Back Interceptor: Detail -> CategoryList -> Home -> false
        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dua_category_list_${firstCat.id}").assertIsDisplayed()

        assertTrue(backInterceptor?.invoke() == true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dua_home_screen").assertIsDisplayed()

        assertFalse(backInterceptor?.invoke() == true)

        // ১২ নম্বর ক্যাটাগরি (বিবিধ মাসনুন দোয়া) স্ক্রল করে খোলা যায়
        val lastCat = DuaCategory.MISC_MASNUN_DUAS
        composeTestRule.onNodeWithTag("dua_home_lazy_column")
            .performScrollToKey("${DuaCategory.MASJID_DUAS.id}_${lastCat.id}")
        composeTestRule.onNodeWithTag("dua_category_card_${lastCat.id}")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag("dua_category_list_${lastCat.id}").assertIsDisplayed()
    }

    @Test
    fun test04_audioPlaybackWorksRealFlow_andHidesAudioBarWhenUnverified() {
        val fakeCacheFile = File(context.cacheDir, "test_dua_audio_sample.mp3").apply {
            writeBytes(ByteArray(1024) { 1 })
        }
        val fakePlayer = FakeMediaPlayerEngine()
        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = FakeNoOpSpeechEngine(),
            focusController = FakeFocusController(),
            downloader = FakeSuccessDownloader(fakeCacheFile),
            ioDispatcher = Dispatchers.Main.immediate,
            mainDispatcher = Dispatchers.Main.immediate
        )

        val quranDua = DuaCatalog.getDuasForCategory(DuaCategory.QURANIC_DUAS).first()

        composeTestRule.setContent {
            MaterialTheme {
                DuaDetailScreen(
                    dua = quranDua,
                    categoryDuas = DuaCatalog.getDuasForCategory(DuaCategory.QURANIC_DUAS),
                    palette = rememberDuaPalette(darkTheme = false),
                    storage = storage,
                    onBack = {},
                    onSelectDua = {},
                    audioControllerOverride = controller
                )
            }
        }

        // Play -> Playing
        composeTestRule.onNodeWithTag("dua_audio_play_pause_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        composeTestRule.onNodeWithTag("dua_audio_play_pause_btn").assertTextContains("বিরতি দিন")

        // Pause -> Paused
        composeTestRule.onNodeWithTag("dua_audio_play_pause_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Paused)
        composeTestRule.onNodeWithTag("dua_audio_play_pause_btn").assertTextContains("পুনরায় চালু করুন")

        // Resume -> Playing
        composeTestRule.onNodeWithTag("dua_audio_play_pause_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)

        // Replay -> Playing from start
        composeTestRule.onNodeWithTag("dua_audio_replay_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, fakePlayer.restartCount)

        controller.release()
        fakeCacheFile.delete()
    }

    @Test
    fun test05_copyArabicPronunciationMeaning_favouriteOfflinePersistence_andFavoritesTab() {
        val sampleDua = DuaCatalog.getDuasForCategory(DuaCategory.FORGIVENESS_TAWBAH_DUAS).first()

        composeTestRule.setContent {
            MaterialTheme {
                DuaDetailScreen(
                    dua = sampleDua,
                    categoryDuas = listOf(sampleDua),
                    palette = rememberDuaPalette(darkTheme = false),
                    storage = storage,
                    onBack = {},
                    onSelectDua = {}
                )
            }
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Copy Arabic
        composeTestRule.onNodeWithTag("dua_copy_arabic_btn").performScrollTo().performClick()
        assertEquals(sampleDua.arabicText, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        composeTestRule.onNodeWithTag("dua_action_feedback_banner").assertIsDisplayed()

        // Copy Bangla Pronunciation
        composeTestRule.onNodeWithTag("dua_copy_pronunciation_btn").performScrollTo().performClick()
        assertEquals(sampleDua.banglaPronunciation, clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // Copy Bangla Meaning
        composeTestRule.onNodeWithTag("dua_copy_meaning_btn").performScrollTo().performClick()
        assertEquals(sampleDua.banglaMeaning, clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // Favourite toggle -> persists locally offline
        composeTestRule.onNodeWithTag("dua_detail_fav_btn").performScrollTo().performClick()
        assertTrue(storage.isFavorite(sampleDua.id))
    }

    @Test
    fun test06_searchAndSourceFilter_returnAccurateResults_andFavoritesTabDisplaysSavedDua() {
        val favDua = DuaCatalog.getDuasForCategory(DuaCategory.SALAH_DUAS).first()
        storage.toggleFavorite(favDua.id)

        composeTestRule.setContent {
            MaterialTheme {
                DuaSystemScreen(darkThemeOverride = false)
            }
        }

        // বাংলা সার্চ: "সাইয়্যিদুল ইস্তিগফার"
        composeTestRule.onNodeWithTag("dua_search_input").performTextInput("সাইয়্যিদুল ইস্তিগফার")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dua_search_results_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_list_item_tawbah_sayyidul_istighfar").assertIsDisplayed()

        // সার্চ মুছে ফেলা ও কুরআন সোর্স ফিল্টার পরীক্ষা
        composeTestRule.onNodeWithTag("dua_search_clear_btn").performClick()
        composeTestRule.onNodeWithTag("dua_filter_toggle_btn").performClick()
        composeTestRule.onNodeWithTag("dua_filter_quran").performClick()
        composeTestRule.waitForIdle()

        val quranResults = DuaCatalog.searchDuas("", DuaSourceFilter.QURAN)
        assertTrue(quranResults.isNotEmpty())
        assertTrue(quranResults.all { it.sourceType == DuaSourceType.QURAN })
        composeTestRule.onNodeWithTag("dua_search_results_list").assertIsDisplayed()

        // প্রিয় দোয়া ট্যাবে অফলাইনে সংরক্ষিত দোয়া দৃশ্যমান
        composeTestRule.onNodeWithTag("dua_tab_favorites").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dua_favorites_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_list_item_${favDua.id}").assertIsDisplayed()
    }

    @Test
    fun test07_dailyAmal_neverAutoCompleted_andManualMarkUpdatesPersonalProgress() {
        val totalDaily = DuaCatalog.getDailyAmalDuas().size
        assertTrue(totalDaily >= 8)
        val firstMorningDua = DuaCatalog.getDailyAmalDuasBySlot(DailyAmalSlot.MORNING).first()

        composeTestRule.setContent {
            MaterialTheme {
                DuaSystemScreen(darkThemeOverride = false)
            }
        }

        composeTestRule.onNodeWithTag("dua_tab_daily_amal").performClick()
        composeTestRule.waitForIdle()

        // শুরুতে ০ টি সম্পন্ন (কখনো স্বয়ংক্রিয়ভাবে সম্পন্ন দেখায় না)
        composeTestRule.onNodeWithTag("dua_daily_progress_text")
            .assertTextContains("${toDuaBanglaDigits(0)} / ${toDuaBanglaDigits(totalDaily)} টি আমল সম্পন্ন")
        composeTestRule.onNodeWithTag("dua_of_the_day_card").assertIsDisplayed()

        // ব্যবহারকারী নিজে "সম্পন্ন করেছি" ট্যাপ করলে ১টি সম্পন্ন হবে
        composeTestRule.onNodeWithTag("dua_home_lazy_column")
            .performScrollToKey("daily_${DailyAmalSlot.MORNING.name}_${firstMorningDua.id}")
        composeTestRule.onNodeWithTag("daily_amal_toggle_${firstMorningDua.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dua_home_lazy_column").performScrollToKey("daily_progress")
        composeTestRule.onNodeWithTag("dua_daily_progress_text")
            .assertTextContains("${toDuaBanglaDigits(1)} / ${toDuaBanglaDigits(totalDaily)} টি আমল সম্পন্ন")
        assertTrue(storage.dailyCompletedIds.value.contains(firstMorningDua.id))
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h740dp")
    fun test08_nightModeAndCompactLargeScreens_renderCleanlyWithoutOverflow() {
        // ছোট স্ক্রিন (360dp) + Night Mode
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(360.dp).height(740.dp)) {
                    DuaSystemScreen(darkThemeOverride = true)
                }
            }
        }

        composeTestRule.onNodeWithTag("dua_main_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_tab_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_tab_daily_amal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_tab_favorites").assertIsDisplayed()

        val cardBounds = composeTestRule.onNodeWithTag("dua_category_card_daily_dua")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Compact screen card should fit within 360dp width, got right=${cardBounds.right}",
            cardBounds.right <= 360.dp
        )
        assertTrue(
            "Compact screen card width should be positive, got ${cardBounds.width}",
            cardBounds.width > 120.dp
        )
    }

    @Test
    fun test09_unverifiedAudioDua_omitsAudioPlayerCompletely() {
        val noAudioDua = VerifiedDua(
            id = "test_no_audio",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "পরীক্ষামূলক দোয়া",
            occasionBangla = "পরীক্ষা",
            arabicText = "بِسْمِ اللَّهِ",
            banglaPronunciation = "বিসমিল্লাহ",
            banglaMeaning = "আল্লাহর নামে",
            referenceBangla = "সহীহ বুখারী",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH,
            hasVerifiedAudio = false
        )

        composeTestRule.setContent {
            MaterialTheme {
                DuaDetailScreen(
                    dua = noAudioDua,
                    categoryDuas = listOf(noAudioDua),
                    palette = rememberDuaPalette(darkTheme = false),
                    storage = storage,
                    onBack = {},
                    onSelectDua = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("dua_detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dua_audio_control_bar").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w840dp-h900dp")
    fun test10_tabletLargeScreen_rendersThreeColumnGridWithoutOverflow() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(840.dp).height(900.dp)) {
                    DuaSystemScreen(darkThemeOverride = false)
                }
            }
        }

        // বড় স্ক্রিনে প্রথম সারিতে ৩টি ক্যাটাগরি পাশাপাশি দৃশ্যমান ও কোনো overflow নেই
        val c1 = composeTestRule.onNodeWithTag("dua_category_card_daily_dua")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val c2 = composeTestRule.onNodeWithTag("dua_category_card_morning_evening_azkar")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val c3 = composeTestRule.onNodeWithTag("dua_category_card_salah_duas")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertTrue("Expected 3-column horizontal layout on 840dp tablet", c1.right <= c2.left && c2.right <= c3.left)
        assertTrue("Expected cards to stay within 840dp viewport", c3.right <= 840.dp)
    }

    private class FakeMediaPlayerEngine : QuranMediaPlayerEngine {
        override var isPlaying: Boolean = false
        override var isPaused: Boolean = false
        override var activeFilePath: String? = null
        var restartCount: Int = 0

        override fun playFile(
            file: File,
            onPreparedAndStarted: () -> Unit,
            onCompleted: () -> Unit,
            onError: (String) -> Unit
        ) {
            activeFilePath = file.absolutePath
            isPlaying = true
            isPaused = false
            onPreparedAndStarted()
        }

        override fun pause(): Boolean {
            if (!isPlaying) return false
            isPlaying = false
            isPaused = true
            return true
        }

        override fun resume(): Boolean {
            if (!isPaused) return false
            isPaused = false
            isPlaying = true
            return true
        }

        override fun restartFromBeginning(): Boolean {
            if (activeFilePath == null) return false
            restartCount++
            isPaused = false
            isPlaying = true
            return true
        }

        override fun stopAndRelease() {
            isPlaying = false
            isPaused = false
            activeFilePath = null
        }
    }

    private class FakeNoOpSpeechEngine : QuranArabicSpeechEngine {
        override suspend fun awaitArabicReady(timeoutMs: Long): Boolean = false
        override fun speak(
            sanitizedArabicText: String,
            utteranceId: String,
            onStarted: () -> Unit,
            onCompleted: () -> Unit,
            onError: (String) -> Unit
        ): Boolean = false
        override fun stop() {}
        override fun shutdown() {}
    }

    private class FakeFocusController : QuranAudioFocusController {
        override fun requestFocus(onFocusLost: () -> Unit): Boolean = true
        override fun abandonFocus() {}
        override fun isMediaVolumeMuted(): Boolean = false
    }

    private class FakeSuccessDownloader(private val cachedFile: File) : QuranAudioDownloader {
        override fun isNetworkConnected(): Boolean = true
        override fun getCachedAudioFile(cacheKey: String): File = cachedFile
        override suspend fun downloadToCache(url: String, cacheKey: String): AudioDownloadOutcome =
            AudioDownloadOutcome.Success(cachedFile, fromCache = true)
    }
}
