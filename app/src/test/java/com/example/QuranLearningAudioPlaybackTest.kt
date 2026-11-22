package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.quranlearning.AudioDownloadOutcome
import com.example.ui.quranlearning.QuranArabicSpeechEngine
import com.example.ui.quranlearning.QuranAudioFocusController
import com.example.ui.quranlearning.QuranAudioPlaybackState
import com.example.ui.quranlearning.QuranInteractiveLessonScreen
import com.example.ui.quranlearning.QuranLearningAudioCatalog
import com.example.ui.quranlearning.QuranLearningAudioController
import com.example.ui.quranlearning.QuranLearningCatalog
import com.example.ui.quranlearning.QuranLearningLevel
import com.example.ui.quranlearning.QuranLearningStorageManager
import com.example.ui.quranlearning.QuranMediaPlayerEngine
import com.example.ui.quranlearning.QuranAudioDownloader
import com.example.ui.quranlearning.rememberQuranAcademyPalette
import kotlinx.coroutines.CompletableDeferred
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
@Config(sdk = [34])
class QuranLearningAudioPlaybackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var storage: QuranLearningStorageManager
    private lateinit var dummyMp3File: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = QuranLearningStorageManager.getInstance(context)
        storage.resetForTesting()
        dummyMp3File = File(context.cacheDir, "test_husary_sample.mp3").apply {
            writeBytes(ByteArray(2048) { 0x42 })
        }
    }

    @Test
    fun test01_verifiedCatalog_exact6236Ayahs_validUrls_sanitizer_andNullForUnverified() {
        // 114 surahs summing to exact 6236 ayahs
        assertEquals(114, QuranLearningAudioCatalog.SURAH_AYAH_COUNTS.size)
        assertEquals(6236, QuranLearningAudioCatalog.SURAH_AYAH_COUNTS.sum())

        // Exact global ayah index checks
        assertEquals(1, QuranLearningAudioCatalog.computeGlobalAyahNumber(1, 1))
        assertEquals(262, QuranLearningAudioCatalog.computeGlobalAyahNumber(2, 255))
        assertEquals(6222, QuranLearningAudioCatalog.computeGlobalAyahNumber(112, 1))
        assertEquals(6236, QuranLearningAudioCatalog.computeGlobalAyahNumber(114, 6))
        assertNull(QuranLearningAudioCatalog.computeGlobalAyahNumber(0, 1))
        assertNull(QuranLearningAudioCatalog.computeGlobalAyahNumber(1, 99))

        // Verified CDN URL generation for Bismillah & Ayat al-Kursi
        val bismillah = QuranLearningAudioCatalog.resolveAudioSource(
            lessonId = "primary_m10_l02",
            rawArabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        )
        assertNotNull(bismillah)
        assertTrue(bismillah!!.isVerifiedAyahRecitation)
        assertEquals("https://everyayah.com/data/Husary_64kbps/001001.mp3", bismillah.primaryMp3Url)
        assertEquals("https://cdn.islamic.network/quran/audio/64/ar.husary/1.mp3", bismillah.fallbackMp3Url)

        val kursiy = QuranLearningAudioCatalog.resolveAudioSource(
            lessonId = "advanced_m10_l01",
            rawArabicText = "اللَّهُ لَا إِلٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ"
        )
        assertNotNull(kursiy)
        assertEquals("https://everyayah.com/data/Husary_64kbps/002255.mp3", kursiy!!.primaryMp3Url)
        assertEquals("https://cdn.islamic.network/quran/audio/64/ar.husary/262.mp3", kursiy.fallbackMp3Url)

        // Arabic sanitizer expands isolated letters & removes Bangla parentheses / bullets / tatweel
        val isolated = QuranLearningAudioCatalog.sanitizeArabicForSpeech("ا  ب  ت")
        assertEquals("أَلِف، بَاء، تَاء", isolated)

        val noisy = QuranLearningAudioCatalog.sanitizeArabicForSpeech("قَالَ (২) • السَّمَاءِ (৪-৫) → عَـ ـعَـ")
        assertFalse(noisy.contains("২"))
        assertFalse(noisy.contains("("))
        assertFalse(noisy.contains("•"))
        assertFalse(noisy.contains("ـ"))
        assertTrue(noisy.contains("قَالَ"))
        assertTrue(noisy.contains("السَّمَاءِ"))

        // Null, blank, or non-Arabic returns null so no active Audio Button is rendered
        assertNull(QuranLearningAudioCatalog.resolveAudioSource("x", null))
        assertNull(QuranLearningAudioCatalog.resolveAudioSource("x", "   "))
        assertNull(QuranLearningAudioCatalog.resolveAudioSource("x", "বাংলা লেখা মাত্র 123"))
    }

    @Test
    fun test02_playPauseResumeReplayAndCompletionResetCleanly() {
        val fakePlayer = FakeMediaPlayerEngine()
        val fakeSpeech = FakeArabicSpeechEngine(ready = false)
        val fakeFocus = FakeAudioFocusController()
        val fakeDownloader = FakeAudioDownloader(
            cachedFile = dummyMp3File,
            networkConnected = true
        )
        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = fakeSpeech,
            focusController = fakeFocus,
            downloader = fakeDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        val source = QuranLearningAudioCatalog.resolveAudioSource(
            "primary_m10_l02",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        )!!

        // Play -> Loading -> Playing
        controller.onPlayOrPauseTapped(source)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, fakePlayer.playCallCount)
        assertEquals(1, fakeFocus.requestCount)

        // Pause -> Paused
        controller.onPlayOrPauseTapped(source)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Paused)
        assertEquals(1, fakePlayer.pauseCallCount)

        // Resume -> Playing
        controller.onPlayOrPauseTapped(source)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, fakePlayer.resumeCallCount)

        // Replay -> restarts from beginning cleanly
        controller.onReplayTapped(source)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, fakePlayer.restartCallCount)

        // Completion -> resets to Idle & abandons focus
        fakePlayer.simulateCompletion()
        assertEquals(QuranAudioPlaybackState.Idle, controller.playbackState.value)
        assertTrue(fakeFocus.abandonCount >= 1)

        controller.release()
    }

    @Test
    fun test03_rapidRepeatedPlayClicks_neverCreateDuplicateAudioInstances() {
        val fakePlayer = FakeMediaPlayerEngine()
        val fakeSpeech = FakeArabicSpeechEngine(ready = false)
        val fakeFocus = FakeAudioFocusController()
        val gates = mutableListOf<CompletableDeferred<AudioDownloadOutcome>>()

        val fakeDownloader = FakeAudioDownloader(
            cachedFile = null,
            networkConnected = true,
            customDownload = { _, _ ->
                val gate = CompletableDeferred<AudioDownloadOutcome>()
                gates.add(gate)
                gate.await()
            }
        )
        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = fakeSpeech,
            focusController = fakeFocus,
            downloader = fakeDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        val source = QuranLearningAudioCatalog.resolveAudioSource(
            "primary_m10_l08",
            "قُلْ هُوَ اللَّهُ أَحَدٌ"
        )!!

        // Simulate 5 rapid consecutive Play taps before first download finishes
        repeat(5) {
            controller.onPlayOrPauseTapped(source)
        }
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Loading)

        // Complete all in-flight downloads (stale ones + latest one)
        gates.forEach { it.complete(AudioDownloadOutcome.Success(dummyMp3File, fromCache = false)) }

        // Only the final active session should have created a MediaPlayer instance
        assertEquals(1, fakePlayer.playCallCount)
        assertEquals(1, fakePlayer.activeInstances)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)

        controller.release()
    }

    @Test
    fun test04_http403And404Failover_networkFailureErrorMessage_andRetrySuccess() {
        val fakePlayer = FakeMediaPlayerEngine()
        val fakeSpeech = FakeArabicSpeechEngine(ready = false)
        val fakeFocus = FakeAudioFocusController()
        val attemptedUrls = mutableListOf<String>()

        // Scenario A: Primary returns HTTP 403 -> automatic failover to backup CDN succeeds
        val failoverDownloader = FakeAudioDownloader(
            cachedFile = null,
            networkConnected = true,
            customDownload = { url, _ ->
                attemptedUrls.add(url)
                if (url.contains("everyayah.com")) {
                    AudioDownloadOutcome.HttpError(403, url)
                } else {
                    AudioDownloadOutcome.Success(dummyMp3File, fromCache = false)
                }
            }
        )
        val failoverController = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = fakeSpeech,
            focusController = fakeFocus,
            downloader = failoverDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        val source = QuranLearningAudioCatalog.resolveAudioSource(
            "primary_m10_l06",
            "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ"
        )!!
        failoverController.onPlayOrPauseTapped(source)

        assertEquals(2, attemptedUrls.size)
        assertTrue(attemptedUrls[0].contains("everyayah.com"))
        assertTrue(attemptedUrls[1].contains("cdn.islamic.network"))
        assertTrue(failoverController.playbackState.value is QuranAudioPlaybackState.Playing)
        failoverController.release()

        // Scenario B: Network offline & no cache & no TTS -> clear Bangla Error + Retry recovers
        var onlineNow = false
        val retryDownloader = FakeAudioDownloader(
            cachedFile = null,
            networkConnected = false,
            customDownload = { _, _ ->
                if (!onlineNow) {
                    AudioDownloadOutcome.NoNetwork
                } else {
                    AudioDownloadOutcome.Success(dummyMp3File, fromCache = false)
                }
            }
        )
        val retryPlayer = FakeMediaPlayerEngine()
        val retryController = QuranLearningAudioController(
            mediaPlayerEngine = retryPlayer,
            speechEngine = FakeArabicSpeechEngine(ready = false),
            focusController = FakeAudioFocusController(),
            downloader = retryDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        retryController.onPlayOrPauseTapped(source)
        val errState = retryController.playbackState.value
        assertTrue("Expected Error state on offline failure, got $errState", errState is QuranAudioPlaybackState.Error)
        assertTrue((errState as QuranAudioPlaybackState.Error).canRetry)
        assertTrue(errState.messageBangla.contains("ইন্টারনেট"))

        // Network recovers -> Retry plays successfully
        onlineNow = true
        retryController.onRetryTapped(source)
        assertTrue(retryController.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, retryPlayer.playCallCount)
        retryController.release()
    }

    @Test
    fun test05_switchingLessonsStopsPreviousAudio_andOfflineTtsFallbackWorksForLetters() {
        val fakePlayer = FakeMediaPlayerEngine()
        val fakeSpeech = FakeArabicSpeechEngine(ready = true)
        val fakeFocus = FakeAudioFocusController()
        val ayahLesson = QuranLearningAudioCatalog.resolveAudioSource(
            "primary_m10_l02",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        )!!
        val letterLesson = QuranLearningAudioCatalog.resolveAudioSource(
            "primary_m01_l01",
            "ا  ب  ت"
        )!!

        // Offline: Ayah lesson is cached locally, Letter lesson is NOT cached -> falls back to offline Arabic TTS
        val fakeDownloader = object : QuranAudioDownloader {
            override fun isNetworkConnected(): Boolean = false
            override fun getCachedAudioFile(cacheKey: String): File? =
                if (cacheKey == ayahLesson.audioKey) dummyMp3File else null
            override suspend fun downloadToCache(url: String, cacheKey: String): AudioDownloadOutcome =
                AudioDownloadOutcome.NoNetwork
        }

        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = fakeSpeech,
            focusController = fakeFocus,
            downloader = fakeDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        // Start Ayah lesson (cached MP3 offline)
        controller.onPlayOrPauseTapped(ayahLesson)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)
        assertEquals(1, fakePlayer.activeInstances)

        // Switch immediately to Letter lesson (offline, not cached -> falls back to Arabic TTS)
        controller.onPlayOrPauseTapped(letterLesson)

        // Previous MP3 player must be stopped & released before TTS plays
        assertEquals(0, fakePlayer.activeInstances)
        assertTrue(fakePlayer.stopReleaseCount >= 1)
        assertEquals("أَلِف، بَاء، تَاء", fakeSpeech.lastSpokenText)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)

        // Completion resets to Idle
        fakeSpeech.simulateDone()
        assertEquals(QuranAudioPlaybackState.Idle, controller.playbackState.value)
        controller.release()
    }

    @Test
    fun test08_letterLessonAlifBaTa_playsViaMp3EvenWhenDeviceHasNoArabicTtsPack() {
        val letterSource = QuranLearningAudioCatalog.resolveAudioSource(
            lessonId = "primary_m01_l01",
            rawArabicText = "ا  ب  ت"
        )
        assertNotNull(letterSource)
        assertTrue(letterSource!!.hasRemoteMp3)
        assertTrue(letterSource.primaryMp3Url!!.startsWith("https://translate.google.com/translate_tts"))
        assertTrue(letterSource.fallbackMp3Url!!.startsWith("https://translate.googleapis.com/translate_tts"))

        val fakePlayer = FakeMediaPlayerEngine()
        // Device has NO Arabic TTS pack installed (exact Redmi/Xiaomi scenario)
        val noDeviceTts = FakeArabicSpeechEngine(ready = false)
        val fakeDownloader = FakeAudioDownloader(
            cachedFile = null,
            networkConnected = true,
            customDownload = { _, _ -> AudioDownloadOutcome.Success(dummyMp3File, fromCache = false) }
        )

        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = noDeviceTts,
            focusController = FakeAudioFocusController(),
            downloader = fakeDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        controller.onPlayOrPauseTapped(letterSource)
        val state = controller.playbackState.value
        assertTrue("Expected Playing state via MP3 even without device Arabic TTS, got $state", state is QuranAudioPlaybackState.Playing)
        assertTrue((state as QuranAudioPlaybackState.Playing).canPauseResume)
        assertEquals(1, fakePlayer.playCallCount)

        // Pause & Resume work on letter lesson too
        controller.onPlayOrPauseTapped(letterSource)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Paused)
        controller.onPlayOrPauseTapped(letterSource)
        assertTrue(controller.playbackState.value is QuranAudioPlaybackState.Playing)

        controller.release()
    }

    @Test
    fun test06_composeUi_audioControlsLoadingPlayingPausedErrorRetry_andHidesWhenUnverified() {
        val fakePlayer = FakeMediaPlayerEngine()
        var failDownload = true
        val fakeDownloader = FakeAudioDownloader(
            cachedFile = null,
            networkConnected = true,
            customDownload = { url, _ ->
                if (failDownload) {
                    AudioDownloadOutcome.HttpError(429, url)
                } else {
                    AudioDownloadOutcome.Success(dummyMp3File, fromCache = false)
                }
            }
        )
        val controller = QuranLearningAudioController(
            mediaPlayerEngine = fakePlayer,
            speechEngine = FakeArabicSpeechEngine(ready = false),
            focusController = FakeAudioFocusController(muted = true),
            downloader = fakeDownloader,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

        val bismillahLesson = QuranLearningCatalog.getLessonsForLevel(QuranLearningLevel.PRIMARY)
            .first { it.id == "primary_m10_l02" }

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(412.dp).height(915.dp)) {
                    QuranInteractiveLessonScreen(
                        lesson = bismillahLesson,
                        palette = rememberQuranAcademyPalette(darkTheme = false),
                        storage = storage,
                        onNavigateToNextLesson = {},
                        onRetryMistakes = { _, _ -> },
                        onExitToPath = {},
                        audioControllerOverride = controller
                    )
                }
            }
        }

        // Play fails with HTTP 429 -> Error banner + Retry button shown (never fake Playing)
        composeTestRule.onNodeWithTag("audio_play_pause_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("audio_error_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_retry_btn").performScrollTo().assertIsDisplayed()

        // Recover & tap Retry -> transitions to Playing + shows active indicator & muted hint
        failDownload = false
        composeTestRule.onNodeWithTag("audio_retry_btn").performClick()
        composeTestRule.onNodeWithTag("audio_playing_indicator").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_volume_muted_hint").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_play_pause_btn")
            .assertTextContains("বিরতি (Pause)")

        // Tap Pause -> shows Resume
        composeTestRule.onNodeWithTag("audio_play_pause_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("audio_play_pause_btn")
            .assertTextContains("চালিয়ে যান (Resume)")

        // Advance step -> audio stops cleanly
        composeTestRule.onNodeWithTag("teaching_continue_btn").performScrollTo().performClick()
        assertEquals(QuranAudioPlaybackState.Idle, controller.playbackState.value)

        controller.release()
    }

    @Test
    fun test07_composeUi_unverifiedAudioLesson_doesNotShowActiveAudioButton() {
        val noAudioLesson = QuranLearningCatalog.pendingCmsLessons.first()
        assertNull(noAudioLesson.audioReference)

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(412.dp).height(915.dp)) {
                    QuranInteractiveLessonScreen(
                        lesson = noAudioLesson,
                        palette = rememberQuranAcademyPalette(darkTheme = false),
                        storage = storage,
                        onNavigateToNextLesson = {},
                        onRetryMistakes = { _, _ -> },
                        onExitToPath = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("step_teaching_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("verified_audio_control_bar").assertDoesNotExist()
    }

    // ==================== Deterministic Test Doubles ====================

    private class FakeMediaPlayerEngine : QuranMediaPlayerEngine {
        var playCallCount = 0
        var pauseCallCount = 0
        var resumeCallCount = 0
        var restartCallCount = 0
        var stopReleaseCount = 0
        var activeInstances = 0

        private var playing = false
        private var paused = false
        private var path: String? = null
        private var completionCallback: (() -> Unit)? = null

        override val isPlaying: Boolean get() = playing
        override val isPaused: Boolean get() = paused
        override val activeFilePath: String? get() = path

        override fun playFile(
            file: File,
            onPreparedAndStarted: () -> Unit,
            onCompleted: () -> Unit,
            onError: (String) -> Unit
        ) {
            stopAndRelease()
            playCallCount += 1
            activeInstances = 1
            playing = true
            paused = false
            path = file.absolutePath
            completionCallback = onCompleted
            onPreparedAndStarted()
        }

        override fun pause(): Boolean {
            if (!playing) return false
            playing = false
            paused = true
            pauseCallCount += 1
            return true
        }

        override fun resume(): Boolean {
            if (!paused) return false
            playing = true
            paused = false
            resumeCallCount += 1
            return true
        }

        override fun restartFromBeginning(): Boolean {
            if (path == null) return false
            playing = true
            paused = false
            restartCallCount += 1
            return true
        }

        override fun stopAndRelease() {
            if (activeInstances > 0) {
                stopReleaseCount += 1
            }
            activeInstances = 0
            playing = false
            paused = false
            path = null
            completionCallback = null
        }

        fun simulateCompletion() {
            playing = false
            paused = false
            completionCallback?.invoke()
        }
    }

    private class FakeArabicSpeechEngine(
        private val ready: Boolean
    ) : QuranArabicSpeechEngine {
        var lastSpokenText: String? = null
        private var doneCallback: (() -> Unit)? = null

        override suspend fun awaitArabicReady(timeoutMs: Long): Boolean = ready

        override fun speak(
            sanitizedArabicText: String,
            utteranceId: String,
            onStarted: () -> Unit,
            onCompleted: () -> Unit,
            onError: (String) -> Unit
        ): Boolean {
            if (!ready) return false
            lastSpokenText = sanitizedArabicText
            doneCallback = onCompleted
            onStarted()
            return true
        }

        override fun stop() {
            doneCallback = null
        }

        override fun shutdown() {
            doneCallback = null
        }

        fun simulateDone() {
            doneCallback?.invoke()
        }
    }

    private class FakeAudioFocusController(
        private val muted: Boolean = false
    ) : QuranAudioFocusController {
        var requestCount = 0
        var abandonCount = 0

        override fun requestFocus(onFocusLost: () -> Unit): Boolean {
            requestCount += 1
            return true
        }

        override fun abandonFocus() {
            abandonCount += 1
        }

        override fun isMediaVolumeMuted(): Boolean = muted
    }

    private class FakeAudioDownloader(
        private val cachedFile: File?,
        private val networkConnected: Boolean,
        private val customDownload: (suspend (String, String) -> AudioDownloadOutcome)? = null
    ) : QuranAudioDownloader {
        override fun isNetworkConnected(): Boolean = networkConnected
        override fun getCachedAudioFile(cacheKey: String): File? = cachedFile
        override suspend fun downloadToCache(url: String, cacheKey: String): AudioDownloadOutcome {
            if (customDownload != null) {
                return customDownload.invoke(url, cacheKey)
            }
            return cachedFile?.let { AudioDownloadOutcome.Success(it, fromCache = true) }
                ?: AudioDownloadOutcome.NoNetwork
        }
    }
}
