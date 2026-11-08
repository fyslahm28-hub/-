package com.example.ui.quran

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.CookieHandler
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class DrivePdfStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY_OFFLINE,
    NEEDS_DRIVE_ACCESS,
    ERROR
}

enum class QuranMushafRenderSource {
    DRIVE_PDF_MUSHAF,
    DIGITAL_15LINE_MUSHAF
}

data class DrivePdfState(
    val status: DrivePdfStatus = DrivePdfStatus.NOT_DOWNLOADED,
    val driveFileId: String = HafeziDrivePdfManager.DEFAULT_DRIVE_FILE_ID,
    val driveViewUrl: String = HafeziDrivePdfManager.DEFAULT_DRIVE_VIEW_URL,
    val drivePreviewUrl: String = HafeziDrivePdfManager.DEFAULT_DRIVE_PREVIEW_URL,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val pdfPageCount: Int = 0,
    val pageOffset: Int = 0,
    val renderSource: QuranMushafRenderSource = QuranMushafRenderSource.DRIVE_PDF_MUSHAF,
    val statusMessageBangla: String = "গুগল ড্রাইভ হাফেজি কুরআন শরীফ (PDF) সংযুক্ত রয়েছে"
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val downloadedMbText: String
        get() {
            val mb = downloadedBytes / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }
}

/**
 * Manages the User's Google Drive Quran Sharif PDF (`19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2`),
 * including:
 * 1. Direct & Large-File-Confirm Download from Google Drive to Offline App Storage
 * 2. Local Device PDF Import (`content://` URI copy to Offline App Storage)
 * 3. High-Resolution Native `android.graphics.pdf.PdfRenderer` Page Rendering with LruCache
 * 4. In-App Embedded Google Drive PDF Viewer (`WebView` preview & Drive app launcher)
 */
object HafeziDrivePdfManager {

    const val DEFAULT_DRIVE_FILE_ID = "19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2"
    const val DEFAULT_DRIVE_VIEW_URL =
        "https://drive.google.com/file/d/19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2/view?usp=drivesdk"
    const val DEFAULT_DRIVE_PREVIEW_URL =
        "https://drive.google.com/file/d/19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2/preview"

    private const val PREFS_NAME = "qawmi_hafezi_drive_pdf_prefs_v1"
    private const val KEY_PDF_PAGE_COUNT = "pdf_page_count"
    private const val KEY_PDF_PAGE_OFFSET = "pdf_page_offset"
    private const val KEY_RENDER_SOURCE = "mushaf_render_source"
    private const val PDF_FILE_NAME = "hafezi_quran_drive_19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2.pdf"

    private val _stateFlow = MutableStateFlow(DrivePdfState())
    val stateFlow: StateFlow<DrivePdfState> = _stateFlow.asStateFlow()

    private var downloadJob: Job? = null
    private val pdfMutex = Mutex()
    private var cachedDescriptor: ParcelFileDescriptor? = null
    private var cachedRenderer: PdfRenderer? = null
    private var cachedFileLength: Long = -1L

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun getOfflinePdfFile(context: Context): File {
        val dir = HafeziQuranStorageManager.getOfflineMushafDir(context)
        return File(dir, PDF_FILE_NAME)
    }

    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedOffset = prefs.getInt(KEY_PDF_PAGE_OFFSET, 0)
        val savedSourceStr = prefs.getString(
            KEY_RENDER_SOURCE,
            QuranMushafRenderSource.DRIVE_PDF_MUSHAF.name
        )
        val savedSource = runCatching {
            QuranMushafRenderSource.valueOf(savedSourceStr ?: "")
        }.getOrDefault(QuranMushafRenderSource.DRIVE_PDF_MUSHAF)

        val pdfFile = getOfflinePdfFile(appContext)
        if (pdfFile.exists() && pdfFile.length() > 1024L && isValidPdfHeader(pdfFile)) {
            val pageCount = inspectPdfPageCount(pdfFile).coerceAtLeast(1)
            prefs.edit().putInt(KEY_PDF_PAGE_COUNT, pageCount).apply()
            _stateFlow.value = DrivePdfState(
                status = DrivePdfStatus.READY_OFFLINE,
                downloadedBytes = pdfFile.length(),
                totalBytes = pdfFile.length(),
                pdfPageCount = pageCount,
                pageOffset = savedOffset,
                renderSource = savedSource,
                statusMessageBangla = "গুগল ড্রাইভ কুরআন শরীফ পিডিএফ (${HafeziQuranIndexData.toBanglaDigits(pageCount)} পৃষ্ঠা) অফলাইনে প্রস্তুত"
            )
        } else {
            _stateFlow.value = DrivePdfState(
                status = DrivePdfStatus.NOT_DOWNLOADED,
                pageOffset = savedOffset,
                renderSource = savedSource,
                statusMessageBangla = "গুগল ড্রাইভ কুরআন শরীফ পিডিএফ সংযুক্ত রয়েছে (সরাসরি পড়ুন বা অফলাইনে ডাউনলোড করুন)"
            )
        }
    }

    fun setRenderSource(context: Context, source: QuranMushafRenderSource) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RENDER_SOURCE, source.name).apply()
        _stateFlow.value = _stateFlow.value.copy(renderSource = source)
    }

    fun setPageOffset(context: Context, offset: Int) {
        val safeOffset = offset.coerceIn(-10, 20)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_PDF_PAGE_OFFSET, safeOffset).apply()
        bitmapCache.evictAll()
        _stateFlow.value = _stateFlow.value.copy(pageOffset = safeOffset)
    }

    fun openDriveLinkExternally(context: Context) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_DRIVE_VIEW_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Imports a user-selected PDF file (`content://`) into the app's offline Mushaf storage.
     */
    fun importPdfFromUri(
        context: Context,
        uri: Uri,
        scope: CoroutineScope,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val appContext = context.applicationContext ?: context
        scope.launch(Dispatchers.IO) {
            try {
                _stateFlow.value = _stateFlow.value.copy(
                    status = DrivePdfStatus.DOWNLOADING,
                    statusMessageBangla = "পিডিএফ ফাইলটি অফলাইন মেমোরিতে সংরক্ষণ করা হচ্ছে..."
                )
                val tempFile = File(appContext.cacheDir, "temp_import_quran.pdf")
                if (tempFile.exists()) tempFile.delete()

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var copied = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            copied += read
                            _stateFlow.value = _stateFlow.value.copy(
                                status = DrivePdfStatus.DOWNLOADING,
                                downloadedBytes = copied,
                                totalBytes = copied
                            )
                        }
                        output.flush()
                    }
                } ?: throw IllegalStateException("পিডিএফ ফাইলটি পড়া যায়নি")

                if (!isValidPdfHeader(tempFile)) {
                    tempFile.delete()
                    _stateFlow.value = _stateFlow.value.copy(
                        status = DrivePdfStatus.ERROR,
                        statusMessageBangla = "নির্বাচিত ফাইলটি সঠিক পিডিএফ (PDF) নয়।"
                    )
                    withContext(Dispatchers.Main) {
                        onComplete(false, "নির্বাচিত ফাইলটি সঠিক পিডিএফ (PDF) নয়।")
                    }
                    return@launch
                }

                val targetFile = getOfflinePdfFile(appContext)
                closeCachedRenderer()
                if (targetFile.exists()) targetFile.delete()
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()

                val pageCount = inspectPdfPageCount(targetFile).coerceAtLeast(1)
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_PDF_PAGE_COUNT, pageCount)
                    .putString(KEY_RENDER_SOURCE, QuranMushafRenderSource.DRIVE_PDF_MUSHAF.name)
                    .apply()

                bitmapCache.evictAll()
                _stateFlow.value = _stateFlow.value.copy(
                    status = DrivePdfStatus.READY_OFFLINE,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    pdfPageCount = pageCount,
                    renderSource = QuranMushafRenderSource.DRIVE_PDF_MUSHAF,
                    statusMessageBangla = "আলহামদুলিল্লাহ! কুরআন শরীফ পিডিএফ (${HafeziQuranIndexData.toBanglaDigits(pageCount)} পৃষ্ঠা) সফলভাবে যুক্ত হয়েছে"
                )
                withContext(Dispatchers.Main) {
                    onComplete(
                        true,
                        "আলহামদুলিল্লাহ! কুরআন শরীফ পিডিএফ (${HafeziQuranIndexData.toBanglaDigits(pageCount)} পৃষ্ঠা) যুক্ত হয়েছে।"
                    )
                }
            } catch (e: Exception) {
                _stateFlow.value = _stateFlow.value.copy(
                    status = DrivePdfStatus.ERROR,
                    statusMessageBangla = "পিডিএফ যুক্ত করতে সমস্যা হয়েছে: ${e.localizedMessage ?: "আবার চেষ্টা করুন"}"
                )
                withContext(Dispatchers.Main) {
                    onComplete(false, "পিডিএফ যুক্ত করতে সমস্যা হয়েছে।")
                }
            }
        }
    }

    /**
     * Downloads the Quran PDF directly from Google Drive (`19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2`).
     * Supports Google Drive large-file confirmation tokens and WebView cookies if signed in.
     */
    fun startDrivePdfDownload(
        context: Context,
        scope: CoroutineScope,
        fileId: String = DEFAULT_DRIVE_FILE_ID
    ) {
        if (downloadJob?.isActive == true) return
        val appContext = context.applicationContext ?: context

        _stateFlow.value = _stateFlow.value.copy(
            status = DrivePdfStatus.DOWNLOADING,
            downloadedBytes = 0L,
            totalBytes = 0L,
            statusMessageBangla = "গুগল ড্রাইভ থেকে কুরআন শরীফ পিডিএফ ডাউনলোড শুরু হচ্ছে..."
        )

        downloadJob = scope.launch(Dispatchers.IO) {
            val tempFile = File(appContext.cacheDir, "temp_drive_quran_$fileId.pdf")
            try {
                if (tempFile.exists()) tempFile.delete()

                val cookieManager = java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL)
                CookieHandler.setDefault(cookieManager)

                val candidateUrls = listOf(
                    "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t",
                    "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                )

                var downloadedSuccess = false
                var requiresDriveAccess = false

                for (candidateUrl in candidateUrls) {
                    if (!isActive) break
                    val result = tryDownloadPdfStream(candidateUrl, fileId, tempFile)
                    if (result == DownloadAttemptResult.SUCCESS && isValidPdfHeader(tempFile)) {
                        downloadedSuccess = true
                        break
                    } else if (result == DownloadAttemptResult.NEEDS_ACCESS_PERMISSION) {
                        requiresDriveAccess = true
                    }
                }

                if (downloadedSuccess && isValidPdfHeader(tempFile)) {
                    val targetFile = getOfflinePdfFile(appContext)
                    closeCachedRenderer()
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()

                    val pageCount = inspectPdfPageCount(targetFile).coerceAtLeast(1)
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_PDF_PAGE_COUNT, pageCount)
                        .putString(KEY_RENDER_SOURCE, QuranMushafRenderSource.DRIVE_PDF_MUSHAF.name)
                        .apply()

                    bitmapCache.evictAll()
                    _stateFlow.value = _stateFlow.value.copy(
                        status = DrivePdfStatus.READY_OFFLINE,
                        downloadedBytes = targetFile.length(),
                        totalBytes = targetFile.length(),
                        pdfPageCount = pageCount,
                        renderSource = QuranMushafRenderSource.DRIVE_PDF_MUSHAF,
                        statusMessageBangla = "আলহামদুলিল্লাহ! গুগল ড্রাইভ কুরআন শরীফ (${HafeziQuranIndexData.toBanglaDigits(pageCount)} পৃষ্ঠা) অফলাইনে সংরক্ষিত হয়েছে"
                    )
                } else {
                    tempFile.delete()
                    if (requiresDriveAccess) {
                        _stateFlow.value = _stateFlow.value.copy(
                            status = DrivePdfStatus.NEEDS_DRIVE_ACCESS,
                            statusMessageBangla = "ড্রাইভ ফাইলটি 'Restricted' রয়েছে। ইন-অ্যাপ ড্রাইভ ভিউয়ারে পড়ুন অথবা গুগল ড্রাইভে 'Anyone with the link' চালু করুন।"
                        )
                    } else {
                        _stateFlow.value = _stateFlow.value.copy(
                            status = DrivePdfStatus.ERROR,
                            statusMessageBangla = "ডাউনলোড সম্পন্ন হয়নি। ইন্টারনেট সংযোগ বা ড্রাইভ লিংক পারমিশন চেক করে আবার চেষ্টা করুন।"
                        )
                    }
                }
            } catch (e: Exception) {
                tempFile.delete()
                _stateFlow.value = _stateFlow.value.copy(
                    status = DrivePdfStatus.ERROR,
                    statusMessageBangla = "গুগল ড্রাইভ থেকে ডাউনলোডে ত্রুটি: ${e.localizedMessage ?: "আবার চেষ্টা করুন"}"
                )
            }
        }
    }

    private enum class DownloadAttemptResult {
        SUCCESS,
        NEEDS_ACCESS_PERMISSION,
        FAILED
    }

    private fun tryDownloadPdfStream(
        initialUrl: String,
        fileId: String,
        destFile: File
    ): DownloadAttemptResult {
        var currentUrl = initialUrl
        var redirects = 0

        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                // Pass WebView cookies if the user signed into Google inside the in-app WebView
                val webCookie = runCatching {
                    CookieManager.getInstance().getCookie(currentUrl)
                        ?: CookieManager.getInstance().getCookie("https://drive.google.com")
                }.getOrNull()
                if (!webCookie.isNullOrBlank()) {
                    setRequestProperty("Cookie", webCookie)
                }
            }

            val code = conn.responseCode
            if (code in 301..308) {
                val loc = conn.getHeaderField("Location") ?: return DownloadAttemptResult.FAILED
                conn.disconnect()
                if (loc.contains("accounts.google.com") || loc.contains("ServiceLogin")) {
                    return DownloadAttemptResult.NEEDS_ACCESS_PERMISSION
                }
                currentUrl = loc
                redirects++
                continue
            }

            val finalUrlStr = conn.url?.toString().orEmpty()
            if (finalUrlStr.contains("accounts.google.com") || finalUrlStr.contains("ServiceLogin")) {
                conn.disconnect()
                return DownloadAttemptResult.NEEDS_ACCESS_PERMISSION
            }

            val contentType = conn.contentType?.lowercase().orEmpty()
            if (contentType.contains("text/html")) {
                val htmlText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (htmlText.contains("accounts.google.com") || htmlText.contains("ServiceLogin")) {
                    return DownloadAttemptResult.NEEDS_ACCESS_PERMISSION
                }
                // Parse Google Drive large file virus scan confirmation form
                val confirmUrl = parseDriveVirusScanConfirmUrl(htmlText, fileId)
                if (confirmUrl != null && confirmUrl != currentUrl) {
                    currentUrl = confirmUrl
                    redirects++
                    continue
                }
                return DownloadAttemptResult.NEEDS_ACCESS_PERMISSION
            }

            val totalLength = conn.contentLengthLong.coerceAtLeast(0L)
            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        _stateFlow.value = _stateFlow.value.copy(
                            status = DrivePdfStatus.DOWNLOADING,
                            downloadedBytes = downloaded,
                            totalBytes = totalLength,
                            statusMessageBangla = if (totalLength > 0L) {
                                val pct = ((downloaded * 100L) / totalLength).toInt().coerceIn(0, 100)
                                "ডাউনলোড হচ্ছে: ${HafeziQuranIndexData.toBanglaDigits(pct)}% (${String.format("%.1f", downloaded / (1024.0 * 1024.0))} MB)"
                            } else {
                                "ডাউনলোড হচ্ছে: ${String.format("%.1f", downloaded / (1024.0 * 1024.0))} MB..."
                            }
                        )
                    }
                    output.flush()
                }
            }
            conn.disconnect()
            return if (isValidPdfHeader(destFile)) {
                DownloadAttemptResult.SUCCESS
            } else {
                DownloadAttemptResult.NEEDS_ACCESS_PERMISSION
            }
        }
        return DownloadAttemptResult.FAILED
    }

    private fun parseDriveVirusScanConfirmUrl(html: String, fileId: String): String? {
        val actionRegex = Regex("""action=["'](https://drive\.usercontent\.google\.com/download[^"']*)["']""")
        val actionUrl = actionRegex.find(html)?.groupValues?.getOrNull(1)
            ?: "https://drive.usercontent.google.com/download"

        val params = mutableMapOf<String, String>()
        val inputRegex = Regex("""<input[^>]+name=["']([^"']+)["'][^>]+value=["']([^"']*)["']""")
        for (match in inputRegex.findAll(html)) {
            val k = match.groupValues[1]
            val v = match.groupValues[2]
            params[k] = v
        }
        if (params.isEmpty()) return null
        params.putIfAbsent("id", fileId)
        params.putIfAbsent("export", "download")
        params.putIfAbsent("confirm", "t")

        val queryString = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "$actionUrl?$queryString"
    }

    fun isValidPdfHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 100L) return false
        return runCatching {
            val header = ByteArray(5)
            file.inputStream().use { it.read(header) }
            val magic = String(header, Charsets.US_ASCII)
            magic.startsWith("%PDF-")
        }.getOrDefault(false)
    }

    private fun inspectPdfPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrDefault(0)
    }

    private fun closeCachedRenderer() {
        runCatching { cachedRenderer?.close() }
        runCatching { cachedDescriptor?.close() }
        cachedRenderer = null
        cachedDescriptor = null
        cachedFileLength = -1L
    }

    /**
     * Renders a single Mushaf page (`1..610`) from the downloaded Google Drive PDF using Android's
     * native `PdfRenderer`. Maps Mushaf page numbers safely to the PDF's actual page index.
     */
    suspend fun renderPdfPageBitmap(
        context: Context,
        mushafPageNumber: Int,
        targetWidthPx: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        val pdfFile = getOfflinePdfFile(context)
        if (!pdfFile.exists() || !isValidPdfHeader(pdfFile)) return@withContext null

        val offset = _stateFlow.value.pageOffset
        val cacheKey = "pdf_${pdfFile.length()}_p_${mushafPageNumber}_off_${offset}_w_${targetWidthPx}"
        bitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        pdfMutex.withLock {
            bitmapCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }
            try {
                if (cachedRenderer == null || cachedFileLength != pdfFile.length()) {
                    closeCachedRenderer()
                    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    cachedDescriptor = pfd
                    cachedRenderer = PdfRenderer(pfd)
                    cachedFileLength = pdfFile.length()
                }
                val renderer = cachedRenderer ?: return@withLock null
                val totalPdfPages = renderer.pageCount
                if (totalPdfPages <= 0) return@withLock null

                val targetIndex = (mushafPageNumber - 1 + offset).coerceIn(0, totalPdfPages - 1)
                renderer.openPage(targetIndex).use { page ->
                    val width = targetWidthPx.coerceIn(720, 1600)
                    val aspect = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.45f
                    val height = (width * aspect).toInt().coerceAtLeast(900)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapCache.put(cacheKey, bitmap)
                    bitmap
                }
            } catch (e: Exception) {
                closeCachedRenderer()
                null
            }
        }
    }
}

/**
 * Renders a single page of the downloaded Google Drive PDF via `PdfRenderer`, applying
 * Mushaf paper theme filters (White, Cream, Sepia, or Night Mode).
 */
@Composable
fun HafeziDrivePdfPageSurface(
    pageNumber: Int,
    themeMode: MushafThemeMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pageBitmap by remember(pageNumber) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageNumber) { mutableStateOf(true) }

    LaunchedEffect(pageNumber) {
        isLoading = true
        pageBitmap = HafeziDrivePdfManager.renderPdfPageBitmap(context, pageNumber)
        isLoading = false
    }

    val colorFilter = remember(themeMode) {
        when (themeMode) {
            MushafThemeMode.NIGHT_MODE -> {
                // Invert black-on-white PDF scan to crisp light-on-dark Night Mode
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            -0.88f, 0f, 0f, 0f, 240f,
                            0f, -0.88f, 0f, 0f, 236f,
                            0f, 0f, -0.85f, 0f, 224f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            MushafThemeMode.SEPIA_HERITAGE -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.97f, 0f, 0f, 0f, 0f,
                            0f, 0.94f, 0f, 0f, 0f,
                            0f, 0f, 0.86f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            MushafThemeMode.CLASSIC_WHITE -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.99f, 0f, 0f, 0f, 0f,
                            0f, 0.98f, 0f, 0f, 0f,
                            0f, 0f, 0.95f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            MushafThemeMode.EMDADIA_CREAM -> null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeMode.pageInnerBackground)
            .testTag("hafezi_drive_pdf_page_$pageNumber"),
        contentAlignment = Alignment.Center
    ) {
        val bmp = pageBitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "হাফেজি কুরআন শরীফ পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageNumber)}",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0F6B4B),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageNumber)} লোড হচ্ছে...",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeMode.textColor
                )
            }
        }
    }
}

/**
 * Full-Screen In-App Google Drive PDF Mushaf Viewer (`WebView`) for instant viewing of
 * `https://drive.google.com/file/d/19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2/preview`
 * even before the user downloads the offline PDF file, with 1-tap Offline Download & Local PDF Import.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleDriveEmbeddedMushafView(
    drivePdfState: DrivePdfState,
    onStartOfflineDownload: () -> Unit,
    onPickLocalPdf: () -> Unit,
    onOpenInDriveApp: () -> Unit,
    onSwitchToDigital15Line: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loadProgress by remember { mutableFloatStateOf(0.1f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF8))
            .testTag("quran_google_drive_embedded_viewer")
    ) {
        // Action Banner above Embedded Drive Viewer
        Surface(
            color = Color(0xFFEAF4EF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = drivePdfState.statusMessageBangla,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0F6B4B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "রিলোড",
                        tint = Color(0xFF0F6B4B),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { webViewRef?.reload() }
                    )
                }

                if (drivePdfState.status == DrivePdfStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { drivePdfState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color(0xFF0F6B4B),
                        trackColor = Color(0xFFCFE6DC)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Offline Download from Drive
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F6B4B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onStartOfflineDownload)
                            .testTag("drive_viewer_download_offline_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "অফলাইন পিডিএফ সেভ",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Import PDF from Phone Storage
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier
                            .border(1.dp, Color(0xFF0F6B4B), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onPickLocalPdf)
                            .testTag("drive_viewer_pick_local_pdf_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF0F6B4B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ফোন থেকে PDF",
                                color = Color(0xFF0F6B4B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3. Open in Google Drive App
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier
                            .border(1.dp, Color(0xFFD9A82E), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenInDriveApp)
                            .testTag("drive_viewer_open_external_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF0B1914),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ড্রাইভ অ্যাপ",
                                color = Color(0xFF0B1914),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4. Switch to Built-in 15-Line Mushaf
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier
                            .border(1.dp, Color(0xFFCFE6DC), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSwitchToDigital15Line)
                            .testTag("drive_viewer_switch_digital_btn")
                    ) {
                        Text(
                            text = "১৫-লাইন ভিউ",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = Color(0xFF12241D),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (isPageLoading) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFFD9A82E),
                trackColor = Color.Transparent
            )
        }

        // Embedded Google Drive PDF Viewer WebView
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = (newProgress / 100f).coerceIn(0.05f, 1f)
                                isPageLoading = newProgress < 95
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString().orEmpty()
                                if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                    return false
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isPageLoading = false
                                CookieManager.getInstance().flush()
                            }
                        }

                        loadUrl(HafeziDrivePdfManager.DEFAULT_DRIVE_PREVIEW_URL)
                        webViewRef = this
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef = null
        }
    }
}
