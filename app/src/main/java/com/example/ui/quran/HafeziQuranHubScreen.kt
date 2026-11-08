package com.example.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.IslamicStarEmblem
import kotlinx.coroutines.launch

private val QuranPrimaryGreen = Color(0xFF0F6B4B)
private val QuranDeepEmerald = Color(0xFF0A4D36)
private val QuranDarkCard = Color(0xFF0C593E)
private val QuranGold = Color(0xFFD9A82E)
private val QuranSoftCream = Color(0xFFF3E8C8)
private val QuranLightBg = Color(0xFFF6FAF8)
private val QuranMintBorder = Color(0xFFCFE6DC)

enum class QuranHubTab(val titleBangla: String, val testTag: String) {
    SURAHS("১১৪ সূরা", "quran_hub_tab_surahs"),
    PARAS("৩০ পারা", "quran_hub_tab_paras"),
    BOOKMARKS("বুকমার্ক ও পৃষ্ঠা", "quran_hub_tab_bookmarks"),
    HISTORY("ইতিহাস ও অগ্রগতি", "quran_hub_tab_history")
}

/**
 * Full-Screen Dialog wrapper for the 15-Line Indo-Pak Hafezi Quran Hub.
 */
@Composable
fun HafeziQuranHubDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HafeziQuranHubScreen(onCloseHub = onDismiss)
    }
}

/**
 * Full-Screen Islamic Quran Reader Dashboard & Hub (হাফেজি কোরআন শরীফ - ১৫ লাইন ইন্দো-পাক মুসহাফ).
 *
 * Follows the 8-panel UI design reference screenshot and uses the bundled 616-page 15-Line
 * Indo-Pak Quran PDF (`assets/quran_hafezi_15line.pdf`, SHA-256:
 * `1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828`) as the sole Quran source.
 */
@Composable
fun HafeziQuranHubScreen(
    onCloseHub: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        HafeziQuranStorageManager.init(context)
        HafeziDrivePdfManager.init(context)
    }

    val progress by HafeziQuranStorageManager.progressFlow.collectAsState()
    val bookmarks by HafeziQuranStorageManager.bookmarksFlow.collectAsState()
    val history by HafeziQuranStorageManager.historyFlow.collectAsState()
    val downloadState by HafeziQuranStorageManager.downloadStateFlow.collectAsState()
    val themeMode by HafeziQuranStorageManager.themeModeFlow.collectAsState()
    val showReadingProgress by HafeziQuranStorageManager.showReadingProgressFlow.collectAsState()
    val readerFontScalePx by HafeziQuranStorageManager.readerFontScalePxFlow.collectAsState()

    val isNightMode = themeMode == MushafThemeMode.NIGHT_MODE
    val screenBg = if (isNightMode) Color(0xFF0B1914) else QuranLightBg
    val cardBg = if (isNightMode) Color(0xFF132A22) else Color.White
    val primaryText = if (isNightMode) Color(0xFFF3EFE2) else Color(0xFF12241D)
    val secondaryText = if (isNightMode) Color(0xFFB7CFC4) else Color(0xFF4A6359)

    var activeReaderPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(QuranHubTab.SURAHS.ordinal) }
    val selectedTab = QuranHubTab.entries.getOrElse(selectedTabOrdinal) { QuranHubTab.SURAHS }

    var surahFilterOrdinal by rememberSaveable { mutableIntStateOf(SurahFilterType.ALL.ordinal) }
    val selectedSurahFilter = SurahFilterType.entries.getOrElse(surahFilterOrdinal) { SurahFilterType.ALL }

    var showBookmarksSubTabHistory by rememberSaveable { mutableStateOf(false) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var directPageQuery by rememberSaveable { mutableStateOf("") }
    var showDirectPageModal by rememberSaveable { mutableStateOf(false) }
    var showTilawatDuaModal by rememberSaveable { mutableStateOf(false) }
    var showQuranSettingsModal by rememberSaveable { mutableStateOf(false) }
    var showOfflineReadyModal by rememberSaveable { mutableStateOf(false) }

    val searchResults = remember(searchQuery) {
        HafeziQuranIndexData.searchAll(searchQuery)
    }

    val filteredSurahs = remember(searchResults.matchingSurahs, selectedSurahFilter) {
        when (selectedSurahFilter) {
            SurahFilterType.ALL -> searchResults.matchingSurahs
            SurahFilterType.MAKKI -> searchResults.matchingSurahs.filter { it.isMakki }
            SurahFilterType.MADANI -> searchResults.matchingSurahs.filter { !it.isMakki }
        }
    }

    // If Reader is open, render the full-screen Internal Native PDF Reader
    if (activeReaderPage != null) {
        BackHandler {
            activeReaderPage = null
        }
        HafeziQuranReaderView(
            initialPage = activeReaderPage ?: 2,
            onBackToHub = {
                activeReaderPage = null
            }
        )
        return
    }

    BackHandler {
        onCloseHub()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("hafezi_quran_hub_screen")
    ) {
        // 1. Top Islamic Quran Hub AppBar
        Surface(
            color = QuranDeepEmerald,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onCloseHub,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("quran_hub_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IslamicStarEmblem(size = 15.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "হাফেজি কোরআন শরীফ",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "১৫ লাইন ইন্দো-পাক হাফেজি মুসহাফ • ৬১৬ পৃষ্ঠা",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Direct Page Lookup Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranPrimaryGreen,
                        modifier = Modifier
                            .border(1.dp, QuranGold, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { showDirectPageModal = true }
                            .testTag("quran_hub_direct_page_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindInPage,
                                contentDescription = "পৃষ্ঠা খুঁজুন",
                                tint = QuranGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "পৃষ্ঠা খুঁজুন",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quran Settings Icon Button
                    IconButton(
                        onClick = { showQuranSettingsModal = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("quran_hub_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "কোরআন সেটিংস",
                            tint = QuranGold
                        )
                    }

                    // Night Mode Toggle
                    IconButton(
                        onClick = {
                            val next = if (isNightMode) MushafThemeMode.EMDADIA_CREAM else MushafThemeMode.NIGHT_MODE
                            HafeziQuranStorageManager.setMushafTheme(context, next)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("quran_hub_night_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "নাইট মোড",
                            tint = QuranGold
                        )
                    }
                }
            }
        }

        // 2. Hub Scrollable Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("quran_hub_lazy_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A. Quran Home Dashboard Hero Card (Matching Panel 1 of Design Reference Screenshot)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(22.dp))
                        .border(1.5.dp, QuranGold, RoundedCornerShape(22.dp))
                        .testTag("quran_hub_hero_resume_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = QuranDeepEmerald)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0D5F42),
                                        QuranDeepEmerald,
                                        Color(0xFF073B29)
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Glowing Quran Rehal Emblem + Title + Subtitle
                        Surface(
                            shape = CircleShape,
                            color = QuranGold.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.5.dp, QuranGold, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "কোরআন শরীফ",
                                    tint = Color(0xFFFFE082),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "কোরআন শরীফ",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "পড়ুন, বুঝুন, আমল করুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE5F5EE),
                                fontSize = 13.5.sp
                            )
                        }

                        // Gold CTA Button: "📖 কোরআন পড়া শুরু করুন →"
                        Button(
                            onClick = { activeReaderPage = progress.lastReadPage },
                            colors = ButtonDefaults.buttonColors(containerColor = QuranGold),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_hub_resume_last_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF0B1914),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (progress.uniquePagesReadCount > 0) {
                                    "কোরআন পড়া চালিয়ে যান"
                                } else {
                                    "কোরআন পড়া শুরু করুন"
                                },
                                color = Color(0xFF0B1914),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0B1914),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Last Read Info Card + Reading Progress Box (Matching Panel 1)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = QuranDarkCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QuranGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { activeReaderPage = progress.lastReadPage }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = QuranGold.copy(alpha = 0.18f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = QuranGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "শেষ পড়া পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadPage)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "সূরা ${progress.lastReadSurahNameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadParaNumber)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFDDF3EC)
                                        )
                                    }
                                }

                                if (showReadingProgress) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "পড়ার অগ্রগতি (${HafeziQuranIndexData.toBanglaDigits(progress.uniquePagesReadCount)}/${HafeziQuranIndexData.toBanglaDigits(HafeziQuranIndexData.TOTAL_PDF_PAGES)} পৃষ্ঠা)",
                                                color = Color(0xFFDDF3EC),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${HafeziQuranIndexData.toBanglaDigits(progress.progressPercent)}%",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress.progressFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = Color(0xFF34D399),
                                            trackColor = Color.White.copy(alpha = 0.18f)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Cream Pill Buttons Row matching Panel 1: [🔖 বুকমার্ক] [🎧 তিলাওয়াত ও খতম]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = QuranSoftCream,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        showBookmarksSubTabHistory = false
                                        selectedTabOrdinal = QuranHubTab.BOOKMARKS.ordinal
                                    }
                                    .testTag("quran_hub_bookmarks_quick_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = QuranDeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "বুকমার্ক (${HafeziQuranIndexData.toBanglaDigits(bookmarks.size)})",
                                        color = Color(0xFF0B1914),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = QuranSoftCream,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showTilawatDuaModal = true }
                                    .testTag("quran_hub_tilawat_dua_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = QuranDeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "তিলাওয়াত ও খতম",
                                        color = Color(0xFF0B1914),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B. Offline Native PDF Status & Integrity Card (No Google Drive / No External App)
            item {
                OfflineMushafDownloadCard(
                    downloadState = downloadState,
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onShowOfflineModal = { showOfflineReadyModal = true },
                    onVerifyIntegrity = {
                        scope.launch {
                            HafeziQuranStorageManager.verifyOfflineFilesIntegrity(context)
                            showOfflineReadyModal = true
                        }
                    }
                )
            }

            // B2. ফজিলতপূর্ণ সূরা ও ৭ মঞ্জিল দ্রুত তিলাওয়াত শর্টকাট কার্ড
            item {
                FazilatSurahsAndManzilQuickCard(
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onOpenPage = { targetPage -> activeReaderPage = targetPage }
                )
            }

            // C. Unified Search (Surah, Para, Page, Ayah) & Quick Direct Page Input Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "সূরা, পারা, পৃষ্ঠা বা আয়াত খুঁজুন (যেমন: ইয়াসিন, ২:২৫৫, ৪৪১)...",
                                    fontSize = 12.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = QuranPrimaryGreen
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "মুছুন",
                                            tint = secondaryText
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_hub_unified_search_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = directPageQuery,
                                onValueChange = { directPageQuery = it },
                                placeholder = { Text("পৃষ্ঠা নম্বর লিখুন (১ - ৬১৬)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quran_hub_quick_page_input")
                            )
                            Button(
                                onClick = {
                                    val page = HafeziQuranIndexData.parseBanglaOrEnglishNumber(directPageQuery)
                                    if (page != null && page in 1..HafeziQuranIndexData.TOTAL_PDF_PAGES) {
                                        activeReaderPage = page
                                    } else {
                                        showDirectPageModal = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("quran_hub_quick_page_go_button")
                            ) {
                                Text(
                                    text = "সরাসরি পৃষ্ঠায় যান",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (searchQuery.isNotBlank()) {
                            SearchInstantMatchesSection(
                                searchResults = searchResults,
                                onOpenPage = { page -> activeReaderPage = page }
                            )
                        }
                    }
                }
            }

            // D. 4 Navigation Tabs (১. ১১৪ সূরা | ২. ৩০ পারা | ৩. বুকমার্ক ও পৃষ্ঠা | ৪. ইতিহাস ও অগ্রগতি)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuranHubTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) QuranGold else QuranMintBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTabOrdinal = tab.ordinal }
                                .testTag(tab.testTag),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) QuranPrimaryGreen else cardBg
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.titleBangla,
                                    color = if (isSelected) Color.White else primaryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // E. Tab Content List
            when (selectedTab) {
                QuranHubTab.SURAHS -> {
                    // Filter Chips matching Panel 2 of Screenshot: [সব সূরা] [মাক্কি] [মাদানী]
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SurahFilterType.entries.forEach { filterType ->
                                val selected = filterType == selectedSurahFilter
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) QuranPrimaryGreen else cardBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) QuranGold else QuranMintBorder,
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clip(RoundedCornerShape(50))
                                        .clickable { surahFilterOrdinal = filterType.ordinal }
                                        .testTag(filterType.tag)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = filterType.labelBangla,
                                            color = if (selected) Color.White else primaryText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = filteredSurahs,
                        key = { "surah_${it.number}" }
                    ) { surah ->
                        SurahListItemCard(
                            surah = surah,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { activeReaderPage = surah.startPage }
                        )
                    }
                }

                QuranHubTab.PARAS -> {
                    items(
                        items = searchResults.matchingParas,
                        key = { "para_${it.number}" }
                    ) { para ->
                        ParaListItemCard(
                            para = para,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { activeReaderPage = para.startPage }
                        )
                    }
                }

                QuranHubTab.BOOKMARKS -> {
                    // Sub-toggle matching Panel 5 of Screenshot: [বুকমার্ক] [পড়ার ইতিহাস]
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (!showBookmarksSubTabHistory) QuranPrimaryGreen else cardBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (!showBookmarksSubTabHistory) QuranGold else QuranMintBorder,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showBookmarksSubTabHistory = false }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "বুকমার্ক (${HafeziQuranIndexData.toBanglaDigits(bookmarks.size)})",
                                        color = if (!showBookmarksSubTabHistory) Color.White else primaryText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (showBookmarksSubTabHistory) QuranPrimaryGreen else cardBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (showBookmarksSubTabHistory) QuranGold else QuranMintBorder,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showBookmarksSubTabHistory = true }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "পড়ার ইতিহাস (${HafeziQuranIndexData.toBanglaDigits(history.size)})",
                                        color = if (showBookmarksSubTabHistory) Color.White else primaryText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (showBookmarksSubTabHistory) {
                        if (history.isEmpty()) {
                            item {
                                EmptyStateInfoCard(
                                    title = "পড়ার ইতিহাস এখনো শুরু হয়নি",
                                    subtitle = "আপনি যে পৃষ্ঠাগুলো তিলাওয়াত করবেন তার ক্রমানুসারে তালিকা এখানে সংরক্ষিত থাকবে।",
                                    cardBg = cardBg,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText
                                )
                            }
                        } else {
                            items(
                                items = history,
                                key = { "bm_hist_${it.pageNumber}_${it.visitedAtMillis}" }
                            ) { item ->
                                HistoryItemCard(
                                    entry = item,
                                    cardBg = cardBg,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = { activeReaderPage = item.pageNumber }
                                )
                            }
                        }
                    } else {
                        if (bookmarks.isEmpty()) {
                            item {
                                EmptyBookmarkIllustratedCard(
                                    cardBg = cardBg,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onAddBookmarkCurrentPage = {
                                        HafeziQuranStorageManager.addOrUpdateBookmark(
                                            context,
                                            progress.lastReadPage,
                                            "শেষ পড়া পৃষ্ঠা বুকমার্ক"
                                        )
                                    }
                                )
                            }
                        } else {
                            items(
                                items = bookmarks,
                                key = { "bm_${it.pageNumber}" }
                            ) { bm ->
                                BookmarkItemCard(
                                    bookmark = bm,
                                    cardBg = cardBg,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onOpenPage = { activeReaderPage = bm.pageNumber },
                                    onDelete = {
                                        HafeziQuranStorageManager.removeBookmark(context, bm.pageNumber)
                                    }
                                )
                            }
                        }

                        // Featured Ayahs & Quick Pages Reference
                        item {
                            Text(
                                text = "গুরুত্বপূর্ণ সূরা ও আয়াতের পৃষ্ঠাসমূহ",
                                style = MaterialTheme.typography.titleSmall,
                                color = primaryText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(
                            items = HafeziQuranIndexData.featuredAyahSearchIndex,
                            key = { "feat_${it.surahNumber}_${it.ayahNumber}" }
                        ) { entry ->
                            FeaturedAyahCard(
                                entry = entry,
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { activeReaderPage = entry.pageNumber }
                            )
                        }
                    }
                }

                QuranHubTab.HISTORY -> {
                    item {
                        ReadingProgressSummaryCard(
                            progress = progress,
                            historyCount = history.size,
                            onResumeClick = { activeReaderPage = progress.lastReadPage }
                        )
                    }
                    if (history.isEmpty()) {
                        item {
                            EmptyStateInfoCard(
                                title = "পড়ার ইতিহাস এখনো শুরু হয়নি",
                                subtitle = "আপনি যে পৃষ্ঠাগুলো তিলাওয়াত করবেন তার ক্রমানুসারে তালিকা এখানে সংরক্ষিত থাকবে।",
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }
                    } else {
                        items(
                            items = history,
                            key = { "hist_${it.pageNumber}_${it.visitedAtMillis}" }
                        ) { item ->
                            HistoryItemCard(
                                entry = item,
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { activeReaderPage = item.pageNumber }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDirectPageModal) {
        DirectPageJumpDialog(
            currentPage = progress.lastReadPage,
            onDismiss = { showDirectPageModal = false },
            onJumpToPage = { targetPage ->
                showDirectPageModal = false
                activeReaderPage = targetPage
            }
        )
    }

    if (showTilawatDuaModal) {
        TilawatAndKhatamDuaDialog(
            onDismiss = { showTilawatDuaModal = false },
            onOpenKhatamDuaPage = {
                showTilawatDuaModal = false
                activeReaderPage = 612
            }
        )
    }

    if (showQuranSettingsModal) {
        QuranSettingsModalDialog(
            themeMode = themeMode,
            fontScalePx = readerFontScalePx,
            showProgress = showReadingProgress,
            progress = progress,
            onThemeSelected = { HafeziQuranStorageManager.setMushafTheme(context, it) },
            onFontScaleChanged = { HafeziQuranStorageManager.setReaderFontScalePx(context, it) },
            onToggleShowProgress = { HafeziQuranStorageManager.setShowReadingProgress(context, it) },
            onDismiss = { showQuranSettingsModal = false }
        )
    }

    if (showOfflineReadyModal) {
        OfflineReadyConfirmationDialog(
            onDismiss = { showOfflineReadyModal = false }
        )
    }
}

/**
 * Illustrated Empty Bookmark Card matching Panel 5 of the Design Reference Screenshot.
 */
@Composable
private fun EmptyBookmarkIllustratedCard(
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onAddBookmarkCurrentPage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(18.dp))
            .testTag("quran_empty_bookmarks_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = QuranPrimaryGreen.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(72.dp)
                    .border(1.dp, QuranGold, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = QuranPrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "কোনো বুকমার্ক নেই",
                style = MaterialTheme.typography.titleMedium,
                color = primaryText,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "পছন্দের পৃষ্ঠা বুকমার্ক করুন সহজে ফিরে যেতে",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddBookmarkCurrentPage,
                colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("quran_add_bookmark_empty_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = QuranGold,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "বুকমার্ক যোগ করুন",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Quran Settings Dialog matching Panel 6 of the Design Reference Screenshot.
 */
@Composable
private fun QuranSettingsModalDialog(
    themeMode: MushafThemeMode,
    fontScalePx: Int,
    showProgress: Boolean,
    progress: QuranReadingProgress,
    onThemeSelected: (MushafThemeMode) -> Unit,
    onFontScaleChanged: (Int) -> Unit,
    onToggleShowProgress: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isNightMode = themeMode == MushafThemeMode.NIGHT_MODE
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF6FAF8),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = QuranPrimaryGreen
                )
                Text(
                    text = "কোরআন সেটিংস",
                    style = MaterialTheme.typography.titleMedium,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Reader Theme Selector
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = QuranPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "রিডার থিম",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF12241D),
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = themeMode.titleBangla,
                                color = QuranPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MushafThemeMode.entries.forEach { mode ->
                                val selected = mode == themeMode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) QuranPrimaryGreen else Color(0xFFEAF4EF),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onThemeSelected(mode) }
                                ) {
                                    Text(
                                        text = when (mode) {
                                            MushafThemeMode.EMDADIA_CREAM -> "গাঢ় সবুজ"
                                            MushafThemeMode.CLASSIC_WHITE -> "ক্রিম"
                                            MushafThemeMode.SEPIA_HERITAGE -> "সেপিয়া"
                                            MushafThemeMode.NIGHT_MODE -> "রাত"
                                        },
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center,
                                        color = if (selected) Color.White else Color(0xFF12241D),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Zoom / Font Scale Slider
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Aa  ফন্ট ও পৃষ্ঠা জুম স্কেল",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF12241D),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${HafeziQuranIndexData.toBanglaDigits(fontScalePx)}px",
                                color = QuranPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onFontScaleChanged(fontScalePx - 2) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = QuranPrimaryGreen)
                            }
                            Slider(
                                value = fontScalePx.toFloat(),
                                onValueChange = { onFontScaleChanged(it.toInt()) },
                                valueRange = 12f..28f,
                                colors = SliderDefaults.colors(
                                    thumbColor = QuranPrimaryGreen,
                                    activeTrackColor = QuranPrimaryGreen
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onFontScaleChanged(fontScalePx + 2) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = QuranPrimaryGreen)
                            }
                        }
                    }
                }

                // 3. Toggles: রাতের মোড & পড়ার অগ্রগতি দেখান
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌙  রাতের মোড",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF12241D),
                                fontSize = 13.sp
                            )
                            Switch(
                                checked = isNightMode,
                                onCheckedChange = { checked ->
                                    onThemeSelected(
                                        if (checked) MushafThemeMode.NIGHT_MODE else MushafThemeMode.EMDADIA_CREAM
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = QuranPrimaryGreen)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊  পড়ার অগ্রগতি দেখান",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF12241D),
                                fontSize = 13.sp
                            )
                            Switch(
                                checked = showProgress,
                                onCheckedChange = onToggleShowProgress,
                                colors = SwitchDefaults.colors(checkedTrackColor = QuranPrimaryGreen)
                            )
                        }
                    }
                }

                // 4. Summary Footer matching Panel 6: "আপনার পড়ার তথ্য: মোট পৃষ্ঠা: ৬১৬ (মূল পাঠ ৬১১) | শেষ পৃষ্ঠা: ..."
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEAF4EF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "আপনার পড়ার তথ্য",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4A6359),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "মোট পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(HafeziQuranIndexData.TOTAL_PDF_PAGES)} | শেষ পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadPage)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF12241D),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen)
            ) {
                Text("ঠিক আছে", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Offline Mode Confirmation Dialog matching Panel 8 of the Design Reference Screenshot.
 */
@Composable
private fun OfflineReadyConfirmationDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = QuranDeepEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, QuranGold, RoundedCornerShape(22.dp))
                .testTag("quran_offline_ready_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F6B4B), QuranDeepEmerald)
                        )
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(78.dp)
                        .border(2.5.dp, Color(0xFF34D399), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Text(
                    text = "কোরআন শরীফ\nসফলভাবে ডাউনলোড হয়েছে",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "এখন আপনি ইন্টারনেট ছাড়াই পড়তে পারবেন।\nকোনো গুগল অ্যাকাউন্ট বা বাহ্যিক অ্যাপের প্রয়োজন নেই।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFDDF3EC),
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "মোট পৃষ্ঠা: ৬১৬ (মূল কোরআন পাঠ: ২ - ৬১১)",
                            color = Color(0xFFFFE082),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SHA-256: ${HafeziQuranIndexData.ORIGINAL_PDF_SHA256.take(16)}...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranPrimaryGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, QuranGold, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .testTag("quran_offline_modal_ok_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ঠিক আছে",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private data class FazilatQuickShortcut(
    val titleBangla: String,
    val subtitleBangla: String,
    val pageNumber: Int,
    val tag: String
)

private data class ManzilQuickShortcut(
    val manzilNumber: Int,
    val titleBangla: String,
    val rangeBangla: String,
    val startPage: Int
)

@Composable
private fun FazilatSurahsAndManzilQuickCard(
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onOpenPage: (Int) -> Unit
) {
    val fazilatShortcuts = remember {
        listOf(
            FazilatQuickShortcut("সূরা ইয়াসিন", "কুরআনের হৃদয় • পৃষ্ঠা ৪৪১", 441, "quran_quick_yasin"),
            FazilatQuickShortcut("সূরা আল-মুলক", "রাতের আমল • পৃষ্ঠা ৫৬৩", 563, "quran_quick_mulk"),
            FazilatQuickShortcut("সূরা আর-রহমান", "কুরআনের সৌন্দর্য • পৃষ্ঠা ৫৩২", 532, "quran_quick_rahman"),
            FazilatQuickShortcut("সূরা ওয়াকিয়াহ", "রিযিকের প্রাচুর্য • পৃষ্ঠা ৫৩৫", 535, "quran_quick_waqiah"),
            FazilatQuickShortcut("সূরা আল-কাহফ", "জুমুআর আমল • পৃষ্ঠা ২৯৪", 294, "quran_quick_kahf"),
            FazilatQuickShortcut("আয়াতুল কুরসি", "সার্বক্ষণিক হিফাজত • পৃষ্ঠা ৪৩", 43, "quran_quick_ayatul_kursi"),
            FazilatQuickShortcut("সূরা হাশর (শেষ ৩ আয়াত)", "সকাল-সন্ধ্যার আমল • পৃষ্ঠা ৫৪৯", 549, "quran_quick_hashr")
        )
    }

    val manzilShortcuts = remember {
        listOf(
            ManzilQuickShortcut(1, "১ম মঞ্জিল", "সূরা ফাতিহা – নিসা", 2),
            ManzilQuickShortcut(2, "২য় মঞ্জিল", "সূরা মায়িদাহ – তাওবাহ", 107),
            ManzilQuickShortcut(3, "৩য় মঞ্জিল", "সূরা ইউনুস – নাহল", 209),
            ManzilQuickShortcut(4, "৪র্থ মঞ্জিল", "সূরা বনী ইসরাঈল – ফুরকান", 283),
            ManzilQuickShortcut(5, "৫ম মঞ্জিল", "সূরা শুআরা – ইয়াসিন", 367),
            ManzilQuickShortcut(6, "৬ষ্ঠ মঞ্জিল", "সূরা সাফফাত – হুজুরাত", 446),
            ManzilQuickShortcut(7, "৭ম মঞ্জিল", "সূরা ক্বাফ – নাস", 519)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .testTag("quran_hub_fazilat_and_manzil_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ফজিলতপূর্ণ সূরা ও প্রতিদিনের আমল",
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryText,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "এক ট্যাপে ওপেন",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = QuranPrimaryGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fazilatShortcuts.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuranPrimaryGreen.copy(alpha = 0.09f),
                        modifier = Modifier
                            .border(1.dp, QuranMintBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenPage(item.pageNumber) }
                            .testTag(item.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = item.titleBangla,
                                style = MaterialTheme.typography.labelLarge,
                                color = QuranPrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.subtitleBangla,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "সাপ্তাহিক খতমে কুরআন (৭ মঞ্জিল শর্টকাট)",
                style = MaterialTheme.typography.labelMedium,
                color = secondaryText,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                manzilShortcuts.forEach { manzil ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = QuranGold.copy(alpha = 0.14f),
                        modifier = Modifier
                            .border(1.dp, QuranGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenPage(manzil.startPage) }
                            .testTag("quran_manzil_chip_${manzil.manzilNumber}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${manzil.titleBangla} • পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(manzil.startPage)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = manzil.rangeBangla,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TilawatAndKhatamDuaDialog(
    onDismiss: () -> Unit,
    onOpenKhatamDuaPage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "তিলাওয়াত ও খতমে কুরআনের মাসনুন দোয়া",
                style = MaterialTheme.typography.titleMedium,
                color = QuranPrimaryGreen,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF6FAF8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "১. তিলাওয়াত শুরুর পূর্বে:",
                            style = MaterialTheme.typography.labelLarge,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ ۝ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF12241D),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "অর্থ: বিতাড়িত শয়তান থেকে আল্লাহর কাছে আশ্রয় চাচ্ছি। পরম করুণাময় অসীম দয়ালু আল্লাহর নামে শুরু করছি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A6359)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF8E1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranGold, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "২. কুরআন তিলাওয়াত ও খতমের দোয়া:",
                            style = MaterialTheme.typography.labelLarge,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اللَّهُمَّ ارْحَمْنِي بِالْقُرْآنِ وَاجْعَلْهُ لِي إِمَامًا وَنُورًا وَهُدًى وَرَحْمَةً",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF12241D),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "অর্থ: হে আল্লাহ! কুরআনের বরকতে আমার ওপর রহম করুন এবং কুরআনকে আমার জন্য পথপ্রদর্শক, নূর, হিদায়াত ও রহমত বানিয়ে দিন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A6359)
                        )
                    }
                }

                Button(
                    onClick = onOpenKhatamDuaPage,
                    colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "মুসহাফে খতমে কুরআনের দোয়া পৃষ্ঠা (৬১২) খুলুন",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("আলহামদুলিল্লাহ, বন্ধ করুন", color = QuranPrimaryGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun OfflineMushafDownloadCard(
    downloadState: QuranDownloadState,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onShowOfflineModal: () -> Unit,
    onVerifyIntegrity: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(16.dp))
            .testTag("quran_hub_offline_download_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = QuranPrimaryGreen.copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = QuranPrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "১৫-লাইন ইন্দো-পাক কোরআন শরীফ (অফলাইন প্রস্তুত)",
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = downloadState.statusMessageBangla,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মোট পৃষ্ঠা: ৬১৬ (মূল কোরআন ২-৬১১) • ১০০% অফলাইন",
                    style = MaterialTheme.typography.labelSmall,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranPrimaryGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onShowOfflineModal)
                            .testTag("quran_hub_offline_status_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "অফলাইন মোড",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranGold.copy(alpha = 0.2f),
                        modifier = Modifier
                            .border(1.dp, QuranGold, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onVerifyIntegrity)
                            .testTag("quran_offline_verify_integrity_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = QuranPrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHA-256 যাচাই",
                                color = QuranPrimaryGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInstantMatchesSection(
    searchResults: HafeziQuranIndexData.QuranUnifiedSearchResults,
    onOpenPage: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (searchResults.directPageMatch != null) {
            val page = searchResults.directPageMatch
            val meta = HafeziQuranIndexData.getPageMetadata(page)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEAF4EF),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuranPrimaryGreen, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenPage(page) }
                    .testTag("quran_search_direct_page_result_$page")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "সরাসরি পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(page)}-এ যান",
                            style = MaterialTheme.typography.labelLarge,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সূরা ${meta.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(meta.paraNumber)} (${meta.paraNameBangla})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF141A17)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = QuranPrimaryGreen
                    )
                }
            }
        }

        searchResults.matchingAyahs.take(3).forEach { ayah ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFF8E1),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuranGold, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenPage(ayah.pageNumber) }
                    .testTag("quran_search_ayah_result_${ayah.surahNumber}_${ayah.ayahNumber}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ayah.banglaTopicOrTranslation} (${HafeziQuranIndexData.toBanglaDigits(ayah.referenceKey)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(ayah.pageNumber)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = ayah.arabicText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF141A17),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Surah List Card matching Panel 2 of the Design Reference Screenshot & Requirement 3:
 * - Bangla Surah name
 * - Arabic Surah name
 * - Surah number
 * - Ayah count
 * - Makki or Madani
 * - Verified PDF page number
 */
@Composable
private fun SurahListItemCard(
    surah: HafeziSurahInfo,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("quran_surah_item_${surah.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = QuranPrimaryGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = HafeziQuranIndexData.toBanglaDigits(surah.number),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "সূরা ${surah.nameBangla}",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "(${surah.revelationTypeBangla} • ${HafeziQuranIndexData.toBanglaDigits(surah.ayahCount)} আয়াত) • পারা ${HafeziQuranIndexData.toBanglaDigits(surah.startPara)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                        fontSize = 11.5.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = surah.nameArabic,
                        style = MaterialTheme.typography.titleMedium,
                        color = QuranPrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranSoftCream
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(surah.startPage)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = QuranDeepEmerald,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = QuranPrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Para List Card matching Panel 3 of the Design Reference Screenshot & Requirement 4:
 * - Bangla Para name (`প্রথম পারা • আলিফ-লাম-মীম`)
 * - Arabic Para name (`آلم`)
 * - Para number (`১`..`৩০`)
 * - Starting Surah & Ayah (`সূরা আল-ফাতিহা - আল-বাকারা • ১:১`)
 * - Verified PDF page number in a gold badge (`পৃষ্ঠা ২`, `পৃষ্ঠা ২৩`, ...)
 */
@Composable
private fun ParaListItemCard(
    para: HafeziParaInfo,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("quran_para_item_${para.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = QuranPrimaryGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = HafeziQuranIndexData.toBanglaDigits(para.number),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${para.displayOrdinalBangla} (${para.nameBangla})",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "${para.displaySurahRangeBangla} • আরম্ভ: ${para.startAyahLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                        fontSize = 11.5.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = para.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranSoftCream,
                    modifier = Modifier.border(0.8.dp, QuranGold, RoundedCornerShape(50))
                ) {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(para.startPage)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        color = Color(0xFF0B1914),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: QuranBookmark,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onOpenPage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranGold, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenPage)
            .testTag("quran_bookmark_card_${bookmark.pageNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = QuranGold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(bookmark.pageNumber)} • সূরা ${bookmark.surahNameBangla}",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পারা ${HafeziQuranIndexData.toBanglaDigits(bookmark.paraNumber)} • ${bookmark.note}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("quran_bookmark_delete_${bookmark.pageNumber}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "মুছে ফেলুন",
                    tint = Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
private fun FeaturedAyahCard(
    entry: HafeziAyahSearchEntry,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.banglaTopicOrTranslation,
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(entry.pageNumber)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = QuranPrimaryGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.arabicText,
                style = MaterialTheme.typography.bodyMedium,
                color = QuranPrimaryGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "সূরা ${entry.surahNameBangla} (${HafeziQuranIndexData.toBanglaDigits(entry.referenceKey)}) • পারা ${HafeziQuranIndexData.toBanglaDigits(entry.paraNumber)}",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText
            )
        }
    }
}

/**
 * Reading Progress Card matching Panel 7 of the Design Reference Screenshot:
 * - Circular Gold/Emerald Progress Ring with Book Icon & `%`
 * - `মোট পৃষ্ঠা: ৬১৬ | পড়া হয়েছে: ...`
 * - Gold-bordered `শেষ পড়া` box
 * - Gold CTA button `পড়া চালিয়ে যান →`
 */
@Composable
private fun ReadingProgressSummaryCard(
    progress: QuranReadingProgress,
    historyCount: Int,
    onResumeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, QuranGold, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuranDeepEmerald)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F6B4B), QuranDeepEmerald)
                    )
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular Progress Ring matching Panel 7
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 10.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = strokeW)
                    )
                    drawArc(
                        color = QuranGold,
                        startAngle = -90f,
                        sweepAngle = (progress.progressFraction * 360f).coerceIn(8f, 360f),
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = QuranGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "${HafeziQuranIndexData.toBanglaDigits(progress.progressPercent)}%",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "মোট পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(HafeziQuranIndexData.TOTAL_PDF_PAGES)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "পড়া হয়েছে: ${HafeziQuranIndexData.toBanglaDigits(progress.uniquePagesReadCount)} • খতম: ${HafeziQuranIndexData.toBanglaDigits(progress.completedKhatamCount)} বার",
                    color = Color(0xFFDDF3EC),
                    fontSize = 13.sp
                )
            }

            // Gold-bordered "শেষ পড়া" Box matching Panel 7
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = QuranDarkCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuranGold, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "শেষ পড়া",
                        color = QuranGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "সূরা ${progress.lastReadSurahNameBangla}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadPage)} • পারা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadParaNumber)} • ইতিহাস: ${HafeziQuranIndexData.toBanglaDigits(historyCount)}টি",
                        color = Color(0xFFDDF3EC),
                        fontSize = 12.sp
                    )
                }
            }

            // Gold CTA Button: "পড়া চালিয়ে যান →"
            Button(
                onClick = onResumeClick,
                colors = ButtonDefaults.buttonColors(containerColor = QuranGold),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "পড়া চালিয়ে যান",
                    color = Color(0xFF0B1914),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF0B1914),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: QuranReadingHistoryEntry,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quran_history_card_${entry.pageNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = QuranPrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(entry.pageNumber)} • সূরা ${entry.surahNameBangla}",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পারা ${HafeziQuranIndexData.toBanglaDigits(entry.paraNumber)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = QuranPrimaryGreen
            )
        }
    }
}

@Composable
private fun EmptyStateInfoCard(
    title: String,
    subtitle: String,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = primaryText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
