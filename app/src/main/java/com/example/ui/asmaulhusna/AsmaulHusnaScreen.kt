package com.example.ui.asmaulhusna

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HindSiliguriFontFamily

// নির্দেশনা অনুযায়ী নির্ধারিত কালার প্যালেট
val AsmaulDeepGreen = Color(0xFF0F6B4B)
val AsmaulDarkEmerald = Color(0xFF094832)
val AsmaulGold = Color(0xFFD9A82E)
val AsmaulSoftGoldBg = Color(0xFFFFF8E6)
val AsmaulLightGreen = Color(0xFFF6FAF8)
val AsmaulMintBorder = Color(0xFFD3E7DF)
val AsmaulCardSurface = Color(0xFFFFFFFF)
val AsmaulTextPrimary = Color(0xFF102A21)
val AsmaulTextSecondary = Color(0xFF4A635A)

val AmiriArabicFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

private fun toBanglaDigits(number: Int): String {
    val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return number.toString().map { ch ->
        if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
    }.joinToString("")
}

/**
 * আসমাউল হুসনা (أَسْمَاءُ اللَّهِ الْحُسْنَىٰ — আল্লাহর ৯৯টি সুন্দর নাম) পূর্ণাঙ্গ শিক্ষামূলক বিভাগ।
 */
@Composable
fun AsmaulHusnaScreen(
    registerBackInterceptor: ((() -> Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val manager = remember { AsmaulHusnaManager.getInstance(context) }

    val bookmarkedNumbers by manager.bookmarkedNumbers.collectAsState()
    val learnedNumbers by manager.learnedNumbers.collectAsState()
    val isGridView by manager.isGridView.collectAsState()

    var currentSubScreen by rememberSaveable { mutableStateOf(AsmaulHusnaSubScreen.LIST_HOME) }
    var selectedNameNumber by rememberSaveable { mutableIntStateOf(1) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(AsmaulHusnaFilter.ALL) }
    var showScholarlyNote by rememberSaveable { mutableStateOf(false) }

    val dailyName = remember { manager.getDailyNameToLearn() }

    val filteredNames = remember(searchQuery, selectedFilter, bookmarkedNumbers, learnedNumbers) {
        manager.filterAndSearchNames(
            query = searchQuery,
            filter = selectedFilter,
            bookmarks = bookmarkedNumbers,
            learned = learnedNumbers
        )
    }

    // MainAppScaffold TopBar Back button interceptor so there is only ONE Back button at the top
    DisposableEffect(currentSubScreen) {
        registerBackInterceptor?.invoke {
            if (currentSubScreen == AsmaulHusnaSubScreen.NAME_DETAIL) {
                currentSubScreen = AsmaulHusnaSubScreen.LIST_HOME
                true
            } else {
                false
            }
        }
        onDispose {
            registerBackInterceptor?.invoke { false }
        }
    }

    BackHandler(enabled = currentSubScreen == AsmaulHusnaSubScreen.NAME_DETAIL) {
        currentSubScreen = AsmaulHusnaSubScreen.LIST_HOME
    }

    when (currentSubScreen) {
        AsmaulHusnaSubScreen.LIST_HOME -> {
            AsmaulHusnaHomeListView(
                filteredNames = filteredNames,
                dailyName = dailyName,
                bookmarkedNumbers = bookmarkedNumbers,
                learnedNumbers = learnedNumbers,
                isGridView = isGridView,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                showScholarlyNote = showScholarlyNote,
                onSearchQueryChange = { searchQuery = it },
                onFilterChange = { selectedFilter = it },
                onToggleGridView = { manager.setGridView(!isGridView) },
                onToggleScholarlyNote = { showScholarlyNote = !showScholarlyNote },
                onToggleBookmark = { num -> manager.toggleBookmark(num) },
                onToggleLearned = { num -> manager.toggleLearned(num) },
                onOpenNameDetail = { num ->
                    selectedNameNumber = num
                    currentSubScreen = AsmaulHusnaSubScreen.NAME_DETAIL
                }
            )
        }

        AsmaulHusnaSubScreen.NAME_DETAIL -> {
            val activeName = remember(selectedNameNumber) {
                AsmaulHusnaCatalog.getNameByNumber(selectedNameNumber)
            }
            AsmaulHusnaDetailFullScreen(
                nameItem = activeName,
                isBookmarked = bookmarkedNumbers.contains(activeName.number),
                isLearned = learnedNumbers.contains(activeName.number),
                onToggleBookmark = { manager.toggleBookmark(activeName.number) },
                onToggleLearned = { manager.toggleLearned(activeName.number) },
                onSelectAdjacentName = { targetNum ->
                    if (targetNum in 1..99) {
                        selectedNameNumber = targetNum
                    }
                }
            )
        }
    }
}

@Composable
private fun AsmaulHusnaHomeListView(
    filteredNames: List<AsmaulHusnaName>,
    dailyName: AsmaulHusnaName,
    bookmarkedNumbers: Set<Int>,
    learnedNumbers: Set<Int>,
    isGridView: Boolean,
    searchQuery: String,
    selectedFilter: AsmaulHusnaFilter,
    showScholarlyNote: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (AsmaulHusnaFilter) -> Unit,
    onToggleGridView: () -> Unit,
    onToggleScholarlyNote: () -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onToggleLearned: (Int) -> Unit,
    onOpenNameDetail: (Int) -> Unit
) {
    val gridRows = remember(filteredNames) { filteredNames.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AsmaulLightGreen)
            .testTag("asmaul_husna_home_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ১. মূল হেডার ও সংক্ষিপ্ত পরিচিতি
        item {
            AsmaulHusnaHeroHeaderCard(
                learnedCount = learnedNumbers.size,
                bookmarkedCount = bookmarkedNumbers.size,
                showScholarlyNote = showScholarlyNote,
                onToggleScholarlyNote = onToggleScholarlyNote
            )
        }

        // ২. দৈনিক একটি নাম শেখার বিভাগ (Daily Name to Learn)
        item {
            DailyNameLearningCard(
                dailyName = dailyName,
                isLearned = learnedNumbers.contains(dailyName.number),
                isBookmarked = bookmarkedNumbers.contains(dailyName.number),
                onToggleLearned = { onToggleLearned(dailyName.number) },
                onToggleBookmark = { onToggleBookmark(dailyName.number) },
                onOpenDetail = { onOpenNameDetail(dailyName.number) }
            )
        }

        // ৩. সার্চ বার, ফিল্টার ও গ্রিড/লিস্ট ভিউ সুইচ
        item {
            AsmaulHusnaSearchAndFilterBar(
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                isGridView = isGridView,
                bookmarkedCount = bookmarkedNumbers.size,
                learnedCount = learnedNumbers.size,
                onSearchQueryChange = onSearchQueryChange,
                onFilterChange = onFilterChange,
                onToggleGridView = onToggleGridView
            )
        }

        // ৪. নামের তালিকা (৯৯টি নাম — গ্রিড অথবা কার্ড লিস্ট)
        if (filteredNames.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("asmaul_husna_empty_state"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
                    border = BorderStroke(1.dp, AsmaulMintBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AsmaulDeepGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "কোনো নাম খুঁজে পাওয়া যায়নি",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AsmaulTextPrimary
                        )
                        Text(
                            text = "অন্য নাম, অর্থ বা ক্রমিক নম্বর দিয়ে অনুসন্ধান করুন অথবা ‘সব নাম’ ফিল্টার নির্বাচন করুন।",
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 13.sp,
                            color = AsmaulTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        if (selectedFilter != AsmaulHusnaFilter.ALL || searchQuery.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    onFilterChange(AsmaulHusnaFilter.ALL)
                                },
                                border = BorderStroke(1.dp, AsmaulDeepGreen)
                            ) {
                                Text(
                                    text = "সব ৯৯টি নাম দেখুন",
                                    fontFamily = HindSiliguriFontFamily,
                                    color = AsmaulDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else if (isGridView) {
            items(gridRows, key = { row -> "grid_row_${row.first().number}" }) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        AsmaulHusnaGridCard(
                            item = item,
                            isBookmarked = bookmarkedNumbers.contains(item.number),
                            isLearned = learnedNumbers.contains(item.number),
                            onToggleBookmark = { onToggleBookmark(item.number) },
                            onClick = { onOpenNameDetail(item.number) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(filteredNames, key = { item -> "list_item_${item.number}" }) { item ->
                AsmaulHusnaListRowCard(
                    item = item,
                    isBookmarked = bookmarkedNumbers.contains(item.number),
                    isLearned = learnedNumbers.contains(item.number),
                    onToggleBookmark = { onToggleBookmark(item.number) },
                    onToggleLearned = { onToggleLearned(item.number) },
                    onClick = { onOpenNameDetail(item.number) }
                )
            }
        }
    }
}

@Composable
private fun AsmaulHusnaHeroHeaderCard(
    learnedCount: Int,
    bookmarkedCount: Int,
    showScholarlyNote: Boolean,
    onToggleScholarlyNote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asmaul_husna_hero_header"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AsmaulDeepGreen),
        border = BorderStroke(1.5.dp, AsmaulGold.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AsmaulDeepGreen, AsmaulDarkEmerald)
                    )
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Ornamental Badge
            Surface(
                color = AsmaulGold.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, AsmaulGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AsmaulGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "তাওহীদ ও সিফাতে ইলাহী",
                        fontFamily = HindSiliguriFontFamily,
                        color = AsmaulGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ১. আরবি শিরোনাম: أَسْمَاءُ اللَّهِ الْحُسْنَىٰ
            Text(
                text = AsmaulHusnaCatalog.ARABIC_HEADER_TITLE,
                fontFamily = AmiriArabicFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = AsmaulGold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asmaul_husna_arabic_title")
            )

            // ২. বাংলা শিরোনাম: আল্লাহর ৯৯টি সুন্দর নাম
            Text(
                text = AsmaulHusnaCatalog.BANGLA_HEADER_TITLE,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("asmaul_husna_bangla_title")
            )

            // কুরআনের আয়াত
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = AsmaulHusnaCatalog.QURAN_INTRO_VERSE_ARABIC,
                        fontFamily = AmiriArabicFontFamily,
                        fontSize = 18.sp,
                        color = AsmaulGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = AsmaulHusnaCatalog.QURAN_INTRO_VERSE_BANGLA,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFFE5F6F0),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // নিচে সংক্ষিপ্ত পরিচিতি
            Text(
                text = AsmaulHusnaCatalog.SHORT_INTRO_BANGLA,
                fontFamily = HindSiliguriFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFFEBF7F3),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("asmaul_husna_intro_text")
            )

            // শেখার অগ্রগতি ও বুকমার্ক সামারি
            Surface(
                color = Color.Black.copy(alpha = 0.22f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "শেখা সম্পন্ন: ${toBanglaDigits(learnedCount)} / ৯৯ টি",
                            fontFamily = HindSiliguriFontFamily,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("asmaul_husna_learned_progress_text")
                        )
                        Text(
                            text = "পছন্দের নাম: ${toBanglaDigits(bookmarkedCount)} টি",
                            fontFamily = HindSiliguriFontFamily,
                            color = AsmaulGold,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (learnedCount.toFloat() / 99f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AsmaulGold,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            // শরয়ী নীতিমালা ও ইলমী নোট টগল
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleScholarlyNote() }
                    .testTag("asmaul_husna_scholarly_note_toggle")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = AsmaulGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "শরয়ী নীতিমালা, সূত্র ও মুহাদ্দিসগণের অভিমত",
                                fontFamily = HindSiliguriFontFamily,
                                color = AsmaulGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showScholarlyNote) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AsmaulGold
                        )
                    }
                    AnimatedVisibility(visible = showScholarlyNote) {
                        Text(
                            text = AsmaulHusnaCatalog.SCHOLARLY_METHODOLOGY_NOTE_BANGLA,
                            fontFamily = HindSiliguriFontFamily,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("asmaul_husna_scholarly_note_body")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyNameLearningCard(
    dailyName: AsmaulHusnaName,
    isLearned: Boolean,
    isBookmarked: Boolean,
    onToggleLearned: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asmaul_husna_daily_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AsmaulSoftGoldBg),
        border = BorderStroke(1.2.dp, AsmaulGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AsmaulDeepGreen,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AsmaulGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "দৈনিক একটি নাম শিখুন • নাম নং ${dailyName.numberBangla}",
                            fontFamily = HindSiliguriFontFamily,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "পছন্দের তালিকায় সংরক্ষণ",
                        tint = if (isBookmarked) AsmaulGold else AsmaulDeepGreen
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetail() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dailyName.transliterationBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AsmaulDeepGreen
                    )
                    Text(
                        text = dailyName.meaningBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = AsmaulTextPrimary
                    )
                    Text(
                        text = dailyName.quranicReference,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 11.5.sp,
                        color = AsmaulTextSecondary
                    )
                }

                Surface(
                    color = AsmaulDeepGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AsmaulGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = dailyName.arabicName,
                        fontFamily = AmiriArabicFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = AsmaulDeepGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleLearned,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("asmaul_husna_daily_mark_learned_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AsmaulDeepGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isLearned) Color(0xFFE3F4EC) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = AsmaulDeepGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLearned) "শেখা সম্পন্ন" else "শেখা হয়েছে চিহ্নিত করুন",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AsmaulDeepGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onOpenDetail,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("asmaul_husna_daily_open_detail_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AsmaulDeepGreen)
                ) {
                    Text(
                        text = "বিস্তারিত ও দলিল",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AsmaulHusnaSearchAndFilterBar(
    searchQuery: String,
    selectedFilter: AsmaulHusnaFilter,
    isGridView: Boolean,
    bookmarkedCount: Int,
    learnedCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (AsmaulHusnaFilter) -> Unit,
    onToggleGridView: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("asmaul_husna_search_input"),
                placeholder = {
                    Text(
                        text = "নাম, অর্থ বা ক্রমিক নম্বর (১-৯৯) দিয়ে খুঁজুন...",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.sp,
                        color = AsmaulTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "অনুসন্ধান",
                        tint = AsmaulDeepGreen
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("asmaul_husna_search_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "মুছুন",
                                tint = AsmaulTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AsmaulDeepGreen,
                    unfocusedBorderColor = AsmaulMintBorder,
                    focusedTextColor = AsmaulTextPrimary,
                    unfocusedTextColor = AsmaulTextPrimary
                )
            )

            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleGridView() }
                    .testTag("asmaul_husna_view_mode_toggle"),
                color = AsmaulDeepGreen,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "তালিকা ভিউ" else "গ্রিড ভিউ",
                        tint = AsmaulGold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsmaulHusnaFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val countLabel = when (filter) {
                    AsmaulHusnaFilter.ALL -> "৯৯"
                    AsmaulHusnaFilter.BOOKMARKED -> toBanglaDigits(bookmarkedCount)
                    AsmaulHusnaFilter.LEARNED -> toBanglaDigits(learnedCount)
                    AsmaulHusnaFilter.UNLEARNED -> toBanglaDigits(99 - learnedCount)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = "${filter.labelBangla} ($countLabel)",
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AsmaulDeepGreen,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = AsmaulDeepGreen
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = AsmaulMintBorder,
                        selectedBorderColor = AsmaulGold
                    ),
                    modifier = Modifier.testTag("asmaul_husna_filter_${filter.id}")
                )
            }
        }
    }
}

/**
 * ২. নামের তালিকা — গ্রিড কার্ড (বাংলা ক্রমিক নম্বর, আরবি নাম, বাংলা উচ্চারণ, নির্ভুল বাংলা অর্থ)
 */
@Composable
private fun AsmaulHusnaGridCard(
    item: AsmaulHusnaName,
    isBookmarked: Boolean,
    isLearned: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("asmaul_husna_card_${item.number}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
        border = BorderStroke(
            width = if (isLearned) 1.4.dp else 1.dp,
            color = if (isLearned) AsmaulGold else AsmaulMintBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top bar: Bangla Serial Number Badge + Learned / Bookmark icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AsmaulDeepGreen,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AsmaulGold)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("asmaul_husna_card_number_${item.number}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.numberBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AsmaulGold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLearned) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "শেখা সম্পন্ন",
                            tint = AsmaulDeepGreen,
                            modifier = Modifier
                                .size(17.dp)
                                .testTag("asmaul_husna_card_learned_badge_${item.number}")
                        )
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("asmaul_husna_card_bookmark_${item.number}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "পছন্দের নাম",
                            tint = if (isBookmarked) AsmaulGold else AsmaulTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // আরবি নাম (সঠিক আরবি ফন্ট ও হারাকাতসহ)
            Surface(
                color = AsmaulLightGreen,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AsmaulMintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.arabicName,
                    fontFamily = AmiriArabicFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = AsmaulDeepGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                )
            }

            // বাংলা উচ্চারণ
            Text(
                text = item.transliterationBangla,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AsmaulDeepGreen,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // নির্ভুল বাংলা অর্থ
            Text(
                text = item.meaningBangla,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = AsmaulTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(34.dp)
            )
        }
    }
}

/**
 * ২. নামের তালিকা — ১-কলাম কার্ড ভিউ
 */
@Composable
private fun AsmaulHusnaListRowCard(
    item: AsmaulHusnaName,
    isBookmarked: Boolean,
    isLearned: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleLearned: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("asmaul_husna_card_${item.number}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
        border = BorderStroke(
            width = if (isLearned) 1.4.dp else 1.dp,
            color = if (isLearned) AsmaulGold else AsmaulMintBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = AsmaulDeepGreen,
                shape = CircleShape,
                border = BorderStroke(1.2.dp, AsmaulGold)
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.numberBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AsmaulGold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.transliterationBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AsmaulDeepGreen
                )
                Text(
                    text = item.meaningBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 13.sp,
                    color = AsmaulTextPrimary
                )
                Text(
                    text = item.quranicReference,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 11.5.sp,
                    color = AsmaulTextSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.arabicName,
                    fontFamily = AmiriArabicFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = AsmaulDeepGreen
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleLearned,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "শেখা চিহ্নিত করুন",
                            tint = if (isLearned) AsmaulDeepGreen else AsmaulTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("asmaul_husna_card_bookmark_${item.number}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "পছন্দের নাম",
                            tint = if (isBookmarked) AsmaulGold else AsmaulTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ৩. নামের বিস্তারিত পৃষ্ঠা (Full-Screen Name Detail Page)
 * - বড় আকারে আরবি নাম
 * - বাংলা উচ্চারণ ও অর্থ
 * - নির্ভরযোগ্য সূত্রভিত্তিক সংক্ষিপ্ত ব্যাখ্যা ও মুহাদ্দিসগণের নোট
 * - প্রাসঙ্গিক কুরআনের আয়াত ও সূরা-আয়াত নম্বর
 * - নামটি থেকে প্রাপ্ত শিক্ষা
 * - নির্ভরযোগ্য সূত্রে পাওয়া সংশ্লিষ্ট দোয়া
 * - আগের নাম ও পরের নামে যাওয়ার সুবিধা
 * - বুকমার্ক ও শেখা চিহ্নিত করার অপশন
 */
@Composable
private fun AsmaulHusnaDetailFullScreen(
    nameItem: AsmaulHusnaName,
    isBookmarked: Boolean,
    isLearned: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleLearned: () -> Unit,
    onSelectAdjacentName: (Int) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AsmaulLightGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("asmaul_husna_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ১. ক্যালিগ্রাফিক হিরো কার্ড: বড় আরবি নাম, বাংলা উচ্চারণ, অর্থ, বুকমার্ক ও শেখা বাটন
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulDeepGreen),
            border = BorderStroke(1.5.dp, AsmaulGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AsmaulDeepGreen, AsmaulDarkEmerald)
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AsmaulGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, AsmaulGold)
                    ) {
                        Text(
                            text = "নাম নং: ${nameItem.numberBangla} / ৯৯",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = AsmaulGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .testTag("asmaul_husna_detail_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "বুকমার্ক",
                                tint = if (isBookmarked) AsmaulGold else Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    append("أَسْمَاءُ اللَّهِ الْحُسْنَىٰ — আল্লাহর ৯৯টি সুন্দর নাম\n")
                                    append("নাম নং ${nameItem.numberBangla}: ${nameItem.arabicName} (${nameItem.transliterationBangla})\n")
                                    append("অর্থ: ${nameItem.meaningBangla}\n\n")
                                    append("সংক্ষিপ্ত ব্যাখ্যা: ${nameItem.detailedExplanationBangla}\n\n")
                                    append("কুরআনের দলিল: ${nameItem.quranicVerseBangla} (${nameItem.quranicReference})\n")
                                    append("দোয়া: ${nameItem.authenticDuaMeaning} (${nameItem.authenticDuaSource})")
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "আসমাউল হুসনা শেয়ার করুন")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .testTag("asmaul_husna_detail_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "শেয়ার করুন",
                                tint = Color.White
                            )
                        }
                    }
                }

                // বড় আকারে আরবি নাম
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AsmaulGold.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nameItem.arabicName,
                        fontFamily = AmiriArabicFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        color = AsmaulGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                            .testTag("asmaul_husna_detail_arabic_name")
                    )
                }

                // বাংলা উচ্চারণ
                Text(
                    text = nameItem.transliterationBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("asmaul_husna_detail_transliteration")
                )

                // নির্ভুল বাংলা অর্থ
                Text(
                    text = nameItem.meaningBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFFE6F7F1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("asmaul_husna_detail_meaning")
                )

                // শেখা সম্পন্ন ও বুকমার্ক অ্যাকশন রো
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleLearned,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("asmaul_husna_detail_learned_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLearned) AsmaulGold else Color.White.copy(alpha = 0.16f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isLearned) AsmaulTextPrimary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLearned) "শেখা সম্পন্ন হয়েছে" else "শেখা সম্পন্ন চিহ্নিত করুন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isLearned) AsmaulTextPrimary else Color.White
                        )
                    }
                }
            }
        }

        // ২. আরবি উচ্চারণ, মাখরাজ ও যাচাইকৃত অডিও নির্দেশিকা
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("asmaul_husna_detail_pronunciation_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
            border = BorderStroke(1.dp, AsmaulMintBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = AsmaulDeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "শুদ্ধ আরবি উচ্চারণ ও তাজবীদ নির্দেশিকা",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AsmaulDeepGreen
                    )
                }
                Text(
                    text = nameItem.pronunciationGuideBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = AsmaulTextPrimary
                )
                Surface(
                    color = AsmaulLightGreen,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AsmaulMintBorder)
                ) {
                    Text(
                        text = if (nameItem.verifiedAudioUrl != null) {
                            "যাচাইকৃত অডিও তিলাওয়াত উপলব্ধ।"
                        } else {
                            "দ্রষ্টব্য: আল্লাহর পবিত্র নামের উচ্চারণে যান্ত্রিক বা রোবোটিক TTS বিকৃতি এড়াতে কেবল তাজবীদ-মাখরাজ নির্দেশিকা প্রদর্শিত হচ্ছে।"
                        },
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 11.5.sp,
                        color = AsmaulTextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // ৩. নির্ভরযোগ্য সূত্রভিত্তিক সংক্ষিপ্ত ব্যাখ্যা ও ইলমী নোট
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("asmaul_husna_detail_explanation_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
            border = BorderStroke(1.dp, AsmaulMintBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AsmaulDeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "নির্ভরযোগ্য সূত্রভিত্তিক সংক্ষিপ্ত ব্যাখ্যা",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = AsmaulDeepGreen
                    )
                }
                Text(
                    text = nameItem.detailedExplanationBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = AsmaulTextPrimary
                )
                HorizontalDivider(color = AsmaulMintBorder)
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = AsmaulGold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = "সূত্র ও তাহকীক: ${nameItem.scholarlySourceAndNote}",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = AsmaulTextSecondary
                    )
                }
            }
        }

        // ৪. প্রাসঙ্গিক কুরআনের আয়াত ও সূরা-আয়াত নম্বর
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("asmaul_husna_detail_quran_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
            border = BorderStroke(1.2.dp, AsmaulGold.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = AsmaulDeepGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "প্রাসঙ্গিক কুরআনের আয়াত",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = AsmaulDeepGreen
                        )
                    }
                }

                Surface(
                    color = AsmaulSoftGoldBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AsmaulGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nameItem.quranicVerseArabic,
                        fontFamily = AmiriArabicFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 36.sp,
                        color = AsmaulDeepGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }

                Text(
                    text = "অনুবাদ: “${nameItem.quranicVerseBangla}”",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = AsmaulTextPrimary
                )

                Surface(
                    color = AsmaulDeepGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "সূরা ও আয়াত নম্বর: ${nameItem.quranicReference}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = AsmaulDeepGreen,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("asmaul_husna_detail_quran_ref")
                    )
                }
            }
        }

        // ৫. নামটি থেকে প্রাপ্ত শিক্ষা
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("asmaul_husna_detail_lesson_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
            border = BorderStroke(1.dp, AsmaulMintBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AsmaulGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "নামটি থেকে প্রাপ্ত শিক্ষা ও আমল",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = AsmaulDeepGreen
                    )
                }
                Text(
                    text = nameItem.lessonForBelieverBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = AsmaulTextPrimary
                )
            }
        }

        // ৬. নির্ভরযোগ্য সূত্রে পাওয়া সংশ্লিষ্ট দোয়া
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("asmaul_husna_detail_dua_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AsmaulCardSurface),
            border = BorderStroke(1.dp, AsmaulMintBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = AsmaulDeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "সংশ্লিষ্ট কুরআনী ও মাসনূন দোয়া",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = AsmaulDeepGreen
                    )
                }

                Surface(
                    color = AsmaulLightGreen,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AsmaulMintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nameItem.authenticDuaArabic,
                        fontFamily = AmiriArabicFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        lineHeight = 34.sp,
                        color = AsmaulDeepGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                Text(
                    text = "উচ্চারণ: ${nameItem.authenticDuaPronunciation}",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = AsmaulDeepGreen
                )

                Text(
                    text = "অর্থ: “${nameItem.authenticDuaMeaning}”",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = AsmaulTextPrimary
                )

                Text(
                    text = "দোয়ার সূত্র: ${nameItem.authenticDuaSource}",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = AsmaulTextSecondary
                )
            }
        }

        // ৭. আগের নাম ও পরের নামে যাওয়ার সুবিধা (Previous / Next Navigation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val hasPrev = nameItem.number > 1
            val hasNext = nameItem.number < 99

            OutlinedButton(
                onClick = { if (hasPrev) onSelectAdjacentName(nameItem.number - 1) },
                enabled = hasPrev,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("asmaul_husna_prev_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasPrev) AsmaulDeepGreen else AsmaulMintBorder)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "আগের নাম",
                    tint = if (hasPrev) AsmaulDeepGreen else AsmaulTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasPrev) {
                        val prev = AsmaulHusnaCatalog.getNameByNumber(nameItem.number - 1)
                        "আগের নাম (${prev.numberBangla})"
                    } else {
                        "প্রথম নাম"
                    },
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (hasPrev) AsmaulDeepGreen else AsmaulTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { if (hasNext) onSelectAdjacentName(nameItem.number + 1) },
                enabled = hasNext,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("asmaul_husna_next_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AsmaulDeepGreen,
                    disabledContainerColor = AsmaulMintBorder
                )
            ) {
                Text(
                    text = if (hasNext) {
                        val next = AsmaulHusnaCatalog.getNameByNumber(nameItem.number + 1)
                        "পরের নাম (${next.numberBangla})"
                    } else {
                        "সর্বশেষ নাম"
                    },
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "পরের নাম",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
