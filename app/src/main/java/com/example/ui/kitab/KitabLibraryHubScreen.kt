package com.example.ui.kitab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class KitabLibraryMainTab(val titleBangla: String, val testTag: String) {
    CATALOG("অনলাইন ক্যাটালগ", "kitab_tab_online_catalog"),
    MY_BOOKS("আমার কিতাব", "kitab_tab_my_books"),
    BOOKMARKS("বুকমার্ক", "kitab_tab_bookmarks")
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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        KitabDownloadAndStorageManager.init(context)
    }

    val allCatalogBooks by KitabDownloadAndStorageManager.catalogItemsFlow.collectAsState()
    val allBookmarks by KitabDownloadAndStorageManager.bookmarksFlow.collectAsState()
    val readerThemeMode by KitabDownloadAndStorageManager.readerThemeFlow.collectAsState()

    var selectedMainTab by remember { mutableStateOf(KitabLibraryMainTab.CATALOG) }
    var selectedCategory by remember { mutableStateOf(KitabCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var activeReaderBookId by remember { mutableStateOf<String?>(null) }
    var activeReaderInitialPage by remember { mutableIntStateOf(1) }
    var activeReaderTotalPages by remember { mutableIntStateOf(1) }

    var alertMessageBangla by remember { mutableStateOf<String?>(null) }
    var bookPendingDeleteConfirmation by remember { mutableStateOf<KitabItemUiModel?>(null) }
    var showAdminAddKitabDialog by remember { mutableStateOf(false) }

    val downloadedBooks = remember(allCatalogBooks) {
        allCatalogBooks.filter { it.isReadyToReadOffline }
    }

    val filteredCatalogBooks = remember(allCatalogBooks, selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        allCatalogBooks.filter { book ->
            val matchesCat = selectedCategory == KitabCategory.ALL || book.category == selectedCategory
            val matchesQuery = query.isEmpty() ||
                book.titleBangla.lowercase().contains(query) ||
                book.arabicTitle.lowercase().contains(query) ||
                book.authorBangla.lowercase().contains(query) ||
                book.translatorOrCompilerBangla.lowercase().contains(query) ||
                book.publisherBangla.lowercase().contains(query) ||
                book.category.titleBangla.lowercase().contains(query) ||
                book.shortDescriptionBangla.lowercase().contains(query)
            matchesCat && matchesQuery
        }
    }

    val activeReaderBook = remember(allCatalogBooks, activeReaderBookId) {
        allCatalogBooks.firstOrNull { it.bookId == activeReaderBookId }
    }

    val openBookInInternalReader: (KitabItemUiModel, Int?) -> Unit = { book, overridePage ->
        val fastCheck = KitabDownloadAndStorageManager.verifyReadyBookOnDiskFast(context, book)
        if (fastCheck != null && fastCheck.canOpen) {
            activeReaderTotalPages = fastCheck.totalPages.coerceAtLeast(1)
            activeReaderInitialPage = (overridePage ?: fastCheck.initialPage).coerceIn(1, activeReaderTotalPages)
            activeReaderBookId = book.bookId
        } else {
            coroutineScope.launch {
                val check = KitabDownloadAndStorageManager.verifyAndPrepareBookForInternalReader(
                    context = context,
                    bookId = book.bookId
                )
                if (check.canOpen) {
                    activeReaderTotalPages = check.totalPages.coerceAtLeast(1)
                    activeReaderInitialPage = (overridePage ?: check.initialPage).coerceIn(1, activeReaderTotalPages)
                    activeReaderBookId = book.bookId
                } else {
                    alertMessageBangla = check.errorMessageBangla
                }
            }
        }
    }

    BackHandler {
        if (activeReaderBookId != null) {
            activeReaderBookId = null
        } else {
            onCloseLibrary()
        }
    }

    if (activeReaderBook != null) {
        val siblingVolumes = remember(allCatalogBooks, activeReaderBook.seriesId) {
            allCatalogBooks
                .filter { it.seriesId == activeReaderBook.seriesId }
                .sortedBy { it.volumeNumber }
        }
        val bookBookmarks = remember(allBookmarks, activeReaderBook.bookId) {
            allBookmarks.filter { it.bookId == activeReaderBook.bookId }
        }

        KitabNativePdfReaderView(
            book = activeReaderBook,
            siblingVolumes = siblingVolumes,
            initialPage = activeReaderInitialPage,
            totalPages = activeReaderTotalPages,
            bookmarksForBook = bookBookmarks,
            themeMode = readerThemeMode,
            onThemeModeChange = { newMode ->
                KitabDownloadAndStorageManager.setReaderThemeMode(context, newMode)
            },
            onSwitchVolume = { targetVolume ->
                openBookInInternalReader(targetVolume, null)
            },
            onCloseReader = {
                activeReaderBookId = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitabCreamBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("kitab_library_hub_screen")
    ) {
        // 1. Top Islamic Header
        Surface(
            color = KitabGreenPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCloseLibrary,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("kitab_library_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "◈",
                                color = KitabGoldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "বাংলা হাদিস শরাহ ও কিতাব লাইব্রেরি",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "মোট কিতাব: ${KitabFormatUtils.toBanglaDigits(allCatalogBooks.size)}টি • ফোনে সংরক্ষিত: ${KitabFormatUtils.toBanglaDigits(downloadedBooks.size)}টি • ১০০% নিজস্ব PDF রিডার",
                            color = KitabGoldLight,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isAdminUser) {
                        OutlinedButton(
                            onClick = { showAdminAddKitabDialog = true },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, KitabGoldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KitabGoldLight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("kitab_admin_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কিতাব যোগ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Primary Navigation Tabs: অনলাইন ক্যাটালগ | আমার কিতাব | বুকমার্ক
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitabGreenDark, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KitabLibraryMainTab.entries.forEach { tab ->
                        val isSelected = selectedMainTab == tab
                        val tabLabel = when (tab) {
                            KitabLibraryMainTab.CATALOG ->
                                "${tab.titleBangla} (${KitabFormatUtils.toBanglaDigits(allCatalogBooks.size)})"
                            KitabLibraryMainTab.MY_BOOKS ->
                                "${tab.titleBangla} (${KitabFormatUtils.toBanglaDigits(downloadedBooks.size)})"
                            KitabLibraryMainTab.BOOKMARKS ->
                                "${tab.titleBangla} (${KitabFormatUtils.toBanglaDigits(allBookmarks.size)})"
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSelected) KitabGoldAccent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedMainTab = tab }
                                .testTag(tab.testTag)
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (isSelected) KitabGreenDark else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Main Tab Body
        when (selectedMainTab) {
            KitabLibraryMainTab.CATALOG -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "কিতাবের নাম, লেখক, অনুবাদক বা প্রকাশনী খুঁজুন...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF5D766B)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = KitabGreenPrimary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "মুছুন",
                                                tint = KitabGreenPrimary
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KitabGreenPrimary,
                                    unfocusedBorderColor = KitabCardBorder,
                                    focusedContainerColor = Color(0xFFF9FCFA),
                                    unfocusedContainerColor = Color(0xFFF9FCFA)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kitab_catalog_search_input")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kitab_category_filter_row")
                            ) {
                                items(KitabCategory.entries, key = { it.id }) { category ->
                                    val selected = selectedCategory == category
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedCategory = category },
                                        label = {
                                            Text(
                                                text = category.shortBadgeBangla,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = KitabGreenPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFFEEF5F0),
                                            labelColor = KitabGreenPrimary
                                        ),
                                        modifier = Modifier.testTag(category.tag)
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("kitab_catalog_lazy_list"),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 10.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "catalog_info_banner") {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4EE)),
                                border = BorderStroke(1.dp, KitabGreenPrimary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = KitabGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "যে কিতাবটি ডাউনলোড করবেন শুধুমাত্র সেটিই আপনার ফোনে সংরক্ষিত হবে এবং ইন্টারনেট ছাড়াই অ্যাপের নিজস্ব PDF রিডারে পড়তে পারবেন।",
                                        color = KitabGreenDark,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (filteredCatalogBooks.isEmpty()) {
                            item(key = "empty_catalog_search") {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "এই বিভাগে বা অনুসন্ধানে কোনো কিতাব পাওয়া যায়নি",
                                            color = KitabGreenPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredCatalogBooks, key = { it.bookId }) { book ->
                                KitabCatalogBookCard(
                                    book = book,
                                    onDownloadOrResumeClick = {
                                        if (!KitabDownloadAndStorageManager.isDeviceOnline(context)) {
                                            alertMessageBangla =
                                                "ইন্টারনেট সংযোগ বন্ধ রয়েছে। '${book.titleBangla}' ডাউনলোড করতে ইন্টারনেট চালু করুন।"
                                        } else {
                                            KitabDownloadAndStorageManager.startOrResumeDownload(
                                                context = context,
                                                bookId = book.bookId
                                            )
                                        }
                                    },
                                    onPauseClick = {
                                        KitabDownloadAndStorageManager.pauseDownload(
                                            context = context,
                                            bookId = book.bookId
                                        )
                                    },
                                    onCancelClick = {
                                        KitabDownloadAndStorageManager.cancelDownload(
                                            context = context,
                                            bookId = book.bookId
                                        )
                                    },
                                    onReadClick = {
                                        openBookInInternalReader(book, null)
                                    },
                                    onDeleteDownloadClick = {
                                        bookPendingDeleteConfirmation = book
                                    }
                                )
                            }
                        }
                    }
                }
            }

            KitabLibraryMainTab.MY_BOOKS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("kitab_my_books_lazy_list"),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (downloadedBooks.isEmpty()) {
                        item(key = "no_downloaded_books") {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, KitabCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kitab_my_books_empty_state")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = KitabGreenPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "এখনো কোনো কিতাব ডাউনলোড করা হয়নি",
                                        color = KitabGreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "অনলাইন ক্যাটালগ থেকে আপনার প্রয়োজনীয় হাদিস শরাহ বা কিতাব ডাউনলোড করলে সেটি এখানে জমা থাকবে এবং ইন্টারনেট ছাড়াই পড়তে পারবেন।",
                                        color = Color(0xFF4A6358),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { selectedMainTab = KitabLibraryMainTab.CATALOG },
                                        colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("অনলাইন ক্যাটালগ দেখুন")
                                    }
                                }
                            }
                        }
                    } else {
                        items(downloadedBooks, key = { it.bookId }) { book ->
                            KitabDownloadedMyBookCard(
                                book = book,
                                onReadClick = {
                                    openBookInInternalReader(book, null)
                                },
                                onDeleteDownloadClick = {
                                    bookPendingDeleteConfirmation = book
                                }
                            )
                        }
                    }
                }
            }

            KitabLibraryMainTab.BOOKMARKS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("kitab_bookmarks_lazy_list"),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allBookmarks.isEmpty()) {
                        item(key = "no_bookmarks") {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, KitabCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = KitabGoldAccent,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "কোনো বুকমার্ক সংরক্ষিত নেই",
                                        color = KitabGreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "কিতাব পড়ার সময় উপরের বুকমার্ক আইকনে ট্যাপ করে পছন্দের পৃষ্ঠা সংরক্ষণ করুন।",
                                        color = Color(0xFF4A6358),
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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
                                        val book = allCatalogBooks.firstOrNull { it.bookId == bm.bookId }
                                        if (book != null) {
                                            openBookInInternalReader(book, bm.pageNumber)
                                        }
                                    }
                                    .testTag("kitab_bookmark_item_${bm.bookId}_${bm.pageNumber}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = KitabGreenPrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = KitabGreenPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.bookTitleBangla,
                                            fontWeight = FontWeight.Bold,
                                            color = KitabGreenPrimary,
                                            fontSize = 14.5.sp
                                        )
                                        Text(
                                            text = "পৃষ্ঠা নং ${KitabFormatUtils.toBanglaDigits(bm.pageNumber)} • ${bm.note}",
                                            color = Color(0xFF3E594E),
                                            fontSize = 12.5.sp
                                        )
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

    if (bookPendingDeleteConfirmation != null) {
        val target = bookPendingDeleteConfirmation!!
        AlertDialog(
            onDismissRequest = { bookPendingDeleteConfirmation = null },
            modifier = Modifier.testTag("kitab_delete_confirm_dialog"),
            title = {
                Text(
                    text = "ডাউনলোড মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold,
                    color = KitabGreenPrimary
                )
            },
            text = {
                Text(
                    text = "'${target.titleBangla}'-এর অফলাইন PDF ফাইলটি (${target.formattedFileSizeMb}) আপনার ফোন থেকে মুছে যাবে। কিতাবটি অনলাইন ক্যাটালগে থেকে যাবে এবং ভবিষ্যতে যেকোনো সময় পুনরায় ডাউনলোড করতে পারবেন।",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = target.bookId
                        bookPendingDeleteConfirmation = null
                        KitabDownloadAndStorageManager.deleteDownloadedBook(context, idToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    modifier = Modifier.testTag("kitab_confirm_delete_button")
                ) {
                    Text("ফোন থেকে মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingDeleteConfirmation = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (alertMessageBangla != null) {
        AlertDialog(
            onDismissRequest = { alertMessageBangla = null },
            modifier = Modifier.testTag("kitab_alert_dialog"),
            title = {
                Text(
                    text = "কিতাব লাইব্রেরি বার্তা",
                    fontWeight = FontWeight.Bold,
                    color = KitabGreenPrimary
                )
            },
            text = {
                Text(
                    text = alertMessageBangla!!,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { alertMessageBangla = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary),
                    modifier = Modifier.testTag("kitab_alert_dismiss_button")
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    if (showAdminAddKitabDialog && isAdminUser) {
        AdminPublishKitabDialog(
            onDismiss = { showAdminAddKitabDialog = false },
            onPublished = { msg ->
                showAdminAddKitabDialog = false
                alertMessageBangla = msg
            }
        )
    }
}
