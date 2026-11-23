package com.example.ui.duas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.quranlearning.QuranAudioPlaybackState
import com.example.ui.quranlearning.QuranLearningAudioController
import com.example.ui.quranlearning.VerifiedQuranAudioSource
import com.example.ui.theme.HindSiliguriFontFamily
import kotlinx.coroutines.delay

/**
 * দোয়ার বিস্তারিত পাঠ ও অনুশীলন স্ক্রিন।
 *
 * সুনির্দিষ্ট ক্রম (A → G):
 * A. দোয়ার নাম (শিরোনাম, উপলক্ষ, প্রিয় বাটন)
 * B. আরবি মূল পাঠ (স্পষ্ট RTL ফন্ট ও ফন্ট সাইজ নিয়ন্ত্রণ)
 * C. বাংলা উচ্চারণ (আরবির ঠিক নিচেই + সহায়ক নির্দেশনা)
 * D. বাংলা অর্থ (শেখার মোডে অর্থ লুকিয়ে অনুশীলন সুবিধা)
 * E. অডিও প্লেয়ার (কেবল যাচাইকৃত অডিও সোর্স থাকলে প্রদর্শিত)
 * F. রেফারেন্স ও হাদিসের মান (সহীহ/হাসান ও মতভেদ নোট)
 * G. অন্যান্য সুবিধা (Copy Arabic, Copy উচ্চারণ, Copy অর্থ, Share, প্রিয়, শিখেছি, পূর্ববর্তী/পরবর্তী)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuaDetailScreen(
    dua: VerifiedDua,
    categoryDuas: List<VerifiedDua>,
    palette: DuaPalette,
    storage: DuaStorageManager,
    onBack: () -> Unit,
    onSelectDua: (VerifiedDua) -> Unit,
    audioControllerOverride: QuranLearningAudioController? = null
) {
    val context = LocalContext.current
    val favoriteIds by storage.favoriteDuaIds.collectAsState()
    val learnedIds by storage.learnedDuaIds.collectAsState()
    val dailyCompletedIds by storage.dailyCompletedIds.collectAsState()
    val arabicFontScale by storage.arabicFontScale.collectAsState()

    val isFavorite = favoriteIds.contains(dua.id)
    val isLearned = learnedIds.contains(dua.id)
    val isDailyCompleted = dailyCompletedIds.contains(dua.id)

    var hideMeaningForPractice by remember(dua.id) { mutableStateOf(false) }
    var feedbackBannerMessage by remember(dua.id) { mutableStateOf<String?>(null) }

    val audioSource = remember(dua.id, dua.hasVerifiedAudio, dua.arabicText, dua.quranSurahAyah) {
        DuaCatalog.resolveAudioSource(dua)
    }

    val ownsController = audioControllerOverride == null
    val audioController = remember(context, audioControllerOverride) {
        audioControllerOverride ?: QuranLearningAudioController.create(context)
    }
    val playbackState by audioController.playbackState.collectAsState()
    val volumeMutedHint by audioController.volumeMutedHint.collectAsState()

    LaunchedEffect(dua.id) {
        audioController.stop()
    }

    DisposableEffect(audioController, ownsController) {
        onDispose {
            if (ownsController) {
                audioController.release()
            } else {
                audioController.stop()
            }
        }
    }

    LaunchedEffect(feedbackBannerMessage) {
        if (feedbackBannerMessage != null) {
            delay(2800L)
            feedbackBannerMessage = null
        }
    }

    val currentIndex = remember(dua.id, categoryDuas) {
        categoryDuas.indexOfFirst { it.id == dua.id }
    }
    val prevDua = if (currentIndex > 0) categoryDuas[currentIndex - 1] else null
    val nextDua = if (currentIndex in 0 until categoryDuas.lastIndex) categoryDuas[currentIndex + 1] else null

    val scrollState = rememberScrollState()

    fun copyToClipboard(label: String, text: String, confirmationBangla: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText(label, text))
            feedbackBannerMessage = confirmationBangla
        } catch (_: Throwable) {
            feedbackBannerMessage = "কপি করতে সমস্যা হয়েছে।"
        }
    }

    fun shareDua() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, dua.titleBangla)
                putExtra(Intent.EXTRA_TEXT, DuaCatalog.formatShareText(dua))
            }
            val chooser = Intent.createChooser(shareIntent, "দোয়া শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            feedbackBannerMessage = "দোয়া শেয়ার অপশন খোলা হয়েছে।"
        } catch (_: Throwable) {
            feedbackBannerMessage = "শেয়ার করার অ্যাপ পাওয়া যায়নি।"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("dua_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // শীর্ষে ক্যাটাগরি ও ফিরে যাওয়ার নেভিগেশন সারি
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dua_detail_back_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "তালিকায় ফিরে যান",
                            tint = palette.accentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dua.category.titleBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = palette.accentGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (currentIndex >= 0 && categoryDuas.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.chipBackground,
                            border = BorderStroke(1.dp, palette.cardBorder)
                        ) {
                            Text(
                                text = "${toDuaBanglaDigits(currentIndex + 1)} / ${toDuaBanglaDigits(categoryDuas.size)}",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = palette.titleColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // A. দোয়ার নাম (শিরোনাম, উপলক্ষ ও প্রিয় বাটন)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                    border = BorderStroke(1.dp, palette.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dua.titleBangla,
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    lineHeight = 27.sp,
                                    color = palette.titleColor,
                                    modifier = Modifier.testTag("dua_detail_title")
                                )
                                Text(
                                    text = dua.occasionBangla,
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = palette.subtitleColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val nowFav = storage.toggleFavorite(dua.id)
                                    feedbackBannerMessage = if (nowFav) {
                                        "প্রিয় দোয়ায় সংরক্ষিত হয়েছে।"
                                    } else {
                                        "প্রিয় দোয়া থেকে সরানো হয়েছে।"
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFavorite) Color(0xFFFFEBEE) else palette.cardSurfaceSoft
                                    )
                                    .testTag("dua_detail_fav_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "প্রিয় দোয়া থেকে সরান" else "প্রিয় দোয়ায় সংরক্ষণ করুন",
                                    tint = if (isFavorite) Color(0xFFD32F2F) else palette.accentGreen,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DuaMetadataPill(
                                text = dua.sourceType.labelBangla,
                                backgroundColor = palette.chipBackground,
                                textColor = palette.accentGreen
                            )
                            if (dua.recommendedCount > 1) {
                                DuaMetadataPill(
                                    text = "${toDuaBanglaDigits(dua.recommendedCount)} বার পঠনীয়",
                                    backgroundColor = DuaGold.copy(alpha = if (palette.isDark) 0.22f else 0.16f),
                                    textColor = palette.accentGold
                                )
                            }
                            if (isLearned) {
                                DuaMetadataPill(
                                    text = "শিখেছি ✓",
                                    backgroundColor = DuaPrimaryGreen.copy(alpha = 0.16f),
                                    textColor = palette.accentGreen
                                )
                            }
                        }
                    }
                }

                // B. আরবি মূল পাঠ (স্পষ্ট RTL ফন্ট ও ফন্ট সাইজ কন্ট্রোল)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(palette.arabicBoxTop, palette.arabicBoxBottom)
                            )
                        )
                        .border(1.2.dp, palette.goldBorder, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "আরবি মূল পাঠ",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = DuaGoldSheen
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { storage.updateArabicFontScale(arabicFontScale - 0.10f) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(0.8.dp, DuaGoldSheen.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .size(width = 48.dp, height = 36.dp)
                                        .testTag("dua_font_dec_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "A-",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { storage.updateArabicFontScale(arabicFontScale + 0.10f) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(0.8.dp, DuaGoldSheen.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .size(width = 48.dp, height = 36.dp)
                                        .testTag("dua_font_inc_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "A+",
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        val scaledArabicSp = (27f * arabicFontScale).coerceIn(22f, 38f)
                        val scaledLineHeightSp = scaledArabicSp * 1.82f

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = dua.arabicText,
                                fontFamily = DuaArabicFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = scaledArabicSp.sp,
                                lineHeight = scaledLineHeightSp.sp,
                                color = palette.arabicTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("dua_detail_arabic")
                            )
                        }
                    }
                }

                // C. বাংলা উচ্চারণ (আরবি পাঠের ঠিক নিচেই)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "বাংলা উচ্চারণ",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.accentGreen
                        )
                        Text(
                            text = dua.banglaPronunciation,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.5.sp,
                            lineHeight = 24.sp,
                            color = palette.titleColor,
                            modifier = Modifier.testTag("dua_detail_pronunciation")
                        )
                        HorizontalDivider(color = palette.dividerColor, thickness = 0.8.dp)
                        Text(
                            text = DuaCatalog.PRONUNCIATION_GUIDANCE_NOTE,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = palette.mutedColor
                        )
                    }
                }

                // D. বাংলা অর্থ (অর্থ লুকিয়ে অনুশীলন / শেখার মোডসহ)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                    border = BorderStroke(1.dp, palette.cardBorder)
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
                            Text(
                                text = "বাংলা অর্থ",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.accentGreen
                            )

                            TextButton(
                                onClick = { hideMeaningForPractice = !hideMeaningForPractice },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dua_toggle_hide_meaning"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (hideMeaningForPractice) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (hideMeaningForPractice) "অর্থ দেখুন" else "অর্থ লুকিয়ে অনুশীলন করুন",
                                    tint = palette.accentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hideMeaningForPractice) "অর্থ দেখুন" else "অর্থ লুকান",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = palette.accentGold
                                )
                            }
                        }

                        if (hideMeaningForPractice) {
                            Surface(
                                onClick = { hideMeaningForPractice = false },
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardSurfaceSoft,
                                border = BorderStroke(1.dp, palette.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .testTag("dua_meaning_hidden_prompt")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "আগে মনে মনে অর্থটি স্মরণ করুন, তারপর দেখতে এখানে ট্যাপ করুন।",
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = palette.subtitleColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = dua.banglaMeaning,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.5.sp,
                                lineHeight = 25.sp,
                                color = palette.titleColor,
                                modifier = Modifier.testTag("dua_detail_meaning")
                            )
                        }
                    }
                }

                // E. অডিও প্লেয়ার (কেবল যাচাইকৃত অডিও সোর্স থাকলে প্রদর্শিত; না থাকলে সম্পূর্ণ লুকানো)
                if (audioSource != null) {
                    DuaAudioPlayerCard(
                        audioSource = audioSource,
                        playbackState = playbackState,
                        volumeMutedHint = volumeMutedHint,
                        palette = palette,
                        onPlayPause = { audioController.onPlayOrPauseTapped(audioSource) },
                        onReplay = { audioController.onReplayTapped(audioSource) },
                        onRetry = { audioController.onRetryTapped(audioSource) }
                    )
                }

                // F. রেফারেন্স ও হাদিসের মান (সম্পূর্ণ স্বচ্ছ ও যাচাইকৃত সূত্র)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .testTag("dua_detail_reference"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurfaceSoft),
                    border = BorderStroke(1.dp, palette.goldBorder)
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
                                imageVector = Icons.Default.Verified,
                                contentDescription = "যাচাইকৃত রেফারেন্স",
                                tint = palette.accentGreen,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "সূত্র ও রেফারেন্স",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.accentGreen
                            )
                        }

                        Text(
                            text = dua.referenceBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            color = palette.titleColor
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DuaMetadataPill(
                                text = "মান: ${dua.gradingBangla}",
                                backgroundColor = palette.cardSurface,
                                textColor = palette.accentGreen
                            )
                        }

                        if (!dua.scholarlyNoteBangla.isNullOrBlank()) {
                            HorizontalDivider(color = palette.dividerColor, thickness = 0.8.dp)
                            Text(
                                text = "আলেমদের টীকা: ${dua.scholarlyNoteBangla}",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = palette.subtitleColor
                            )
                        }
                    }
                }

                // G. অন্যান্য সুবিধা: Copy Arabic, Copy উচ্চারণ, Copy অর্থ, Share, প্রিয়, শিখেছি, দৈনিক আমল
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "কপি, শেয়ার ও অনুশীলন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.titleColor
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DuaActionChipButton(
                                label = "আরবি কপি",
                                icon = Icons.Default.ContentCopy,
                                palette = palette,
                                testTag = "dua_copy_arabic_btn",
                                onClick = {
                                    copyToClipboard("Dua Arabic", dua.arabicText, "আরবি পাঠ কপি করা হয়েছে।")
                                }
                            )
                            DuaActionChipButton(
                                label = "উচ্চারণ কপি",
                                icon = Icons.Default.ContentCopy,
                                palette = palette,
                                testTag = "dua_copy_pronunciation_btn",
                                onClick = {
                                    copyToClipboard(
                                        "Dua Pronunciation",
                                        dua.banglaPronunciation,
                                        "বাংলা উচ্চারণ কপি করা হয়েছে।"
                                    )
                                }
                            )
                            DuaActionChipButton(
                                label = "অর্থ কপি",
                                icon = Icons.Default.ContentCopy,
                                palette = palette,
                                testTag = "dua_copy_meaning_btn",
                                onClick = {
                                    copyToClipboard("Dua Meaning", dua.banglaMeaning, "বাংলা অর্থ কপি করা হয়েছে।")
                                }
                            )
                            DuaActionChipButton(
                                label = "শেয়ার",
                                icon = Icons.Default.Share,
                                palette = palette,
                                testTag = "dua_share_btn",
                                onClick = { shareDua() }
                            )
                        }

                        HorizontalDivider(color = palette.dividerColor, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val nowFav = storage.toggleFavorite(dua.id)
                                    feedbackBannerMessage = if (nowFav) {
                                        "প্রিয় দোয়ায় সংরক্ষিত হয়েছে।"
                                    } else {
                                        "প্রিয় দোয়া থেকে সরানো হয়েছে।"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dua_detail_fav_action_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFavorite) Color(0xFFD32F2F) else palette.cardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isFavorite) {
                                        Color(0xFFD32F2F).copy(alpha = 0.10f)
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = if (isFavorite) Color(0xFFD32F2F) else palette.titleColor
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFavorite) "সংরক্ষিত" else "বুকমার্ক",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    val nowLearned = storage.toggleLearned(dua.id)
                                    feedbackBannerMessage = if (nowLearned) {
                                        "আলহামদুলিল্লাহ! দোয়াটি শেখা তালিকায় যুক্ত হয়েছে।"
                                    } else {
                                        "শেখা তালিকা থেকে সরানো হয়েছে।"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dua_mark_learned_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLearned) DuaPrimaryGreen else palette.cardSurfaceSoft,
                                    contentColor = if (isLearned) Color.White else palette.accentGreen
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLearned) "শিখেছি ✓" else "শিখেছি চিহ্নিত",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (dua.dailyAmalSlot != null) {
                            Button(
                                onClick = {
                                    val done = storage.toggleDailyAmalCompleted(dua.id)
                                    feedbackBannerMessage = if (done) {
                                        "আজকের দৈনিক আমল সম্পন্ন হিসেবে চিহ্নিত হয়েছে।"
                                    } else {
                                        "আজকের আমলের চিহ্ন সরানো হয়েছে।"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dua_detail_complete_amal_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDailyCompleted) DuaPrimaryGreen else DuaGold,
                                    contentColor = if (isDailyCompleted) Color.White else Color(0xFF1F1600)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isDailyCompleted) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Outlined.CheckCircle
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDailyCompleted) {
                                        "আজকের আমল সম্পন্ন হয়েছে ✓"
                                    } else {
                                        "সম্পন্ন করেছি (দৈনিক আমল)"
                                    },
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // ক্যাটাগরির পূর্ববর্তী / পরবর্তী দোয়া নেভিগেশন
                if (prevDua != null || nextDua != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { prevDua?.let(onSelectDua) },
                            enabled = prevDua != null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dua_prev_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.cardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "পূর্ববর্তী দোয়া",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "পূর্ববর্তী",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        }

                        Button(
                            onClick = { nextDua?.let(onSelectDua) },
                            enabled = nextDua != null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dua_next_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DuaPrimaryGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "পরবর্তী দোয়া",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "পরবর্তী দোয়া",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))
            }
        }

        // তাৎক্ষণিক ভাসমান কনফার্মেশন ব্যানার (যেকোনো স্ক্রল অবস্থানে দৃশ্যমান, নিচের বাটন ঢাকে না)
        AnimatedVisibility(
            visible = feedbackBannerMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            feedbackBannerMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .testTag("dua_action_feedback_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = DuaDeepEmerald,
                    border = BorderStroke(1.dp, DuaGoldSheen.copy(alpha = 0.7f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DuaGoldSheen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaAudioPlayerCard(
    audioSource: VerifiedQuranAudioSource,
    playbackState: QuranAudioPlaybackState,
    volumeMutedHint: Boolean,
    palette: DuaPalette,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onRetry: () -> Unit
) {
    val isThisLoading = playbackState is QuranAudioPlaybackState.Loading &&
        playbackState.activeKey == audioSource.audioKey
    val isThisPlaying = playbackState is QuranAudioPlaybackState.Playing &&
        playbackState.activeKey == audioSource.audioKey
    val isThisPaused = playbackState is QuranAudioPlaybackState.Paused &&
        playbackState.activeKey == audioSource.audioKey
    val thisError = (playbackState as? QuranAudioPlaybackState.Error)
        ?.takeIf { it.activeKey == audioSource.audioKey }

    val statusText = when {
        isThisLoading -> (playbackState as QuranAudioPlaybackState.Loading).statusBangla
        isThisPlaying -> (playbackState as QuranAudioPlaybackState.Playing).sourceLabelBangla
        isThisPaused -> "বিরতি • ${(playbackState as QuranAudioPlaybackState.Paused).sourceLabelBangla}"
        else -> audioSource.sourceLabelBangla
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dua_audio_control_bar"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = BorderStroke(
            1.dp,
            if (isThisPlaying) palette.accentGreen else palette.cardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "অডিও প্লেয়ার",
                    tint = palette.accentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "অডিও শুনুন",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = palette.titleColor
                    )
                    Text(
                        text = statusText,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = palette.mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayPause,
                    enabled = !isThisLoading,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("dua_audio_play_pause_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isThisPlaying) DuaGold else DuaPrimaryGreen,
                        contentColor = if (isThisPlaying) Color(0xFF1F1600) else Color.White
                    )
                ) {
                    if (isThisLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "লোড হচ্ছে...",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisPlaying) "বিরতি" else "শুনুন",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isThisPlaying -> "বিরতি দিন"
                                isThisPaused -> "পুনরায় চালু করুন"
                                else -> "দোয়া শুনুন"
                            },
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onReplay,
                    enabled = !isThisLoading,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dua_audio_replay_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.goldBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "পুনরায় শুনুন",
                        tint = palette.accentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "পুনরায়",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = palette.titleColor
                    )
                }
            }

            if (volumeMutedHint && isThisPlaying) {
                Text(
                    text = "পরামর্শ: ফোনের মিডিয়া ভলিউম বর্তমানে ০ আছে, ভলিউম বাড়িয়ে শুনুন।",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = palette.accentGold
                )
            }

            if (thisError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dua_audio_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFB71C1C).copy(alpha = if (palette.isDark) 0.22f else 0.09f),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "অডিও ত্রুটি",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = thisError.messageBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = palette.titleColor
                            )
                        }
                        if (thisError.canRetry) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dua_audio_retry_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF5350))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "আবার চেষ্টা করুন",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "আবার চেষ্টা করুন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE53935)
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
private fun DuaActionChipButton(
    label: String,
    icon: ImageVector,
    palette: DuaPalette,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = palette.cardSurfaceSoft,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = palette.accentGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = palette.titleColor
            )
        }
    }
}

@Composable
internal fun DuaMetadataPill(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = text,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
