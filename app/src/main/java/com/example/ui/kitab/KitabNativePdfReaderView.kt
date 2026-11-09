package com.example.ui.kitab

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReaderDeepGreen = Color(0xFF074E35)
private val ReaderDarkHeader = Color(0xFF053B27)
private val ReaderGold = Color(0xFFDDA83A)
private val ReaderGoldSoft = Color(0xFFF3E2B3)

/**
 * Full-featured internal Native PDF Reader for downloaded Bangla Hadith Sharah & Qawmi Kitabs.
 * Never opens external apps, Chrome, or Google Drive.
 */
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
    onCloseReader: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val resolvedTotalPages = totalPages.coerceAtLeast(1)
    var currentPage by remember(book.bookId) {
        mutableIntStateOf(initialPage.coerceIn(1, resolvedTotalPages))
    }
    var zoomScale by remember(book.bookId, currentPage) { mutableFloatStateOf(1f) }
    var panOffset by remember(book.bookId, currentPage) { mutableStateOf(Offset.Zero) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var showBookmarkNoteDialog by remember { mutableStateOf(false) }
    var showThemePickerDialog by remember { mutableStateOf(false) }
    var volumeSwitchMessage by remember { mutableStateOf<String?>(null) }

    var renderedBitmap by remember(book.bookId, currentPage) { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember(book.bookId, currentPage) { mutableStateOf(true) }

    val isCurrentPageBookmarked = bookmarksForBook.any { it.pageNumber == currentPage }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            onCloseReader()
        }
    }

    // Automatically persist last read page to local Room database whenever page changes
    LaunchedEffect(book.bookId, currentPage) {
        KitabDownloadAndStorageManager.recordPageRead(
            context = context,
            bookId = book.bookId,
            pageNumber = currentPage,
            totalPages = resolvedTotalPages
        )
    }

    // Render current PDF page using native PdfRenderer
    LaunchedEffect(book.bookId, currentPage, isLandscape) {
        isRenderingPage = true
        val targetWidth = if (isLandscape) 1440 else 1080
        renderedBitmap = KitabDownloadAndStorageManager.renderKitabPageBitmap(
            context = context,
            bookId = book.bookId,
            pageNumber = currentPage,
            targetWidthPx = targetWidth
        )
        isRenderingPage = false
    }

    val pageSurfaceBg = when (themeMode) {
        KitabReaderThemeMode.MUSHAF_CREAM -> Color(0xFFFCF8EE)
        KitabReaderThemeMode.CLASSIC_WHITE -> Color.White
        KitabReaderThemeMode.SEPIA_WARM -> Color(0xFFF4EAD5)
        KitabReaderThemeMode.NIGHT_DARK -> Color(0xFF121A16)
    }

    val colorFilter: ColorFilter? = remember(themeMode) {
        when (themeMode) {
            KitabReaderThemeMode.CLASSIC_WHITE -> null
            KitabReaderThemeMode.MUSHAF_CREAM -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.98f, 0f, 0f, 0f, 0f,
                        0f, 0.96f, 0f, 0f, 0f,
                        0f, 0f, 0.90f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            KitabReaderThemeMode.SEPIA_WARM -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0.04f, 0f, 0f, 0f,
                        0.02f, 0.91f, 0f, 0f, 0f,
                        0f, 0f, 0.80f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            KitabReaderThemeMode.NIGHT_DARK -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.88f, 0f, 0f, 0f, 240f,
                        0f, -0.88f, 0f, 0f, 245f,
                        0f, 0f, -0.88f, 0f, 235f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background( Color(0xFF062C1E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("kitab_internal_pdf_reader_screen")
    ) {
        // 1. Top Header Bar (Hidden when Full-screen mode is active)
        if (!isFullScreen) {
            Surface(
                color = ReaderDarkHeader,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCloseReader,
                            modifier = Modifier.testTag("kitab_reader_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "কিতাব রিডার বন্ধ করুন",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = book.titleBangla,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("kitab_reader_book_title")
                            )
                            Text(
                                text = "পৃষ্ঠা: ${KitabFormatUtils.toBanglaDigits(currentPage)} / ${KitabFormatUtils.toBanglaDigits(resolvedTotalPages)} • ${book.formattedVolumeBadge}",
                                color = ReaderGoldSoft,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("kitab_reader_page_counter")
                            )
                        }

                        IconButton(
                            onClick = { showThemePickerDialog = true },
                            modifier = Modifier.testTag("kitab_reader_theme_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "পড়ার থিম পরিবর্তন",
                                tint = ReaderGoldSoft
                            )
                        }

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
                                    showBookmarkNoteDialog = true
                                }
                            },
                            modifier = Modifier.testTag("kitab_reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "পৃষ্ঠা বুকমার্ক করুন",
                                tint = if (isCurrentPageBookmarked) ReaderGold else Color.White
                            )
                        }

                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier.testTag("kitab_reader_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ফুল-স্ক্রিন মোড",
                                tint = Color.White
                            )
                        }
                    }

                    // Multi-Volume Switcher Strip (if this Kitab series has multiple volumes in catalog)
                    if (siblingVolumes.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF042B1C))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("kitab_reader_volume_switcher_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = ReaderGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "খণ্ড পরিবর্তন:",
                                        color = ReaderGoldSoft,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            items(siblingVolumes, key = { it.bookId }) { volItem ->
                                val isCurrentVol = volItem.bookId == book.bookId
                                val isDownloaded = volItem.isReadyToReadOffline
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        isCurrentVol -> ReaderGold
                                        isDownloaded -> ReaderDeepGreen
                                        else -> Color(0xFF173D2E)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrentVol) ReaderGold else ReaderGold.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            if (isCurrentVol) return@clickable
                                            if (isDownloaded) {
                                                onSwitchVolume(volItem)
                                            } else {
                                                volumeSwitchMessage =
                                                    "${volItem.titleBangla} (${KitabFormatUtils.toBanglaDigits(volItem.volumeNumber)}ম খণ্ড) এখনো ডাউনলোড করা হয়নি। ক্যাটালগ থেকে ডাউনলোড করে পড়ুন।"
                                            }
                                        }
                                        .testTag("kitab_reader_volume_chip_${volItem.volumeNumber}")
                                ) {
                                    Text(
                                        text = "${KitabFormatUtils.toBanglaDigits(volItem.volumeNumber)}ম খণ্ড" +
                                            if (isDownloaded) " ✓" else " (ডাউনলোড প্রয়োজন)",
                                        color = if (isCurrentVol) Color(0xFF062C1E) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrentVol) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Main PDF Page Canvas Area (Supports Portrait & Landscape + Pinch-to-Zoom)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0A3826))
                .padding(horizontal = if (isFullScreen) 2.dp else 8.dp, vertical = if (isFullScreen) 2.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(if (isFullScreen) 4.dp else 12.dp),
                colors = CardDefaults.cardColors(containerColor = pageSurfaceBg),
                border = BorderStroke(1.5.dp, ReaderGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentPage) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                            zoomScale = newScale
                            panOffset = if (newScale > 1.02f) {
                                Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-900f, 900f),
                                    y = (panOffset.y + pan.y).coerceIn(-1200f, 1200f)
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .testTag("kitab_pdf_page_canvas_$currentPage")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = renderedBitmap
                    if (bmp != null && !bmp.isRecycled) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "${book.titleBangla} - পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}",
                            contentScale = if (isLandscape) ContentScale.FillWidth else ContentScale.Fit,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                                .padding(6.dp)
                        )
                    } else if (isRenderingPage) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = ReaderDeepGreen)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)} লোড হচ্ছে...",
                                color = ReaderDeepGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Floating Exit Fullscreen Button when Fullscreen mode is active
            if (isFullScreen) {
                Surface(
                    shape = CircleShape,
                    color = ReaderDarkHeader.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ReaderGold),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .testTag("kitab_reader_exit_fullscreen_button")
                ) {
                    IconButton(onClick = { isFullScreen = false }) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "ফুল-স্ক্রিন বন্ধ করুন",
                            tint = ReaderGold
                        )
                    }
                }
            }
        }

        // 3. Bottom Navigation & Zoom Controls Bar (Hidden in Full-screen mode)
        if (!isFullScreen) {
            Surface(
                color = ReaderDarkHeader,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Prev / Zoom Out / Zoom In / Next Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentPage > 1) {
                                    currentPage -= 1
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                    KitabDownloadAndStorageManager.recordPageRead(
                                        context = context,
                                        bookId = book.bookId,
                                        pageNumber = currentPage,
                                        totalPages = resolvedTotalPages
                                    )
                                }
                            },
                            enabled = currentPage > 1,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ReaderGold.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("kitab_reader_prev_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পূর্ববর্তী", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Zoom Out / Zoom Badge / Zoom In
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ReaderDeepGreen,
                                border = BorderStroke(1.dp, ReaderGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        zoomScale = (zoomScale - 0.25f).coerceAtLeast(1.0f)
                                        if (zoomScale <= 1.02f) panOffset = Offset.Zero
                                    }
                                    .testTag("kitab_reader_zoom_out_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "জুম আউট",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${KitabFormatUtils.toBanglaDigits((zoomScale * 100).toInt())}%",
                                color = ReaderGoldSoft,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                    .padding(horizontal = 4.dp)
                                    .testTag("kitab_reader_zoom_percentage")
                            )

                            Surface(
                                shape = CircleShape,
                                color = ReaderDeepGreen,
                                border = BorderStroke(1.dp, ReaderGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                    }
                                    .testTag("kitab_reader_zoom_in_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "জুম ইন",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (currentPage < resolvedTotalPages) {
                                    currentPage += 1
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                    KitabDownloadAndStorageManager.recordPageRead(
                                        context = context,
                                        bookId = book.bookId,
                                        pageNumber = currentPage,
                                        totalPages = resolvedTotalPages
                                    )
                                }
                            },
                            enabled = currentPage < resolvedTotalPages,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ReaderGold.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("kitab_reader_next_page_button")
                        ) {
                            Text("পরবর্তী", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Direct Page Jump Button + Bookmark Button + Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPageJumpDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ReaderGold.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ReaderGoldSoft),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("kitab_reader_page_jump_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পৃষ্ঠা নম্বর", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (resolvedTotalPages > 1) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { newVal ->
                                    currentPage = newVal.toInt().coerceIn(1, resolvedTotalPages)
                                },
                                valueRange = 1f..resolvedTotalPages.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = ReaderGold,
                                    activeTrackColor = ReaderGold,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .testTag("kitab_reader_page_slider")
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Text(
                            text = "${KitabFormatUtils.toBanglaDigits(((currentPage * 100) / resolvedTotalPages).coerceIn(1, 100))}%",
                            color = ReaderGoldSoft,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Direct Page Jump Dialog
    if (showPageJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPage.toString()) }
        var pageError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPageJumpDialog = false },
            modifier = Modifier.testTag("kitab_reader_page_jump_dialog"),
            title = {
                Text(
                    text = "সরাসরি পৃষ্ঠায় যান",
                    fontWeight = FontWeight.Bold,
                    color = ReaderDeepGreen
                )
            },
            text = {
                Column {
                    Text(
                        text = "১ থেকে ${KitabFormatUtils.toBanglaDigits(resolvedTotalPages)}-এর মধ্যে যেকোনো পৃষ্ঠা নম্বর লিখুন:",
                        fontSize = 13.sp,
                        color = Color(0xFF2D4F3E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = {
                            pageInput = it
                            pageError = null
                        },
                        label = { Text("পৃষ্ঠা নম্বর") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kitab_reader_page_jump_input")
                    )
                    if (pageError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pageError!!,
                            color = Color(0xFFB71C1C),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = KitabFormatUtils.parseBanglaOrEnglishInt(pageInput)
                        if (parsed == null || parsed !in 1..resolvedTotalPages) {
                            pageError = "অনুগ্রহ করে ১ থেকে ${KitabFormatUtils.toBanglaDigits(resolvedTotalPages)}-এর মধ্যে সঠিক পৃষ্ঠা নম্বর দিন।"
                        } else {
                            currentPage = parsed
                            zoomScale = 1f
                            panOffset = Offset.Zero
                            showPageJumpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReaderDeepGreen),
                    modifier = Modifier.testTag("kitab_reader_page_jump_confirm_button")
                ) {
                    Text("পৃষ্ঠায় যান")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPageJumpDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Bookmark Note Dialog
    if (showBookmarkNoteDialog) {
        var noteText by remember {
            mutableStateOf("${book.titleBangla} • পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(currentPage)}")
        }
        AlertDialog(
            onDismissRequest = { showBookmarkNoteDialog = false },
            modifier = Modifier.testTag("kitab_reader_bookmark_dialog"),
            title = {
                Text(
                    text = "পৃষ্ঠা বুকমার্ক সংরক্ষণ করুন",
                    fontWeight = FontWeight.Bold,
                    color = ReaderDeepGreen
                )
            },
            text = {
                Column {
                    Text(
                        text = "পৃষ্ঠা নং ${KitabFormatUtils.toBanglaDigits(currentPage)}-এর জন্য একটি সংক্ষিপ্ত নোট লিখুন (ঐচ্ছিক):",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("বুকমার্ক নোট") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kitab_reader_bookmark_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        KitabDownloadAndStorageManager.toggleBookmark(
                            context = context,
                            bookId = book.bookId,
                            bookTitleBangla = book.titleBangla,
                            volumeNumber = book.volumeNumber,
                            pageNumber = currentPage,
                            note = noteText
                        )
                        showBookmarkNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReaderDeepGreen),
                    modifier = Modifier.testTag("kitab_reader_bookmark_save_button")
                ) {
                    Text("বুকমার্ক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkNoteDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemePickerDialog) {
        AlertDialog(
            onDismissRequest = { showThemePickerDialog = false },
            title = {
                Text(
                    text = "কিতাব রিডার থিম",
                    fontWeight = FontWeight.Bold,
                    color = ReaderDeepGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KitabReaderThemeMode.entries.forEach { mode ->
                        val selected = mode == themeMode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ReaderDeepGreen.copy(alpha = 0.12f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selected) ReaderDeepGreen else Color.LightGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemePickerDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = mode.titleBangla,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = ReaderDeepGreen
                                )
                                if (selected) {
                                    Text("✓ নির্বাচিত", color = ReaderDeepGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePickerDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // Volume Not Downloaded Prompt
    if (volumeSwitchMessage != null) {
        AlertDialog(
            onDismissRequest = { volumeSwitchMessage = null },
            title = { Text("খণ্ডটি ডাউনলোড প্রয়োজন", fontWeight = FontWeight.Bold, color = ReaderDeepGreen) },
            text = { Text(volumeSwitchMessage!!, textAlign = TextAlign.Start) },
            confirmButton = {
                Button(
                    onClick = { volumeSwitchMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ReaderDeepGreen)
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }
}
