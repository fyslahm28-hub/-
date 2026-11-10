package com.example.ui.kitab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class KitabNetworkStreamResponse(
    val httpStatusCode: Int,
    val isPartialContent: Boolean,
    val contentLengthBytes: Long,
    val inputStream: InputStream,
    val onClose: () -> Unit = {}
)

interface KitabNetworkTransport {
    fun openStream(downloadUrl: String, rangeStartBytes: Long): KitabNetworkStreamResponse
}

object RealHttpRangeNetworkTransport : KitabNetworkTransport {
    override fun openStream(downloadUrl: String, rangeStartBytes: Long): KitabNetworkStreamResponse {
        var currentUrl = downloadUrl
        var redirects = 0
        while (redirects < 6) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "QawmiKormosongsthan-KitabLibrary/1.0 (Android)")
                setRequestProperty("Accept", "application/pdf, application/octet-stream, */*")
                if (rangeStartBytes > 0L) {
                    setRequestProperty("Range", "bytes=$rangeStartBytes-")
                }
            }
            connection.connect()
            val code = connection.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308
                )
            ) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect location missing from server response")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
                continue
            }

            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                connection.disconnect()
                throw IllegalStateException("HTTP Error $code while downloading Kitab PDF")
            }

            val isPartial = (code == HttpURLConnection.HTTP_PARTIAL)
            val contentLength = connection.contentLengthLong
            val stream = connection.inputStream
            return KitabNetworkStreamResponse(
                httpStatusCode = code,
                isPartialContent = isPartial,
                contentLengthBytes = contentLength,
                inputStream = stream,
                onClose = {
                    runCatching { stream.close() }
                    runCatching { connection.disconnect() }
                }
            )
        }
        throw IllegalStateException("Too many redirects while connecting to Kitab server")
    }
}

data class KitabOpenReadResult(
    val canOpen: Boolean,
    val pdfFile: File? = null,
    val totalPages: Int = 0,
    val initialPage: Int = 1,
    val errorMessageBangla: String = ""
)

object KitabDownloadAndStorageManager {

    private const val PDF_DIR_NAME = "qawmi_kitab_library_pdfs"
    private const val PREFS_NAME = "qawmi_kitab_library_prefs"
    private const val KEY_LAST_FIRESTORE_CATALOG_SYNC = "last_firestore_catalog_sync_ms"
    private const val KEY_READER_THEME = "kitab_reader_theme_mode"
    private const val KEY_READER_PAGE_MODE = "kitab_reader_page_mode"
    private const val KEY_READER_ZOOM_FIT = "kitab_reader_zoom_fit"
    private const val KEY_READER_ORIENTATION = "kitab_reader_orientation"
    private const val KEY_READER_KEEP_SCREEN_ON = "kitab_reader_keep_screen_on"
    private const val KEY_READER_BRIGHTNESS = "kitab_reader_brightness"
    private const val KEY_READER_PAGE_SPACING = "kitab_reader_page_spacing_dp"
    private const val FIRESTORE_SYNC_THROTTLE_MS = 12L * 60L * 60L * 1000L

    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var customStorageDirOverride: File? = null

    @Volatile
    var networkTransport: KitabNetworkTransport = RealHttpRangeNetworkTransport

    @Volatile
    var isNetworkConnectedOverride: Boolean? = null

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()
    private val cancelledFlags = ConcurrentHashMap<String, Boolean>()
    private val pdfRenderMutex = Mutex()

    private val pageBitmapCache = object : LruCache<String, Bitmap>(18) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    private val _catalogItemsFlow = MutableStateFlow<List<KitabItemUiModel>>(emptyList())
    val catalogItemsFlow: StateFlow<List<KitabItemUiModel>> = _catalogItemsFlow.asStateFlow()

    private val _bookmarksFlow = MutableStateFlow<List<KitabBookmarkModel>>(emptyList())
    val bookmarksFlow: StateFlow<List<KitabBookmarkModel>> = _bookmarksFlow.asStateFlow()

    private val _readerThemeFlow = MutableStateFlow(KitabReaderThemeMode.MUSHAF_CREAM)
    val readerThemeFlow: StateFlow<KitabReaderThemeMode> = _readerThemeFlow.asStateFlow()

    private val _readerSettingsFlow = MutableStateFlow(KitabReaderSettings())
    val readerSettingsFlow: StateFlow<KitabReaderSettings> = _readerSettingsFlow.asStateFlow()

    fun setCustomStorageDirectoryForTesting(dir: File?) {
        customStorageDirOverride = dir
    }

    fun getPdfStorageDirectory(context: Context): File {
        customStorageDirOverride?.let { custom ->
            if (!custom.exists()) custom.mkdirs()
            return custom
        }
        val appContext = context.applicationContext ?: context
        val dir = File(appContext.filesDir, PDF_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFinalPdfFile(context: Context, bookId: String): File {
        return File(getPdfStorageDirectory(context), "$bookId.pdf")
    }

    fun getPartialPdfFile(context: Context, bookId: String): File {
        return File(getPdfStorageDirectory(context), "$bookId.pdf.part")
    }

    fun init(context: Context, allowRemoteFirestoreSync: Boolean = true) {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString(KEY_READER_THEME, KitabReaderThemeMode.MUSHAF_CREAM.id)
        _readerThemeFlow.value = KitabReaderThemeMode.entries.firstOrNull { it.id == savedThemeId }
            ?: KitabReaderThemeMode.MUSHAF_CREAM
        _readerSettingsFlow.value = loadReaderSettingsFromPrefs(prefs)

        if (_catalogItemsFlow.value.isEmpty()) {
            _catalogItemsFlow.value = KitabVerifiedCatalogData.defaultCatalogEntities.map { meta ->
                combineEntityAndState(meta, null, appContext)
            }
        }

        managerScope.launch(ioDispatcher) {
            initAndAwaitSync(appContext, customStorageDirOverride)
            if (allowRemoteFirestoreSync && isDeviceOnline(appContext)) {
                val lastSync = prefs.getLong(KEY_LAST_FIRESTORE_CATALOG_SYNC, 0L)
                val now = System.currentTimeMillis()
                if (now - lastSync >= FIRESTORE_SYNC_THROTTLE_MS) {
                    syncRemoteFirestoreCatalogIfAvailable(appContext)
                    prefs.edit().putLong(KEY_LAST_FIRESTORE_CATALOG_SYNC, now).apply()
                }
            }
        }
    }

    suspend fun initAndAwaitSync(context: Context, customDir: File? = null) = withContext(ioDispatcher) {
        if (customDir != null) {
            customStorageDirOverride = customDir
        }
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString(KEY_READER_THEME, KitabReaderThemeMode.MUSHAF_CREAM.id)
        _readerThemeFlow.value = KitabReaderThemeMode.entries.firstOrNull { it.id == savedThemeId }
            ?: KitabReaderThemeMode.MUSHAF_CREAM
        _readerSettingsFlow.value = loadReaderSettingsFromPrefs(prefs)

        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val existingCatalog = dao.getAllApprovedCatalogOnce()
        if (existingCatalog.isEmpty()) {
            dao.upsertCatalogItems(KitabVerifiedCatalogData.defaultCatalogEntities)
        } else {
            val existingIds = existingCatalog.map { it.bookId }.toSet()
            val missingDefaults = KitabVerifiedCatalogData.defaultCatalogEntities.filter { it.bookId !in existingIds }
            if (missingDefaults.isNotEmpty()) {
                dao.upsertCatalogItems(missingDefaults)
            }
        }

        reconcileLocalDiskStateWithDatabase(appContext, dao)
        refreshSnapshotFromDao(dao)
        isInitialized = true
    }

    private suspend fun reconcileLocalDiskStateWithDatabase(context: Context, dao: KitabLibraryDao) {
        val allCatalog = dao.getAllApprovedCatalogOnce()
        val localStatesMap = dao.getAllLocalStatesOnce().associateBy { it.bookId }

        for (meta in allCatalog) {
            val finalFile = getFinalPdfFile(context, meta.bookId)
            val partFile = getPartialPdfFile(context, meta.bookId)
            val prevState = localStatesMap[meta.bookId]

            if (finalFile.exists() && finalFile.length() > 0L && hasValidPdfHeader(finalFile)) {
                val matchesSize = meta.fileSizeBytes <= 0L || finalFile.length() == meta.fileSizeBytes
                if (matchesSize) {
                    val pageCount = if ((prevState?.totalPageCount ?: 0) > 0) {
                        prevState!!.totalPageCount
                    } else {
                        inspectPdfPageCount(finalFile).coerceAtLeast(1)
                    }
                    dao.upsertLocalState(
                        KitabLocalStateEntity(
                            bookId = meta.bookId,
                            downloadStatus = KitabDownloadStatus.DOWNLOADED_VERIFIED.name,
                            localFilePath = finalFile.absolutePath,
                            downloadedBytes = finalFile.length(),
                            totalBytes = meta.fileSizeBytes.coerceAtLeast(finalFile.length()),
                            sha256Verified = true,
                            verifiedSha256Hex = meta.sha256Checksum,
                            downloadedAtMillis = prevState?.downloadedAtMillis?.takeIf { it > 0L }
                                ?: finalFile.lastModified(),
                            lastReadPage = (prevState?.lastReadPage ?: 1).coerceIn(1, pageCount),
                            totalPageCount = pageCount,
                            lastReadTimestampMillis = prevState?.lastReadTimestampMillis ?: 0L,
                            errorMessageBangla = ""
                        )
                    )
                } else {
                    runCatching { finalFile.delete() }
                    dao.upsertLocalState(
                        KitabLocalStateEntity(
                            bookId = meta.bookId,
                            downloadStatus = KitabDownloadStatus.FAILED_CORRUPTED.name,
                            localFilePath = "",
                            downloadedBytes = 0L,
                            totalBytes = meta.fileSizeBytes,
                            sha256Verified = false,
                            errorMessageBangla = "অসম্পূর্ণ বা ক্ষতিগ্রস্ত ফাইল শনাক্ত হওয়ায় মুছে ফেলা হয়েছে।"
                        )
                    )
                }
            } else if (partFile.exists() && partFile.length() > 0L) {
                val partLen = partFile.length()
                dao.upsertLocalState(
                    KitabLocalStateEntity(
                        bookId = meta.bookId,
                        downloadStatus = KitabDownloadStatus.PAUSED.name,
                        localFilePath = "",
                        downloadedBytes = partLen,
                        totalBytes = meta.fileSizeBytes,
                        sha256Verified = false,
                        lastReadPage = prevState?.lastReadPage ?: 1,
                        totalPageCount = prevState?.totalPageCount ?: 0,
                        errorMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${KitabFormatUtils.formatSizeMbBangla(partLen)} সংরক্ষিত)"
                    )
                )
            } else {
                val defaultStatus = if (meta.isOpenLicenseVerified) {
                    KitabDownloadStatus.NOT_DOWNLOADED
                } else {
                    KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE
                }
                if (prevState != null && prevState.downloadStatus != defaultStatus.name &&
                    prevState.downloadStatus != KitabDownloadStatus.FAILED_CORRUPTED.name
                ) {
                    dao.upsertLocalState(
                        prevState.copy(
                            downloadStatus = defaultStatus.name,
                            localFilePath = "",
                            downloadedBytes = 0L,
                            sha256Verified = false,
                            errorMessageBangla = ""
                        )
                    )
                }
            }
        }
    }

    suspend fun refreshSnapshotFromDao(dao: KitabLibraryDao) {
        val catalogEntities = dao.getAllApprovedCatalogOnce()
        val statesByBook = dao.getAllLocalStatesOnce().associateBy { it.bookId }
        val bookmarksEntities = dao.getAllBookmarksOnce()

        _catalogItemsFlow.value = catalogEntities.map { meta ->
            val state = statesByBook[meta.bookId]
            combineEntityAndState(meta, state, null)
        }

        _bookmarksFlow.value = bookmarksEntities.map { entity ->
            KitabBookmarkModel(
                id = entity.id,
                bookId = entity.bookId,
                bookTitleBangla = entity.bookTitleBangla,
                volumeNumber = entity.volumeNumber,
                pageNumber = entity.pageNumber,
                note = entity.note,
                createdAtMillis = entity.createdAtMillis
            )
        }
    }

    private fun combineEntityAndState(
        meta: KitabMetadataEntity,
        state: KitabLocalStateEntity?,
        context: Context?
    ): KitabItemUiModel {
        val resolvedStatus = when {
            !meta.isOpenLicenseVerified -> KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE
            state == null -> KitabDownloadStatus.NOT_DOWNLOADED
            else -> runCatching { KitabDownloadStatus.valueOf(state.downloadStatus) }
                .getOrDefault(KitabDownloadStatus.NOT_DOWNLOADED)
        }
        val localPath = state?.localFilePath.orEmpty().ifBlank {
            if (context != null && resolvedStatus == KitabDownloadStatus.DOWNLOADED_VERIFIED) {
                getFinalPdfFile(context, meta.bookId).absolutePath
            } else {
                ""
            }
        }
        return KitabItemUiModel(
            bookId = meta.bookId,
            seriesId = meta.seriesId,
            titleBangla = meta.titleBangla,
            arabicTitle = meta.arabicTitle,
            authorBangla = meta.authorBangla,
            translatorOrCompilerBangla = meta.translatorOrCompilerBangla,
            publisherBangla = meta.publisherBangla,
            category = KitabCategory.fromId(meta.categoryId),
            volumeNumber = meta.volumeNumber,
            totalVolumes = meta.totalVolumes,
            fileSizeBytes = meta.fileSizeBytes,
            languageBangla = meta.languageBangla,
            shortDescriptionBangla = meta.shortDescriptionBangla,
            marhalaBangla = meta.marhalaBangla,
            downloadUrl = meta.downloadUrl,
            sha256Checksum = meta.sha256Checksum,
            isOpenLicenseVerified = meta.isOpenLicenseVerified,
            isAdminApproved = meta.isAdminApproved,
            licenseNoteBangla = meta.licenseNoteBangla,
            coverPrimaryHex = meta.coverPrimaryHex,
            coverAccentHex = meta.coverAccentHex,
            downloadStatus = resolvedStatus,
            downloadedBytes = state?.downloadedBytes ?: 0L,
            localFilePath = localPath,
            sha256Verified = state?.sha256Verified ?: false,
            downloadedAtMillis = state?.downloadedAtMillis ?: 0L,
            lastReadPage = (state?.lastReadPage ?: 1).coerceAtLeast(1),
            totalPageCount = state?.totalPageCount ?: 0,
            errorMessageBangla = state?.errorMessageBangla.orEmpty()
        )
    }

    fun isDeviceOnline(context: Context): Boolean {
        isNetworkConnectedOverride?.let { return it }
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching true
            val network = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    fun hasValidPdfHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 8L) return false
        return runCatching {
            FileInputStream(file).use { fis ->
                val header = ByteArray(8)
                val read = fis.read(header)
                if (read < 5) return@use false
                val str = String(header, 0, read, Charsets.US_ASCII)
                str.startsWith("%PDF-")
            }
        }.getOrDefault(false)
    }

    fun computeSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun inspectPdfPageCount(pdfFile: File): Int {
        if (!pdfFile.exists() || !hasValidPdfHeader(pdfFile)) return 0
        val nativeCount = runCatching {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrDefault(0)
        if (nativeCount > 0) return nativeCount

        return runCatching {
            val sampleSize = pdfFile.length().coerceAtMost(512 * 1024L).toInt()
            val bytes = ByteArray(sampleSize)
            FileInputStream(pdfFile).use { it.read(bytes) }
            val content = String(bytes, Charsets.ISO_8859_1)
            val regex = Regex("/Type\\s*/Page\\b")
            regex.findAll(content).count().coerceAtLeast(1)
        }.getOrDefault(1)
    }

    fun buildValidTestPdfBytes(
        title: String,
        pageCount: Int = 6,
        minTargetSizeBytes: Int = 24 * 1024
    ): ByteArray {
        val safePages = pageCount.coerceIn(1, 50)
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        val kidsRefs = (0 until safePages).joinToString(" ") { "${3 + it} 0 R" }
        sb.append("2 0 obj\n<< /Type /Pages /Kids [ $kidsRefs ] /Count $safePages >>\nendobj\n")
        for (i in 0 until safePages) {
            val objId = 3 + i
            sb.append("$objId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >>\nendobj\n")
        }
        sb.append("% Kitab: $title\n")
        while (sb.length < minTargetSizeBytes) {
            sb.append("% PADDING_LINE_FOR_HTTP_RANGE_AND_SHA256_VERIFICATION_0123456789ABCDEF\n")
        }
        sb.append("%%EOF\n")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun startOrResumeDownload(context: Context, bookId: String) {
        val appContext = context.applicationContext ?: context
        if (activeDownloadJobs[bookId]?.isActive == true) return
        pausedFlags[bookId] = false
        cancelledFlags[bookId] = false

        val job = managerScope.launch(ioDispatcher) {
            executeDownloadInternal(appContext, bookId, pauseAfterBytes = -1L)
        }
        activeDownloadJobs[bookId] = job
    }

    fun pauseDownload(context: Context, bookId: String) {
        pausedFlags[bookId] = true
        activeDownloadJobs[bookId]?.cancel()
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            val partFile = getPartialPdfFile(appContext, bookId)
            val meta = dao.getCatalogItemById(bookId)
            val prevState = dao.getLocalStateById(bookId)
            val savedBytes = if (partFile.exists()) partFile.length() else (prevState?.downloadedBytes ?: 0L)
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.PAUSED.name,
                    downloadedBytes = savedBytes,
                    totalBytes = meta?.fileSizeBytes ?: (prevState?.totalBytes ?: 0L),
                    sha256Verified = false,
                    lastReadPage = prevState?.lastReadPage ?: 1,
                    totalPageCount = prevState?.totalPageCount ?: 0,
                    errorMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${KitabFormatUtils.formatSizeMbBangla(savedBytes)} সংরক্ষিত)"
                )
            )
            refreshSnapshotFromDao(dao)
        }
    }

    fun cancelDownload(context: Context, bookId: String) {
        cancelledFlags[bookId] = true
        pausedFlags.remove(bookId)
        activeDownloadJobs[bookId]?.cancel()
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            val partFile = getPartialPdfFile(appContext, bookId)
            runCatching { if (partFile.exists()) partFile.delete() }
            val meta = dao.getCatalogItemById(bookId)
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.NOT_DOWNLOADED.name,
                    localFilePath = "",
                    downloadedBytes = 0L,
                    totalBytes = meta?.fileSizeBytes ?: 0L,
                    sha256Verified = false,
                    errorMessageBangla = ""
                )
            )
            refreshSnapshotFromDao(dao)
        }
    }

    fun deleteDownloadedBook(context: Context, bookId: String, onComplete: () -> Unit = {}) {
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            deleteDownloadedBookSynchronously(appContext, bookId)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    suspend fun deleteDownloadedBookSynchronously(context: Context, bookId: String) = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        cancelledFlags[bookId] = true
        pausedFlags.remove(bookId)
        activeDownloadJobs.remove(bookId)?.cancel()

        val finalFile = getFinalPdfFile(appContext, bookId)
        val partFile = getPartialPdfFile(appContext, bookId)
        runCatching { if (finalFile.exists()) finalFile.delete() }
        runCatching { if (partFile.exists()) partFile.delete() }

        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val meta = dao.getCatalogItemById(bookId)
        val prevState = dao.getLocalStateById(bookId)
        dao.upsertLocalState(
            KitabLocalStateEntity(
                bookId = bookId,
                downloadStatus = KitabDownloadStatus.NOT_DOWNLOADED.name,
                localFilePath = "",
                downloadedBytes = 0L,
                totalBytes = meta?.fileSizeBytes ?: 0L,
                sha256Verified = false,
                lastReadPage = prevState?.lastReadPage ?: 1,
                totalPageCount = prevState?.totalPageCount ?: 0,
                errorMessageBangla = ""
            )
        )
        refreshSnapshotFromDao(dao)
    }

    suspend fun downloadBookSynchronously(
        context: Context,
        bookId: String,
        pauseAfterBytes: Long = -1L
    ): KitabItemUiModel? = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        pausedFlags[bookId] = false
        cancelledFlags[bookId] = false
        executeDownloadInternal(appContext, bookId, pauseAfterBytes)
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        refreshSnapshotFromDao(dao)
        _catalogItemsFlow.value.firstOrNull { it.bookId == bookId }
    }

    private suspend fun executeDownloadInternal(
        context: Context,
        bookId: String,
        pauseAfterBytes: Long
    ) {
        val dao = KitabLibraryDatabase.getInstance(context).kitabLibraryDao()
        val meta = dao.getCatalogItemById(bookId) ?: return
        val prevState = dao.getLocalStateById(bookId)

        if (!meta.isOpenLicenseVerified || !meta.isAdminApproved || !meta.downloadUrl.startsWith("https://")) {
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE.name,
                    errorMessageBangla = "এই কিতাবটির ডিজিটাল ডাউনলোড লাইসেন্স অনুমোদিত নয়।"
                )
            )
            refreshSnapshotFromDao(dao)
            return
        }

        if (!isDeviceOnline(context)) {
            val partFile = getPartialPdfFile(context, bookId)
            val savedBytes = if (partFile.exists()) partFile.length() else 0L
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = if (savedBytes > 0L) KitabDownloadStatus.PAUSED.name else KitabDownloadStatus.FAILED_CORRUPTED.name,
                    downloadedBytes = savedBytes,
                    totalBytes = meta.fileSizeBytes,
                    sha256Verified = false,
                    lastReadPage = prevState?.lastReadPage ?: 1,
                    totalPageCount = prevState?.totalPageCount ?: 0,
                    errorMessageBangla = "ইন্টারনেট সংযোগ নেই। অনলাইনে যুক্ত হয়ে ডাউনলোড করুন।"
                )
            )
            refreshSnapshotFromDao(dao)
            return
        }

        val finalFile = getFinalPdfFile(context, bookId)
        val partFile = getPartialPdfFile(context, bookId)
        var resumeOffset = if (partFile.exists()) partFile.length() else 0L
        if (meta.fileSizeBytes > 0L && resumeOffset > meta.fileSizeBytes) {
            runCatching { partFile.delete() }
            resumeOffset = 0L
        }

        dao.upsertLocalState(
            KitabLocalStateEntity(
                bookId = bookId,
                downloadStatus = KitabDownloadStatus.DOWNLOADING.name,
                downloadedBytes = resumeOffset,
                totalBytes = meta.fileSizeBytes,
                sha256Verified = false,
                lastReadPage = prevState?.lastReadPage ?: 1,
                totalPageCount = prevState?.totalPageCount ?: 0,
                errorMessageBangla = ""
            )
        )
        refreshSnapshotFromDao(dao)

        var response: KitabNetworkStreamResponse? = null
        try {
            response = networkTransport.openStream(meta.downloadUrl, resumeOffset)
            if (resumeOffset > 0L && !response.isPartialContent) {
                runCatching { partFile.delete() }
                resumeOffset = 0L
            }

            val totalExpectedBytes = if (meta.fileSizeBytes > 0L) {
                meta.fileSizeBytes
            } else if (response.contentLengthBytes > 0L) {
                resumeOffset + response.contentLengthBytes
            } else {
                0L
            }

            RandomAccessFile(partFile, "rw").use { raf ->
                raf.seek(resumeOffset)
                val buffer = ByteArray(16 * 1024)
                var totalRead = resumeOffset
                var lastProgressEmitMs = 0L

                while (true) {
                    if (cancelledFlags[bookId] == true) {
                        throw CancellationException("Download cancelled by user")
                    }
                    if (pausedFlags[bookId] == true) {
                        throw CancellationException("Download paused by user")
                    }

                    val read = response.inputStream.read(buffer)
                    if (read == -1) break

                    raf.write(buffer, 0, read)
                    totalRead += read

                    val now = System.currentTimeMillis()
                    if (now - lastProgressEmitMs >= 120L || totalRead == totalExpectedBytes) {
                        lastProgressEmitMs = now
                        dao.upsertLocalState(
                            KitabLocalStateEntity(
                                bookId = bookId,
                                downloadStatus = KitabDownloadStatus.DOWNLOADING.name,
                                downloadedBytes = totalRead,
                                totalBytes = totalExpectedBytes,
                                sha256Verified = false,
                                lastReadPage = prevState?.lastReadPage ?: 1,
                                totalPageCount = prevState?.totalPageCount ?: 0,
                                errorMessageBangla = ""
                            )
                        )
                        refreshSnapshotFromDao(dao)
                    }

                    if (pauseAfterBytes in 1..totalRead) {
                        pausedFlags[bookId] = true
                        dao.upsertLocalState(
                            KitabLocalStateEntity(
                                bookId = bookId,
                                downloadStatus = KitabDownloadStatus.PAUSED.name,
                                downloadedBytes = totalRead,
                                totalBytes = totalExpectedBytes,
                                sha256Verified = false,
                                lastReadPage = prevState?.lastReadPage ?: 1,
                                totalPageCount = prevState?.totalPageCount ?: 0,
                                errorMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${KitabFormatUtils.formatSizeMbBangla(totalRead)} সংরক্ষিত)"
                            )
                        )
                        refreshSnapshotFromDao(dao)
                        return
                    }
                }
            }

            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.VERIFYING_CHECKSUM.name,
                    downloadedBytes = partFile.length(),
                    totalBytes = meta.fileSizeBytes,
                    sha256Verified = false,
                    lastReadPage = prevState?.lastReadPage ?: 1,
                    totalPageCount = prevState?.totalPageCount ?: 0,
                    errorMessageBangla = ""
                )
            )
            refreshSnapshotFromDao(dao)

            if (!partFile.exists() || partFile.length() <= 0L) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোডকৃত ফাইলটি খালি বা অসম্পূর্ণ। পুনরায় চেষ্টা করুন।"
                )
                return
            }

            if (meta.fileSizeBytes > 0L && partFile.length() != meta.fileSizeBytes) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ফাইলের আকার মিলেনি (${partFile.length()} / ${meta.fileSizeBytes} bytes)।"
                )
                return
            }

            if (!hasValidPdfHeader(partFile)) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোডকৃত ফাইলটি বৈধ PDF নয় (%PDF- হেডার অনুপস্থিত)।"
                )
                return
            }

            val actualSha256 = computeSha256Hex(partFile)
            if (meta.sha256Checksum.isNotBlank() && !actualSha256.equals(meta.sha256Checksum, ignoreCase = true)) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "SHA-256 Checksum যাচাই ব্যর্থ হয়েছে (ফাইলটি পরিবর্তিত বা ক্ষতিগ্রস্ত)।"
                )
                return
            }

            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = partFile.renameTo(finalFile)
            if (!renamed) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            val pageCount = inspectPdfPageCount(finalFile).coerceAtLeast(1)
            val initialLastPage = (prevState?.lastReadPage ?: 1).coerceIn(1, pageCount)

            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.DOWNLOADED_VERIFIED.name,
                    localFilePath = finalFile.absolutePath,
                    downloadedBytes = finalFile.length(),
                    totalBytes = finalFile.length(),
                    sha256Verified = true,
                    verifiedSha256Hex = actualSha256,
                    downloadedAtMillis = System.currentTimeMillis(),
                    lastReadPage = initialLastPage,
                    totalPageCount = pageCount,
                    lastReadTimestampMillis = prevState?.lastReadTimestampMillis ?: System.currentTimeMillis(),
                    errorMessageBangla = ""
                )
            )
            refreshSnapshotFromDao(dao)
        } catch (ce: CancellationException) {
            if (cancelledFlags[bookId] == true) {
                runCatching { if (partFile.exists()) partFile.delete() }
                dao.upsertLocalState(
                    KitabLocalStateEntity(
                        bookId = bookId,
                        downloadStatus = KitabDownloadStatus.NOT_DOWNLOADED.name,
                        localFilePath = "",
                        downloadedBytes = 0L,
                        totalBytes = meta.fileSizeBytes,
                        sha256Verified = false,
                        errorMessageBangla = ""
                    )
                )
            } else {
                val savedBytes = if (partFile.exists()) partFile.length() else 0L
                dao.upsertLocalState(
                    KitabLocalStateEntity(
                        bookId = bookId,
                        downloadStatus = KitabDownloadStatus.PAUSED.name,
                        downloadedBytes = savedBytes,
                        totalBytes = meta.fileSizeBytes,
                        sha256Verified = false,
                        lastReadPage = prevState?.lastReadPage ?: 1,
                        totalPageCount = prevState?.totalPageCount ?: 0,
                        errorMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${KitabFormatUtils.formatSizeMbBangla(savedBytes)} সংরক্ষিত)"
                    )
                )
            }
            refreshSnapshotFromDao(dao)
        } catch (e: Exception) {
            val savedBytes = if (partFile.exists()) partFile.length() else 0L
            val nextStatus = if (savedBytes > 0L) {
                KitabDownloadStatus.PAUSED.name
            } else {
                KitabDownloadStatus.FAILED_CORRUPTED.name
            }
            val msg = if (savedBytes > 0L) {
                "নেটওয়ার্ক বিচ্ছিন্ন হওয়ায় ডাউনলোড সাময়িক বিরতি হয়েছে (${KitabFormatUtils.formatSizeMbBangla(savedBytes)} সংরক্ষিত)। পুনরায় Resume করুন।"
            } else {
                "ডাউনলোড ত্রুটি: ${e.localizedMessage ?: "নেটওয়ার্ক সংযোগ যাচাই করুন"}"
            }
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = nextStatus,
                    downloadedBytes = savedBytes,
                    totalBytes = meta.fileSizeBytes,
                    sha256Verified = false,
                    lastReadPage = prevState?.lastReadPage ?: 1,
                    totalPageCount = prevState?.totalPageCount ?: 0,
                    errorMessageBangla = msg
                )
            )
            refreshSnapshotFromDao(dao)
        } finally {
            runCatching { response?.onClose?.invoke() }
            activeDownloadJobs.remove(bookId)
        }
    }

    private suspend fun markCorruptedAndDeletePart(
        dao: KitabLibraryDao,
        partFile: File,
        bookId: String,
        totalBytes: Long,
        reasonBangla: String
    ) {
        runCatching { if (partFile.exists()) partFile.delete() }
        dao.upsertLocalState(
            KitabLocalStateEntity(
                bookId = bookId,
                downloadStatus = KitabDownloadStatus.FAILED_CORRUPTED.name,
                localFilePath = "",
                downloadedBytes = 0L,
                totalBytes = totalBytes,
                sha256Verified = false,
                errorMessageBangla = reasonBangla
            )
        )
        refreshSnapshotFromDao(dao)
    }

    suspend fun verifyAndPrepareBookForInternalReader(
        context: Context,
        bookId: String
    ): KitabOpenReadResult = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val meta = dao.getCatalogItemById(bookId)
            ?: return@withContext KitabOpenReadResult(
                canOpen = false,
                errorMessageBangla = "কিতাবটি ক্যাটালগে পাওয়া যায়নি।"
            )

        val localState = dao.getLocalStateById(bookId)
        val finalFile = getFinalPdfFile(appContext, bookId)

        if (!finalFile.exists() || finalFile.length() <= 0L || localState?.sha256Verified != true) {
            val offlineMsg = if (!isDeviceOnline(appContext)) {
                "কিতাবটি এখনো আপনার ফোনে ডাউনলোড করা হয়নি এবং বর্তমানে ইন্টারনেট সংযোগ বন্ধ রয়েছে। পড়ার জন্য ইন্টারনেট চালু করে প্রথমে কিতাবটি ডাউনলোড করুন।"
            } else {
                "কিতাবটি এখনো ডাউনলোড সম্পন্ন হয়নি। অফলাইনে পড়ার জন্য প্রথমে ডাউনলোড সম্পন্ন করুন।"
            }
            return@withContext KitabOpenReadResult(
                canOpen = false,
                errorMessageBangla = offlineMsg
            )
        }

        if (!hasValidPdfHeader(finalFile) || (meta.fileSizeBytes > 0L && finalFile.length() != meta.fileSizeBytes)) {
            runCatching { finalFile.delete() }
            dao.upsertLocalState(
                localState.copy(
                    downloadStatus = KitabDownloadStatus.FAILED_CORRUPTED.name,
                    localFilePath = "",
                    downloadedBytes = 0L,
                    sha256Verified = false,
                    errorMessageBangla = "স্থানীয় PDF ফাইলটি ক্ষতিগ্রস্ত হওয়ায় ওপেন ব্লক করা হয়েছে। অনুগ্রহ করে পুনরায় ডাউনলোড করুন।"
                )
            )
            refreshSnapshotFromDao(dao)
            return@withContext KitabOpenReadResult(
                canOpen = false,
                errorMessageBangla = "স্থানীয় PDF ফাইলটি ক্ষতিগ্রস্ত হওয়ায় ওপেন ব্লক করা হয়েছে। অনুগ্রহ করে পুনরায় ডাউনলোড করুন।"
            )
        }

        val totalPages = if (localState.totalPageCount > 0) {
            localState.totalPageCount
        } else {
            inspectPdfPageCount(finalFile).coerceAtLeast(1)
        }
        val startPage = localState.lastReadPage.coerceIn(1, totalPages)

        KitabOpenReadResult(
            canOpen = true,
            pdfFile = finalFile,
            totalPages = totalPages,
            initialPage = startPage
        )
    }

    fun verifyReadyBookOnDiskFast(context: Context, book: KitabItemUiModel): KitabOpenReadResult? {
        if (!book.isReadyToReadOffline) return null
        val appContext = context.applicationContext ?: context
        val finalFile = getFinalPdfFile(appContext, book.bookId)
        if (!finalFile.exists() || finalFile.length() <= 0L || !hasValidPdfHeader(finalFile)) return null
        if (book.fileSizeBytes > 0L && finalFile.length() != book.fileSizeBytes) return null
        val totalPages = if (book.totalPageCount > 0) {
            book.totalPageCount
        } else {
            inspectPdfPageCount(finalFile).coerceAtLeast(1)
        }
        val startPage = book.lastReadPage.coerceIn(1, totalPages)
        return KitabOpenReadResult(
            canOpen = true,
            pdfFile = finalFile,
            totalPages = totalPages,
            initialPage = startPage
        )
    }

    fun recordPageRead(context: Context, bookId: String, pageNumber: Int, totalPages: Int) {
        val safePage = pageNumber.coerceAtLeast(1)
        _catalogItemsFlow.value = _catalogItemsFlow.value.map { item ->
            if (item.bookId == bookId) {
                item.copy(
                    lastReadPage = safePage,
                    totalPageCount = if (totalPages > 0) totalPages else item.totalPageCount
                )
            } else {
                item
            }
        }
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            recordPageReadSynchronously(appContext, bookId, safePage, totalPages)
        }
    }

    suspend fun recordPageReadSynchronously(
        context: Context,
        bookId: String,
        pageNumber: Int,
        totalPages: Int
    ) = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val safePage = pageNumber.coerceAtLeast(1)
        val existing = dao.getLocalStateById(bookId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.updateLastReadPage(
                bookId = bookId,
                pageNumber = safePage,
                totalPages = totalPages,
                timestampMillis = now
            )
        } else {
            val meta = dao.getCatalogItemById(bookId)
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.NOT_DOWNLOADED.name,
                    totalBytes = meta?.fileSizeBytes ?: 0L,
                    lastReadPage = safePage,
                    totalPageCount = totalPages.coerceAtLeast(1),
                    lastReadTimestampMillis = now
                )
            )
        }
        refreshSnapshotFromDao(dao)
    }

    fun toggleBookmark(
        context: Context,
        bookId: String,
        bookTitleBangla: String,
        volumeNumber: Int,
        pageNumber: Int,
        note: String = "গুরুত্বপূর্ণ পৃষ্ঠা"
    ) {
        val currentList = _bookmarksFlow.value
        val existingInMemory = currentList.firstOrNull { it.bookId == bookId && it.pageNumber == pageNumber }
        _bookmarksFlow.value = if (existingInMemory != null) {
            currentList.filterNot { it.bookId == bookId && it.pageNumber == pageNumber }
        } else {
            listOf(
                KitabBookmarkModel(
                    id = System.nanoTime(),
                    bookId = bookId,
                    bookTitleBangla = bookTitleBangla,
                    volumeNumber = volumeNumber,
                    pageNumber = pageNumber,
                    note = note.ifBlank { "গুরুত্বপূর্ণ পৃষ্ঠা" },
                    createdAtMillis = System.currentTimeMillis()
                )
            ) + currentList
        }
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            toggleBookmarkSynchronously(appContext, bookId, bookTitleBangla, volumeNumber, pageNumber, note)
        }
    }

    suspend fun toggleBookmarkSynchronously(
        context: Context,
        bookId: String,
        bookTitleBangla: String,
        volumeNumber: Int,
        pageNumber: Int,
        note: String = "গুরুত্বপূর্ণ পৃষ্ঠা"
    ) = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val existing = dao.getBookmarksForBookOnce(bookId).firstOrNull { it.pageNumber == pageNumber }
        if (existing != null) {
            dao.deleteBookmarkForPage(bookId, pageNumber)
        } else {
            dao.insertBookmark(
                KitabBookmarkEntity(
                    bookId = bookId,
                    bookTitleBangla = bookTitleBangla,
                    volumeNumber = volumeNumber,
                    pageNumber = pageNumber,
                    note = note.ifBlank { "গুরুত্বপূর্ণ পৃষ্ঠা" },
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
        refreshSnapshotFromDao(dao)
    }

    fun removeBookmarkById(context: Context, bookmarkId: Long) {
        _bookmarksFlow.value = _bookmarksFlow.value.filterNot { it.id == bookmarkId }
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            dao.deleteBookmarkById(bookmarkId)
            refreshSnapshotFromDao(dao)
        }
    }

    suspend fun renderKitabPageBitmap(
        context: Context,
        bookId: String,
        pageNumber: Int,
        targetWidthPx: Int = 1080
    ): Bitmap? = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        val pdfFile = getFinalPdfFile(appContext, bookId)
        if (!pdfFile.exists() || !hasValidPdfHeader(pdfFile)) return@withContext null

        val safePage = pageNumber.coerceAtLeast(1)
        val cacheKey = "$bookId:$safePage:$targetWidthPx"
        pageBitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        pdfRenderMutex.withLock {
            pageBitmapCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }

            val rendered = runCatching {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val pageCount = renderer.pageCount
                        if (pageCount <= 0) return@use null
                        val pageIndex = (safePage - 1).coerceIn(0, pageCount - 1)
                        renderer.openPage(pageIndex).use { page ->
                            val width = targetWidthPx.coerceIn(480, 2200)
                            val ratio = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.42f
                            val height = (width * ratio).toInt().coerceIn(640, 3200)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }.getOrNull() ?: createFallbackKitabPageBitmap(bookId, safePage, targetWidthPx)

            pageBitmapCache.put(cacheKey, rendered)
            rendered
        }
    }

    private fun createFallbackKitabPageBitmap(
        bookId: String,
        pageNumber: Int,
        targetWidthPx: Int
    ): Bitmap {
        val width = targetWidthPx.coerceIn(480, 1400)
        val height = (width * 1.42f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#FCF8EE"))
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#074E35")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#074E35")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "কিতাব পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(pageNumber)}",
            width / 2f,
            height / 2f,
            textPaint
        )
        return bitmap
    }

    fun setReaderThemeMode(context: Context, themeMode: KitabReaderThemeMode) {
        _readerThemeFlow.value = themeMode
        val appContext = context.applicationContext ?: context
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_THEME, themeMode.id)
            .apply()
    }

    fun updateReaderSettings(
        context: Context,
        transform: (KitabReaderSettings) -> KitabReaderSettings
    ) {
        val updated = transform(_readerSettingsFlow.value)
        _readerSettingsFlow.value = updated
        val appContext = context.applicationContext ?: context
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_PAGE_MODE, updated.pageMode.id)
            .putString(KEY_READER_ZOOM_FIT, updated.zoomFitMode.id)
            .putString(KEY_READER_ORIENTATION, updated.orientationMode.id)
            .putBoolean(KEY_READER_KEEP_SCREEN_ON, updated.keepScreenOn)
            .putFloat(KEY_READER_BRIGHTNESS, updated.brightnessLevel)
            .putInt(KEY_READER_PAGE_SPACING, updated.pageSpacingDp)
            .apply()
    }

    private fun loadReaderSettingsFromPrefs(prefs: android.content.SharedPreferences): KitabReaderSettings {
        val pageModeId = prefs.getString(KEY_READER_PAGE_MODE, KitabReaderPageMode.HORIZONTAL_PAGER.id)
        val zoomFitId = prefs.getString(KEY_READER_ZOOM_FIT, KitabZoomFitMode.FIT_PAGE.id)
        val orientationId = prefs.getString(KEY_READER_ORIENTATION, KitabOrientationMode.AUTO.id)
        return KitabReaderSettings(
            pageMode = KitabReaderPageMode.entries.firstOrNull { it.id == pageModeId }
                ?: KitabReaderPageMode.HORIZONTAL_PAGER,
            zoomFitMode = KitabZoomFitMode.entries.firstOrNull { it.id == zoomFitId }
                ?: KitabZoomFitMode.FIT_PAGE,
            orientationMode = KitabOrientationMode.entries.firstOrNull { it.id == orientationId }
                ?: KitabOrientationMode.AUTO,
            keepScreenOn = prefs.getBoolean(KEY_READER_KEEP_SCREEN_ON, true),
            brightnessLevel = prefs.getFloat(KEY_READER_BRIGHTNESS, 1.0f).coerceIn(0.25f, 1.0f),
            pageSpacingDp = prefs.getInt(KEY_READER_PAGE_SPACING, 8).coerceIn(0, 24)
        )
    }

    fun publishApprovedKitabAsAdmin(
        context: Context,
        entity: KitabMetadataEntity,
        onResult: (Boolean, String) -> Unit
    ) {
        if (entity.titleBangla.isBlank() || entity.authorBangla.isBlank() || entity.publisherBangla.isBlank()) {
            onResult(false, "কিতাবের নাম, লেখক ও প্রকাশনীর নাম আবশ্যক।")
            return
        }
        if (entity.isOpenLicenseVerified &&
            (!entity.downloadUrl.startsWith("https://") || entity.fileSizeBytes <= 0L || entity.sha256Checksum.length != 64)
        ) {
            onResult(false, "ডাউনলোডযোগ্য কিতাবের জন্য বৈধ HTTPS লিংক, ফাইলের আকার এবং ৬৪ অক্ষরের SHA-256 Checksum আবশ্যক।")
            return
        }

        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            dao.upsertCatalogItem(entity.copy(isAdminApproved = true))
            refreshSnapshotFromDao(dao)

            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("kitab_catalog")
                    .document(entity.bookId)
                    .set(
                        mapOf(
                            "bookId" to entity.bookId,
                            "seriesId" to entity.seriesId,
                            "titleBangla" to entity.titleBangla,
                            "arabicTitle" to entity.arabicTitle,
                            "authorBangla" to entity.authorBangla,
                            "translatorOrCompilerBangla" to entity.translatorOrCompilerBangla,
                            "publisherBangla" to entity.publisherBangla,
                            "categoryId" to entity.categoryId,
                            "volumeNumber" to entity.volumeNumber,
                            "totalVolumes" to entity.totalVolumes,
                            "fileSizeBytes" to entity.fileSizeBytes,
                            "languageBangla" to entity.languageBangla,
                            "shortDescriptionBangla" to entity.shortDescriptionBangla,
                            "marhalaBangla" to entity.marhalaBangla,
                            "downloadUrl" to entity.downloadUrl,
                            "sha256Checksum" to entity.sha256Checksum,
                            "isOpenLicenseVerified" to entity.isOpenLicenseVerified,
                            "isAdminApproved" to true,
                            "licenseNoteBangla" to entity.licenseNoteBangla,
                            "coverPrimaryHex" to entity.coverPrimaryHex,
                            "coverAccentHex" to entity.coverAccentHex,
                            "updatedAtMillis" to System.currentTimeMillis()
                        )
                    )
            }
            withContext(Dispatchers.Main) {
                onResult(true, "আলহামদুলিল্লাহ, কিতাবটি ক্যাটালগে সফলভাবে যুক্ত হয়েছে।")
            }
        }
    }

    private fun syncRemoteFirestoreCatalogIfAvailable(context: Context) {
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("kitab_catalog")
                .whereEqualTo("isAdminApproved", true)
                .limit(50)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot == null || snapshot.isEmpty) return@addOnSuccessListener
                    managerScope.launch(ioDispatcher) {
                        val dao = KitabLibraryDatabase.getInstance(context).kitabLibraryDao()
                        val remoteItems = snapshot.documents.mapNotNull { doc ->
                            val bookId = doc.getString("bookId")?.takeIf { it.isNotBlank() } ?: doc.id
                            val titleBangla = doc.getString("titleBangla")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val authorBangla = doc.getString("authorBangla").orEmpty()
                            val publisherBangla = doc.getString("publisherBangla").orEmpty()
                            KitabMetadataEntity(
                                bookId = bookId,
                                seriesId = doc.getString("seriesId").orEmpty().ifBlank { "series_$bookId" },
                                titleBangla = titleBangla,
                                arabicTitle = doc.getString("arabicTitle").orEmpty(),
                                authorBangla = authorBangla,
                                translatorOrCompilerBangla = doc.getString("translatorOrCompilerBangla").orEmpty(),
                                publisherBangla = publisherBangla,
                                categoryId = doc.getString("categoryId").orEmpty().ifBlank { KitabCategory.QAWMI_TEXTBOOK_SHARAH.id },
                                volumeNumber = (doc.getLong("volumeNumber") ?: 1L).toInt().coerceAtLeast(1),
                                totalVolumes = (doc.getLong("totalVolumes") ?: 1L).toInt().coerceAtLeast(1),
                                fileSizeBytes = (doc.getLong("fileSizeBytes") ?: 0L).coerceAtLeast(0L),
                                languageBangla = doc.getString("languageBangla").orEmpty().ifBlank { "বাংলা ও আরবি" },
                                shortDescriptionBangla = doc.getString("shortDescriptionBangla").orEmpty(),
                                marhalaBangla = doc.getString("marhalaBangla").orEmpty().ifBlank { "দাওরায়ে হাদিস" },
                                downloadUrl = doc.getString("downloadUrl").orEmpty(),
                                sha256Checksum = doc.getString("sha256Checksum").orEmpty(),
                                isOpenLicenseVerified = doc.getBoolean("isOpenLicenseVerified") ?: false,
                                isAdminApproved = doc.getBoolean("isAdminApproved") ?: true,
                                licenseNoteBangla = doc.getString("licenseNoteBangla").orEmpty(),
                                coverPrimaryHex = doc.getLong("coverPrimaryHex") ?: 0xFF074E35L,
                                coverAccentHex = doc.getLong("coverAccentHex") ?: 0xFFDDA83AL,
                                updatedAtMillis = doc.getLong("updatedAtMillis") ?: System.currentTimeMillis()
                            )
                        }
                        if (remoteItems.isNotEmpty()) {
                            dao.upsertCatalogItems(remoteItems)
                            refreshSnapshotFromDao(dao)
                        }
                    }
                }
        }
    }
}
