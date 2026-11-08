package com.example.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Internal Native 15-Line Indo-Pak Hafezi Quran PDF Reader.
 *
 * - Renders the bundled 616-page PDF (`assets/quran_hafezi_15line.pdf`) natively inside the app
 *   using Android's `PdfRenderer` with zero Google Drive, zero Browser, and zero Google Login.
 * - Matches Panel 4 of the user's UI design reference screenshot:
 *   - Top header: Back button, Surah Bangla name, `পৃষ্ঠা: X / ৬১৬`, Full Screen, Night Mode, Bookmark icon.
 *   - Center: High-resolution PDF page inside gold/emerald frame.
 *   - Bottom controls:
 *     - Row 1: `← পূর্ববর্তী`, Zoom `-` / `+` buttons, `পরবর্তী →`
 *     - Row 2: `পৃষ্ঠা নম্বর` button, `অটো-স্ক্রল` button, `বুকমার্ক` button
 *     - Row 3: Interactive Gold Page Scrubber Slider (`1..616`)
 */
@Composable
fun HafeziQuranReaderView(
    initialPage: Int,
    initialOpenDriveEmbedded: Boolean = false,
    onBackToHub: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        HafeziDrivePdfManager.init(context)
    }

    val safeInitialPage = initialPage.coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES)
    var isVerticalScrollMode by rememberSaveable { mutableStateOf(false) }
    var isFullScreenReading by rememberSaveable { mutableStateOf(false) }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (safeInitialPage - 1).coerceAtLeast(0)
    )
    val pagerState = rememberPagerState(
        initialPage = (safeInitialPage - 1).coerceAtLeast(0),
        pageCount = { HafeziQuranIndexData.TOTAL_PDF_PAGES }
    )

    val currentPageNumber by remember(isVerticalScrollMode) {
        derivedStateOf {
            val rawPage = if (isVerticalScrollMode) {
                lazyListState.firstVisibleItemIndex + 1
            } else {
                pagerState.currentPage + 1
            }
            rawPage.coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES)
        }
    }

    val currentPageMeta = remember(currentPageNumber) {
        HafeziQuranIndexData.getPageMetadata(currentPageNumber)
    }

    val bookmarks by HafeziQuranStorageManager.bookmarksFlow.collectAsState()
    val themeMode by HafeziQuranStorageManager.themeModeFlow.collectAsState()

    val isCurrentPageBookmarked = remember(bookmarks, currentPageNumber) {
        bookmarks.any { it.pageNumber == currentPageNumber }
    }

    var currentZoomScale by remember(currentPageNumber) { mutableFloatStateOf(1f) }
    var showGoToPageDialog by rememberSaveable { mutableStateOf(false) }
    var showBookmarkNoteDialog by rememberSaveable { mutableStateOf(false) }
    var isAutoScrollActive by rememberSaveable { mutableStateOf(false) }
    var feedbackToast by remember { mutableStateOf<String?>(null) }

    fun jumpToPage(targetPage: Int) {
        val safeTarget = targetPage.coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES)
        val idx = safeTarget - 1
        scope.launch {
            if (isVerticalScrollMode) {
                lazyListState.scrollToItem(idx)
            } else {
                pagerState.scrollToPage(idx)
            }
        }
    }

    // Hands-free Auto-Scroll Tilawat Mode
    LaunchedEffect(isAutoScrollActive, isVerticalScrollMode) {
        if (isAutoScrollActive) {
            if (!isVerticalScrollMode) {
                val targetIdx = (currentPageNumber - 1).coerceAtLeast(0)
                isVerticalScrollMode = true
                lazyListState.scrollToItem(targetIdx)
            }
            while (isAutoScrollActive) {
                delay(45L)
                lazyListState.scrollBy(1.8f)
            }
        }
    }

    // Automatically record visited page for Last Read & Reading History/Progress
    LaunchedEffect(currentPageNumber) {
        HafeziQuranStorageManager.recordPageVisited(context, currentPageNumber)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeMode.pageBackground)
            .testTag("hafezi_quran_reader_screen")
    ) {
        // 1. Top Emerald Green Reader Bar (Hidden when Full Screen Mode is active)
        if (!isFullScreenReading) {
            Surface(
                color = Color.Transparent,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0A4D36),
                                    Color(0xFF0F6B4B),
                                    Color(0xFF0A4D36)
                                )
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val step = 38.dp.toPx()
                        val patternColor = Color.White.copy(alpha = 0.05f)
                        val cols = (size.width / step).toInt() + 2
                        val rows = (size.height / step).toInt() + 2
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val cx = c * step
                                val cy = r * step
                                val radius = step * 0.35f
                                drawCircle(
                                    color = patternColor,
                                    radius = radius,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                val diamond = Path().apply {
                                    moveTo(cx, cy - radius)
                                    lineTo(cx + radius, cy)
                                    lineTo(cx, cy + radius)
                                    lineTo(cx - radius, cy)
                                    close()
                                }
                                drawPath(diamond, color = patternColor, style = Stroke(width = 1.dp.toPx()))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Back Button
                            IconButton(
                                onClick = onBackToHub,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quran_reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "ফিরে যান",
                                    tint = Color.White
                                )
                            }

                            // Center Title: "সূরা আল-বাকারা" & "পৃষ্ঠা: ৩ / ৬১৬ • পারা ১"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showGoToPageDialog = true }
                            ) {
                                Text(
                                    text = "সূরা ${currentPageMeta.primarySurah.nameBangla}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} / ${HafeziQuranIndexData.toBanglaDigits(HafeziQuranIndexData.TOTAL_PDF_PAGES)} • পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)}",
                                    color = Color(0xFFFFE082),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            // Right Action Icons: Search, FullScreen, NightMode, ScrollMode, Bookmark
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { showGoToPageDialog = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("quran_reader_goto_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "পৃষ্ঠা খুঁজুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { isFullScreenReading = !isFullScreenReading },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("quran_reader_fullscreen_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "পূর্ণ স্ক্রিন",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val nextMode = when (themeMode) {
                                            MushafThemeMode.EMDADIA_CREAM -> MushafThemeMode.NIGHT_MODE
                                            MushafThemeMode.NIGHT_MODE -> MushafThemeMode.CLASSIC_WHITE
                                            MushafThemeMode.CLASSIC_WHITE -> MushafThemeMode.SEPIA_HERITAGE
                                            MushafThemeMode.SEPIA_HERITAGE -> MushafThemeMode.EMDADIA_CREAM
                                        }
                                        HafeziQuranStorageManager.setMushafTheme(context, nextMode)
                                        feedbackToast = "থিম: ${nextMode.titleBangla}"
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("quran_reader_theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (themeMode == MushafThemeMode.NIGHT_MODE) {
                                            Icons.Default.LightMode
                                        } else {
                                            Icons.Default.DarkMode
                                        },
                                        contentDescription = "নাইট মোড",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val targetPage = currentPageNumber
                                        val switchingToVertical = !isVerticalScrollMode
                                        isVerticalScrollMode = switchingToVertical
                                        scope.launch {
                                            if (switchingToVertical) {
                                                lazyListState.scrollToItem((targetPage - 1).coerceAtLeast(0))
                                            } else {
                                                pagerState.scrollToPage((targetPage - 1).coerceAtLeast(0))
                                            }
                                        }
                                        feedbackToast = if (switchingToVertical) {
                                            "উপর-নিচ স্ক্রল মোড চালু হয়েছে"
                                        } else {
                                            "পৃষ্ঠা সোয়াইপ মোড চালু হয়েছে"
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("quran_reader_scroll_mode_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isVerticalScrollMode) {
                                            Icons.Default.SwapVert
                                        } else {
                                            Icons.Default.SwapHoriz
                                        },
                                        contentDescription = "স্ক্রল মোড",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isCurrentPageBookmarked) {
                                            HafeziQuranStorageManager.removeBookmark(context, currentPageNumber)
                                            feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্ক থেকে সরানো হয়েছে"
                                        } else {
                                            HafeziQuranStorageManager.addOrUpdateBookmark(context, currentPageNumber)
                                            feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্কে সংরক্ষিত হয়েছে"
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("quran_reader_bookmark_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPageBookmarked) {
                                            Icons.Default.Bookmark
                                        } else {
                                            Icons.Default.BookmarkBorder
                                        },
                                        contentDescription = "বুকমার্ক করুন",
                                        tint = Color(0xFFD9A82E),
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                        }

                        if (feedbackToast != null || currentZoomScale > 1.02f) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = feedbackToast
                                        ?: "জুম সক্রিয়: ${HafeziQuranIndexData.toBanglaDigits((currentZoomScale * 100).toInt())}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE6F4EE),
                                    fontSize = 11.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (currentZoomScale > 1.02f) {
                                        Text(
                                            text = "জুম ১০০% করুন",
                                            color = Color(0xFFFFE082),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { currentZoomScale = 1f }
                                                .testTag("quran_reader_reset_zoom_chip")
                                        )
                                    }
                                    if (feedbackToast != null) {
                                        Text(
                                            text = "নোট যুক্ত করুন",
                                            color = Color(0xFFFFE082),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                feedbackToast = null
                                                showBookmarkNoteDialog = true
                                            }
                                        )
                                        Text(
                                            text = "✕",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { feedbackToast = null }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Main Native PDF Page Surface (Vertical Continuous or Horizontal Pager)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(themeMode.pageBackground)
        ) {
            if (isVerticalScrollMode) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = currentZoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hafezi_quran_vertical_pager")
                ) {
                    items(
                        count = HafeziQuranIndexData.TOTAL_PDF_PAGES,
                        key = { pageIdx -> pageIdx + 1 }
                    ) { pageIdx ->
                        val pageNum = (pageIdx + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES)
                        val meta = remember(pageNum) { HafeziQuranIndexData.getPageMetadata(pageNum) }
                        HafeziMushafPageView(
                            pageMetadata = meta,
                            themeMode = themeMode,
                            externalZoomScale = if (pageNum == currentPageNumber) currentZoomScale else 1f,
                            onZoomChanged = { updatedScale ->
                                if (pageNum == currentPageNumber) {
                                    currentZoomScale = updatedScale
                                }
                            },
                            isVerticalContinuousMode = true,
                            isPrimaryActivePage = (pageNum == currentPageNumber)
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = currentZoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hafezi_quran_horizontal_pager")
                ) { pageIdx ->
                    val pageNum = (pageIdx + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES)
                    val meta = remember(pageNum) { HafeziQuranIndexData.getPageMetadata(pageNum) }
                    HafeziMushafPageView(
                        pageMetadata = meta,
                        themeMode = themeMode,
                        externalZoomScale = if (pageNum == currentPageNumber) currentZoomScale else 1f,
                        onZoomChanged = { updatedScale ->
                            if (pageNum == currentPageNumber) {
                                currentZoomScale = updatedScale
                            }
                        },
                        isVerticalContinuousMode = false,
                        isPrimaryActivePage = (pageNum == currentPageNumber)
                    )
                }
            }

            // Floating Exit Full Screen Pill when in Full Screen Mode
            if (isFullScreenReading) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0A4D36).copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .border(1.dp, Color(0xFFD9A82E), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { isFullScreenReading = false }
                        .testTag("quran_reader_exit_fullscreen_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "পূর্ণ স্ক্রিন বন্ধ করুন",
                            tint = Color(0xFFD9A82E),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} • মেনু দেখান",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Bottom Control Bar matching Panel 4 of the Design Reference Screenshot
        if (!isFullScreenReading) {
            Surface(
                color = Color(0xFF0A4D36),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quran_reader_bottom_control_bar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: ← পূর্ববর্তী | [-] [+] Zoom Controls | পরবর্তী →
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // ← পূর্ববর্তী
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0F6B4B),
                            modifier = Modifier
                                .border(1.dp, Color(0xFFD9A82E).copy(alpha = 0.65f), RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .clickable(enabled = currentPageNumber > 1) {
                                    jumpToPage(currentPageNumber - 1)
                                }
                                .testTag("quran_reader_prev_page_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "পূর্ববর্তী পৃষ্ঠা",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "পূর্ববর্তী",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Zoom Out (-) and Zoom In (+) Circular Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F6B4B),
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, Color(0xFFD9A82E).copy(alpha = 0.7f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        currentZoomScale = (currentZoomScale - 0.25f).coerceIn(1f, 3.5f)
                                    }
                                    .testTag("quran_reader_zoom_out_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "জুম কমান",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F6B4B),
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, Color(0xFFD9A82E).copy(alpha = 0.7f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        currentZoomScale = (currentZoomScale + 0.25f).coerceIn(1f, 3.5f)
                                    }
                                    .testTag("quran_reader_zoom_in_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "জুম বাড়ান",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // পরবর্তী →
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0F6B4B),
                            modifier = Modifier
                                .border(1.dp, Color(0xFFD9A82E).copy(alpha = 0.65f), RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .clickable(enabled = currentPageNumber < HafeziQuranIndexData.TOTAL_PDF_PAGES) {
                                    jumpToPage(currentPageNumber + 1)
                                }
                                .testTag("quran_reader_next_page_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "পরবর্তী",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "পরবর্তী পৃষ্ঠা",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Row 2: [⊞ পৃষ্ঠা নম্বর] | [অটো-স্ক্রল] | [🔖 বুকমার্ক]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F6B4B),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showGoToPageDialog = true }
                                .testTag("quran_reader_bottom_page_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = Color(0xFFD9A82E),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "পৃষ্ঠা নম্বর (${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)})",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAutoScrollActive) Color(0xFFD9A82E) else Color(0xFF0F6B4B),
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isAutoScrollActive = !isAutoScrollActive
                                    feedbackToast = if (isAutoScrollActive) {
                                        "অটো-স্ক্রল তিলাওয়াত চালু হয়েছে"
                                    } else {
                                        "অটো-স্ক্রল তিলাওয়াত বন্ধ করা হয়েছে"
                                    }
                                }
                                .testTag("quran_reader_autoscroll_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAutoScrollActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "অটো-স্ক্রল",
                                    tint = if (isAutoScrollActive) Color(0xFF0B1914) else Color(0xFFFFE082),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAutoScrollActive) "থামান" else "অটো-স্ক্রল",
                                    color = if (isAutoScrollActive) Color(0xFF0B1914) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrentPageBookmarked) Color(0xFFD9A82E) else Color(0xFF0F6B4B),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isCurrentPageBookmarked) {
                                        HafeziQuranStorageManager.removeBookmark(context, currentPageNumber)
                                        feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্ক থেকে সরানো হয়েছে"
                                    } else {
                                        HafeziQuranStorageManager.addOrUpdateBookmark(context, currentPageNumber)
                                        feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্কে সংরক্ষিত হয়েছে"
                                    }
                                }
                                .testTag("quran_reader_bottom_bookmark_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (isCurrentPageBookmarked) Color(0xFF0B1914) else Color(0xFFD9A82E),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isCurrentPageBookmarked) "বুকমার্ককৃত" else "বুকমার্ক",
                                    color = if (isCurrentPageBookmarked) Color(0xFF0B1914) else Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Row 3: Interactive Gold Page Scrubber Slider (1..616)
                    Slider(
                        value = currentPageNumber.toFloat(),
                        onValueChange = { newVal ->
                            jumpToPage(newVal.toInt())
                        },
                        valueRange = 1f..HafeziQuranIndexData.TOTAL_PDF_PAGES.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD9A82E),
                            activeTrackColor = Color(0xFFD9A82E),
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .testTag("quran_reader_page_slider")
                    )
                }
            }
        }
    }

    if (showGoToPageDialog) {
        DirectPageJumpDialog(
            currentPage = currentPageNumber,
            onDismiss = { showGoToPageDialog = false },
            onJumpToPage = { targetPage ->
                showGoToPageDialog = false
                jumpToPage(targetPage)
            }
        )
    }

    if (showBookmarkNoteDialog) {
        var noteText by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBookmarkNoteDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্ক ও নোট",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0F6B4B),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সূরা ${currentPageMeta.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A5D54)
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("বুকমার্ক নোট (ঐচ্ছিক, যেমন: হিফজ সবক / তিলাওয়াত)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_bookmark_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HafeziQuranStorageManager.addOrUpdateBookmark(context, currentPageNumber, noteText)
                        showBookmarkNoteDialog = false
                        feedbackToast = "বুকমার্ক ও নোট সংরক্ষিত হয়েছে"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6B4B)),
                    modifier = Modifier.testTag("quran_bookmark_save_note_button")
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkNoteDialog = false }) {
                    Text("বাতিল", color = Color(0xFF4A5D54))
                }
            }
        )
    }
}

@Composable
fun DirectPageJumpDialog(
    currentPage: Int,
    onDismiss: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    var pageInput by rememberSaveable { mutableStateOf(currentPage.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedPreviewPage = HafeziQuranIndexData.parseBanglaOrEnglishNumber(pageInput)
    val previewMeta = remember(parsedPreviewPage) {
        if (parsedPreviewPage != null && parsedPreviewPage in 1..HafeziQuranIndexData.TOTAL_PDF_PAGES) {
            HafeziQuranIndexData.getPageMetadata(parsedPreviewPage)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "পৃষ্ঠা নম্বর দিয়ে সরাসরি খুঁজুন",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F6B4B),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "১৫ লাইনের ইন্দো-পাক হাফেজি কোরআন শরীফ PDF-এর যেকোনো পৃষ্ঠা নম্বর লিখুন (১ থেকে ৬১৬):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A5D54)
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = {
                        pageInput = it
                        errorMsg = null
                    },
                    label = { Text("পৃষ্ঠা নম্বর (১ - ৬১৬)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_direct_page_input_field")
                )

                Slider(
                    value = (parsedPreviewPage ?: currentPage)
                        .coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES).toFloat(),
                    onValueChange = { newVal ->
                        pageInput = newVal.toInt()
                            .coerceIn(1, HafeziQuranIndexData.TOTAL_PDF_PAGES).toString()
                        errorMsg = null
                    },
                    valueRange = 1f..HafeziQuranIndexData.TOTAL_PDF_PAGES.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD9A82E),
                        activeTrackColor = Color(0xFF0F6B4B)
                    )
                )

                if (previewMeta != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3F9F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.pageNumber)} • সূরা ${previewMeta.primarySurah.nameBangla}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF0F6B4B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "পারা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.paraNumber)} (${previewMeta.paraNameBangla}) • মঞ্জিল ${HafeziQuranIndexData.toBanglaDigits(previewMeta.manzilNumber)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A5D54)
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = HafeziQuranIndexData.parseBanglaOrEnglishNumber(pageInput)
                    if (p != null && p in 1..HafeziQuranIndexData.TOTAL_PDF_PAGES) {
                        onJumpToPage(p)
                    } else {
                        errorMsg = "অনুগ্রহ করে ১ থেকে ৬১৬-এর মধ্যে সঠিক পৃষ্ঠা নম্বর লিখুন"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6B4B)),
                modifier = Modifier.testTag("quran_direct_page_confirm_button")
            ) {
                Text("পৃষ্ঠায় যান", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = Color(0xFF4A5D54))
            }
        }
    )
}
