package com.example.ui.islamicbooks

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LibMintBg)
                    .testTag("islamic_book_library_root")
            ) {
                val columnsCount = when {
                    maxWidth >= 840.dp -> 3
                    maxWidth >= 560.dp -> 2
                    else -> 1
                }
                val chunkedBooks = remember(displayedBooks, columnsCount) {
                    displayedBooks.chunked(columnsCount)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("islamic_book_library_list"),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    // 1. Top Islamic Header Banner + Self-Publishing Portal Button + Search Bar
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(LibDarkGreen, LibDeepGreen)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = onNavigateBackHome,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("islamic_book_library_back_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "হোমে ফিরে যান",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ডিজিটাল ইসলামিক বই লাইব্রেরি",
                                            color = Color.White,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "প্রামাণ্য কুরআন-তাফসির, হাদিস, ফিকহ, সিরাত ও আকাবিরদের গ্রন্থসম্ভার",
                                            color = Color(0xFFD9F2E6),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Row: Self-Publishing Button + My Books Filter + Admin Dashboard Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            currentSubView = LibrarySubView.SELF_PUBLISHING_PORTAL
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LibGold,
                                            contentColor = LibDarkGreen
                                        ),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("open_self_publishing_portal_btn")
                                    ) {
                                        Icon(Icons.Default.Publish, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "আপনার লেখা বই প্রকাশ করুন",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    FilterChip(
                                        selected = showOnlyDownloadedMyBooks,
                                        onClick = { showOnlyDownloadedMyBooks = !showOnlyDownloadedMyBooks },
                                        label = {
                                            Text(
                                                text = "আমার ডাউনলোডকৃত বই (My Books)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (showOnlyDownloadedMyBooks) LibDarkGreen else Color.White
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DownloadDone,
                                                contentDescription = null,
                                                tint = if (showOnlyDownloadedMyBooks) LibDarkGreen else LibGold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LibGoldLight,
                                            containerColor = Color.White.copy(alpha = 0.14f)
                                        ),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("islamic_book_my_books_tab")
                                    )

                                    if (isAdmin) {
                                        OutlinedButton(
                                            onClick = {
                                                currentSubView = LibrarySubView.ADMIN_APPROVAL_DASHBOARD
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.White
                                            ),
                                            border = BorderStroke(1.dp, LibGold),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("open_admin_book_dashboard_btn")
                                        ) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LibGold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("অ্যাডমিন অনুমোদন প্যানেল", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "বইয়ের নাম, লেখক, অনুবাদক বা প্রকাশনী দিয়ে খুঁজুন...",
                                            color = Color.White.copy(alpha = 0.78f),
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "অনুসন্ধান", tint = LibGold)
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = Color.White)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = LibGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
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

                    // 2. 15 Category Horizontal Selector Chips
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "লাইব্রেরির বিভাগসমূহ (১৫টি বিভাগ):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = LibDarkGreen,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null && !showOnlyDownloadedMyBooks,
                                    onClick = {
                                        selectedCategory = null
                                        showOnlyDownloadedMyBooks = false
                                    },
                                    label = {
                                        Text(
                                            text = "সকল বিভাগ",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LibDeepGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("islamic_book_category_chip_ALL")
                                )

                                IslamicBookCategory.entries.forEach { cat ->
                                    val isSelected = selectedCategory == cat && !showOnlyDownloadedMyBooks
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCategory = cat
                                            showOnlyDownloadedMyBooks = false
                                        },
                                        label = {
                                            Text(
                                                text = cat.titleBangla,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LibDeepGreen,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("islamic_book_category_chip_${cat.id}")
                                    )
                                }
                            }
                        }
                    }

                    // 3. Verified Publishers Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LibGoldLight),
                            border = BorderStroke(1.dp, LibGold.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = LibDeepGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedCategory != null) {
                                            "${selectedCategory!!.titleBangla} — ${selectedCategory!!.subtitleBangla}"
                                        } else if (showOnlyDownloadedMyBooks) {
                                            "আমার ডাউনলোডকৃত বই (অফলাইনে ইন্টারনেট ছাড়াই পাঠযোগ্য)"
                                        } else {
                                            "নির্ভরযোগ্য প্রকাশনী ও যাচাইকৃত কপিরাইট নীতিমালা"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = LibDarkGreen
                                    )
                                    Text(
                                        text = "ইসলামিক ফাউন্ডেশন বাংলাদেশ • মাকতাবাতুল ফুরকান • তাওহীদ পাবলিকেশন্স • আল-ফাতাহ • মাকতাবাতুল আশরাফ • রাহনুমা • দারুল কলম",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    localActionMessage?.let { msg ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(msg, fontSize = 12.sp, color = LibDarkGreen, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { localActionMessage = null }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                                    }
                                }
                            }
                        }
                    }

                    // 4. Books List / Grid
                    if (chunkedBooks.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .testTag("islamic_book_empty_state")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = LibDeepGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (showOnlyDownloadedMyBooks) {
                                            "আপনার ফোনে এখনো কোনো ইসলামিক বই ডাউনলোড করা হয়নি।"
                                        } else {
                                            "অনুসন্ধান অনুযায়ী কোনো বই পাওয়া যায়নি।"
                                        },
                                        fontWeight = FontWeight.Bold,
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
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowBooks.forEach { book ->
                                    val key = bookManager.downloadKey(book.bookId, 1)
                                    val dlProgress = downloadProgressMap[key]
                                    val isDownloaded = dlProgress?.state == IslamicBookDownloadState.DOWNLOADED ||
                                        bookManager.isBookDownloaded(book.bookId, 1)

                                    IslamicBookCardItem(
                                        book = book,
                                        isDownloaded = isDownloaded,
                                        downloadProgress = dlProgress,
                                        modifier = Modifier.weight(1f),
                                        onOpenDetails = {
                                            selectedBook = book
                                            selectedVolumeNumber = 1
                                            currentSubView = LibrarySubView.BOOK_DETAILS
                                        },
                                        onPrimaryReadOrDownload = {
                                            selectedBook = book
                                            selectedVolumeNumber = 1
                                            if (isDownloaded) {
                                                currentSubView = LibrarySubView.PDF_READER
                                            } else {
                                                bookManager.startOrResumeDownload(book, 1)
                                                currentSubView = LibrarySubView.BOOK_DETAILS
                                            }
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
 * ৪. প্রতিটি বই কার্ডের তথ্যের ডিজাইন
 */
@Composable
private fun IslamicBookCardItem(
    book: IslamicBookCatalogItem,
    isDownloaded: Boolean,
    downloadProgress: IslamicBookDownloadProgress?,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit,
    onPrimaryReadOrDownload: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LibCreamCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LibGold.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onOpenDetails() }
            .testTag("islamic_book_card_${book.bookId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // १. বইয়ের নান্দনিক ইসলামিক কভার প্রচ্ছদ
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(122.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(book.coverBadgeColorHex), LibDarkGreen)
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "﷽",
                            color = LibGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = book.titleBangla,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.pageCount} পৃষ্ঠা",
                            color = LibGoldLight,
                            fontSize = 9.sp
                        )
                    }
                }

                // ২-১০. বইয়ের বিস্তারিত মেটাডেটা
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LibDeepGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = book.category.titleBangla,
                                color = LibDeepGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        if (book.isPublicationYearVerified && book.publicationYear.isNotBlank()) {
                            Surface(
                                color = LibGoldLight,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, LibGold)
                            ) {
                                Text(
                                    text = "প্রকাশ: ${book.publicationYear}",
                                    color = LibDarkGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.titleBangla,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LibDarkGreen,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.titleArabicOrOriginal.isNotBlank()) {
                        Text(
                            text = book.titleArabicOrOriginal,
                            fontSize = 12.sp,
                            color = LibDeepGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "লেখক: ${book.authorName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    if (book.translatorOrEditorName.isNotBlank()) {
                        Text(
                            text = "অনুবাদক/সংকলক: ${book.translatorOrEditorName}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF374151)
                        )
                    }
                    Text(
                        text = "প্রকাশনী: ${book.publisherName}",
                        fontSize = 11.5.sp,
                        color = LibDeepGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "পৃষ্ঠা: ${book.pageCount} • সাইজ: ${book.fileSizeTextBangla}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.shortIntroduction,
                fontSize = 12.sp,
                color = Color(0xFF374151),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("book_card_details_btn_${book.bookId}")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("বিস্তারিত ও সূচিপত্র", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onPrimaryReadOrDownload,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) LibDeepGreen else Color(0xFF14532D)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("book_card_action_btn_${book.bookId}")
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.MenuBook else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isDownloaded -> "পড়ুন (অফলাইন)"
                            downloadProgress?.state == IslamicBookDownloadState.DOWNLOADING ->
                                "ডাউনলোড ${downloadProgress.progressPercent}%"
                            downloadProgress?.state == IslamicBookDownloadState.PAUSED ->
                                "পুনরায় শুরু (${downloadProgress.progressPercent}%)"
                            else -> "পড়ুন / ডাউনলোড"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
