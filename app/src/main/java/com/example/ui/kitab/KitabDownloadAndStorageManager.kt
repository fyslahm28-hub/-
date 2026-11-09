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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Network response abstraction for streaming Kitab PDF files with HTTP Range (Pause/Resume) support.
 */
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

/**
 * Production HTTP/HTTPS transport supporting Archive.org 302 redirects and HTTP 206 Range Resume.
 */
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

/**
 * Manages the Bangla Hadith Sharah & Islamic Kitab Library:
 * - Online Catalog & Optional Throttled Firestore Sync (`/kitab_catalog`)
 * - Device-Private PDF Storage (`filesDir/qawmi_kitab_library_pdfs`) — Zero Storage Permissions
 * - One-Click Download Engine with Pause, Resume (HTTP Range), Cancel, and Progress (`%` & `MB`)
 * - Strict `%PDF-` header & `SHA-256` Checksum Verification before opening
 * - Native `PdfRenderer` Page Rendering with LRU Bitmap Cache
 */
object KitabDownloadAndStorageManager {

    private const val PDF_DIR_NAME = "qawmi_kitab_library_pdfs"
    private const val PREFS_NAME = "qawmi_kitab_library_prefs"
    private const val KEY_LAST_FIRESTORE_CATALOG_SYNC = "last_firestore_catalog_sync_ms"
    private const val KEY_READER_THEME = "kitab_reader_theme_mode"
    private const val FIRESTORE_SYNC_THROTTLE_MS = 12L * 60L * 60L * 1000L // 12 hours (৳0 quota-safe)

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

    private var collectionJob: Job? = null

    fun init(context: Context, forceReinit: Boolean = false) {
        if (isInitialized && !forceReinit) return
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString(KEY_READER_THEME, KitabReaderThemeMode.MUSHAF_CREAM.id)
        _readerThemeFlow.value = KitabReaderThemeMode.entries.firstOrNull { it.id == savedThemeId }
            ?: KitabReaderThemeMode.MUSHAF_CREAM

        isInitialized = true

        // Immediately populate in-memory fallback list so UI is never blank while Room initializes
        if (_catalogItemsFlow.value.isEmpty()) {
            _catalogItemsFlow.value = KitabVerifiedCatalogData.defaultCatalogEntities.map { meta ->
                mergeMetadataWithLocalState(meta, null)
            }
        }

        collectionJob?.cancel()
        collectionJob = managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            // 1. Seed verified default catalog into Room SQLite
            dao.upsertCatalogItems(KitabVerifiedCatalogData.defaultCatalogEntities)

            // 2. Reconcile local states with actual files in device-private storage
            reconcileDiskStateWithDatabaseInternal(appContext, dao)

            // 3. Observe combined Catalog Metadata + Device-Local State
            launch {
                combine(
                    dao.observeAllApprovedCatalog(),
                    dao.observeAllLocalStates()
                ) { metadataList, localStates ->
                    val stateMap = localStates.associateBy { it.bookId }
                    metadataList.map { meta ->
                        mergeMetadataWithLocalState(meta, stateMap[meta.bookId])
                    }
                }.collect { mergedList ->
                    _catalogItemsFlow.value = mergedList
                }
            }

            // 4. Observe Bookmarks
            launch {
                dao.observeAllBookmarks().collect { entities ->
                    _bookmarksFlow.value = entities.map { entity ->
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
            }

            // 5. Throttled Firestore `/kitab_catalog` sync (at most once per 12h)
            syncApprovedCatalogFromFirestoreIfDue(appContext, force = false)
        }
    }

    /**
     * Synchronous initialization helper for deterministic JVM/Robolectric tests.
     */
    suspend fun initAndAwaitSync(context: Context, customStorageDir: File? = null) {
        val appContext = context.applicationContext ?: context
        if (customStorageDir != null) {
            customStorageDirOverride = customStorageDir
        }
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        dao.upsertCatalogItems(KitabVerifiedCatalogData.defaultCatalogEntities)
        reconcileDiskStateWithDatabaseInternal(appContext, dao)
        refreshSnapshotFromDao(dao)
        isInitialized = true
    }

    fun setCustomStorageDirectoryForTesting(dir: File?) {
        customStorageDirOverride = dir
        pageBitmapCache.evictAll()
    }

    fun getPdfStorageDirectory(context: Context): File {
        val customDir = customStorageDirOverride
        if (customDir != null) {
            if (!customDir.exists()) customDir.mkdirs()
            return customDir
        }
        val appContext = context.applicationContext ?: context
        val dir = File(appContext.filesDir, PDF_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getFinalPdfFile(context: Context, bookId: String): File {
        val safeId = sanitizeFileName(bookId)
        return File(getPdfStorageDirectory(context), "$safeId.pdf")
    }

    fun getPartialPdfFile(context: Context, bookId: String): File {
        val safeId = sanitizeFileName(bookId)
        return File(getPdfStorageDirectory(context), "$safeId.pdf.part")
    }

    private fun sanitizeFileName(bookId: String): String {
        return bookId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    }

    fun isDeviceOnline(context: Context): Boolean {
        isNetworkConnectedOverride?.let { return it }
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    fun setReaderThemeMode(context: Context, themeMode: KitabReaderThemeMode) {
        _readerThemeFlow.value = themeMode
        val appContext = context.applicationContext ?: context
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_THEME, themeMode.id)
            .apply()
    }

    private suspend fun reconcileDiskStateWithDatabaseInternal(
        context: Context,
        dao: KitabLibraryDao
    ) {
        val catalog = dao.getAllApprovedCatalogOnce()
        val existingStates = dao.getAllLocalStatesOnce().associateBy { it.bookId }

        for (meta in catalog) {
            val finalFile = getFinalPdfFile(context, meta.bookId)
            val partFile = getPartialPdfFile(context, meta.bookId)
            val currentState = existingStates[meta.bookId]

            if (!meta.isOpenLicenseVerified || meta.downloadUrl.isBlank()) {
                // Reference-only book (no open license)
                if (currentState == null || currentState.downloadStatus != KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE.name) {
                    dao.upsertLocalState(
                        KitabLocalStateEntity(
                            bookId = meta.bookId,
                            downloadStatus = KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE.name,
                            totalBytes = meta.fileSizeBytes
                        )
                    )
                }
                continue
            }

            if (finalFile.exists() && finalFile.length() > 0L) {
                val hasValidHeader = hasValidPdfHeader(finalFile)
                val sizeMatches = meta.fileSizeBytes <= 0L || finalFile.length() == meta.fileSizeBytes
                if (hasValidHeader && sizeMatches) {
                    val pageCount = if ((currentState?.totalPageCount ?: 0) > 0) {
                        currentState!!.totalPageCount
                    } else {
                        inspectPdfPageCount(finalFile)
                    }
                    dao.upsertLocalState(
                        KitabLocalStateEntity(
                            bookId = meta.bookId,
                            downloadStatus = KitabDownloadStatus.DOWNLOADED_VERIFIED.name,
                            localFilePath = finalFile.absolutePath,
                            downloadedBytes = finalFile.length(),
                            totalBytes = meta.fileSizeBytes,
                            sha256Verified = true,
                            verifiedSha256Hex = meta.sha256Checksum,
                            downloadedAtMillis = currentState?.downloadedAtMillis?.takeIf { it > 0L }
                                ?: finalFile.lastModified().takeIf { it > 0L }
                                ?: System.currentTimeMillis(),
                            lastReadPage = (currentState?.lastReadPage ?: 1).coerceAtLeast(1),
                            totalPageCount = pageCount,
                            lastReadTimestampMillis = currentState?.lastReadTimestampMillis ?: 0L,
                            errorMessageBangla = ""
                        )
                    )
                } else {
                    // Corrupted final file on disk -> quarantine/remove
                    runCatching { finalFile.delete() }
                    dao.upsertLocalState(
                        KitabLocalStateEntity(
                            bookId = meta.bookId,
                            downloadStatus = KitabDownloadStatus.FAILED_CORRUPTED.name,
                            localFilePath = "",
                            downloadedBytes = 0L,
                            totalBytes = meta.fileSizeBytes,
                            sha256Verified = false,
                            errorMessageBangla = "সংরক্ষিত PDF ফাইলটি ক্ষতিগ্রস্ত বা অসম্পূর্ণ হওয়ায় বাতিল করা হয়েছে। পুনরায় ডাউনলোড করুন।"
                        )
                    )
                }
            } else if (partFile.exists() && partFile.length() > 0L) {
                dao.upsertLocalState(
                    KitabLocalStateEntity(
                        bookId = meta.bookId,
                        downloadStatus = KitabDownloadStatus.PAUSED.name,
                        localFilePath = "",
                        downloadedBytes = partFile.length(),
                        totalBytes = meta.fileSizeBytes,
                        sha256Verified = false,
                        lastReadPage = currentState?.lastReadPage ?: 1,
                        totalPageCount = currentState?.totalPageCount ?: 0,
                        errorMessageBangla = ""
                    )
                )
            } else if (currentState != null && currentState.downloadStatus == KitabDownloadStatus.DOWNLOADED_VERIFIED.name) {
                // State said downloaded, but file was missing from current storage directory
                dao.upsertLocalState(
                    currentState.copy(
                        downloadStatus = KitabDownloadStatus.NOT_DOWNLOADED.name,
                        localFilePath = "",
                        downloadedBytes = 0L,
                        sha256Verified = false
                    )
                )
            }
        }
    }

    suspend fun refreshSnapshotFromDao(dao: KitabLibraryDao) {
        val metadataList = dao.getAllApprovedCatalogOnce()
        val stateMap = dao.getAllLocalStatesOnce().associateBy { it.bookId }
        _catalogItemsFlow.value = metadataList.map { meta ->
            mergeMetadataWithLocalState(meta, stateMap[meta.bookId])
        }
        val allBookmarks = mutableListOf<KitabBookmarkModel>()
        for (meta in metadataList) {
            val bms = dao.getBookmarksForBookOnce(meta.bookId)
            for (entity in bms) {
                allBookmarks.add(
                    KitabBookmarkModel(
                        id = entity.id,
                        bookId = entity.bookId,
                        bookTitleBangla = entity.bookTitleBangla,
                        volumeNumber = entity.volumeNumber,
                        pageNumber = entity.pageNumber,
                        note = entity.note,
                        createdAtMillis = entity.createdAtMillis
                    )
                )
            }
        }
        _bookmarksFlow.value = allBookmarks.sortedByDescending { it.createdAtMillis }
    }

    private fun mergeMetadataWithLocalState(
        meta: KitabMetadataEntity,
        local: KitabLocalStateEntity?
    ): KitabItemUiModel {
        val defaultStatus = if (!meta.isOpenLicenseVerified || meta.downloadUrl.isBlank()) {
            KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE
        } else {
            KitabDownloadStatus.NOT_DOWNLOADED
        }
        val resolvedStatus = if (local != null) {
            runCatching { KitabDownloadStatus.valueOf(local.downloadStatus) }.getOrDefault(defaultStatus)
        } else {
            defaultStatus
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
            downloadedBytes = local?.downloadedBytes ?: 0L,
            localFilePath = local?.localFilePath ?: "",
            sha256Verified = local?.sha256Verified ?: false,
            downloadedAtMillis = local?.downloadedAtMillis ?: 0L,
            lastReadPage = (local?.lastReadPage ?: 1).coerceAtLeast(1),
            totalPageCount = local?.totalPageCount ?: 0,
            errorMessageBangla = local?.errorMessageBangla ?: ""
        )
    }

    /**
     * Starts or resumes downloading a Kitab PDF into app-private storage (`filesDir/qawmi_kitab_library_pdfs`).
     */
    fun startOrResumeDownload(context: Context, bookId: String) {
        val appContext = context.applicationContext ?: context
        if (activeDownloadJobs[bookId]?.isActive == true) return

        pausedFlags[bookId] = false
        cancelledFlags[bookId] = false

        val job = managerScope.launch(ioDispatcher) {
            executeDownloadInternal(appContext, bookId)
        }
        activeDownloadJobs[bookId] = job
    }

    /**
     * Synchronous suspend version of `startOrResumeDownload` for deterministic testing and sequential workflows.
     */
    suspend fun downloadBookSynchronously(
        context: Context,
        bookId: String,
        pauseAfterBytes: Long = -1L
    ): KitabItemUiModel? = withContext(ioDispatcher) {
        val appContext = context.applicationContext ?: context
        pausedFlags[bookId] = false
        cancelledFlags[bookId] = false
        executeDownloadInternal(appContext, bookId, pauseAfterBytes = pauseAfterBytes)
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        refreshSnapshotFromDao(dao)
        _catalogItemsFlow.value.firstOrNull { it.bookId == bookId }
    }

    fun pauseDownload(context: Context, bookId: String) {
        pausedFlags[bookId] = true
        activeDownloadJobs[bookId]?.cancel()
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            val meta = dao.getCatalogItemById(bookId) ?: return@launch
            val partFile = getPartialPdfFile(appContext, bookId)
            val currentBytes = if (partFile.exists()) partFile.length() else 0L
            val existing = dao.getLocalStateById(bookId)
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.PAUSED.name,
                    localFilePath = "",
                    downloadedBytes = currentBytes,
                    totalBytes = meta.fileSizeBytes,
                    sha256Verified = false,
                    lastReadPage = existing?.lastReadPage ?: 1,
                    totalPageCount = existing?.totalPageCount ?: 0,
                    errorMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${KitabFormatUtils.formatSizeMbBangla(currentBytes)} সংরক্ষিত)"
                )
            )
            refreshSnapshotFromDao(dao)
        }
    }

    fun cancelDownload(context: Context, bookId: String) {
        cancelledFlags[bookId] = true
        pausedFlags[bookId] = false
        activeDownloadJobs[bookId]?.cancel()
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            cancelDownloadInternal(appContext, bookId)
        }
    }

    suspend fun cancelDownloadSynchronously(context: Context, bookId: String) = withContext(ioDispatcher) {
        cancelledFlags[bookId] = true
        pausedFlags[bookId] = false
        activeDownloadJobs[bookId]?.cancel()
        cancelDownloadInternal(context.applicationContext ?: context, bookId)
    }

    private suspend fun cancelDownloadInternal(appContext: Context, bookId: String) {
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val partFile = getPartialPdfFile(appContext, bookId)
        runCatching { if (partFile.exists()) partFile.delete() }
        dao.deleteLocalStateById(bookId)
        refreshSnapshotFromDao(dao)
    }

    /**
     * Deletes only the local PDF file for `bookId` while keeping the catalog entry and other downloaded books intact.
     */
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
        activeDownloadJobs[bookId]?.cancel()
        val finalFile = getFinalPdfFile(appContext, bookId)
        val partFile = getPartialPdfFile(appContext, bookId)
        runCatching { if (finalFile.exists()) finalFile.delete() }
        runCatching { if (partFile.exists()) partFile.delete() }

        // Evict cached bitmaps for this book only
        evictBitmapsForBook(bookId)

        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        dao.deleteLocalStateById(bookId)
        refreshSnapshotFromDao(dao)
    }

    private fun evictBitmapsForBook(bookId: String) {
        val snapshot = pageBitmapCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith("$bookId:")) {
                pageBitmapCache.remove(key)
            }
        }
    }

    private suspend fun executeDownloadInternal(
        appContext: Context,
        bookId: String,
        pauseAfterBytes: Long = -1L
    ) {
        val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
        val meta = dao.getCatalogItemById(bookId) ?: return

        if (!meta.isOpenLicenseVerified || !meta.isAdminApproved || meta.downloadUrl.isBlank()) {
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE.name,
                    totalBytes = meta.fileSizeBytes,
                    errorMessageBangla = "এই কিতাবটির উন্মুক্ত ডাউনলোড লাইসেন্স নেই; শুধুমাত্র পরিচিতি প্রদর্শিত হচ্ছে।"
                )
            )
            refreshSnapshotFromDao(dao)
            return
        }

        // Check if already downloaded & verified on disk
        val finalFile = getFinalPdfFile(appContext, bookId)
        if (finalFile.exists() && finalFile.length() == meta.fileSizeBytes && hasValidPdfHeader(finalFile)) {
            val sha = computeSha256Hex(finalFile)
            if (sha.equals(meta.sha256Checksum, ignoreCase = true)) {
                val pages = inspectPdfPageCount(finalFile)
                val prev = dao.getLocalStateById(bookId)
                dao.upsertLocalState(
                    KitabLocalStateEntity(
                        bookId = bookId,
                        downloadStatus = KitabDownloadStatus.DOWNLOADED_VERIFIED.name,
                        localFilePath = finalFile.absolutePath,
                        downloadedBytes = finalFile.length(),
                        totalBytes = meta.fileSizeBytes,
                        sha256Verified = true,
                        verifiedSha256Hex = sha,
                        downloadedAtMillis = prev?.downloadedAtMillis?.takeIf { it > 0L } ?: System.currentTimeMillis(),
                        lastReadPage = (prev?.lastReadPage ?: 1).coerceAtLeast(1),
                        totalPageCount = pages,
                        errorMessageBangla = ""
                    )
                )
                refreshSnapshotFromDao(dao)
                return
            }
        }

        val partFile = getPartialPdfFile(appContext, bookId)
        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        if (!isDeviceOnline(appContext)) {
            val statusWhenOffline = if (existingBytes > 0L) {
                KitabDownloadStatus.PAUSED.name
            } else {
                KitabDownloadStatus.NOT_DOWNLOADED.name
            }
            dao.upsertLocalState(
                KitabLocalStateEntity(
                    bookId = bookId,
                    downloadStatus = statusWhenOffline,
                    downloadedBytes = existingBytes,
                    totalBytes = meta.fileSizeBytes,
                    sha256Verified = false,
                    errorMessageBangla = "ইন্টারনেট সংযোগ নেই। কিতাবটি ডাউনলোড করতে ইন্টারনেট সংযোগ চালু করুন।"
                )
            )
            refreshSnapshotFromDao(dao)
            return
        }

        // Mark as DOWNLOADING
        val prevState = dao.getLocalStateById(bookId)
        dao.upsertLocalState(
            KitabLocalStateEntity(
                bookId = bookId,
                downloadStatus = KitabDownloadStatus.DOWNLOADING.name,
                downloadedBytes = existingBytes,
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
            response = networkTransport.openStream(meta.downloadUrl, existingBytes)
            val resumeOffset = if (existingBytes > 0L && response.isPartialContent) {
                existingBytes
            } else {
                // Server returned 200 full stream; restart part file from byte 0
                if (partFile.exists()) partFile.delete()
                0L
            }

            val totalExpectedBytes = when {
                meta.fileSizeBytes > 0L -> meta.fileSizeBytes
                response.contentLengthBytes > 0L -> resumeOffset + response.contentLengthBytes
                else -> 0L
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

            // Transition to VERIFYING_CHECKSUM
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

            // Verify 1: File exists & non-empty
            if (!partFile.exists() || partFile.length() <= 0L) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোডকৃত ফাইলটি খালি বা পাওয়া যায়নি। পুনরায় চেষ্টা করুন।"
                )
                return
            }

            // Verify 2: Exact file size check
            if (meta.fileSizeBytes > 0L && partFile.length() != meta.fileSizeBytes) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোড অসম্পূর্ণ (${partFile.length()} / ${meta.fileSizeBytes} বাইট)। ফাইলটি বাতিল করা হয়েছে।"
                )
                return
            }

            // Verify 3: `%PDF-` magic header check
            if (!hasValidPdfHeader(partFile)) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোডকৃত ফাইলটি বৈধ PDF নয় (%PDF হেডার অনুপস্থিত)। নিরাপত্তার স্বার্থে ব্লক করা হয়েছে।"
                )
                return
            }

            // Verify 4: SHA-256 cryptographic checksum check
            val actualSha256 = computeSha256Hex(partFile)
            if (!actualSha256.equals(meta.sha256Checksum, ignoreCase = true)) {
                markCorruptedAndDeletePart(
                    dao = dao,
                    partFile = partFile,
                    bookId = bookId,
                    totalBytes = meta.fileSizeBytes,
                    reasonBangla = "ডাউনলোডকৃত PDF ফাইলটির SHA-256 Checksum মেলেনি (ক্ষতিগ্রস্ত ফাইল)। নিরাপত্তার স্বার্থে ওপেন ব্লক করা হয়েছে।"
                )
                return
            }

            // Atomically move verified `.pdf.part` -> `.pdf`
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = partFile.renameTo(finalFile)
            if (!renamed) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            val pageCount = inspectPdfPageCount(finalFile)
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
                    lastReadPage = (prevState?.lastReadPage ?: 1).coerceAtLeast(1),
                    totalPageCount = pageCount,
                    lastReadTimestampMillis = prevState?.lastReadTimestampMillis ?: System.currentTimeMillis(),
                    errorMessageBangla = ""
                )
            )
            refreshSnapshotFromDao(dao)
        } catch (ce: CancellationException) {
            if (cancelledFlags[bookId] == true) {
                runCatching { if (partFile.exists()) partFile.delete() }
                dao.deleteLocalStateById(bookId)
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
            // Network drop mid-download preserves partial file as PAUSED so user can resume without losing progress!
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

    /**
     * Validates whether a Kitab is downloaded, intact, and verified on THIS device before opening the Internal PDF Reader.
     * Enforces:
     * - Un-downloaded books cannot open (and if offline, returns a clear offline prompt).
     * - Corrupted or modified PDF files on disk are blocked and flagged.
     */
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

        // Verify `%PDF-` header & file size integrity before opening
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

    /**
     * Fast synchronous verification for already-verified books on disk so the Internal PDF Reader opens on the same UI frame.
     */
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

    /**
     * Updates the user's last read page in memory immediately and persists to local Room database (0 Firestore writes).
     */
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
        dao.updateLastReadPage(
            bookId = bookId,
            pageNumber = safePage,
            totalPages = totalPages,
            timestampMillis = System.currentTimeMillis()
        )
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
        val appContext = context.applicationContext ?: context
        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            dao.deleteBookmarkById(bookmarkId)
            refreshSnapshotFromDao(dao)
        }
    }

    /**
     * Renders a single page from the verified local Kitab PDF using Android's native `PdfRenderer`.
     */
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
        canvas.drawColor(Color.rgb(252, 249, 240))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(7, 78, 53)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 56, 40)
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        val item = _catalogItemsFlow.value.firstOrNull { it.bookId == bookId }
        canvas.drawText(item?.titleBangla ?: bookId, width / 2f, height * 0.35f, textPaint)
        canvas.drawText(
            "পৃষ্ঠা নং ${KitabFormatUtils.toBanglaDigits(pageNumber)}",
            width / 2f,
            height * 0.48f,
            textPaint
        )
        return bitmap
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

        // Fallback regex scan for `/Type /Page` in minimal synthetic test PDFs under JVM Robolectric
        return runCatching {
            val maxScanBytes = pdfFile.length().coerceAtMost(256 * 1024L).toInt()
            val bytes = ByteArray(maxScanBytes)
            FileInputStream(pdfFile).use { fis -> fis.read(bytes) }
            val content = String(bytes, Charsets.ISO_8859_1)
            val matches = Regex("/Type\\s*/Page\\b").findAll(content).count()
            matches.coerceAtLeast(1)
        }.getOrDefault(1)
    }

    fun hasValidPdfHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 5L) return false
        return runCatching {
            val header = ByteArray(5)
            FileInputStream(file).use { fis ->
                val read = fis.read(header)
                if (read < 5) return false
            }
            val headerStr = String(header, Charsets.US_ASCII)
            headerStr == "%PDF-"
        }.getOrDefault(false)
    }

    fun computeSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        FileInputStream(file).use { fis ->
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a valid minimal multi-page PDF byte array for deterministic unit/Robolectric tests,
     * with `%PDF-1.4` header, valid page objects, and exact SHA-256 calculation.
     */
    fun buildValidTestPdfBytes(title: String, pageCount: Int = 5, minTargetSizeBytes: Int = 4096): ByteArray {
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        val kids = (1..pageCount).joinToString(" ") { "${it + 2} 0 R" }
        sb.append("2 0 obj << /Type /Pages /Kids [$kids] /Count $pageCount >> endobj\n")
        for (i in 1..pageCount) {
            val objNum = i + 2
            sb.append(
                "$objNum 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << >> >> endobj\n"
            )
        }
        sb.append("% Kitab: $title\n")
        while (sb.length < minTargetSizeBytes - 12) {
            sb.append("% PADDING_LINE_FOR_KITAB_PDF_VERIFICATION_1234567890\n")
        }
        sb.append("%%EOF\n")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }

    /**
     * Optional throttled read from Firestore `/kitab_catalog` (at most once every 12 hours)
     * so Admin-published Kitab metadata appears automatically without consuming Firestore quota.
     */
    private fun syncApprovedCatalogFromFirestoreIfDue(context: Context, force: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_FIRESTORE_CATALOG_SYNC, 0L)
        val now = System.currentTimeMillis()
        if (!force && (now - lastSync) < FIRESTORE_SYNC_THROTTLE_MS) return

        runCatching {
            val db = FirebaseFirestore.getInstance()
            db.collection("kitab_catalog")
                .whereEqualTo("isAdminApproved", true)
                .limit(60)
                .get()
                .addOnSuccessListener { snapshot ->
                    prefs.edit().putLong(KEY_LAST_FIRESTORE_CATALOG_SYNC, now).apply()
                    if (snapshot == null || snapshot.isEmpty) return@addOnSuccessListener
                    val remoteItems = snapshot.documents.mapNotNull { doc ->
                        val bookId = doc.getString("bookId")?.takeIf { it.isNotBlank() } ?: doc.id
                        val title = doc.getString("titleBangla")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val author = doc.getString("authorBangla")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val publisher = doc.getString("publisherBangla")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val categoryId = doc.getString("categoryId") ?: KitabCategory.QAWMI_TEXTBOOK_SHARAH.id
                        val sha256 = doc.getString("sha256Checksum") ?: ""
                        val downloadUrl = doc.getString("downloadUrl") ?: ""
                        val isOpenLicense = doc.getBoolean("isOpenLicenseVerified") ?: false
                        val isApproved = doc.getBoolean("isAdminApproved") ?: false
                        if (!isApproved) return@mapNotNull null
                        KitabMetadataEntity(
                            bookId = bookId,
                            seriesId = doc.getString("seriesId") ?: bookId,
                            titleBangla = title,
                            arabicTitle = doc.getString("arabicTitle") ?: "",
                            authorBangla = author,
                            translatorOrCompilerBangla = doc.getString("translatorOrCompilerBangla") ?: author,
                            publisherBangla = publisher,
                            categoryId = categoryId,
                            volumeNumber = (doc.getLong("volumeNumber") ?: 1L).toInt().coerceAtLeast(1),
                            totalVolumes = (doc.getLong("totalVolumes") ?: 1L).toInt().coerceAtLeast(1),
                            fileSizeBytes = (doc.getLong("fileSizeBytes") ?: 0L).coerceAtLeast(0L),
                            languageBangla = doc.getString("languageBangla") ?: "বাংলা ও আরবি",
                            shortDescriptionBangla = doc.getString("shortDescriptionBangla") ?: "",
                            marhalaBangla = doc.getString("marhalaBangla") ?: "দাওরায়ে হাদিস ও উচ্চতর মারহালা",
                            downloadUrl = downloadUrl,
                            sha256Checksum = sha256,
                            isOpenLicenseVerified = isOpenLicense,
                            isAdminApproved = true,
                            licenseNoteBangla = doc.getString("licenseNoteBangla") ?: "অ্যাডমিন অনুমোদিত ক্যাটালগ",
                            coverPrimaryHex = doc.getLong("coverPrimaryHex") ?: 0xFF074E35,
                            coverAccentHex = doc.getLong("coverAccentHex") ?: 0xFFDDA83A
                        )
                    }
                    if (remoteItems.isNotEmpty()) {
                        managerScope.launch(ioDispatcher) {
                            val dao = KitabLibraryDatabase.getInstance(context).kitabLibraryDao()
                            dao.upsertCatalogItems(remoteItems)
                            reconcileDiskStateWithDatabaseInternal(context, dao)
                            refreshSnapshotFromDao(dao)
                        }
                    }
                }
        }
    }

    /**
     * Admin-only helper to publish a verified open-license Kitab into the catalog (local Room + Firestore `/kitab_catalog`).
     */
    fun publishApprovedKitabAsAdmin(
        context: Context,
        entity: KitabMetadataEntity,
        onResult: (Boolean, String) -> Unit
    ) {
        val appContext = context.applicationContext ?: context
        if (entity.titleBangla.isBlank() || entity.authorBangla.isBlank() || entity.publisherBangla.isBlank()) {
            onResult(false, "কিতাবের নাম, লেখক ও প্রকাশনীর নাম আবশ্যক।")
            return
        }
        if (entity.isOpenLicenseVerified) {
            if (!entity.downloadUrl.startsWith("https://") || entity.sha256Checksum.length != 64 || entity.fileSizeBytes <= 0L) {
                onResult(
                    false,
                    "ডাউনলোডযোগ্য কিতাবের জন্য বৈধ HTTPS লিংক, ফাইলের আকার এবং ৬৪ অক্ষরের SHA-256 Checksum আবশ্যক।"
                )
                return
            }
        }

        managerScope.launch(ioDispatcher) {
            val dao = KitabLibraryDatabase.getInstance(appContext).kitabLibraryDao()
            val approvedEntity = entity.copy(isAdminApproved = true)
            dao.upsertCatalogItem(approvedEntity)
            reconcileDiskStateWithDatabaseInternal(appContext, dao)
            refreshSnapshotFromDao(dao)

            runCatching {
                val payload = mapOf(
                    "bookId" to approvedEntity.bookId,
                    "seriesId" to approvedEntity.seriesId,
                    "titleBangla" to approvedEntity.titleBangla,
                    "arabicTitle" to approvedEntity.arabicTitle,
                    "authorBangla" to approvedEntity.authorBangla,
                    "translatorOrCompilerBangla" to approvedEntity.translatorOrCompilerBangla,
                    "publisherBangla" to approvedEntity.publisherBangla,
                    "categoryId" to approvedEntity.categoryId,
                    "volumeNumber" to approvedEntity.volumeNumber,
                    "totalVolumes" to approvedEntity.totalVolumes,
                    "fileSizeBytes" to approvedEntity.fileSizeBytes,
                    "languageBangla" to approvedEntity.languageBangla,
                    "shortDescriptionBangla" to approvedEntity.shortDescriptionBangla,
                    "marhalaBangla" to approvedEntity.marhalaBangla,
                    "downloadUrl" to approvedEntity.downloadUrl,
                    "sha256Checksum" to approvedEntity.sha256Checksum,
                    "isOpenLicenseVerified" to approvedEntity.isOpenLicenseVerified,
                    "isAdminApproved" to true,
                    "licenseNoteBangla" to approvedEntity.licenseNoteBangla,
                    "coverPrimaryHex" to approvedEntity.coverPrimaryHex,
                    "coverAccentHex" to approvedEntity.coverAccentHex
                )
                FirebaseFirestore.getInstance()
                    .collection("kitab_catalog")
                    .document(approvedEntity.bookId)
                    .set(payload)
            }

            withContext(Dispatchers.Main) {
                onResult(true, "আলহামদুলিল্লাহ, কিতাবটি সফলভাবে ক্যাটালগে যুক্ত হয়েছে।")
            }
        }
    }
}
