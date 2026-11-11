package com.example.ui.islamicbooks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val LibDeepGreen = Color(0xFF0E5A3E)
private val LibDarkGreen = Color(0xFF093B28)
private val LibMintBg = Color(0xFFF3F8F5)
private val LibCreamCard = Color(0xFFFCFBF7)
private val LibGold = Color(0xFFC99A2E)
private val LibGoldLight = Color(0xFFFFF8E6)

private enum class LibrarySubView {
    CATALOG_HOME,
    BOOK_DETAILS,
    PDF_READER,
    SELF_PUBLISHING_PORTAL,
    ADMIN_APPROVAL_DASHBOARD
}

/**
 * ১, ২, ৩, ৪ ও ৫ নং নির্দেশনা অনুযায়ী পূর্ণাঙ্গ ফুল-স্ক্রিন ডিজিটাল ইসলামিক বই লাইব্রেরি পেজ।
 * কোনো অপ্রয়োজনীয় Modal বা Popup ব্যবহার করা হয়নি — সব ভিউ পূর্ণ স্ক্রিন (Full-Screen Content Area) জুড়ে রেন্ডার হয়।
 */
@Composable
fun IslamicBookLibraryScreen(
    currentUserId: String = "user_qawmi_default",
    currentUserName: String = "মাওলানা আব্দুল্লাহ",
    currentUserPhone: String = "01700000000",
    currentUserEmail: String = "",
    isAdmin: Boolean = false,
    onNavigateBackHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val bookManager = remember { IslamicBookManager.getInstance(context) }

    var currentSubView by remember { mutableStateOf(LibrarySubView.CATALOG_HOME) }
    var selectedCategory by remember { mutableStateOf<IslamicBookCategory?>(null) }
    var showOnlyDownloadedMyBooks by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedBook by remember { mutableStateOf<IslamicBookCatalogItem?>(null) }
    var selectedVolumeNumber by remember { mutableIntStateOf(1) }
    var localActionMessage by remember { mutableStateOf<String?>(null) }

    val downloadProgressMap by bookManager.downloadProgressMap.collectAsState()
    val dynamicCatalog by bookManager.dynamicCatalogBooks.collectAsState()

    val displayedBooks = remember(selectedCategory, showOnlyDownloadedMyBooks, searchQuery, dynamicCatalog, downloadProgressMap) {
        val base = if (showOnlyDownloadedMyBooks) {
            bookManager.getDownloadedBooks()
        } else {
            bookManager.searchPublicBooks(searchQuery, selectedCategory)
        }
        if (showOnlyDownloadedMyBooks && searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.titleBangla.lowercase().contains(q) ||
                    it.authorName.lowercase().contains(q) ||
                    it.publisherName.lowercase().contains(q)
            }
        } else {
            base
        }
    }

    // Android Back Navigation
    BackHandler {
        when (currentSubView) {
            LibrarySubView.PDF_READER -> currentSubView = LibrarySubView.BOOK_DETAILS
            LibrarySubView.BOOK_DETAILS -> currentSubView = LibrarySubView.CATALOG_HOME
            LibrarySubView.SELF_PUBLISHING_PORTAL -> currentSubView = LibrarySubView.CATALOG_HOME
            LibrarySubView.ADMIN_APPROVAL_DASHBOARD -> currentSubView = LibrarySubView.CATALOG_HOME
            LibrarySubView.CATALOG_HOME -> onNavigateBackHome()
        }
    }

    when (currentSubView) {
        LibrarySubView.SELF_PUBLISHING_PORTAL -> {
            AuthorPublisherSelfPublishingPortalScreen(
                bookManager = bookManager,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhone = currentUserPhone,
                currentUserEmail = currentUserEmail,
                onBackToLibrary = { currentSubView = LibrarySubView.CATALOG_HOME }
            )
        }

        LibrarySubView.ADMIN_APPROVAL_DASHBOARD -> {
            AdminBookApprovalDashboardScreen(
                bookManager = bookManager,
                adminId = currentUserId,
                adminEmail = currentUserEmail,
                adminPhone = currentUserPhone,
                onBackToLibrary = { currentSubView = LibrarySubView.CATALOG_HOME }
            )
        }

        LibrarySubView.PDF_READER -> {
            val book = selectedBook
            if (book != null) {
                IslamicBookNativePdfReaderScreen(
                    book = book,
                    volumeNumber = selectedVolumeNumber,
                    bookManager = bookManager,
                    onBackToDetails = { currentSubView = LibrarySubView.BOOK_DETAILS }
                )
            } else {
                currentSubView = LibrarySubView.CATALOG_HOME
            }
        }

        LibrarySubView.BOOK_DETAILS -> {
            val book = selectedBook
            if (book != null) {
                val progressKey = bookManager.downloadKey(book.bookId, selectedVolumeNumber)
                val progress = downloadProgressMap[progressKey] ?: IslamicBookDownloadProgress(
                    bookId = book.bookId,
                    volumeNumber = selectedVolumeNumber,
                    state = if (bookManager.isBookDownloaded(book.bookId, selectedVolumeNumber)) {
                        IslamicBookDownloadState.DOWNLOADED
                    } else {
                        IslamicBookDownloadState.NOT_DOWNLOADED
                    }
                )
                IslamicBookDetailsFullScreenView(
                    book = book,
                    selectedVolumeNumber = selectedVolumeNumber,
                    onSelectVolume = { selectedVolumeNumber = it },
                    downloadProgress = progress,
                    lastReadPage = bookManager.getLastReadPage(book.bookId, selectedVolumeNumber),
                    bookmarks = bookManager.getBookmarks(book.bookId, selectedVolumeNumber),
                    onStartOrResumeDownload = {
                        bookManager.startOrResumeDownload(book, selectedVolumeNumber)
                    },
                    onPauseDownload = {
                        bookManager.pauseDownload(book.bookId, selectedVolumeNumber)
                    },
                    onCancelDownload = {
                        bookManager.cancelDownload(book.bookId, selectedVolumeNumber)
                    },
                    onDeleteLocalCopy = {
                        bookManager.deleteLocalDownloadedBook(book.bookId, selectedVolumeNumber)
                        localActionMessage = "ফোন থেকে অফলাইন পিডিএফ কপি মুছে ফেলা হয়েছে। মূল লাইব্রেরিতে বইটি অক্ষুণ্ণ রয়েছে।"
                    },
                    onOpenPdfReader = {
                        if (!bookManager.isBookDownloaded(book.bookId, selectedVolumeNumber)) {
                            bookManager.startOrResumeDownload(book, selectedVolumeNumber)
                        }
                        currentSubView = LibrarySubView.PDF_READER
                    },
                    onBackToCatalog = {
                        currentSubView = LibrarySubView.CATALOG_HOME
                    }
                )
            } else {
                currentSubView = LibrarySubView.CATALOG_HOME
            }
        }

        LibrarySubView.CATALOG_HOME -> {
            var isSearchExpanded by remember { mutableStateOf(false) }
            var bookmarkRefreshTick by remember { mutableIntStateOf(0) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7FAF8))
                    .testTag("islamic_book_library_root")
            ) {
                // মোবাইলে এক সারিতে ৪টি ছোট কিতাবের কার্ড, এবং স্ক্রিনের প্রস্থ অনুযায়ী ৫টি বা ৬টি কার্ডের Responsive Grid
                val columnsCount = when {
                    maxWidth >= 700.dp -> 6
                    maxWidth >= 520.dp -> 5
                    else -> 4
                }
                val chunkedBooks = remember(displayedBooks, columnsCount) {
                    displayedBooks.chunked(columnsCount)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("islamic_book_library_list"),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // 1. Compact Deep Green Header Bar (matching reference image)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LibDeepGreen)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = onNavigateBackHome,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("islamic_book_library_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "হোমে ফিরে যান",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = LibGold.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, LibGold),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = LibGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ইসলামিক বই",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                // Compact Portal & Admin Access + Search Icon
                                Surface(
                                    color = LibGold,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .clickable { currentSubView = LibrarySubView.SELF_PUBLISHING_PORTAL }
                                        .testTag("open_self_publishing_portal_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Publish,
                                            contentDescription = "বই প্রকাশ",
                                            tint = LibDarkGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "বই প্রকাশ",
                                            color = LibDarkGreen,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(0.8.dp, LibGold),
                                        modifier = Modifier
                                            .clickable { currentSubView = LibrarySubView.ADMIN_APPROVAL_DASHBOARD }
                                            .testTag("open_admin_book_dashboard_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = "অ্যাডমিন প্যানেল",
                                                tint = LibGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "অ্যাডমিন",
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { isSearchExpanded = !isSearchExpanded },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("islamic_book_search_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "অনুসন্ধান",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            if (isSearchExpanded || searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "কিতাবের নাম দিয়ে খুঁজুন...",
                                            color = Color.White.copy(alpha = 0.78f),
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = LibGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "মুছুন",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = LibGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.10f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("islamic_book_search_input")
                                )
                            }
                        }
                    }

                    // 2. Clean Category Filter Pills Row (matching Reference Image: "সব বই", "আকীদা", etc.)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF7FAF8))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allSelected = selectedCategory == null && !showOnlyDownloadedMyBooks
                            Surface(
                                color = if (allSelected) LibDeepGreen else Color.White,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(
                                    width = 0.8.dp,
                                    color = if (allSelected) LibDeepGreen else Color(0xFFD5E2DA)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedCategory = null
                                        showOnlyDownloadedMyBooks = false
                                    }
                                    .testTag("islamic_book_category_chip_ALL")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (allSelected) Color.White else LibDeepGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "সব বই",
                                        color = if (allSelected) Color.White else LibDarkGreen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IslamicBookCategory.entries.forEach { cat ->
                                val isSelected = selectedCategory == cat && !showOnlyDownloadedMyBooks
                                Surface(
                                    color = if (isSelected) LibDeepGreen else Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        width = 0.8.dp,
                                        color = if (isSelected) LibDeepGreen else Color(0xFFD5E2DA)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategory = cat
                                            showOnlyDownloadedMyBooks = false
                                        }
                                        .testTag("islamic_book_category_chip_${cat.id}")
                                ) {
                                    Text(
                                        text = cat.titleBangla,
                                        color = if (isSelected) Color.White else LibDarkGreen,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Surface(
                                color = if (showOnlyDownloadedMyBooks) LibDeepGreen else Color.White,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(
                                    width = 0.8.dp,
                                    color = if (showOnlyDownloadedMyBooks) LibDeepGreen else Color(0xFFD5E2DA)
                                ),
                                modifier = Modifier
                                    .clickable { showOnlyDownloadedMyBooks = !showOnlyDownloadedMyBooks }
                                    .testTag("islamic_book_my_books_tab")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = if (showOnlyDownloadedMyBooks) Color.White else LibDeepGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ডাউনলোডকৃত",
                                        color = if (showOnlyDownloadedMyBooks) Color.White else LibDarkGreen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    localActionMessage?.let { msg ->
                        item {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(msg, fontSize = 11.sp, color = LibDarkGreen, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { localActionMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "বন্ধ করুন",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Ultra Compact Book Grid (4 columns on mobile, 5+ on wider screens)
                    if (chunkedBooks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 36.dp)
                                    .testTag("islamic_book_empty_state"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = LibDeepGreen.copy(alpha = 0.65f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (showOnlyDownloadedMyBooks) {
                                            "কোনো ডাউনলোডকৃত কিতাব নেই"
                                        } else {
                                            "এখনো কোনো কিতাব আপলোড করা হয়নি"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = LibDarkGreen,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(chunkedBooks) { rowBooks ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                rowBooks.forEach { book ->
                                    val isBookmarked = remember(book.bookId, bookmarkRefreshTick) {
                                        bookManager.getBookmarks(book.bookId, 1).isNotEmpty()
                                    }

                                    IslamicBookCardItem(
                                        book = book,
                                        isBookmarked = isBookmarked,
                                        modifier = Modifier.weight(1f),
                                        onOpenDetails = {
                                            selectedBook = book
                                            selectedVolumeNumber = 1
                                            currentSubView = LibrarySubView.BOOK_DETAILS
                                        },
                                        onToggleBookmark = {
                                            bookManager.addOrRemoveBookmark(
                                                bookId = book.bookId,
                                                volumeNumber = 1,
                                                pageNumber = 1,
                                                noteBangla = book.titleBangla
                                            )
                                            bookmarkRefreshTick++
                                        }
                                    )
                                }
                                if (rowBooks.size < columnsCount) {
                                    repeat(columnsCount - rowBooks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ১ ও ২ নং নির্দেশনা অনুযায়ী Ultra Compact Book Card:
 * - মোবাইলে এক সারিতে ৪টি ছোট কিতাবের কার্ড, এবং চওড়া স্ক্রিনে ৫/৬টি কার্ড
 * - প্রতিটি কার্ড ছোট, সরল ও একই মাপের (Uniform 94.dp height)
 * - উপরে ছোট কভার Thumbnail (Admin যে ছবিটি Upload করেছে সেটিই দেখাবে)
 * - কভারের ঠিক নিচে শুধু কিতাবের নাম (১–২ লাইনে)
 * - কোনো লেখক, প্রকাশনী, বিবরণ, পৃষ্ঠাসংখ্যা, অতিরিক্ত Badge বা অপ্রয়োজনীয় তথ্য নেই
 * - উপরে ডান কোণায় খুব ছোট ও সরল Bookmark আইকন
 * - কোনো ভারী Shadow, Gradient, Glow, 3D Effect নেই
 */
@Composable
private fun IslamicBookCardItem(
    book: IslamicBookCatalogItem,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, Color(0xFFDCE6DF)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .height(94.dp)
            .clickable { onOpenDetails() }
            .testTag("islamic_book_card_${book.bookId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 5.dp)
        ) {
            // খুব ছোট ও সরল Bookmark আইকন (উপরে ডান কোণায়)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clickable { onToggleBookmark() }
                    .testTag("islamic_book_bookmark_btn_${book.bookId}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "বুকমার্ক",
                    tint = if (isBookmarked) LibGold else LibDeepGreen.copy(alpha = 0.75f),
                    modifier = Modifier.size(12.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // ছোট কভার ছবি Thumbnail (Admin কর্তৃক Upload করা কভার ছবি)
                IslamicBookCoverThumbnail(
                    coverImageUrl = book.coverImageUrl,
                    titleBangla = book.titleBangla,
                    modifier = Modifier
                        .width(38.dp)
                        .height(50.dp)
                        .testTag("islamic_book_cover_${book.bookId}")
                )

                Spacer(modifier = Modifier.height(3.dp))

                // কভারের ঠিক নিচে শুধু কিতাবের নাম (সর্বোচ্চ ১–২ লাইনে)
                Text(
                    text = book.titleBangla,
                    color = LibDarkGreen,
                    fontSize = 10.sp,
                    lineHeight = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("islamic_book_title_${book.bookId}")
                )
            }
        }
    }
}

@Composable
private fun IslamicBookCoverThumbnail(
    coverImageUrl: String,
    titleBangla: String,
    modifier: Modifier = Modifier
) {
    val cleanUrl = coverImageUrl.trim()
    val decodedBase64Bitmap = remember(cleanUrl) {
        if (cleanUrl.startsWith("data:image", ignoreCase = true) && cleanUrl.contains("base64,")) {
            try {
                val base64Part = cleanUrl.substringAfter("base64,")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0E5A3E))
            .border(0.5.dp, LibGold.copy(alpha = 0.55f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBase64Bitmap != null -> {
                Image(
                    bitmap = decodedBase64Bitmap.asImageBitmap(),
                    contentDescription = titleBangla,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            cleanUrl.isNotEmpty() -> {
                // Fallback cover frame behind AsyncImage in case URI is loading or synthetic in tests
                MinimalIslamicBookCoverPlaceholder(titleBangla = titleBangla)
                AsyncImage(
                    model = cleanUrl,
                    contentDescription = titleBangla,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                MinimalIslamicBookCoverPlaceholder(titleBangla = titleBangla)
            }
        }
    }
}

@Composable
private fun MinimalIslamicBookCoverPlaceholder(titleBangla: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = LibGold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = titleBangla,
            color = Color.White,
            fontSize = 6.5.sp,
            lineHeight = 7.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * ৪.২ ও ৫. বই বিস্তারিত পেজ (Book Details Full-Screen Page)
 */
@Composable
private fun IslamicBookDetailsFullScreenView(
    book: IslamicBookCatalogItem,
    selectedVolumeNumber: Int,
    onSelectVolume: (Int) -> Unit,
    downloadProgress: IslamicBookDownloadProgress,
    lastReadPage: Int,
    bookmarks: List<IslamicBookBookmark>,
    onStartOrResumeDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteLocalCopy: () -> Unit,
    onOpenPdfReader: () -> Unit,
    onBackToCatalog: () -> Unit
) {
    val isDownloaded = downloadProgress.state == IslamicBookDownloadState.DOWNLOADED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LibMintBg)
            .testTag("islamic_book_details_screen"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(LibDarkGreen, LibDeepGreen)))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackToCatalog,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("book_details_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "লাইব্রেরিতে ফিরে যান",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "গ্রন্থ পরিচিতি ও ডাউনলোড কেন্দ্র",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Large Book Cover
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(156.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(book.coverBadgeColorHex), Color(0xFF05291C))
                                    )
                                )
                                .padding(10.dp)
                                .testTag("book_details_large_cover"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text("﷽", color = LibGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = book.titleBangla,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = book.publisherName,
                                    color = LibGoldLight,
                                    fontSize = 9.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.titleBangla,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (book.titleArabicOrOriginal.isNotBlank()) {
                                Text(
                                    text = book.titleArabicOrOriginal,
                                    color = LibGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("লেখক: ${book.authorName}", color = Color(0xFFE5F6ED), fontSize = 13.sp)
                            if (book.translatorOrEditorName.isNotBlank()) {
                                Text("অনুবাদক / সম্পাদক: ${book.translatorOrEditorName}", color = Color(0xFFE5F6ED), fontSize = 12.5.sp)
                            }
                            Text("প্রকাশনী: ${book.publisherName}", color = LibGoldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("বিভাগ: ${book.category.titleBangla} • ভাষা: ${book.language}", color = Color(0xFFD9F2E6), fontSize = 12.sp)
                            if (book.isPublicationYearVerified && book.publicationYear.isNotBlank()) {
                                Text("প্রকাশের বছর: ${book.publicationYear} (${book.editionText})", color = Color(0xFFD9F2E6), fontSize = 12.sp)
                            }
                            if (book.isbn.isNotBlank()) {
                                Text("ISBN: ${book.isbn}", color = Color(0xFFD9F2E6), fontSize = 11.5.sp)
                            }
                            Text("পৃষ্ঠা সংখ্যা: ${book.pageCount} • সাইজ: ${book.fileSizeTextBangla}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Volume Selection (if multi-volume)
        if (book.volumes.size > 1) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "খণ্ড নির্বাচন করুন (মোট ${book.volumes.size}টি খণ্ড):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LibDarkGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            book.volumes.forEach { vol ->
                                val selected = selectedVolumeNumber == vol.volumeNumber
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectVolume(vol.volumeNumber) },
                                    label = {
                                        Text("${vol.volumeTitleBangla} (${vol.fileSizeTextBangla})")
                                    },
                                    modifier = Modifier.testTag("book_volume_chip_${vol.volumeNumber}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Download & Offline Reader Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LibGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isDownloaded) "অফলাইনে ডাউনলোড সম্পন্ন ✓" else "পিডিএফ ডাউনলোড ও পঠন নিয়ন্ত্রণ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = LibDarkGreen
                        )
                        Text(
                            text = "সর্বশেষ পঠিত পৃষ্ঠা: $lastReadPage",
                            fontSize = 12.sp,
                            color = LibDeepGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (downloadProgress.state == IslamicBookDownloadState.DOWNLOADING ||
                        downloadProgress.state == IslamicBookDownloadState.PAUSED
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.progressPercent / 100f },
                            color = LibDeepGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("book_download_progress_bar")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ডাউনলোড প্রোগ্রেস: ${downloadProgress.progressPercent}% (${downloadProgress.state})",
                            fontSize = 12.sp,
                            color = LibDarkGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenPdfReader,
                            colors = ButtonDefaults.buttonColors(containerColor = LibDeepGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("book_details_read_btn")
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isDownloaded) "বইটি পড়ুন (অফলাইন)" else "ডাউনলোড ও পড়ুন",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isDownloaded) {
                            OutlinedButton(
                                onClick = onStartOrResumeDownload,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("book_details_download_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (downloadProgress.state == IslamicBookDownloadState.PAUSED) "Resume ডাউনলোড"
                                    else "ডাউনলোড করুন"
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDeleteLocalCopy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("book_delete_local_btn")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("লোকাল কপি মুছুন", fontSize = 12.sp)
                            }
                        }
                    }

                    // Pause / Resume / Cancel Explicit Controls
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPauseDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("book_download_pause_btn")
                        ) {
                            Text("Pause", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onStartOrResumeDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("book_download_resume_btn")
                        ) {
                            Text("Resume", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("book_download_cancel_btn")
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Book Introduction
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "বইয়ের ভূমিকা ও সংক্ষিপ্ত পরিচিতি",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LibDarkGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = book.shortIntroduction,
                        fontSize = 13.5.sp,
                        color = Color(0xFF1F2937),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "কপিরাইট ও প্রকাশনা তথ্য: ${book.copyrightOwnerName} (${book.copyrightPermissionType.labelBangla})",
                        fontSize = 12.sp,
                        color = LibDeepGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Table of Contents
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "সূচিপত্র ও অধ্যায়সমূহ (${book.tableOfContents.size}টি অধ্যায়)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LibDarkGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    book.tableOfContents.forEachIndexed { index, chapter ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(LibDeepGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = LibDeepGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = chapter,
                                fontSize = 13.sp,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }

        // Saved Bookmarks
        if (bookmarks.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LibGoldLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "সংরক্ষিত বুকমার্কসমূহ (${bookmarks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LibDarkGreen
                        )
                        bookmarks.forEach { bm ->
                            Text(
                                text = "• পৃষ্ঠা ${bm.pageNumber} — ${bm.noteBangla}",
                                fontSize = 12.5.sp,
                                color = LibDarkGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ৫. অ্যাপের নিজস্ব ফুল-স্ক্রিন নেটিভ পিডিএফ রিডার (Bookmark, Last Read Page, Zoom, Night Mode সাপোর্টসহ)
 */
@Composable
private fun IslamicBookNativePdfReaderScreen(
    book: IslamicBookCatalogItem,
    volumeNumber: Int,
    bookManager: IslamicBookManager,
    onBackToDetails: () -> Unit
) {
    var totalPages by remember { mutableIntStateOf(12) }
    var currentPage by remember {
        mutableIntStateOf(bookManager.getLastReadPage(book.bookId, volumeNumber).coerceAtLeast(1))
    }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var isNightMode by remember { mutableStateOf(false) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var bookmarks by remember {
        mutableStateOf(bookManager.getBookmarks(book.bookId, volumeNumber))
    }

    val isCurrentPageBookmarked = remember(bookmarks, currentPage) {
        bookmarks.any { it.pageNumber == currentPage }
    }

    LaunchedEffect(book.bookId, volumeNumber, currentPage) {
        isRendering = true
        bookManager.saveLastReadPage(book.bookId, volumeNumber, currentPage)
        withContext(Dispatchers.IO) {
            var pdfFile: File = bookManager.getBookPdfFile(book.bookId, volumeNumber)
            if (!bookManager.isValidPdfFile(pdfFile)) {
                pdfFile = bookManager.downloadBookSynchronously(book, volumeNumber)
            }
            if (bookManager.isValidPdfFile(pdfFile)) {
                try {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            val count = renderer.pageCount.coerceAtLeast(1)
                            totalPages = count
                            val safeIndex = (currentPage - 1).coerceIn(0, count - 1)
                            renderer.openPage(safeIndex).use { page ->
                                val bmp = Bitmap.createBitmap(
                                    page.width.coerceAtLeast(595),
                                    page.height.coerceAtLeast(842),
                                    Bitmap.Config.ARGB_8888
                                )
                                bmp.eraseColor(android.graphics.Color.parseColor("#FCFBF7"))
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                renderedBitmap = bmp
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        isRendering = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF111827) else LibMintBg)
            .testTag("islamic_book_pdf_reader_screen")
    ) {
        // Top Reader Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LibDarkGreen)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToDetails,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pdf_reader_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.titleBangla,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "পৃষ্ঠা $currentPage / $totalPages • খণ্ড $volumeNumber",
                    color = LibGold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.testTag("pdf_reader_page_indicator")
                )
            }

            IconButton(
                onClick = {
                    bookmarks = bookManager.addOrRemoveBookmark(book.bookId, volumeNumber, currentPage)
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pdf_reader_bookmark_btn")
            ) {
                Icon(
                    imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "বুকমার্ক করুন",
                    tint = if (isCurrentPageBookmarked) LibGold else Color.White
                )
            }

            IconButton(
                onClick = { isNightMode = !isNightMode },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pdf_reader_night_mode_btn")
            ) {
                Icon(
                    imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "থিম পরিবর্তন",
                    tint = Color.White
                )
            }
        }

        // PDF Page Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(10.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 3f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = renderedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "${book.titleBangla} পৃষ্ঠা $currentPage",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                        .testTag("pdf_reader_rendered_page")
                )
            } else if (isRendering) {
                CircularProgressIndicator(color = LibDeepGreen)
            }
        }

        // Bottom Page Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LibDarkGreen)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (currentPage > 1) currentPage -= 1 },
                enabled = currentPage > 1,
                colors = ButtonDefaults.buttonColors(containerColor = LibDeepGreen),
                modifier = Modifier.testTag("pdf_reader_prev_page_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                Text("পূর্ববর্তী পৃষ্ঠা")
            }

            Text(
                text = "পৃষ্ঠা $currentPage / $totalPages",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Button(
                onClick = { if (currentPage < totalPages) currentPage += 1 },
                enabled = currentPage < totalPages,
                colors = ButtonDefaults.buttonColors(containerColor = LibDeepGreen),
                modifier = Modifier.testTag("pdf_reader_next_page_btn")
            ) {
                Text("পরবর্তী পৃষ্ঠা")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}
