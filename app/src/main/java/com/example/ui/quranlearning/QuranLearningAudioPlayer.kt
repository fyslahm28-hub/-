package com.example.ui.quranlearning

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * কুরআন শিক্ষা অডিও প্লেব্যাকের সুস্পষ্ট স্টেট মেশিন।
 * কখনোই প্রকৃত অডিও চালু না হলে Playing স্টেট দেখানো হয় না।
 */
sealed interface QuranAudioPlaybackState {
    data object Idle : QuranAudioPlaybackState

    data class Loading(
        val activeKey: String,
        val statusBangla: String = "অডিও লোড হচ্ছে..."
    ) : QuranAudioPlaybackState

    data class Playing(
        val activeKey: String,
        val canPauseResume: Boolean,
        val sourceLabelBangla: String
    ) : QuranAudioPlaybackState

    data class Paused(
        val activeKey: String,
        val sourceLabelBangla: String
    ) : QuranAudioPlaybackState

    data class Error(
        val activeKey: String,
        val messageBangla: String,
        val canRetry: Boolean = true
    ) : QuranAudioPlaybackState
}

/**
 * অডিও ডাউনলোডের ফলাফল — HTTP স্ট্যাটাস কোড (403, 404, 429) ও নেটওয়ার্ক ত্রুটি আলাদাভাবে শনাক্ত করে।
 */
sealed interface AudioDownloadOutcome {
    data class Success(val audioFile: File, val fromCache: Boolean) : AudioDownloadOutcome
    data class HttpError(val code: Int, val url: String) : AudioDownloadOutcome
    data object NoNetwork : AudioDownloadOutcome
    data object Timeout : AudioDownloadOutcome
    data class IoFailure(val reason: String) : AudioDownloadOutcome
}

/**
 * মিডিয়া ফাইল প্লেব্যাক অ্যাডাপ্টার (বাস্তবে Android MediaPlayer, টেস্টে ডাবল ইনজেক্ট করা যায়)।
 */
interface QuranMediaPlayerEngine {
    val isPlaying: Boolean
    val isPaused: Boolean
    val activeFilePath: String?

    fun playFile(
        file: File,
        onPreparedAndStarted: () -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    )

    fun pause(): Boolean
    fun resume(): Boolean
    fun restartFromBeginning(): Boolean
    fun stopAndRelease()
}

/**
 * আরবি টেক্সট-টু-স্পিচ ইঞ্জিন অ্যাডাপ্টার (বাস্তবে Android TextToSpeech, টেস্টে ডাবল ইনজেক্ট করা যায়)।
 */
interface QuranArabicSpeechEngine {
    suspend fun awaitArabicReady(timeoutMs: Long = 2800L): Boolean
    fun speak(
        sanitizedArabicText: String,
        utteranceId: String,
        onStarted: () -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ): Boolean

    fun stop()
    fun shutdown()
}

/**
 * অডিও ফোকাস ও মিডিয়া ভলিউম অ্যাডাপ্টার।
 */
interface QuranAudioFocusController {
    fun requestFocus(onFocusLost: () -> Unit): Boolean
    fun abandonFocus()
    fun isMediaVolumeMuted(): Boolean
}

/**
 * MP3 ডাউনলোড ও লোকাল ক্যাশ অ্যাডাপ্টার।
 */
interface QuranAudioDownloader {
    fun isNetworkConnected(): Boolean
    fun getCachedAudioFile(cacheKey: String): File?
    suspend fun downloadToCache(url: String, cacheKey: String): AudioDownloadOutcome
}

/**
 * প্রোডাকশন Android MediaPlayer ইমপ্লিমেন্টেশন (AudioAttributes: USAGE_MEDIA + CONTENT_TYPE_SPEECH)।
 */
class AndroidQuranMediaPlayerEngine : QuranMediaPlayerEngine {
    private var mediaPlayer: MediaPlayer? = null
    private var pausedFlag: Boolean = false
    private var currentPath: String? = null

    override val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (_: Throwable) {
            false
        }

    override val isPaused: Boolean
        get() = pausedFlag && mediaPlayer != null

    override val activeFilePath: String?
        get() = currentPath

    override fun playFile(
        file: File,
        onPreparedAndStarted: () -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopAndRelease()
        if (!file.exists() || file.length() <= 0L) {
            onError("অডিও ফাইলটি সঠিকভাবে পাওয়া যায়নি।")
            return
        }
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { preparedPlayer ->
                    try {
                        pausedFlag = false
                        preparedPlayer.start()
                        onPreparedAndStarted()
                    } catch (_: Throwable) {
                        stopAndRelease()
                        onError("অডিও চালু করতে সমস্যা হয়েছে।")
                    }
                }
                setOnCompletionListener {
                    pausedFlag = false
                    onCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    stopAndRelease()
                    onError("অডিও প্লেয়ার ত্রুটি (কোড: $what/$extra)।")
                    true
                }
            }
            mediaPlayer = mp
            currentPath = file.absolutePath
            mp.prepareAsync()
        } catch (_: Throwable) {
            stopAndRelease()
            onError("অডিও ফাইল পড়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।")
        }
    }

    override fun pause(): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            if (mp.isPlaying) {
                mp.pause()
                pausedFlag = true
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun resume(): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            if (pausedFlag) {
                mp.start()
                pausedFlag = false
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun restartFromBeginning(): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            mp.seekTo(0)
            mp.start()
            pausedFlag = false
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun stopAndRelease() {
        val mp = mediaPlayer
        mediaPlayer = null
        pausedFlag = false
        currentPath = null
        if (mp != null) {
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
            } catch (_: Throwable) {
            }
            try {
                mp.reset()
            } catch (_: Throwable) {
            }
            try {
                mp.release()
            } catch (_: Throwable) {
            }
        }
    }
}

/**
 * সংশোধিত Android TextToSpeech ইঞ্জিন:
 * - একাধিক আরবি লোকেল (ar-SA, ar, ar-EG, ar-AE, ar-001) ও ডিভাইসে ইনস্টলকৃত যেকোনো "ar" ভয়েস যাচাই করে।
 * - AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_SPEECH) সেট করে।
 * - UtteranceProgressListener-এর কলব্যাক সবসময় Main Thread-এ পাঠায়।
 */
class AndroidQuranArabicSpeechEngine(
    context: Context
) : QuranArabicSpeechEngine {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initDeferred = CompletableDeferred<Boolean>()

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var arabicAvailable: Boolean = false

    @Volatile
    private var isShutdown: Boolean = false

    @Volatile
    private var activeUtteranceId: String? = null

    @Volatile
    private var onStartCallback: (() -> Unit)? = null

    @Volatile
    private var onDoneCallback: (() -> Unit)? = null

    @Volatile
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        try {
            var created: TextToSpeech? = null
            created = TextToSpeech(appContext) { status ->
                if (isShutdown) {
                    if (!initDeferred.isCompleted) initDeferred.complete(false)
                    return@TextToSpeech
                }
                val engine = created
                if (status == TextToSpeech.SUCCESS && engine != null) {
                    arabicAvailable = configureBestArabicVoice(engine)
                    if (arabicAvailable) {
                        try {
                            engine.setSpeechRate(0.72f)
                            engine.setPitch(1.0f)
                            engine.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    if (utteranceId == activeUtteranceId) {
                                        mainHandler.post { onStartCallback?.invoke() }
                                    }
                                }

                                override fun onDone(utteranceId: String?) {
                                    if (utteranceId == activeUtteranceId) {
                                        mainHandler.post { onDoneCallback?.invoke() }
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    if (utteranceId == activeUtteranceId) {
                                        mainHandler.post {
                                            onErrorCallback?.invoke("আরবি ভয়েস প্লেব্যাক সম্পন্ন হয়নি।")
                                        }
                                    }
                                }

                                override fun onError(utteranceId: String?, errorCode: Int) {
                                    if (utteranceId == activeUtteranceId) {
                                        mainHandler.post {
                                            onErrorCallback?.invoke("আরবি ভয়েস ত্রুটি (কোড: $errorCode)।")
                                        }
                                    }
                                }
                            })
                        } catch (_: Throwable) {
                        }
                    }
                    if (!initDeferred.isCompleted) initDeferred.complete(arabicAvailable)
                } else {
                    arabicAvailable = false
                    if (!initDeferred.isCompleted) initDeferred.complete(false)
                }
            }
            tts = created
        } catch (_: Throwable) {
            arabicAvailable = false
            if (!initDeferred.isCompleted) initDeferred.complete(false)
        }
    }

    private fun configureBestArabicVoice(engine: TextToSpeech): Boolean {
        // ১. ইনস্টলকৃত আরবি ভয়েস স্ক্যান করি (অফলাইন ভয়েসকে অগ্রাধিকার)
        try {
            val arabicVoices = engine.voices
                ?.filter { voice ->
                    voice.locale != null && voice.locale.language.equals("ar", ignoreCase = true)
                }
                .orEmpty()

            if (arabicVoices.isNotEmpty()) {
                val preferred = arabicVoices.firstOrNull { !it.isNetworkConnectionRequired }
                    ?: arabicVoices.first()
                if (engine.setVoice(preferred) == TextToSpeech.SUCCESS) {
                    return true
                }
            }
        } catch (_: Throwable) {
        }

        // ২. ফলাব্যাক হিসেবে পরিচিত আরবি লোকেলগুলো একে একে চেষ্টা করি
        val candidateLocales = listOf(
            Locale.forLanguageTag("ar-SA"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("ar-EG"),
            Locale.forLanguageTag("ar-AE"),
            Locale.forLanguageTag("ar-001")
        )
        for (locale in candidateLocales) {
            try {
                val res = engine.setLanguage(locale)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    return true
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    override suspend fun awaitArabicReady(timeoutMs: Long): Boolean {
        if (isShutdown) return false
        if (initDeferred.isCompleted) return arabicAvailable
        return withTimeoutOrNull(timeoutMs) {
            initDeferred.await()
        } ?: arabicAvailable
    }

    override fun speak(
        sanitizedArabicText: String,
        utteranceId: String,
        onStarted: () -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        val engine = tts ?: return false
        if (isShutdown || !arabicAvailable || sanitizedArabicText.isBlank()) return false

        activeUtteranceId = utteranceId
        onStartCallback = onStarted
        onDoneCallback = onCompleted
        onErrorCallback = onError

        return try {
            val result = engine.speak(
                sanitizedArabicText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
            result == TextToSpeech.SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    override fun stop() {
        activeUtteranceId = null
        onStartCallback = null
        onDoneCallback = null
        onErrorCallback = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    override fun shutdown() {
        isShutdown = true
        stop()
        if (!initDeferred.isCompleted) {
            initDeferred.complete(false)
        }
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
    }
}

/**
 * প্রোডাকশন Android AudioManager ফোকাস ও মিডিয়া ভলিউম কন্ট্রোলার।
 */
class AndroidQuranAudioFocusController(
    context: Context
) : QuranAudioFocusController {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null

    override fun requestFocus(onFocusLost: () -> Unit): Boolean {
        val manager = audioManager ?: return true
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                onFocusLost()
            }
        }
        legacyListener = listener
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(listener)
                    .build()
                focusRequest = req
                manager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    listener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (_: Throwable) {
            true
        }
    }

    override fun abandonFocus() {
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { manager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                legacyListener?.let { manager.abandonAudioFocus(it) }
            }
            legacyListener = null
        } catch (_: Throwable) {
        }
    }

    override fun isMediaVolumeMuted(): Boolean {
        val manager = audioManager ?: return false
        return try {
            manager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * প্রোডাকশন HTTP অডিও ডাউনলোডার ও লোকাল অফলাইন ক্যাশ (context.cacheDir/quran_learning_audio)।
 * কোনো Firebase Read/Write ছাড়াই সম্পূর্ণ লোকাল ক্যাশে অডিও সংরক্ষণ করে।
 */
class AndroidQuranAudioDownloader(
    context: Context
) : QuranAudioDownloader {

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.cacheDir, "quran_learning_audio").apply { mkdirs() }

    override fun isNetworkConnected(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    override fun getCachedAudioFile(cacheKey: String): File? {
        val file = File(cacheDir, "${sanitizeFileName(cacheKey)}.mp3")
        return if (file.exists() && file.length() > MIN_VALID_MP3_BYTES) file else null
    }

    override suspend fun downloadToCache(url: String, cacheKey: String): AudioDownloadOutcome {
        getCachedAudioFile(cacheKey)?.let { cached ->
            return AudioDownloadOutcome.Success(cached, fromCache = true)
        }
        if (!isNetworkConnected()) {
            return AudioDownloadOutcome.NoNetwork
        }

        val targetFile = File(cacheDir, "${sanitizeFileName(cacheKey)}.mp3")
        val tempFile = File(cacheDir, "${sanitizeFileName(cacheKey)}_${System.nanoTime()}.tmp")

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "QawmiKormosongsthanAndroid/1.0")
                setRequestProperty("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.5")
            }

            val statusCode = connection.responseCode
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return AudioDownloadOutcome.HttpError(statusCode, url)
            }

            val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
            if (contentType.contains("text/html")) {
                return AudioDownloadOutcome.HttpError(404, url)
            }

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            if (!tempFile.exists() || tempFile.length() <= MIN_VALID_MP3_BYTES) {
                tempFile.delete()
                return AudioDownloadOutcome.IoFailure("ডাউনলোডকৃত অডিও ফাইল অসম্পূর্ণ।")
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            AudioDownloadOutcome.Success(targetFile, fromCache = false)
        } catch (_: SocketTimeoutException) {
            tempFile.delete()
            AudioDownloadOutcome.Timeout
        } catch (e: IOException) {
            tempFile.delete()
            if (!isNetworkConnected()) {
                AudioDownloadOutcome.NoNetwork
            } else {
                AudioDownloadOutcome.IoFailure(e.message ?: "নেটওয়ার্ক সংযোগ বিচ্ছিন্ন হয়েছে।")
            }
        } catch (t: Throwable) {
            tempFile.delete()
            AudioDownloadOutcome.IoFailure(t.message ?: "অডিও লোড করা যায়নি।")
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    private fun sanitizeFileName(key: String): String =
        key.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    companion object {
        private const val MIN_VALID_MP3_BYTES = 1024L
    }
}

/**
 * কুরআন শিক্ষা বিভাগের নির্ভরযোগ্য একক-ইনস্ট্যান্স অডিও কন্ট্রোলার।
 *
 * বৈশিষ্ট্যসমূহ:
 * - একক সেশন টোকেন (AtomicLong) দিয়ে দ্রুত একাধিকবার Play চাপলেও কখনো একাধিক Audio Instance তৈরি হয় না।
 * - Play / Pause / Resume / Replay / Retry / Stop এবং Completion-এ স্টেট রিসেট।
 * - Audio Focus ও Media Volume যাচাই।
 * - যাচাইকৃত MP3 (প্রাথমিক CDN -> ব্যাকআপ CDN -> অফলাইন লোকাল ক্যাশ) + সংশোধিত আরবি TTS ফলাব্যাক।
 * - স্ক্রিন পরিবর্তন বা পাঠ পরিবর্তনে স্বয়ংক্রিয়ভাবে অডিও বন্ধ ও রিসোর্স মুক্ত করে।
 */
class QuranLearningAudioController(
    private val mediaPlayerEngine: QuranMediaPlayerEngine,
    private val speechEngine: QuranArabicSpeechEngine,
    private val focusController: QuranAudioFocusController,
    private val downloader: QuranAudioDownloader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val sessionCounter = AtomicLong(0L)

    private var activeLoadJob: Job? = null
    private var activeSource: VerifiedQuranAudioSource? = null
    private var isReleased: Boolean = false

    private val _playbackState = MutableStateFlow<QuranAudioPlaybackState>(QuranAudioPlaybackState.Idle)
    val playbackState: StateFlow<QuranAudioPlaybackState> = _playbackState.asStateFlow()

    private val _volumeMutedHint = MutableStateFlow(false)
    val volumeMutedHint: StateFlow<Boolean> = _volumeMutedHint.asStateFlow()

    /**
     * Play বাটন চাপলে:
     * - একই অডিও চালু থাকলে -> Pause (MP3 হলে বিরতি, TTS হলে থামান)।
     * - একই অডিও Paused থাকলে -> Resume (যেখান থেকে থেমেছিল সেখান থেকে চালু)।
     * - অন্য অডিও বা Idle/Error থাকলে -> আগের অডিও থামিয়ে নতুন অডিও চালু।
     */
    fun onPlayOrPauseTapped(source: VerifiedQuranAudioSource) {
        if (isReleased) return
        val current = _playbackState.value
        when {
            current is QuranAudioPlaybackState.Playing && current.activeKey == source.audioKey -> {
                if (current.canPauseResume && mediaPlayerEngine.pause()) {
                    _playbackState.value = QuranAudioPlaybackState.Paused(
                        activeKey = source.audioKey,
                        sourceLabelBangla = current.sourceLabelBangla
                    )
                } else {
                    stop()
                }
            }

            current is QuranAudioPlaybackState.Paused && current.activeKey == source.audioKey -> {
                updateVolumeHint()
                focusController.requestFocus { handleFocusLoss() }
                if (mediaPlayerEngine.resume()) {
                    _playbackState.value = QuranAudioPlaybackState.Playing(
                        activeKey = source.audioKey,
                        canPauseResume = true,
                        sourceLabelBangla = current.sourceLabelBangla
                    )
                } else {
                    startFreshPlayback(source)
                }
            }

            else -> {
                startFreshPlayback(source)
            }
        }
    }

    /**
     * Replay বাটন চাপলে শুরু থেকে পুনরায় বাজাবে (একই প্রস্তুত MP3 থাকলে তাৎক্ষণিক seekTo(0))।
     */
    fun onReplayTapped(source: VerifiedQuranAudioSource) {
        if (isReleased) return
        val current = _playbackState.value
        val samePreparedMp3 = activeSource?.audioKey == source.audioKey &&
            mediaPlayerEngine.activeFilePath != null &&
            (current is QuranAudioPlaybackState.Playing || current is QuranAudioPlaybackState.Paused)

        if (samePreparedMp3) {
            updateVolumeHint()
            focusController.requestFocus { handleFocusLoss() }
            if (mediaPlayerEngine.restartFromBeginning()) {
                _playbackState.value = QuranAudioPlaybackState.Playing(
                    activeKey = source.audioKey,
                    canPauseResume = true,
                    sourceLabelBangla = source.sourceLabelBangla
                )
                return
            }
        }
        startFreshPlayback(source)
    }

    /**
     * Error স্টেট থেকে পুনরায় চেষ্টা (Retry)।
     */
    fun onRetryTapped(source: VerifiedQuranAudioSource) {
        if (isReleased) return
        startFreshPlayback(source)
    }

    /**
     * সম্পূর্ণভাবে অডিও থামায় এবং Idle স্টেটে রিসেট করে।
     */
    fun stop() {
        sessionCounter.incrementAndGet()
        activeLoadJob?.cancel()
        activeLoadJob = null
        mediaPlayerEngine.stopAndRelease()
        speechEngine.stop()
        focusController.abandonFocus()
        _playbackState.value = QuranAudioPlaybackState.Idle
    }

    /**
     * স্ক্রিন Dispose হলে সকল রিসোর্স (MediaPlayer, TTS, AudioFocus, CoroutineScope) নিরাপদে মুক্ত করে।
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        stop()
        speechEngine.shutdown()
        scope.cancel()
    }

    private fun startFreshPlayback(source: VerifiedQuranAudioSource) {
        val token = sessionCounter.incrementAndGet()
        activeLoadJob?.cancel()
        mediaPlayerEngine.stopAndRelease()
        speechEngine.stop()
        focusController.abandonFocus()

        activeSource = source
        updateVolumeHint()
        _playbackState.value = QuranAudioPlaybackState.Loading(
            activeKey = source.audioKey,
            statusBangla = if (source.hasRemoteMp3) "তিলাওয়াত অডিও লোড হচ্ছে..." else "আরবি উচ্চারণ প্রস্তুত হচ্ছে..."
        )

        activeLoadJob = scope.launch {
            // ধাপ ১: যদি যাচাইকৃত আয়াত MP3 থাকে, আগে লোকাল ক্যাশ ও তারপর প্রাথমিক/ব্যাকআপ URL চেষ্টা করি
            if (source.hasRemoteMp3) {
                val mp3Outcome = resolveVerifiedMp3File(source)
                if (!isSessionActive(token)) return@launch

                if (mp3Outcome is AudioDownloadOutcome.Success) {
                    val label = if (mp3Outcome.fromCache) {
                        "${source.sourceLabelBangla} (অফলাইন)"
                    } else {
                        source.sourceLabelBangla
                    }
                    playMp3File(source, mp3Outcome.audioFile, label, token)
                    return@launch
                }

                // MP3 ডাউনলোড ব্যর্থ হলে (অফলাইন/নেটওয়ার্ক/HTTP ত্রুটি) অফলাইন আরবি TTS চেষ্টা করি
                val ttsStarted = tryPlayArabicTts(
                    source = source,
                    label = "অফলাইন আরবি উচ্চারণ",
                    token = token
                )
                if (ttsStarted || !isSessionActive(token)) return@launch

                // উভয়ই ব্যর্থ হলে সুনির্দিষ্ট বাংলা ত্রুটি বার্তা + Retry বাটন দেখাই
                focusController.abandonFocus()
                _playbackState.value = QuranAudioPlaybackState.Error(
                    activeKey = source.audioKey,
                    messageBangla = buildErrorMessage(mp3Outcome, hasRemoteMp3 = true),
                    canRetry = true
                )
                return@launch
            }

            // ধাপ ২: হরফ/হরকত/শব্দ পাঠের জন্য সংশোধিত আরবি ভয়েস ইঞ্জিন ব্যবহার করি
            val ttsStarted = tryPlayArabicTts(
                source = source,
                label = source.sourceLabelBangla,
                token = token
            )
            if (!ttsStarted && isSessionActive(token)) {
                focusController.abandonFocus()
                _playbackState.value = QuranAudioPlaybackState.Error(
                    activeKey = source.audioKey,
                    messageBangla = "ডিভাইসে আরবি ভয়েস (Arabic TTS) পাওয়া যায়নি। ফোনের Settings → Text-to-Speech থেকে আরবি ভয়েস চালু করুন অথবা আবার চেষ্টা করুন।",
                    canRetry = true
                )
            }
        }
    }

    private suspend fun resolveVerifiedMp3File(source: VerifiedQuranAudioSource): AudioDownloadOutcome {
        // প্রথমেই লোকাল ক্যাশ চেক করি — অফলাইনেও তাৎক্ষণিক প্লেব্যাক
        downloader.getCachedAudioFile(source.audioKey)?.let { cached ->
            return AudioDownloadOutcome.Success(cached, fromCache = true)
        }

        val urls = listOfNotNull(source.primaryMp3Url, source.fallbackMp3Url)
        var lastError: AudioDownloadOutcome = AudioDownloadOutcome.NoNetwork

        for (url in urls) {
            val outcome = withContext(ioDispatcher) {
                downloader.downloadToCache(url, source.audioKey)
            }
            if (outcome is AudioDownloadOutcome.Success) {
                return outcome
            }
            lastError = outcome
            // ইন্টারনেট একদম না থাকলে ব্যাকআপ URL-এ সময় নষ্ট না করে সরাসরি বের হই
            if (outcome is AudioDownloadOutcome.NoNetwork) {
                break
            }
        }
        return lastError
    }

    private fun playMp3File(
        source: VerifiedQuranAudioSource,
        file: File,
        sourceLabel: String,
        token: Long
    ) {
        if (!isSessionActive(token)) return
        focusController.requestFocus { handleFocusLoss() }

        mediaPlayerEngine.playFile(
            file = file,
            onPreparedAndStarted = {
                if (isSessionActive(token)) {
                    _playbackState.value = QuranAudioPlaybackState.Playing(
                        activeKey = source.audioKey,
                        canPauseResume = true,
                        sourceLabelBangla = sourceLabel
                    )
                } else {
                    mediaPlayerEngine.stopAndRelease()
                }
            },
            onCompleted = {
                if (isSessionActive(token)) {
                    focusController.abandonFocus()
                    _playbackState.value = QuranAudioPlaybackState.Idle
                }
            },
            onError = { errorMsg ->
                if (isSessionActive(token)) {
                    focusController.abandonFocus()
                    _playbackState.value = QuranAudioPlaybackState.Error(
                        activeKey = source.audioKey,
                        messageBangla = errorMsg,
                        canRetry = true
                    )
                }
            }
        )
    }

    private suspend fun tryPlayArabicTts(
        source: VerifiedQuranAudioSource,
        label: String,
        token: Long
    ): Boolean {
        val ready = withContext(ioDispatcher) {
            speechEngine.awaitArabicReady()
        }
        if (!isSessionActive(token) || !ready) return false

        focusController.requestFocus { handleFocusLoss() }
        val utteranceId = "quran_tts_${source.audioKey}_$token"
        val accepted = speechEngine.speak(
            sanitizedArabicText = source.sanitizedArabicSpeechText,
            utteranceId = utteranceId,
            onStarted = {
                if (isSessionActive(token)) {
                    _playbackState.value = QuranAudioPlaybackState.Playing(
                        activeKey = source.audioKey,
                        canPauseResume = false,
                        sourceLabelBangla = label
                    )
                }
            },
            onCompleted = {
                if (isSessionActive(token)) {
                    focusController.abandonFocus()
                    _playbackState.value = QuranAudioPlaybackState.Idle
                }
            },
            onError = { errMsg ->
                if (isSessionActive(token)) {
                    focusController.abandonFocus()
                    _playbackState.value = QuranAudioPlaybackState.Error(
                        activeKey = source.audioKey,
                        messageBangla = errMsg,
                        canRetry = true
                    )
                }
            }
        )

        if (!accepted) {
            focusController.abandonFocus()
        }
        return accepted
    }

    private fun handleFocusLoss() {
        val current = _playbackState.value
        if (current is QuranAudioPlaybackState.Playing) {
            if (current.canPauseResume && mediaPlayerEngine.pause()) {
                _playbackState.value = QuranAudioPlaybackState.Paused(
                    activeKey = current.activeKey,
                    sourceLabelBangla = current.sourceLabelBangla
                )
            } else {
                stop()
            }
        }
    }

    private fun updateVolumeHint() {
        _volumeMutedHint.value = focusController.isMediaVolumeMuted()
    }

    private fun isSessionActive(token: Long): Boolean =
        !isReleased && sessionCounter.get() == token

    private fun buildErrorMessage(outcome: AudioDownloadOutcome, hasRemoteMp3: Boolean): String {
        return when (outcome) {
            is AudioDownloadOutcome.NoNetwork -> {
                "ইন্টারনেট সংযোগ নেই এবং অফলাইন অডিও ক্যাশ পাওয়া যায়নি। ইন্টারনেট চালু করে আবার চেষ্টা করুন।"
            }

            is AudioDownloadOutcome.Timeout -> {
                "অডিও সার্ভার থেকে সাড়া পেতে দেরি হচ্ছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
            }

            is AudioDownloadOutcome.HttpError -> when (outcome.code) {
                403 -> "অডিও সোর্স অ্যাক্সেস অনুমোদিত হয়নি (HTTP 403)। অনুগ্রহ করে একটু পর আবার চেষ্টা করুন।"
                404 -> "এই পাঠের অডিও ফাইলটি সার্ভারে পাওয়া যায়নি (HTTP 404)।"
                429 -> "সার্ভারে অতিরিক্ত অনুরোধের কারণে অডিও সাময়িকভাবে বন্ধ আছে (HTTP 429)। একটু পর আবার চেষ্টা করুন।"
                else -> "অডিও সার্ভার ত্রুটি (HTTP ${outcome.code})। অনুগ্রহ করে আবার চেষ্টা করুন।"
            }

            is AudioDownloadOutcome.IoFailure -> {
                if (hasRemoteMp3) {
                    "নেটওয়ার্ক ত্রুটির কারণে অডিও লোড করা যায়নি। সংযোগ যাচাই করে আবার চেষ্টা করুন।"
                } else {
                    outcome.reason
                }
            }

            is AudioDownloadOutcome.Success -> "অডিও প্লেব্যাক শুরু করা যায়নি।"
        }
    }

    companion object {
        fun create(context: Context): QuranLearningAudioController {
            return QuranLearningAudioController(
                mediaPlayerEngine = AndroidQuranMediaPlayerEngine(),
                speechEngine = AndroidQuranArabicSpeechEngine(context),
                focusController = AndroidQuranAudioFocusController(context),
                downloader = AndroidQuranAudioDownloader(context)
            )
        }
    }
}
