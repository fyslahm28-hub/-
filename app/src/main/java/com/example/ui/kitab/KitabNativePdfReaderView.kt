package com.example.ui.kitab

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

private val ReaderEmeraldDark = Color(0xFF063D29)
private val ReaderEmeraldPrimary = Color(0xFF0A5C3E)
private val ReaderGoldAccent = Color(0xFFDDA83A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KitabNativePdfReaderView(
    book: KitabItemUiModel,
    siblingVolumes: List<KitabItemUiModel>,
    initialPage: Int,
    totalPages: Int,
    bookmarksForBook: List<KitabBookmarkModel>,
    themeMode: KitabReaderThemeMode,
    onThemeModeChange: (KitabReaderThemeMode) -> Unit,
    onSwitchVolume: (KitabItemUiModel) -> Unit,
    onCloseReader: () -> Unit,
    readerSettings: KitabReaderSettings = KitabReaderSettings(),
    onUpdateReaderSettings: ((KitabReaderSettings) -> KitabReaderSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val safeTotalPages = totalPages.coerceAtLeast(1)
    var currentPage by remember(book.bookId) {
        mutableIntStateOf(initialPage.coerceIn(1, safeTotalPages))
    }

    var isFullScreen by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showJumpPageDialog by remember { mutableStateOf(false) }
    var showBookmarkAddDialog by remember { mutableStateOf(false) }
    var showBookmarksListSheet by remember { mutableStateOf(false) }
    var showTocDialog by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }

    val isVerticalScrollMode = readerSettings.pageMode == KitabReaderPageMode.VERTICAL_SCROLL
    val fitToWidthMode = readerSettings.zoomFitMode == KitabZoomFitMode.FIT_WIDTH
    val isNightMode = themeMode == KitabReaderThemeMode.NIGHT_DARK

    BackHandler {
        onCloseReader()
    }

    // Keep Screen On support
    DisposableEffect(readerSettings.keepScreenOn) {
        val prevKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = readerSettings.keepScreenOn
        onDispose {
            view.keepScreenOn = prevKeepScreenOn
        }
    }

    // Screen Orientation support
    DisposableEffect(readerSettings.orientationMode) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (activity != null) {
            activity.requestedOrientation = when (readerSettings.orientationMode) {
                KitabOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                KitabOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                KitabOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Automatically persist current reading page per book
    LaunchedEffect(book.bookId, currentPage, safeTotalPages) {
        KitabDownloadAndStorageManager.recordPageRead(
            context = context,
            bookId = book.bookId,
            pageNumber = currentPage,
            totalPages = safeTotalPages
        )
    }

    val isCurrentPageBookmarked = remember(bookmarksForBook, currentPage) {
        bookmarksForBook.any { it.pageNumber == currentPage }
    }

    val pagerState = rememberPagerState(
        initialPage = (currentPage - 1).coerceIn(0, (safeTotalPages - 1).coerceAtLeast(0)),
        pageCount = { safeTotalPages }
    )

    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentPage - 1).coerceIn(0, (safeTotalPages - 1).coerceAtLeast(0))
    )

    LaunchedEffect(pagerState.currentPage, isVerticalScrollMode) {
        if (!isVerticalScrollMode) {
            val newPage = (pagerState.currentPage + 1).coerceIn(1, safeTotalPages)
            if (newPage != currentPage) {
                currentPage = newPage
                zoomScale = 1f
                panOffset = Offset.Zero
            }
        }
    }

    LaunchedEffect(verticalListState.firstVisibleItemIndex, isVerticalScrollMode) {
        if (isVerticalScrollMode) {
            val newPage = (verticalListState.firstVisibleItemIndex + 1).coerceIn(1, safeTotalPages)
            if (newPage != currentPage) {
                currentPage = newPage
            }
        }
    }

    fun navigateToPage(targetPage: Int) {
        val coerced = targetPage.coerceIn(1, safeTotalPages)
        currentPage = coerced
        zoomScale = 1f
        panOffset = Offset.Zero
        coroutineScope.launch {
            if (isVerticalScrollMode) {
                verticalListState.scrollToItem(coerced - 1)
            } else {
                pagerState.scrollToPage(coerced - 1)
            }
        }
    }

    val bgColor = when (themeMode) {
        KitabReaderThemeMode.NIGHT_DARK -> Color(0xFF101915)
        KitabReaderThemeMode.SEPIA_WARM -> Color(0xFFF3E9D2)
        KitabReaderThemeMode.MUSHAF_CREAM -> Color(0xFFFAF6EC)
        KitabReaderThemeMode.CLASSIC_WHITE -> Color(0xFFF4F6F4)
    }
    val pagePaperColor = when (themeMode) {
        KitabReaderThemeMode.NIGHT_DARK -> Color(0xFF18241E)
        KitabReaderThemeMode.SEPIA_WARM -> Color(0xFFFDF3DC)
        KitabReaderThemeMode.MUSHAF_CREAM -> Color(0xFFFFFCF5)
        KitabReaderThemeMode.CLASSIC_WHITE -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("kitab_native_pdf_reader_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Reader Toolbar
            if (!isFullScreen) {
                Surface(
                    color = ReaderEmeraldDark,
                    contentColor = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onCloseReader,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("kitab_reader_back_btn")
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
                                Text(
                                    text = book.titleBangla,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val currentSectionTitle = remember(currentPage, safeTotalPages) {
                                    getCurrentChapterTitle(currentPage, safeTotalPages)
                                }
                                Text(
                                    text = "$currentSectionTitle • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}/${KitabFormatUtils.toBanglaDigits(safeTotalPages)}",
                                    color = ReaderGoldAccent,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Table of Contents / Chapters Button
                            IconButton(
                                onClick = { showTocDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_reader_toc_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "সূচিপত্র",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Toggle Bookmark on Current Page
                            IconButton(
                                onClick = {
                                    if (isCurrentPageBookmarked) {
                                        KitabDownloadAndStorageManager.toggleBookmark(
                                            context = context,
                                            bookId = book.bookId,
                                            bookTitleBangla = book.titleBangla,
                                            volumeNumber = book.volumeNumber,
                                            pageNumber = currentPage
                                        )
                                    } else {
                                        showBookmarkAddDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_reader_bookmark_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "বুকমার্ক",
                                    tint = if (isCurrentPageBookmarked) ReaderGoldAccent else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Bookmarks List Button
                            IconButton(
                                onClick = { showBookmarksListSheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_reader_bookmarks_list_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (bookmarksForBook.isNotEmpty()) {
                                            Badge(containerColor = ReaderGoldAccent, contentColor = ReaderEmeraldDark) {
                                                Text(KitabFormatUtils.toBanglaDigits(bookmarksForBook.size), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CollectionsBookmark,
                                        contentDescription = "বুকমার্ক তালিকা",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Night / Day Toggle
                            IconButton(
                                onClick = {
                                    val nextTheme = if (isNightMode) {
                                        KitabReaderThemeMode.MUSHAF_CREAM
                                    } else {
                                        KitabReaderThemeMode.NIGHT_DARK
                                    }
                                    onThemeModeChange(nextTheme)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_reader_night_mode_btn")
                            ) {
                                Icon(
                                    imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "নাইট মোড",
                                    tint = if (isNightMode) ReaderGoldAccent else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Reader Settings (Brightness, Orientation, Spacing, Theme, Share)
                            IconButton(
                                onClick = { showReaderSettingsSheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("kitab_reader_settings_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "রিডার সেটিংস",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Secondary Quick Mode & Zoom Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Scroll Mode Toggle (Horizontal Page vs Vertical Scroll)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clickable {
                                        val nextMode = if (isVerticalScrollMode) {
                                            KitabReaderPageMode.HORIZONTAL_PAGER
                                        } else {
                                            KitabReaderPageMode.VERTICAL_SCROLL
                                        }
                                        onUpdateReaderSettings { it.copy(pageMode = nextMode) }
                                    }
                                    .testTag("kitab_reader_scroll_mode_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isVerticalScrollMode) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = ReaderGoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isVerticalScrollMode) "লম্বালম্বি স্ক্রল" else "ডান-বাম সোয়াইপ",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Zoom Controls (- / 100% / +)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "জুম আউট",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                                if (zoomScale <= 1.02f) panOffset = Offset.Zero
                                            }
                                            .padding(2.dp)
                                            .testTag("kitab_reader_zoom_out_btn")
                                    )
                                    Text(
                                        text = "${KitabFormatUtils.toBanglaDigits((zoomScale * 100).toInt())}%",
                                        color = ReaderGoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .clickable {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            }
                                            .testTag("kitab_reader_zoom_reset_btn")
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "জুম ইন",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                            }
                                            .padding(2.dp)
                                            .testTag("kitab_reader_zoom_in_btn")
                                    )
                                }
                            }

                            // Fit to Width / Fit to Page toggle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clickable {
                                        val nextFit = if (fitToWidthMode) {
                                            KitabZoomFitMode.FIT_PAGE
                                        } else {
                                            KitabZoomFitMode.FIT_WIDTH
                                        }
                                        onUpdateReaderSettings { it.copy(zoomFitMode = nextFit) }
                                    }
                                    .testTag("kitab_reader_fit_mode_btn")
                            ) {
                                Text(
                                    text = if (fitToWidthMode) "প্রস্থ ফিট" else "পৃষ্ঠা ফিট",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Share Current Page Info
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "📖 ${book.titleBangla} (${book.arabicTitle})\n" +
                                                "মূল লেখক: ${book.authorBangla}\n" +
                                                "পৃষ্ঠা: ${KitabFormatUtils.toBanglaDigits(currentPage)} / ${KitabFormatUtils.toBanglaDigits(safeTotalPages)}\n" +
                                                "— কওমি কর্মসংস্থান বাংলাদেশ অ্যাপের হাদিস শরাহ কিতাব লাইব্রেরি"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "পৃষ্ঠাটি শেয়ার করুন"))
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("kitab_reader_share_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "শেয়ার",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // FullScreen Toggle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clickable { isFullScreen = true }
                                    .testTag("kitab_reader_fullscreen_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "পূর্ণ স্ক্রিন",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "পূর্ণ স্ক্রিন",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Sibling Volumes Bar (if multi-volume series)
                        if (siblingVolumes.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "খণ্ড পরিবর্তন:",
                                    color = ReaderGoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                siblingVolumes.forEach { vol ->
                                    val isCurrentVol = vol.bookId == book.bookId
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrentVol) ReaderGoldAccent else Color.White.copy(alpha = 0.14f),
                                        modifier = Modifier.clickable(enabled = !isCurrentVol && vol.isReadyToReadOffline) {
                                            onSwitchVolume(vol)
                                        }
                                    ) {
                                        Text(
                                            text = "${KitabFormatUtils.toBanglaDigits(vol.volumeNumber)}ম খণ্ড",
                                            color = if (isCurrentVol) ReaderEmeraldDark else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main PDF Page Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isVerticalScrollMode) {
                    val allPages = remember(safeTotalPages) { (1..safeTotalPages).toList() }
                    LazyColumn(
                        state = verticalListState,
                        userScrollEnabled = zoomScale <= 1.05f,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("kitab_reader_vertical_list"),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = readerSettings.pageSpacingDp.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(readerSettings.pageSpacingDp.dp)
                    ) {
                        items(allPages, key = { it }) { pageNum ->
                            KitabSinglePageView(
                                book = book,
                                pageNumber = pageNum,
                                totalPages = safeTotalPages,
                                nightMode = isNightMode,
                                pagePaperColor = pagePaperColor,
                                fitToWidth = fitToWidthMode,
                                zoomScale = zoomScale,
                                panOffset = panOffset,
                                onTapCenter = { isFullScreen = !isFullScreen },
                                onDoubleTapZoom = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.0f
                                    }
                                },
                                onZoomPanChange = { newZoom, newPan ->
                                    zoomScale = newZoom
                                    panOffset = newPan
                                },
                                onSwipeNextPage = {
                                    if (currentPage < safeTotalPages) navigateToPage(currentPage + 1)
                                },
                                onSwipePrevPage = {
                                    if (currentPage > 1) navigateToPage(currentPage - 1)
                                },
                                enableHorizontalSwipeFallback = false
                            )
                        }
                    }
                } else {
                    // Horizontal Page Mode:
                    // - UserScrollEnabled is false only when zoomed in (> 1.05f) so pan/drag doesn't accidentally flip pages
                    // - Right-to-left swipe advances to next page (currentPage + 1)
                    // - Left-to-right swipe returns to previous page (currentPage - 1)
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = zoomScale <= 1.05f,
                        pageSpacing = readerSettings.pageSpacingDp.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("kitab_reader_horizontal_pager")
                    ) { pageIndex ->
                        val pageNum = pageIndex + 1
                        KitabSinglePageView(
                            book = book,
                            pageNumber = pageNum,
                            totalPages = safeTotalPages,
                            nightMode = isNightMode,
                            pagePaperColor = pagePaperColor,
                            fitToWidth = fitToWidthMode,
                            zoomScale = zoomScale,
                            panOffset = panOffset,
                            onTapCenter = { isFullScreen = !isFullScreen },
                            onDoubleTapZoom = {
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.0f
                                }
                            },
                            onZoomPanChange = { newZoom, newPan ->
                                zoomScale = newZoom
                                panOffset = newPan
                            },
                            onSwipeNextPage = {
                                if (currentPage < safeTotalPages) navigateToPage(currentPage + 1)
                            },
                            onSwipePrevPage = {
                                if (currentPage > 1) navigateToPage(currentPage - 1)
                            },
                            enableHorizontalSwipeFallback = true
                        )
                    }
                }

                // Brightness Dimming Overlay (if brightness < 1.0f)
                val dimAlpha = (1f - readerSettings.brightnessLevel).coerceIn(0f, 0.75f)
                if (dimAlpha > 0.02f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAlpha))
                    )
                }

                // Floating Exit Fullscreen Pill
                if (isFullScreen) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ReaderEmeraldDark.copy(alpha = 0.88f),
                        contentColor = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(14.dp)
                            .clickable { isFullScreen = false }
                            .testTag("kitab_reader_exit_fullscreen_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "টুলবার দেখান",
                                tint = ReaderGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}/${KitabFormatUtils.toBanglaDigits(safeTotalPages)} • টুলবার",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & Page Slider Bar (Respects Android Navigation Bar Insets)
            if (!isFullScreen) {
                Surface(
                    color = if (isNightMode) Color(0xFF1A2821) else Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier.testTag("kitab_reader_bottom_control_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp)
                    ) {
                        // Fast Page Slider + Progress Percentage
                        val readPercent = ((currentPage.toFloat() / safeTotalPages.toFloat()) * 100).toInt().coerceIn(1, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "১",
                                fontSize = 10.sp,
                                color = Color(0xFF78909C),
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { newVal ->
                                    navigateToPage(newVal.toInt())
                                },
                                valueRange = 1f..safeTotalPages.coerceAtLeast(2).toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .padding(horizontal = 6.dp)
                                    .testTag("kitab_reader_page_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = ReaderGoldAccent,
                                    activeTrackColor = ReaderEmeraldPrimary
                                )
                            )
                            Text(
                                text = "${KitabFormatUtils.toBanglaDigits(readPercent)}%",
                                fontSize = 10.sp,
                                color = ReaderEmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Page Button
                            OutlinedButton(
                                onClick = {
                                    if (currentPage > 1) navigateToPage(currentPage - 1)
                                },
                                enabled = currentPage > 1,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("kitab_reader_prev_page_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "আগের পৃষ্ঠা",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("আগের পৃষ্ঠা", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Jump to Page Pill (Also supports First/Last Page jump inside dialog)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ReaderEmeraldPrimary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clickable { showJumpPageDialog = true }
                                    .testTag("kitab_reader_jump_page_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FindInPage,
                                        contentDescription = null,
                                        tint = if (isNightMode) ReaderGoldAccent else ReaderEmeraldPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)} / ${KitabFormatUtils.toBanglaDigits(safeTotalPages)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isNightMode) Color.White else ReaderEmeraldDark
                                    )
                                }
                            }

                            // Next Page Button
                            Button(
                                onClick = {
                                    if (currentPage < safeTotalPages) navigateToPage(currentPage + 1)
                                },
                                enabled = currentPage < safeTotalPages,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ReaderEmeraldPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("kitab_reader_next_page_btn")
                            ) {
                                Text("পরের পৃষ্ঠা", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "পরের পৃষ্ঠা",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Jump to Page Modal Overlay (with First Page & Last Page shortcuts)
        if (showJumpPageDialog) {
            var pageInput by remember { mutableStateOf(currentPage.toString()) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = { showJumpPageDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clickable(enabled = false, onClick = {})
                        .testTag("kitab_reader_jump_page_dialog")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "নির্দিষ্ট পৃষ্ঠায় যান",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ReaderEmeraldDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "১ থেকে ${KitabFormatUtils.toBanglaDigits(safeTotalPages)}-এর মধ্যে যেকোনো পৃষ্ঠা নম্বর লিখুন:",
                            fontSize = 12.sp,
                            color = Color(0xFF546E7A)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { input ->
                                pageInput = input.filter { it.isDigit() || it in '০'..'৯' }
                            },
                            label = { Text("পৃষ্ঠা নম্বর") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kitab_reader_jump_page_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    navigateToPage(1)
                                    showJumpPageDialog = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kitab_reader_first_page_btn"),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("প্রথম পৃষ্ঠা (১)", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    navigateToPage(safeTotalPages)
                                    showJumpPageDialog = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kitab_reader_last_page_btn"),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("শেষ পৃষ্ঠা (${KitabFormatUtils.toBanglaDigits(safeTotalPages)})", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showJumpPageDialog = false }) {
                                Text("বাতিল")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val parsed = KitabFormatUtils.parseBanglaOrEnglishInt(pageInput)
                                    if (parsed != null) {
                                        navigateToPage(parsed)
                                    }
                                    showJumpPageDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ReaderEmeraldPrimary),
                                modifier = Modifier.testTag("kitab_reader_jump_confirm_btn")
                            ) {
                                Text("পৃষ্ঠায় যান")
                            }
                        }
                    }
                }
            }
        }

        // Add Bookmark Modal Overlay
        if (showBookmarkAddDialog) {
            var noteInput by remember { mutableStateOf("") }
            val defaultNote = remember(currentPage) {
                "${getCurrentChapterTitle(currentPage, safeTotalPages)} • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}"
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = { showBookmarkAddDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clickable(enabled = false, onClick = {})
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "পৃষ্ঠা বুকমার্ক করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ReaderEmeraldDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${book.titleBangla} — পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderEmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("সংক্ষিপ্ত নোট বা হাদিস নম্বর (ঐচ্ছিক)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kitab_reader_bookmark_note_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showBookmarkAddDialog = false }) {
                                Text("বাতিল")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    KitabDownloadAndStorageManager.toggleBookmark(
                                        context = context,
                                        bookId = book.bookId,
                                        bookTitleBangla = book.titleBangla,
                                        volumeNumber = book.volumeNumber,
                                        pageNumber = currentPage,
                                        note = noteInput.trim().ifBlank { defaultNote }
                                    )
                                    showBookmarkAddDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ReaderEmeraldPrimary),
                                modifier = Modifier.testTag("kitab_reader_bookmark_save_btn")
                            ) {
                                Text("সংরক্ষণ করুন")
                            }
                        }
                    }
                }
            }
        }

        // In-Reader Bookmarks List Modal Overlay
        if (showBookmarksListSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = { showBookmarksListSheet = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(max = 480.dp)
                        .clickable(enabled = false, onClick = {})
                        .testTag("kitab_reader_bookmarks_dialog")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "এই কিতাবের বুকমার্কসমূহ (${KitabFormatUtils.toBanglaDigits(bookmarksForBook.size)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ReaderEmeraldDark
                            )
                            IconButton(onClick = { showBookmarksListSheet = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        if (bookmarksForBook.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এই কিতাবে এখনো কোনো বুকমার্ক যোগ করা হয়নি।",
                                    fontSize = 12.sp,
                                    color = Color(0xFF607D8B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 360.dp)
                            ) {
                                items(bookmarksForBook, key = { it.id }) { bm ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF3F8F5),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navigateToPage(bm.pageNumber)
                                                showBookmarksListSheet = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = ReaderGoldAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(bm.pageNumber)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = ReaderEmeraldDark
                                                )
                                                if (bm.note.isNotBlank()) {
                                                    Text(
                                                        text = bm.note,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF455A64)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    KitabDownloadAndStorageManager.removeBookmarkById(context, bm.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "মুছে ফেলুন",
                                                    tint = Color(0xFFC62828),
                                                    modifier = Modifier.size(18.dp)
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

        // Table of Contents / Chapters & Volumes Modal Overlay
        if (showTocDialog) {
            val chapters = remember(safeTotalPages) { buildBookChaptersList(safeTotalPages) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = { showTocDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(max = 500.dp)
                        .clickable(enabled = false, onClick = {})
                        .testTag("kitab_reader_toc_dialog")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "কিতাবের সূচিপত্র ও অধ্যায়সমূহ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ReaderEmeraldDark
                                )
                                Text(
                                    text = book.titleBangla,
                                    fontSize = 11.sp,
                                    color = Color(0xFF607D8B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { showTocDialog = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 380.dp)
                        ) {
                            items(chapters) { (chapterTitle, startPage) ->
                                val isCurrentSection = currentPage >= startPage &&
                                    (chapters.indexOfFirst { it.second == startPage }.let { idx ->
                                        val nextStart = chapters.getOrNull(idx + 1)?.second ?: (safeTotalPages + 1)
                                        currentPage < nextStart
                                    })
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrentSection) ReaderEmeraldPrimary.copy(alpha = 0.12f) else Color(0xFFF7F9F8),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigateToPage(startPage)
                                            showTocDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chapterTitle,
                                            fontWeight = if (isCurrentSection) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = ReaderEmeraldDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ReaderEmeraldPrimary.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(startPage)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ReaderEmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

        // Reader Settings Modal Overlay
        if (showReaderSettingsSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = { showReaderSettingsSheet = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(enabled = false, onClick = {})
                        .testTag("kitab_reader_settings_dialog")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পাঠের আরাম ও রিডার সেটিংস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ReaderEmeraldDark
                            )
                            IconButton(onClick = { showReaderSettingsSheet = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Brightness Control
                        Text(
                            text = "স্ক্রিনের উজ্জ্বলতা (${KitabFormatUtils.toBanglaDigits((readerSettings.brightnessLevel * 100).toInt())}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderEmeraldDark
                        )
                        Slider(
                            value = readerSettings.brightnessLevel,
                            onValueChange = { level ->
                                onUpdateReaderSettings { it.copy(brightnessLevel = level) }
                            },
                            valueRange = 0.25f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ReaderGoldAccent,
                                activeTrackColor = ReaderEmeraldPrimary
                            ),
                            modifier = Modifier.testTag("kitab_reader_brightness_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Color Theme Selection (Day, Cream, Sepia, Night)
                        Text(
                            text = "পৃষ্ঠার রঙের মোড",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderEmeraldDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KitabReaderThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    label = { Text(mode.titleBangla, fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Screen Orientation Control
                        Text(
                            text = "স্ক্রিনের দিক (Orientation)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderEmeraldDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KitabOrientationMode.entries.forEach { orient ->
                                FilterChip(
                                    selected = readerSettings.orientationMode == orient,
                                    onClick = {
                                        onUpdateReaderSettings { it.copy(orientationMode = orient) }
                                    },
                                    label = { Text(orient.titleBangla, fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Keep Screen On Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পড়ার সময় স্ক্রিন চালু রাখুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ReaderEmeraldDark
                            )
                            Switch(
                                checked = readerSettings.keepScreenOn,
                                onCheckedChange = { enabled ->
                                    onUpdateReaderSettings { it.copy(keepScreenOn = enabled) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KitabSinglePageView(
    book: KitabItemUiModel,
    pageNumber: Int,
    totalPages: Int,
    nightMode: Boolean,
    pagePaperColor: Color,
    fitToWidth: Boolean,
    zoomScale: Float,
    panOffset: Offset,
    onTapCenter: () -> Unit,
    onDoubleTapZoom: () -> Unit,
    onZoomPanChange: (Float, Offset) -> Unit,
    onSwipeNextPage: () -> Unit,
    onSwipePrevPage: () -> Unit,
    enableHorizontalSwipeFallback: Boolean
) {
    val context = LocalContext.current
    var renderedBitmap by remember(book.bookId, pageNumber) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(book.bookId, pageNumber) {
        renderedBitmap = KitabDownloadAndStorageManager.renderKitabPageBitmap(
            context = context,
            bookId = book.bookId,
            pageNumber = pageNumber,
            targetWidthPx = 1080
        )
    }

    val nightColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = pagePaperColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTapCenter() },
                    onDoubleTap = { onDoubleTapZoom() }
                )
            }
            .pointerInput(zoomScale > 1.01f) {
                if (zoomScale > 1.01f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val updatedZoom = (zoomScale * zoom).coerceIn(1f, 3.5f)
                        val updatedPan = if (updatedZoom > 1.02f) {
                            Offset(
                                x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                                y = (panOffset.y + pan.y).coerceIn(-800f, 800f)
                            )
                        } else {
                            Offset.Zero
                        }
                        onZoomPanChange(updatedZoom, updatedPan)
                    }
                }
            }
            .graphicsLayer {
                scaleX = zoomScale
                scaleY = zoomScale
                translationX = panOffset.x
                translationY = panOffset.y
            }
            .testTag("kitab_reader_page_card_$pageNumber")
    ) {
        val bmp = renderedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "${book.titleBangla} পৃষ্ঠা $pageNumber",
                contentScale = if (fitToWidth) ContentScale.FillWidth else ContentScale.Fit,
                colorFilter = if (nightMode) ColorFilter.colorMatrix(nightColorMatrix) else null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${book.titleBangla} • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(pageNumber)} / ${KitabFormatUtils.toBanglaDigits(totalPages)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (nightMode) Color.White else ReaderEmeraldDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getCurrentChapterTitle(pageNumber: Int, totalPages: Int): String {
    val chapters = buildBookChaptersList(totalPages)
    var currentTitle = chapters.firstOrNull()?.first ?: "ভূমিকা ও মুকাদ্দিমা"
    for ((title, startPage) in chapters) {
        if (pageNumber >= startPage) {
            currentTitle = title
        } else {
            break
        }
    }
    return currentTitle
}

private fun buildBookChaptersList(totalPages: Int): List<Pair<String, Int>> {
    val safeTotal = totalPages.coerceAtLeast(20)
    return listOf(
        "১. মুকাদ্দিমা ও উসুলুল হাদিস" to 1,
        "২. কিতাবুল ঈমান ও আকাইদ" to (safeTotal * 0.08).toInt().coerceAtLeast(2),
        "৩. কিতাবুত তাহারাত (পবিত্রতা পর্ব)" to (safeTotal * 0.18).toInt().coerceAtLeast(4),
        "৪. কিতাবুস সালাত (নামাজ পর্ব)" to (safeTotal * 0.30).toInt().coerceAtLeast(8),
        "৫. কিতাবুয যাকাত ও সওম" to (safeTotal * 0.45).toInt().coerceAtLeast(12),
        "৬. কিতাবুল হজ্জ ও উমরাহ" to (safeTotal * 0.58).toInt().coerceAtLeast(16),
        "৭. কিতাবুল বুয়ু ও মুআমালাত" to (safeTotal * 0.72).toInt().coerceAtLeast(20),
        "৮. কিতাবুল আদাব, রিকাক ও দুআ" to (safeTotal * 0.86).toInt().coerceAtLeast(24)
    )
}
