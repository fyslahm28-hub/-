package com.example.ui.quran

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-Screen Classic Printed 15-Line Indo-Pak / Emdadia Hafezi Mushaf Reader
 * designed to match the exact visual style of the reference Hafezi Quran page:
 * - Rounded-bottom emerald green top header bar with subtle Islamic geometric pattern
 * - Single-line title: "আল-বাকারা · পারা ১ · পৃষ্ঠা ৪"
 * - 4 clean white action icons: Search (🔍), Bookmark (🔖), Night Mode (🌙), and Vertical/Horizontal Scroll Toggle (⇅)
 * - Continuous Vertical Scroll (default, showing seamless transition to next page below) + Horizontal Pager mode
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

    val drivePdfState by HafeziDrivePdfManager.stateFlow.collectAsState()
    var showDriveEmbeddedWebView by rememberSaveable { mutableStateOf(initialOpenDriveEmbedded) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            HafeziDrivePdfManager.importPdfFromUri(context, uri, scope) { success, msg ->
                if (success) {
                    showDriveEmbeddedWebView = false
                }
            }
        }
    }

    val safeInitialPage = initialPage.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)

    // Default to Vertical Continuous Scroll mode (matching the screenshot where page 01:03 flows into 01:04 below it)
    var isVerticalScrollMode by rememberSaveable { mutableStateOf(true) }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (safeInitialPage - 1).coerceAtLeast(0)
    )
    val pagerState = rememberPagerState(
        initialPage = (safeInitialPage - 1).coerceAtLeast(0),
        pageCount = { HafeziQuranIndexData.TOTAL_MUSHAF_PAGES }
    )

    val currentPageNumber by remember(isVerticalScrollMode) {
        derivedStateOf {
            val rawPage = if (isVerticalScrollMode) {
                lazyListState.firstVisibleItemIndex + 1
            } else {
                pagerState.currentPage + 1
            }
            rawPage.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
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

    // Hands-free Auto-Scroll Tilawat Mode
    LaunchedEffect(isAutoScrollActive, isVerticalScrollMode) {
        if (isAutoScrollActive) {
            if (!isVerticalScrollMode) {
                isVerticalScrollMode = true
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
        // 1. Classic Emerald Green Top Bar with Rounded Bottom Corners & Subtle Islamic Geometric Pattern
        Surface(
            color = Color.Transparent,
            shadowElevation = 3.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF164A35),
                                Color(0xFF1D5E44),
                                Color(0xFF164A35)
                            )
                        )
                    )
            ) {
                // Subtle Islamic Geometric Pattern Overlay across Header Bar
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val step = 38.dp.toPx()
                    val patternColor = Color.White.copy(alpha = 0.055f)
                    val cols = (size.width / step).toInt() + 2
                    val rows = (size.height / step).toInt() + 2
                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            val cx = c * step
                            val cy = r * step
                            val radius = step * 0.36f
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
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Back Arrow + "আল-বাকারা · পারা ১ · পৃষ্ঠা ৪"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showGoToPageDialog = true }
                        ) {
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

                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    ) {
                                        append(currentPageMeta.primarySurah.nameBangla)
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.5.sp
                                        )
                                    ) {
                                        append("  ·  পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)}  ·  পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)}")
                                    }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Right: 4 Clean White Icons (Search 🔍, Bookmark 🔖, Night Mode 🌙, Scroll Direction ⇅)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 1. Search / Go-to-Page Icon (🔍)
                            IconButton(
                                onClick = { showGoToPageDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quran_reader_goto_page_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "পৃষ্ঠা বা সূরা খুঁজুন",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 2. Bookmark Toggle Icon (🔖)
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
                                    .size(38.dp)
                                    .testTag("quran_reader_bookmark_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPageBookmarked) {
                                        Icons.Default.Bookmark
                                    } else {
                                        Icons.Default.BookmarkBorder
                                    },
                                    contentDescription = "বুকমার্ক করুন",
                                    tint = if (isCurrentPageBookmarked) Color(0xFFFFE082) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 3. Night Mode / Paper Theme Toggle Icon (🌙)
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
                                    .size(38.dp)
                                    .testTag("quran_reader_theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (themeMode == MushafThemeMode.NIGHT_MODE) {
                                        Icons.Default.LightMode
                                    } else {
                                        Icons.Default.DarkMode
                                    },
                                    contentDescription = "নাইট ও পেপার মোড",
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // 4. Scroll Direction Toggle Icon (⇅)
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
                                        "উপর-নিচ (Vertical) স্ক্রল মোড চালু হয়েছে"
                                    } else {
                                        "ডান-বাম (Horizontal) পেজ সোয়াইপ মোড চালু হয়েছে"
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quran_reader_scroll_mode_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isVerticalScrollMode) {
                                        Icons.Default.SwapVert
                                    } else {
                                        Icons.Default.SwapHoriz
                                    },
                                    contentDescription = "স্ক্রল মোড পরিবর্তন",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Subtle Toast / Zoom Reset Strip (only shown briefly when action triggered or zoomed)
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
                                text = feedbackToast ?: "জুম সক্রিয়: ${(currentZoomScale * 100).toInt()}% (ডাবল ট্যাপ করে রিসেট করুন)",
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

        // 2. Full-Screen Crisp 15-Line Hafezi Mushaf Pages OR Embedded Google Drive PDF Viewer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(themeMode.pageBackground)
        ) {
            if (showDriveEmbeddedWebView && drivePdfState.status != DrivePdfStatus.READY_OFFLINE) {
                GoogleDriveEmbeddedMushafView(
                    drivePdfState = drivePdfState,
                    onStartOfflineDownload = {
                        HafeziDrivePdfManager.startDrivePdfDownload(context, scope)
                    },
                    onPickLocalPdf = {
                        pdfPickerLauncher.launch("application/pdf")
                    },
                    onOpenInDriveApp = {
                        HafeziDrivePdfManager.openDriveLinkExternally(context)
                    },
                    onSwitchToDigital15Line = {
                        showDriveEmbeddedWebView = false
                        HafeziDrivePdfManager.setRenderSource(
                            context,
                            QuranMushafRenderSource.DIGITAL_15LINE_MUSHAF
                        )
                    }
                )
            } else if (isVerticalScrollMode) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = currentZoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hafezi_quran_vertical_pager")
                ) {
                    items(
                        count = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
                        key = { pageIdx -> pageIdx + 1 }
                    ) { pageIdx ->
                        val pageNum = (pageIdx + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
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
                    val pageNum = (pageIdx + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
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
        }

        // 3. Bottom Mushaf Quick Control Bar (পূর্ববর্তী/পরবর্তী পৃষ্ঠা, অটো-স্ক্রল তিলাওয়াত ও মঞ্জিল ইনফো)
        Surface(
            color = Color(0xFF164A35),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quran_reader_bottom_control_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous Page Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = currentPageNumber > 1) {
                            val prevIdx = (currentPageNumber - 2).coerceIn(0, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES - 1)
                            scope.launch {
                                if (isVerticalScrollMode) {
                                    lazyListState.scrollToItem(prevIdx)
                                } else {
                                    pagerState.scrollToPage(prevIdx)
                                }
                            }
                        }
                        .testTag("quran_reader_prev_page_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "পূর্ববর্তী পৃষ্ঠা",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "পূর্ববর্তী",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center Page & Manzil Status Chip + Auto-Scroll Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0E3525),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showGoToPageDialog = true }
                            .testTag("quran_reader_bottom_page_badge")
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)}/৬১০ • মঞ্জিল ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.manzilNumber)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isAutoScrollActive) Color(0xFFD9A82E) else Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                isAutoScrollActive = !isAutoScrollActive
                                feedbackToast = if (isAutoScrollActive) {
                                    "হ্যান্ডস-ফ্রি অটো-স্ক্রল তিলাওয়াত চালু হয়েছে"
                                } else {
                                    "অটো-স্ক্রল তিলাওয়াত বন্ধ করা হয়েছে"
                                }
                            }
                            .testTag("quran_reader_autoscroll_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAutoScrollActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "অটো-স্ক্রল",
                                tint = if (isAutoScrollActive) Color(0xFF0B1914) else Color(0xFFFFE082),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isAutoScrollActive) "থামান" else "অটো-স্ক্রল",
                                color = if (isAutoScrollActive) Color(0xFF0B1914) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Toggle between Google Drive PDF Mushaf and 15-Line Mushaf
                    val isDriveActive = showDriveEmbeddedWebView ||
                        (drivePdfState.status == DrivePdfStatus.READY_OFFLINE &&
                            drivePdfState.renderSource == QuranMushafRenderSource.DRIVE_PDF_MUSHAF)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isDriveActive) Color(0xFFD9A82E) else Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (drivePdfState.status == DrivePdfStatus.READY_OFFLINE) {
                                    val nextSource =
                                        if (drivePdfState.renderSource == QuranMushafRenderSource.DRIVE_PDF_MUSHAF) {
                                            QuranMushafRenderSource.DIGITAL_15LINE_MUSHAF
                                        } else {
                                            QuranMushafRenderSource.DRIVE_PDF_MUSHAF
                                        }
                                    HafeziDrivePdfManager.setRenderSource(context, nextSource)
                                    showDriveEmbeddedWebView = false
                                    feedbackToast = if (nextSource == QuranMushafRenderSource.DRIVE_PDF_MUSHAF) {
                                        "গুগল ড্রাইভ পিডিএফ মুসহাফ চালু হয়েছে"
                                    } else {
                                        "১৫-লাইন ডিজিটাল মুসহাফ চালু হয়েছে"
                                    }
                                } else {
                                    showDriveEmbeddedWebView = !showDriveEmbeddedWebView
                                    feedbackToast = if (showDriveEmbeddedWebView) {
                                        "গুগল ড্রাইভ পিডিএফ ভিউয়ার চালু হয়েছে"
                                    } else {
                                        "১৫-লাইন ডিজিটাল মুসহাফ চালু হয়েছে"
                                    }
                                }
                            }
                            .testTag("quran_reader_drive_pdf_toggle_button")
                    ) {
                        Text(
                            text = if (isDriveActive) "PDF সক্রিয়" else "ড্রাইভ PDF",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = if (isDriveActive) Color(0xFF0B1914) else Color(0xFFFFE082),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Next Page Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = currentPageNumber < HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                            val nextIdx = currentPageNumber.coerceIn(0, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES - 1)
                            scope.launch {
                                if (isVerticalScrollMode) {
                                    lazyListState.scrollToItem(nextIdx)
                                } else {
                                    pagerState.scrollToPage(nextIdx)
                                }
                            }
                        }
                        .testTag("quran_reader_next_page_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পরবর্তী",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "পরবর্তী পৃষ্ঠা",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Direct Page & Surah Lookup Dialog inside Reader (opened via Search icon 🔍 or tapping top bar title)
    if (showGoToPageDialog) {
        DirectPageJumpDialog(
            currentPage = currentPageNumber,
            onDismiss = { showGoToPageDialog = false },
            onJumpToPage = { targetPage ->
                showGoToPageDialog = false
                val targetIdx = (targetPage - 1).coerceIn(0, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES - 1)
                scope.launch {
                    if (isVerticalScrollMode) {
                        lazyListState.scrollToItem(targetIdx)
                    } else {
                        pagerState.scrollToPage(targetIdx)
                    }
                }
            }
        )
    }

    // Bookmark Note Dialog
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
        if (parsedPreviewPage != null && parsedPreviewPage in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
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
                    text = "ইন্দো-পাক / এমদাদিয়া ১৫ লাইনের হাফেজি মুসহাফের যেকোনো পৃষ্ঠা নম্বর লিখুন (১ থেকে ৬১০):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A5D54)
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = {
                        pageInput = it
                        errorMsg = null
                    },
                    label = { Text("পৃষ্ঠা নম্বর (১ - ৬১০)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_direct_page_input_field")
                )

                // Quick Slider inside dialog for rapid page scrubbing
                Slider(
                    value = (parsedPreviewPage ?: currentPage).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES).toFloat(),
                    onValueChange = { newVal ->
                        pageInput = newVal.toInt().coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES).toString()
                        errorMsg = null
                    },
                    valueRange = 1f..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD9A82E),
                        activeTrackColor = Color(0xFF0F6B4B),
                        inactiveTrackColor = Color(0xFF0F6B4B).copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("quran_reader_page_slider")
                )

                if (previewMeta != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEAF4EF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.pageNumber)} • পারা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.paraNumber)} (${previewMeta.paraNameBangla})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0F6B4B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সূরা ${previewMeta.primarySurah.nameBangla} (${previewMeta.primarySurah.nameArabic}) • মঞ্জিল ${HafeziQuranIndexData.toBanglaDigits(previewMeta.manzilNumber)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF141A17)
                            )
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
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
                    val target = HafeziQuranIndexData.parseBanglaOrEnglishNumber(pageInput)
                    if (target != null && target in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                        onJumpToPage(target)
                    } else {
                        errorMsg = "অনুগ্রহ করে ১ থেকে ৬১০ এর মধ্যে সঠিক পৃষ্ঠা নম্বর লিখুন।"
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
                Text("বন্ধ করুন", color = Color(0xFF4A5D54))
            }
        }
    )
}
