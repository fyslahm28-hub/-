package com.example.ui.duas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.quranlearning.QuranLearningAudioController
import com.example.ui.theme.HindSiliguriFontFamily

/**
 * প্রিমিয়াম ইসলামিক দোয়া ও যিকির বিভাগের প্রধান এন্ট্রি স্ক্রিন।
 *
 * কাঠামো:
 * - হোম ভিউ: মাঝারি হেডার + ৩টি ট্যাব (সকল দোয়া, দৈনিক আমল, প্রিয় দোয়া) + সার্চ ও ফিল্টার + ১২টি ক্যাটাগরি কার্ড।
 * - ক্যাটাগরি লিস্ট ভিউ: নির্বাচিত ক্যাটাগরির সকল যাচাইকৃত দোয়া।
 * - বিস্তারিত ভিউ (DuaDetailScreen): A → G ক্রমে পূর্ণ পাঠ, অডিও, রেফারেন্স ও অনুশীলন সুবিধা।
 */
@Composable
fun DuaSystemScreen(
    darkThemeOverride: Boolean? = null,
    registerBackInterceptor: ((() -> Boolean) -> Unit)? = null,
    audioControllerOverride: QuranLearningAudioController? = null
) {
    val context = LocalContext.current
    val isDark = darkThemeOverride ?: isSystemInDarkTheme()
    val palette = rememberDuaPalette(darkTheme = isDark)
    val storage = remember(context) { DuaStorageManager.getInstance(context) }

    var destination by remember { mutableStateOf<DuaDestination>(DuaDestination.Home) }
    var selectedTab by remember { mutableStateOf(DuaMainTab.ALL_DUAS) }
    var searchQuery by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf(DuaSourceFilter.ALL) }
    var showFilterRow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        storage.refreshDailyDateIfChanged()
    }

    fun navigateBackInsideDua(): Boolean {
        return when (val current = destination) {
            is DuaDestination.Detail -> {
                destination = if (current.fromCategory != null) {
                    DuaDestination.CategoryList(current.fromCategory)
                } else {
                    DuaDestination.Home
                }
                true
            }
            is DuaDestination.CategoryList -> {
                destination = DuaDestination.Home
                true
            }
            DuaDestination.Home -> false
        }
    }

    BackHandler(enabled = destination != DuaDestination.Home) {
        navigateBackInsideDua()
    }

    DisposableEffect(registerBackInterceptor) {
        registerBackInterceptor?.invoke { navigateBackInsideDua() }
        onDispose {
            registerBackInterceptor?.invoke { false }
        }
    }

    when (val current = destination) {
        DuaDestination.Home -> {
            DuaHomeContent(
                palette = palette,
                storage = storage,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                sourceFilter = sourceFilter,
                onSourceFilterChange = { sourceFilter = it },
                showFilterRow = showFilterRow,
                onToggleFilterRow = { showFilterRow = !showFilterRow },
                onOpenCategory = { category ->
                    destination = DuaDestination.CategoryList(category)
                },
                onOpenDua = { dua ->
                    destination = DuaDestination.Detail(duaId = dua.id, fromCategory = null)
                }
            )
        }

        is DuaDestination.CategoryList -> {
            DuaCategoryListContent(
                category = current.category,
                palette = palette,
                storage = storage,
                onBack = { destination = DuaDestination.Home },
                onOpenDua = { dua ->
                    destination = DuaDestination.Detail(
                        duaId = dua.id,
                        fromCategory = current.category
                    )
                }
            )
        }

        is DuaDestination.Detail -> {
            val dua = remember(current.duaId) {
                DuaCatalog.getDuaById(current.duaId) ?: DuaCatalog.allDuas.first()
            }
            val categoryDuas = remember(dua.category) {
                DuaCatalog.getDuasForCategory(dua.category)
            }
            DuaDetailScreen(
                dua = dua,
                categoryDuas = categoryDuas,
                palette = palette,
                storage = storage,
                onBack = { navigateBackInsideDua() },
                onSelectDua = { nextDua ->
                    destination = DuaDestination.Detail(
                        duaId = nextDua.id,
                        fromCategory = current.fromCategory
                    )
                },
                audioControllerOverride = audioControllerOverride
            )
        }
    }
}

@Composable
private fun DuaHomeContent(
    palette: DuaPalette,
    storage: DuaStorageManager,
    selectedTab: DuaMainTab,
    onSelectTab: (DuaMainTab) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sourceFilter: DuaSourceFilter,
    onSourceFilterChange: (DuaSourceFilter) -> Unit,
    showFilterRow: Boolean,
    onToggleFilterRow: () -> Unit,
    onOpenCategory: (DuaCategory) -> Unit,
    onOpenDua: (VerifiedDua) -> Unit
) {
    val favoriteIds by storage.favoriteDuaIds.collectAsState()
    val learnedIds by storage.learnedDuaIds.collectAsState()
    val dailyCompletedIds by storage.dailyCompletedIds.collectAsState()

    val isSearchingOrFiltering = searchQuery.isNotBlank() || sourceFilter != DuaSourceFilter.ALL
    val searchResults = remember(searchQuery, sourceFilter) {
        if (isSearchingOrFiltering) {
            DuaCatalog.searchDuas(searchQuery, sourceFilter)
        } else {
            emptyList()
        }
    }
    val favoriteDuas = remember(favoriteIds) {
        DuaCatalog.allDuas.filter { favoriteIds.contains(it.id) }
    }
    val dailyAmalDuas = remember { DuaCatalog.getDailyAmalDuas() }
    val duaOfTheDay = remember { DuaCatalog.getDuaOfTheDay() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("dua_home_screen")
    ) {
        val gridColumns = if (maxWidth >= 560.dp) 3 else 2
        val categoryRows = remember(gridColumns) {
            DuaCatalog.allCategories.chunked(gridColumns)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dua_home_lazy_column"),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // ১. মাঝারি প্রিমিয়াম ইসলামিক হেডার
            item(key = "dua_header") {
                DuaSectionHeader(totalDuasCount = DuaCatalog.allDuas.size)
            }

            // ২. ৩টি প্রধান ট্যাব (সকল দোয়া, দৈনিক আমল, প্রিয় দোয়া)
            item(key = "dua_tabs") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DuaMainTabPill(
                            label = DuaMainTab.ALL_DUAS.labelBangla,
                            icon = Icons.Default.Star,
                            selected = selectedTab == DuaMainTab.ALL_DUAS,
                            palette = palette,
                            testTag = "dua_tab_all",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(DuaMainTab.ALL_DUAS) }
                        )
                        DuaMainTabPill(
                            label = DuaMainTab.DAILY_AMAL.labelBangla,
                            icon = Icons.Default.CalendarMonth,
                            selected = selectedTab == DuaMainTab.DAILY_AMAL,
                            palette = palette,
                            testTag = "dua_tab_daily_amal",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(DuaMainTab.DAILY_AMAL) }
                        )
                        DuaMainTabPill(
                            label = DuaMainTab.FAVORITES.labelBangla,
                            icon = Icons.Default.Favorite,
                            selected = selectedTab == DuaMainTab.FAVORITES,
                            palette = palette,
                            testTag = "dua_tab_favorites",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(DuaMainTab.FAVORITES) }
                        )
                    }
                }
            }

            // ট্যাব অনুযায়ী কনটেন্ট
            when (selectedTab) {
                DuaMainTab.ALL_DUAS -> {
                    // সার্চ বক্স + ফিল্টার বাটন
                    item(key = "dua_search_bar") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 50.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        color = palette.cardSurface,
                                        border = BorderStroke(1.dp, palette.cardBorder),
                                        shadowElevation = if (palette.isDark) 0.dp else 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "দোয়া খুঁজুন",
                                                tint = palette.accentGreen,
                                                modifier = Modifier.size(21.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "দোয়া খুঁজুন...",
                                                        fontFamily = HindSiliguriFontFamily,
                                                        fontSize = 14.5.sp,
                                                        color = palette.mutedColor
                                                    )
                                                }
                                                BasicTextField(
                                                    value = searchQuery,
                                                    onValueChange = onSearchQueryChange,
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        fontFamily = HindSiliguriFontFamily,
                                                        fontSize = 14.5.sp,
                                                        color = palette.titleColor
                                                    ),
                                                    cursorBrush = SolidColor(palette.accentGreen),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("dua_search_input")
                                                )
                                            }
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { onSearchQueryChange("") },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .testTag("dua_search_clear_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "সার্চ মুছুন",
                                                        tint = palette.mutedColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // সোর্স ফিল্টার টগল বাটন
                                    Surface(
                                        onClick = onToggleFilterRow,
                                        shape = CircleShape,
                                        color = if (showFilterRow || sourceFilter != DuaSourceFilter.ALL) {
                                            DuaPrimaryGreen
                                        } else {
                                            palette.cardSurfaceSoft
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (showFilterRow || sourceFilter != DuaSourceFilter.ALL) {
                                                DuaPrimaryGreen
                                            } else {
                                                palette.cardBorder
                                            }
                                        ),
                                        modifier = Modifier
                                            .size(50.dp)
                                            .testTag("dua_filter_toggle_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "উৎস অনুযায়ী ফিল্টার করুন",
                                                tint = if (showFilterRow || sourceFilter != DuaSourceFilter.ALL) {
                                                    Color.White
                                                } else {
                                                    palette.accentGreen
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = showFilterRow || sourceFilter != DuaSourceFilter.ALL) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DuaSourceFilter.entries.forEach { filter ->
                                            val active = sourceFilter == filter
                                            val tag = when (filter) {
                                                DuaSourceFilter.ALL -> "dua_filter_all"
                                                DuaSourceFilter.QURAN -> "dua_filter_quran"
                                                DuaSourceFilter.HADITH -> "dua_filter_hadith"
                                            }
                                            Surface(
                                                onClick = { onSourceFilterChange(filter) },
                                                shape = RoundedCornerShape(50),
                                                color = if (active) DuaPrimaryGreen else palette.cardSurface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (active) DuaPrimaryGreen else palette.cardBorder
                                                ),
                                                modifier = Modifier
                                                    .heightIn(min = 42.dp)
                                                    .testTag(tag)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = filter.labelBangla,
                                                        fontFamily = HindSiliguriFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        color = if (active) Color.White else palette.titleColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "dua_spacer_after_search") {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (isSearchingOrFiltering) {
                        // সার্চ ফলাফল তালিকা
                        item(key = "dua_search_results_header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "অনুসন্ধানের ফলাফল (${toDuaBanglaDigits(searchResults.size)}টি)",
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = palette.titleColor,
                                        modifier = Modifier.testTag("dua_search_results_list")
                                    )
                                    TextButton(
                                        onClick = {
                                            onSearchQueryChange("")
                                            onSourceFilterChange(DuaSourceFilter.ALL)
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            text = "সব ক্যাটাগরি",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = palette.accentGreen
                                        )
                                    }
                                }
                            }
                        }

                        if (searchResults.isEmpty()) {
                            item(key = "dua_search_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 680.dp)
                                            .testTag("dua_search_empty"),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                                        border = BorderStroke(1.dp, palette.cardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = palette.mutedColor,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "এই শব্দে কোনো যাচাইকৃত দোয়া পাওয়া যায়নি।",
                                                fontFamily = HindSiliguriFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = palette.titleColor,
                                                textAlign = TextAlign.Center
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    onSearchQueryChange("")
                                                    onSourceFilterChange(DuaSourceFilter.ALL)
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text(
                                                    text = "সার্চ রিসেট করুন",
                                                    fontFamily = HindSiliguriFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(
                                items = searchResults,
                                key = { "search_${it.id}" }
                            ) { dua ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DuaSummaryRowCard(
                                        dua = dua,
                                        palette = palette,
                                        isFavorite = favoriteIds.contains(dua.id),
                                        isLearned = learnedIds.contains(dua.id),
                                        onClick = { onOpenDua(dua) },
                                        onToggleFavorite = { storage.toggleFavorite(dua.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 680.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // ১২টি ক্যাটাগরি গ্রিড
                        items(
                            items = categoryRows,
                            key = { row -> row.joinToString("_") { it.id } }
                        ) { rowCategories ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 760.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowCategories.forEach { category ->
                                        val count = DuaCatalog.getDuasForCategory(category).size
                                        DuaCategoryGridCard(
                                            category = category,
                                            duaCount = count,
                                            palette = palette,
                                            onClick = { onOpenCategory(category) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(gridColumns - rowCategories.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // নিচে সূক্ষ্ম ইসলামিক সিলুয়েট সমাপ্তি ব্যান্ড
                        item(key = "dua_footer_silhouette") {
                            DuaFooterMosqueAccent(palette = palette)
                        }
                    }
                }

                DuaMainTab.DAILY_AMAL -> {
                    val totalDailyCount = dailyAmalDuas.size
                    val completedDailyCount = dailyAmalDuas.count { dailyCompletedIds.contains(it.id) }
                    val progressFraction = if (totalDailyCount > 0) {
                        completedDailyCount.toFloat() / totalDailyCount.toFloat()
                    } else {
                        0f
                    }

                    // ব্যক্তিগত দৈনিক অগ্রগতি কার্ড (স্বয়ংক্রিয়ভাবে সম্পন্ন দেখায় না)
                    item(key = "daily_progress") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp)
                                    .testTag("dua_daily_progress_card"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                                border = BorderStroke(1.dp, palette.goldBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "আজকের ব্যক্তিগত আমল",
                                                fontFamily = HindSiliguriFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = palette.titleColor
                                            )
                                            Text(
                                                text = "${toDuaBanglaDigits(completedDailyCount)} / ${toDuaBanglaDigits(totalDailyCount)} টি আমল সম্পন্ন",
                                                fontFamily = HindSiliguriFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp,
                                                color = palette.accentGreen,
                                                modifier = Modifier.testTag("dua_daily_progress_text")
                                            )
                                        }
                                        val percent = (progressFraction * 100).toInt()
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = DuaGold.copy(alpha = if (palette.isDark) 0.22f else 0.16f)
                                        ) {
                                            Text(
                                                text = "${toDuaBanglaDigits(percent)}%",
                                                fontFamily = HindSiliguriFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = palette.accentGold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = DuaPrimaryGreen,
                                        trackColor = palette.cardSurfaceSoft,
                                        strokeCap = StrokeCap.Round
                                    )

                                    Text(
                                        text = "প্রতিটি আমল পড়া শেষে নিজে \"সম্পন্ন করেছি\" বাটনে ট্যাপ করে আপনার অগ্রগতি সংরক্ষণ করুন।",
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = palette.mutedColor
                                    )
                                }
                            }
                        }
                    }

                    // আজকের দোয়া স্পটলাইট কার্ড
                    item(key = "dua_of_the_day") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(DuaDeepEmerald, DuaPrimaryGreen)
                                        )
                                    )
                                    .border(1.2.dp, DuaGold.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                                    .clickable { onOpenDua(duaOfTheDay) }
                                    .padding(16.dp)
                                    .testTag("dua_of_the_day_card")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = DuaGoldSheen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "আজকের দোয়া",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = DuaGoldSheen
                                        )
                                    }

                                    Text(
                                        text = duaOfTheDay.titleBangla,
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = duaOfTheDay.arabicText,
                                            fontFamily = DuaArabicFontFamily,
                                            fontSize = 20.sp,
                                            lineHeight = 34.sp,
                                            color = Color(0xFFFFF4CE),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Text(
                                        text = duaOfTheDay.banglaMeaning,
                                        fontFamily = HindSiliguriFontFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = Color(0xFFDDF3EA),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // সময়ভিত্তিক দৈনিক আমলের তালিকা (সকাল, সন্ধ্যা, ফরজ সালাতের পর, ঘুমানোর আগে, নির্দিষ্ট সময়)
                    DailyAmalSlot.entries.forEach { slot ->
                        val slotDuas = DuaCatalog.getDailyAmalDuasBySlot(slot)
                        if (slotDuas.isNotEmpty()) {
                            item(key = "slot_header_${slot.name}") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 680.dp)
                                    ) {
                                        Text(
                                            text = slot.titleBangla,
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = palette.titleColor
                                        )
                                        Text(
                                            text = slot.subtitleBangla,
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.5.sp,
                                            color = palette.mutedColor
                                        )
                                    }
                                }
                            }

                            items(
                                items = slotDuas,
                                key = { "daily_${slot.name}_${it.id}" }
                            ) { dua ->
                                val completed = dailyCompletedIds.contains(dua.id)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DailyAmalItemCard(
                                        dua = dua,
                                        isCompleted = completed,
                                        palette = palette,
                                        onOpenDetail = { onOpenDua(dua) },
                                        onToggleCompleted = { storage.toggleDailyAmalCompleted(dua.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 680.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                DuaMainTab.FAVORITES -> {
                    if (favoriteDuas.isEmpty()) {
                        item(key = "favorites_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp)
                                        .testTag("dua_favorites_empty"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                                    border = BorderStroke(1.dp, palette.cardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(palette.cardSurfaceSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = palette.accentGreen,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = "এখনো কোনো প্রিয় দোয়া সংরক্ষিত হয়নি",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = palette.titleColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "যেকোনো দোয়ার হৃদয় বা বুকমার্ক আইকনে ট্যাপ করলে সেটি অফলাইনে সহজে পড়ার জন্য এখানে জমা থাকবে।",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            color = palette.subtitleColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { onSelectTab(DuaMainTab.ALL_DUAS) },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = DuaPrimaryGreen,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = "সকল দোয়া দেখুন",
                                                fontFamily = HindSiliguriFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "favorites_header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "সংরক্ষিত প্রিয় দোয়া (${toDuaBanglaDigits(favoriteDuas.size)}টি)",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = palette.titleColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp)
                                        .testTag("dua_favorites_list")
                                )
                            }
                        }

                        items(
                            items = favoriteDuas,
                            key = { "fav_${it.id}" }
                        ) { dua ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DuaSummaryRowCard(
                                    dua = dua,
                                    palette = palette,
                                    isFavorite = true,
                                    isLearned = learnedIds.contains(dua.id),
                                    onClick = { onOpenDua(dua) },
                                    onToggleFavorite = { storage.toggleFavorite(dua.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaCategoryListContent(
    category: DuaCategory,
    palette: DuaPalette,
    storage: DuaStorageManager,
    onBack: () -> Unit,
    onOpenDua: (VerifiedDua) -> Unit
) {
    val duas = remember(category) { DuaCatalog.getDuasForCategory(category) }
    val favoriteIds by storage.favoriteDuaIds.collectAsState()
    val learnedIds by storage.learnedDuaIds.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("dua_category_list_${category.id}"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "category_banner_${category.id}") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dua_category_back_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "সকল ক্যাটাগরিতে ফিরুন",
                        tint = palette.accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সকল ক্যাটাগরি",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = palette.accentGreen
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                    border = BorderStroke(1.dp, palette.goldBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DuaCategoryIconBadge(category = category, isDark = palette.isDark)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = category.titleBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = palette.titleColor,
                                modifier = Modifier.testTag("dua_category_list_title")
                            )
                            Text(
                                text = category.shortDescriptionBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = palette.subtitleColor
                            )
                            DuaMetadataPill(
                                text = "${toDuaBanglaDigits(duas.size)}টি যাচাইকৃত দোয়া",
                                backgroundColor = palette.chipBackground,
                                textColor = palette.accentGreen
                            )
                        }
                    }
                }
            }
        }

        items(
            items = duas,
            key = { it.id }
        ) { dua ->
            DuaSummaryRowCard(
                dua = dua,
                palette = palette,
                isFavorite = favoriteIds.contains(dua.id),
                isLearned = learnedIds.contains(dua.id),
                onClick = { onOpenDua(dua) },
                onToggleFavorite = { storage.toggleFavorite(dua.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            )
        }

        item(key = "cat_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DuaMainTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: DuaPalette,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) DuaPrimaryGreen else palette.tabInactiveBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) DuaPrimaryGreen else palette.cardBorder
        ),
        shadowElevation = if (selected && !palette.isDark) 3.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else palette.accentGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = if (selected) Color.White else palette.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DuaCategoryGridCard(
    category: DuaCategory,
    duaCount: Int,
    palette: DuaPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 148.dp)
            .testTag("dua_category_card_${category.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = BorderStroke(1.dp, palette.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DuaCategoryIconBadge(category = category, isDark = palette.isDark)

                Text(
                    text = category.titleBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    color = palette.titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = category.shortDescriptionBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                    color = palette.mutedColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.chipBackground
                ) {
                    Text(
                        text = "${toDuaBanglaDigits(duaCount)}টি দোয়া",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = palette.accentGreen,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.cardSurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "${category.titleBangla} খুলুন",
                        tint = palette.accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaSummaryRowCard(
    dua: VerifiedDua,
    palette: DuaPalette,
    isFavorite: Boolean,
    isLearned: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("dua_list_item_${dua.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = BorderStroke(1.dp, palette.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dua.titleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        lineHeight = 21.sp,
                        color = palette.titleColor
                    )
                    Text(
                        text = dua.occasionBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = palette.mutedColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dua_item_fav_${dua.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "প্রিয় থেকে সরান" else "প্রিয়তে সংরক্ষণ করুন",
                        tint = if (isFavorite) Color(0xFFD32F2F) else palette.mutedColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cardSurfaceSoft
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = dua.arabicText,
                        fontFamily = DuaArabicFontFamily,
                        fontSize = 19.sp,
                        lineHeight = 32.sp,
                        color = palette.titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DuaMetadataPill(
                        text = dua.sourceType.labelBangla,
                        backgroundColor = palette.chipBackground,
                        textColor = palette.accentGreen
                    )
                    if (isLearned) {
                        DuaMetadataPill(
                            text = "শিখেছি ✓",
                            backgroundColor = DuaGold.copy(alpha = if (palette.isDark) 0.20f else 0.14f),
                            textColor = palette.accentGold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "বিস্তারিত",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = palette.accentGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyAmalItemCard(
    dua: VerifiedDua,
    isCompleted: Boolean,
    palette: DuaPalette,
    onOpenDetail: () -> Unit,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpenDetail,
        modifier = modifier.testTag("daily_amal_card_${dua.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) palette.cardSurfaceSoft else palette.cardSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) palette.accentGreen.copy(alpha = 0.65f) else palette.cardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = dua.titleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.titleColor
                    )
                    Text(
                        text = dua.referenceBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = palette.mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (dua.recommendedCount > 1) {
                    DuaMetadataPill(
                        text = "${toDuaBanglaDigits(dua.recommendedCount)} বার",
                        backgroundColor = DuaGold.copy(alpha = if (palette.isDark) 0.22f else 0.16f),
                        textColor = palette.accentGold
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = dua.arabicText,
                    fontFamily = DuaArabicFontFamily,
                    fontSize = 18.sp,
                    lineHeight = 30.sp,
                    color = palette.titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "পূর্ণ দোয়া ও অডিও",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = palette.accentGreen
                    )
                }

                Button(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("daily_amal_toggle_${dua.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) DuaPrimaryGreen else palette.cardSurfaceSoft,
                        contentColor = if (isCompleted) Color.White else palette.titleColor
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Outlined.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) "সম্পন্ন ✓" else "সম্পন্ন করেছি",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaFooterMosqueAccent(palette: DuaPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(54.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val silhouette = if (palette.isDark) {
                Color(0xFF154735).copy(alpha = 0.55f)
            } else {
                DuaPrimaryGreen.copy(alpha = 0.14f)
            }
            val cx = w * 0.5f
            drawArc(
                color = silhouette,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 46f, h - 42f),
                size = Size(92f, 76f)
            )
            drawArc(
                color = silhouette,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 108f, h - 28f),
                size = Size(56f, 48f)
            )
            drawArc(
                color = silhouette,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx + 52f, h - 28f),
                size = Size(56f, 48f)
            )
            drawRect(
                color = silhouette,
                topLeft = Offset(cx - 126f, h - 46f),
                size = Size(10f, 46f)
            )
            drawRect(
                color = silhouette,
                topLeft = Offset(cx + 116f, h - 46f),
                size = Size(10f, 46f)
            )
            drawLine(
                color = DuaGold.copy(alpha = 0.45f),
                start = Offset(0f, h - 2f),
                end = Offset(w, h - 2f),
                strokeWidth = 3f
            )
        }
    }
}
