package com.example.ui.kitab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

enum class KitabLibraryMainTab(
    val titleBangla: String,
    val testTag: String
) {
    CATALOG("অনলাইন ক্যাটালগ", "kitab_tab_online_catalog"),
    MY_BOOKS("আমার কিতাব", "kitab_tab_my_books"),
    BOOKMARKS("বুকমার্কসমূহ", "kitab_tab_bookmarks")
}

@Composable
fun KitabLibraryHubDialog(
    isAdminUser: Boolean = false,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        KitabLibraryHubScreen(
            isAdminUser = isAdminUser,
            onCloseLibrary = onDismiss
        )
    }
}

@Composable
fun KitabLibraryHubScreen(
    isAdminUser: Boolean = false,
    onCloseLibrary: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        KitabDownloadAndStorageManager.init(context)
    }

    val allCatalogBooks by KitabDownloadAndStorageManager.catalogItemsFlow.collectAsState()
    val allBookmarks by KitabDownloadAndStorageManager.bookmarksFlow.collectAsState()
    val readerThemeMode by KitabDownloadAndStorageManager.readerThemeFlow.collectAsState()
    val readerSettings by KitabDownloadAndStorageManager.readerSettingsFlow.collectAsState()

    var selectedMainTab by rememberSaveable { mutableStateOf(KitabLibraryMainTab.CATALOG) }
    var selectedCategory by rememberSaveable { mutableStateOf(KitabCategory.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var activeReaderBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeReaderInitialPage by rememberSaveable { mutableIntStateOf(1) }
    var activeReaderTotalPages by rememberSaveable { mutableIntStateOf(1) }

    var alertDialogMessage by remember { mutableStateOf<String?>(null) }
    var bookPendingDeleteConfirmation by remember { mutableStateOf<KitabItemUiModel?>(null) }
    var showAdminAddBookDialog by remember { mutableStateOf(false) }

    val activeBookModel = remember(allCatalogBooks, activeReaderBookId) {
        activeReaderBookId?.let { id -> allCatalogBooks.firstOrNull { it.bookId == id } }
    }

    fun openDownloadedBookInInternalReader(book: KitabItemUiModel, overridePage: Int? = null) {
        val fastResult = KitabDownloadAndStorageManager.verifyReadyBookOnDiskFast(context, book)
        if (fastResult != null && fastResult.canOpen) {
            activeReaderTotalPages = fastResult.totalPages.coerceAtLeast(1)
            activeReaderInitialPage = (overridePage ?: fastResult.initialPage).coerceIn(1, activeReaderTotalPages)
            activeReaderBookId = book.bookId
            return
        }
        scope.launch {
            val check = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(context, book.bookId)
            if (check.canOpen) {
                activeReaderTotalPages = check.totalPages.coerceAtLeast(1)
                activeReaderInitialPage = (overridePage ?: check.initialPage).coerceIn(1, activeReaderTotalPages)
                activeReaderBookId = book.bookId
            } else {
                alertDialogMessage = check.errorMessageBangla
            }
        }
    }

    // If Internal PDF Reader is active, display full-screen native reader
    if (activeBookModel != null) {
        val siblingVolumes = remember(allCatalogBooks, activeBookModel.seriesId) {
            allCatalogBooks
                .filter { it.seriesId == activeBookModel.seriesId }
                .sortedBy { it.volumeNumber }
        }
        val bookmarksForActiveBook = remember(allBookmarks, activeBookModel.bookId) {
            allBookmarks.filter { it.bookId == activeBookModel.bookId }
        }

        KitabNativePdfReaderView(
            book = activeBookModel,
            siblingVolumes = siblingVolumes,
            initialPage = activeReaderInitialPage,
            totalPages = activeReaderTotalPages,
            bookmarksForBook = bookmarksForActiveBook,
            themeMode = readerThemeMode,
            onThemeModeChange = { newMode ->
                KitabDownloadAndStorageManager.setReaderThemeMode(context, newMode)
            },
            onSwitchVolume = { targetVolume ->
                openDownloadedBookInInternalReader(targetVolume, overridePage = null)
            },
            onCloseReader = {
                activeReaderBookId = null
            },
            readerSettings = readerSettings,
            onUpdateReaderSettings = { transform ->
                KitabDownloadAndStorageManager.updateReaderSettings(context, transform)
            }
        )
        return
    }

    BackHandler {
        onCloseLibrary()
    }

    val downloadedBooks = remember(allCatalogBooks) {
        allCatalogBooks.filter { it.isReadyToReadOffline }
    }

    val bookmarkedBookIds = remember(allBookmarks) {
        allBookmarks.map { it.bookId }.toSet()
    }

    val continueReadingBook = remember(downloadedBooks) {
        downloadedBooks.maxByOrNull { it.lastReadPage }
    }

    val filteredCatalogBooks = remember(allCatalogBooks, selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        allCatalogBooks.filter { item ->
            val categoryMatch = selectedCategory == KitabCategory.ALL || item.category == selectedCategory
            val queryMatch = if (query.isBlank()) {
                true
            } else {
                item.titleBangla.lowercase().contains(query) ||
                    item.arabicTitle.lowercase().contains(query) ||
                    item.authorBangla.lowercase().contains(query) ||
                    item.translatorOrCompilerBangla.lowercase().contains(query) ||
                    item.publisherBangla.lowercase().contains(query) ||
                    item.category.titleBangla.lowercase().contains(query) ||
                    item.marhalaBangla.lowercase().contains(query)
            }
            categoryMatch && queryMatch
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KitabCreamBg)
            .testTag("kitab_library_hub_screen")
    ) {
        // Responsive Grid Column Count:
        // - Mobile Portrait (< 600.dp): 2 compact cards side-by-side
        // - Medium / Landscape (600.dp..839.dp): 3 compact cards side-by-side
        // - Expanded / Tablet (>= 840.dp): 4 compact cards side-by-side
        val gridColumns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // 1. Top Islamic Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(KitabGreenDark, KitabGreenPrimary)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCloseLibrary,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("kitab_library_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "লাইব্রেরি থেকে ফিরে যান",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "বাংলা হাদিস শরাহ ও কিতাব লাইব্রেরি",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "অনলাইন ক্যাটালগ • ডাউনলোডকৃত (${KitabFormatUtils.toBanglaDigits(downloadedBooks.size)}টি) • অফলাইন রিডার",
                                color = KitabGoldLight,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isAdminUser) {
                            IconButton(
                                onClick = { showAdminAddBookDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_admin_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "নতুন কিতাব যুক্ত করুন",
                                    tint = KitabGoldAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "কিতাব, লেখক, অনুবাদক, প্রকাশনী বা বিষয় খুঁজুন...",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = KitabGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("kitab_search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "খোঁজ মুছুন",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.12f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                            focusedBorderColor = KitabGoldAccent,
                            unfocusedBorderColor = KitabGoldAccent.copy(alpha = 0.45f),
                            cursorColor = KitabGoldAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kitab_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main 3-Tab Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KitabLibraryMainTab.entries.forEach { tab ->
                            val isSelected = selectedMainTab == tab
                            val countBadge = when (tab) {
                                KitabLibraryMainTab.CATALOG -> allCatalogBooks.size
                                KitabLibraryMainTab.MY_BOOKS -> downloadedBooks.size
                                KitabLibraryMainTab.BOOKMARKS -> allBookmarks.size
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) KitabGoldAccent else Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) KitabGoldAccent else KitabGoldAccent.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedMainTab = tab }
                                    .testTag(tab.testTag)
                            ) {
                                Text(
                                    text = "${tab.titleBangla} (${KitabFormatUtils.toBanglaDigits(countBadge)})",
                                    color = if (isSelected) KitabGreenDark else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Category Filter Chips (Shown on Online Catalog Tab)
            if (selectedMainTab == KitabLibraryMainTab.CATALOG) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEAF3EE))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("kitab_category_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(KitabCategory.entries, key = { it.id }) { cat ->
                        val selected = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) KitabGreenPrimary else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (selected) KitabGoldAccent else KitabCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCategory = cat }
                                .testTag(cat.tag)
                        ) {
                            Text(
                                text = cat.shortBadgeBangla,
                                color = if (selected) Color.White else KitabGreenDark,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3. Tab Body Content (Responsive 2-column / 3-column / 4-column Grid)
            when (selectedMainTab) {
                KitabLibraryMainTab.CATALOG -> {
                    val rows = remember(filteredCatalogBooks, gridColumns) {
                        filteredCatalogBooks.chunked(gridColumns)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("kitab_catalog_list"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compact Continue Reading Banner (if user has any downloaded book)
                        if (continueReadingBook != null) {
                            item(key = "continue_reading_banner") {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = KitabGreenPrimary,
                                    border = BorderStroke(1.dp, KitabGoldAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openDownloadedBookInInternalReader(continueReadingBook) }
                                        .testTag("kitab_continue_reading_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = KitabGoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "সর্বশেষ পঠিত কিতাব • পড়া চালিয়ে যান",
                                                    color = KitabGoldLight,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${continueReadingBook.titleBangla} (পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(continueReadingBook.lastReadPage)})",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = KitabGoldAccent
                                        ) {
                                            Text(
                                                text = "খুলুন →",
                                                color = KitabGreenDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (rows.isEmpty()) {
                            item {
                                EmptyKitabStateCard(
                                    title = "কোনো কিতাব পাওয়া যায়নি",
                                    subtitle = "অন্য কোনো নাম, লেখক বা ক্যাটাগরি নির্বাচন করে পুনরায় খুঁজুন।"
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = rows,
                                key = { idx, rowItems -> "catalog_row_${idx}_${rowItems.firstOrNull()?.bookId}" }
                            ) { rowIndex, rowBooks ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("kitab_catalog_grid_row_$rowIndex"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    rowBooks.forEach { book ->
                                        val isBookmarked = book.bookId in bookmarkedBookIds
                                        KitabCatalogBookCard(
                                            book = book,
                                            isBookmarked = isBookmarked,
                                            onBookmarkToggleClick = {
                                                KitabDownloadAndStorageManager.toggleBookmark(
                                                    context = context,
                                                    bookId = book.bookId,
                                                    bookTitleBangla = book.titleBangla,
                                                    volumeNumber = book.volumeNumber,
                                                    pageNumber = book.lastReadPage.coerceAtLeast(1),
                                                    note = "${book.titleBangla} • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(book.lastReadPage.coerceAtLeast(1))}"
                                                )
                                            },
                                            onDownloadOrResumeClick = {
                                                KitabDownloadAndStorageManager.startOrResumeDownload(context, book.bookId)
                                            },
                                            onPauseClick = {
                                                KitabDownloadAndStorageManager.pauseDownload(context, book.bookId)
                                            },
                                            onCancelClick = {
                                                KitabDownloadAndStorageManager.cancelDownload(context, book.bookId)
                                            },
                                            onReadClick = {
                                                openDownloadedBookInInternalReader(book)
                                            },
                                            onDeleteDownloadClick = {
                                                bookPendingDeleteConfirmation = book
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(gridColumns - rowBooks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                KitabLibraryMainTab.MY_BOOKS -> {
                    val myBookRows = remember(downloadedBooks, gridColumns) {
                        downloadedBooks.chunked(gridColumns)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("kitab_my_books_list"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (myBookRows.isEmpty()) {
                            item {
                                EmptyKitabStateCard(
                                    title = "এখনো কোনো কিতাব ডাউনলোড করা হয়নি",
                                    subtitle = "‘অনলাইন ক্যাটালগ’ ট্যাব থেকে আপনার প্রয়োজনীয় হাদিস শরাহ কিতাব ডাউনলোড করলে সেটি এই ফোনে অফলাইনে পড়ার জন্য সংরক্ষিত থাকবে।"
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = myBookRows,
                                key = { idx, rowItems -> "my_books_row_${idx}_${rowItems.firstOrNull()?.bookId}" }
                            ) { rowIndex, rowBooks ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("kitab_my_books_grid_row_$rowIndex"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    rowBooks.forEach { book ->
                                        val isBookmarked = book.bookId in bookmarkedBookIds
                                        KitabDownloadedMyBookCard(
                                            book = book,
                                            isBookmarked = isBookmarked,
                                            onBookmarkToggleClick = {
                                                KitabDownloadAndStorageManager.toggleBookmark(
                                                    context = context,
                                                    bookId = book.bookId,
                                                    bookTitleBangla = book.titleBangla,
                                                    volumeNumber = book.volumeNumber,
                                                    pageNumber = book.lastReadPage.coerceAtLeast(1),
                                                    note = "${book.titleBangla} • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(book.lastReadPage.coerceAtLeast(1))}"
                                                )
                                            },
                                            onReadClick = {
                                                openDownloadedBookInInternalReader(book)
                                            },
                                            onDeleteDownloadClick = {
                                                bookPendingDeleteConfirmation = book
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(gridColumns - rowBooks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                KitabLibraryMainTab.BOOKMARKS -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("kitab_bookmarks_list"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (allBookmarks.isEmpty()) {
                            item {
                                EmptyKitabStateCard(
                                    title = "কোনো বুকমার্ক সংরক্ষিত নেই",
                                    subtitle = "কিতাব পড়ার সময় উপরের বুকমার্ক আইকনে অথবা কার্ডের বুকমার্ক বাটনে ট্যাপ করে যেকোনো গুরুত্বপূর্ণ পৃষ্ঠা সংরক্ষণ করতে পারবেন।"
                                )
                            }
                        } else {
                            items(allBookmarks, key = { it.id }) { bm ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, KitabCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val targetBook = allCatalogBooks.firstOrNull { it.bookId == bm.bookId }
                                            if (targetBook != null) {
                                                openDownloadedBookInInternalReader(targetBook, overridePage = bm.pageNumber)
                                            }
                                        }
                                        .testTag("kitab_bookmark_card_${bm.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = KitabGreenPrimary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = KitabGreenPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bm.bookTitleBangla,
                                                fontWeight = FontWeight.Bold,
                                                color = KitabGreenDark,
                                                fontSize = 13.5.sp
                                            )
                                            Text(
                                                text = "খণ্ড ${KitabFormatUtils.toBanglaDigits(bm.volumeNumber)} • পৃষ্ঠা নং ${KitabFormatUtils.toBanglaDigits(bm.pageNumber)}",
                                                color = KitabGreenPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            if (bm.note.isNotBlank()) {
                                                Text(
                                                    text = bm.note,
                                                    color = Color(0xFF4B6358),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                KitabDownloadAndStorageManager.removeBookmarkById(context, bm.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "বুকমার্ক মুছুন",
                                                tint = Color(0xFFB71C1C)
                                            )
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

    // Delete Confirmation Dialog
    if (bookPendingDeleteConfirmation != null) {
        val target = bookPendingDeleteConfirmation!!
        AlertDialog(
            onDismissRequest = { bookPendingDeleteConfirmation = null },
            modifier = Modifier.testTag("kitab_delete_confirm_dialog"),
            title = {
                Text(
                    text = "ডাউনলোডকৃত কিতাব মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold,
                    color = KitabGreenPrimary
                )
            },
            text = {
                Text(
                    text = "‘${target.titleBangla}’ কিতাবটির PDF ফাইল আপনার ফোনের স্টোরেজ (${target.formattedFileSizeMb}) থেকে মুছে ফেলা হবে। ভবিষ্যতে আবার পড়ার জন্য পুনরায় ডাউনলোড করতে হবে।",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.bookId
                        bookPendingDeleteConfirmation = null
                        KitabDownloadAndStorageManager.deleteDownloadedBook(context, id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.testTag("kitab_delete_confirm_yes_button")
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingDeleteConfirmation = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Offline / Validation Alert Dialog
    if (alertDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { alertDialogMessage = null },
            modifier = Modifier.testTag("kitab_alert_dialog"),
            title = {
                Text(
                    text = "কিতাব ওপেন নির্দেশনা",
                    fontWeight = FontWeight.Bold,
                    color = KitabGreenPrimary
                )
            },
            text = {
                Text(alertDialogMessage!!, fontSize = 13.5.sp)
            },
            confirmButton = {
                Button(
                    onClick = { alertDialogMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary)
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Admin Add New Kitab Dialog
    if (showAdminAddBookDialog) {
        AdminPublishKitabDialog(
            onDismiss = { showAdminAddBookDialog = false },
            onPublishedMessage = { msg ->
                alertDialogMessage = msg
            }
        )
    }
}

@Composable
private fun EmptyKitabStateCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, KitabCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = null,
                tint = KitabGreenPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = KitabGreenDark,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFF4E665A),
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
