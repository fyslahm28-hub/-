package com.example.ui.quranlearning

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HindSiliguriFontFamily
import java.util.Locale

/**
 * স্তরভিত্তিক পাঠ তালিকা (প্রাথমিক শিক্ষা / মধ্যবর্তী শিক্ষা / উচ্চ শিক্ষা)
 */
@Composable
fun QuranLevelLessonsScreen(
    level: QuranLearningLevel,
    completedLessonIds: Set<String>,
    palette: QuranAcademyPalette,
    onOpenLesson: (QuranLesson) -> Unit
) {
    val lessons = remember(level) { QuranLearningCatalog.getLessonsForLevel(level) }
    val completedCount = remember(lessons, completedLessonIds) {
        lessons.count { completedLessonIds.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_level_lessons_screen_${level.id}"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // স্তরের রাজকীয় পরিচিতি ব্যানার
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald)
                        )
                    )
                    .border(1.dp, AcademyGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .drawBehind {
                        drawIslamicGeometricPattern(
                            lineColor = Color.White.copy(alpha = 0.05f),
                            accentColor = AcademyGold.copy(alpha = 0.075f),
                            cellSizeDp = 46f
                        )
                    }
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = level.titleBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color(0xFFF6FBF8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = level.descriptionBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontSize = 13.sp,
                                color = Color(0xFFD2ECE2)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = level.arabicSample,
                                fontFamily = QuranAcademyArabicFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = AcademyGoldSheen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পাঠ সম্পন্ন: ${toAcademyBanglaDigits(completedCount)} / ${toAcademyBanglaDigits(lessons.size)}",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = AcademyGoldSheen
                        )
                        Text(
                            text = "${toAcademyBanglaDigits((completedCount * 100) / lessons.size.coerceAtLeast(1))}%",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (completedCount.toFloat() / lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AcademyGold,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                }
            }
        }

        items(lessons, key = { it.id }) { lesson ->
            val isCompleted = completedLessonIds.contains(lesson.id)
            val cardShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.glassCardTop, palette.glassCardBottom)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) AcademyGold.copy(alpha = 0.65f) else palette.glassCardBorderGreen,
                        shape = cardShape
                    )
                    .clickable { onOpenLesson(lesson) }
                    .padding(15.dp)
                    .testTag("quran_lesson_item_${lesson.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = if (isCompleted) AcademyPrimaryGreen else palette.arabicChamberBg,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.55f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "সম্পন্ন",
                                    tint = AcademyGoldSheen,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = lesson.lessonNumberBangla.removePrefix("পাঠ "),
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = palette.titleColor
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${lesson.lessonNumberBangla} • ${lesson.titleBangla}",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = palette.titleColor
                        )
                        Text(
                            text = lesson.arabicHeading,
                            fontFamily = QuranAcademyArabicFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            color = palette.arabicSampleColor
                        )
                        Text(
                            text = lesson.summaryBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = palette.subtitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "পাঠ পড়ুন",
                        tint = palette.navIndicatorIcon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * ইন্টারঅ্যাক্টিভ পাঠ রিডার (সম্পূর্ণ ব্যাখ্যা, আরবি উদাহরণ, মূল নিয়ম, অনুশীলন ও সম্পন্ন চিহ্নিতকরণ)
 */
@Composable
fun QuranLessonReaderScreen(
    lesson: QuranLesson,
    isCompleted: Boolean,
    palette: QuranAcademyPalette,
    onToggleCompleted: () -> Unit,
    onNavigateToLesson: (QuranLesson) -> Unit
) {
    val levelLessons = remember(lesson.level) { QuranLearningCatalog.getLessonsForLevel(lesson.level) }
    val currentIndex = remember(lesson.id, levelLessons) { levelLessons.indexOfFirst { it.id == lesson.id } }
    val prevLesson = levelLessons.getOrNull(currentIndex - 1)
    val nextLesson = levelLessons.getOrNull(currentIndex + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("quran_lesson_reader_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // পাঠের শীর্ষ কার্ড
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald)
                    )
                )
                .border(1.2.dp, AcademyGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = AcademyGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${lesson.level.titleBangla} • ${lesson.lessonNumberBangla}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AcademyGoldSheen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = lesson.arabicHeading,
                    fontFamily = QuranAcademyArabicFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 42.sp,
                    color = AcademyGoldSheen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = lesson.titleBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF6FBF8),
                    textAlign = TextAlign.Center
                )
            }
        }

        // বিস্তারিত ব্যাখ্যা
        SoftEmeraldInfoCard(
            title = "পাঠের মূল বিবরণ",
            palette = palette
        ) {
            Text(
                text = lesson.explanationBangla,
                fontFamily = HindSiliguriFontFamily,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = palette.titleColor
            )
        }

        // আরবি উদাহরণসমূহ
        SoftEmeraldInfoCard(
            title = "কুরআনী ও আরবি উদাহরণ",
            palette = palette
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lesson.examples.forEachIndexed { idx, ex ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.arabicChamberBg)
                            .border(1.dp, palette.arabicChamberBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = ex.arabic,
                            fontFamily = QuranAcademyArabicFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 38.sp,
                            color = palette.arabicSampleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = ex.pronunciationBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.titleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = ex.noteBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = palette.subtitleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (idx < lesson.examples.lastIndex) {
                        HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // মূল নিয়ম ও অনুশীলনের পরামর্শ
        SoftEmeraldInfoCard(
            title = "মনে রাখার জরুরি নিয়ম ও অনুশীলন",
            palette = palette
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = AcademyGold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = lesson.keyRuleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = palette.titleColor
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = palette.accentGreen,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = lesson.practiceTipBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = palette.subtitleColor
                    )
                }
            }
        }

        // পাঠ সম্পন্ন বাটন
        Button(
            onClick = onToggleCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("quran_lesson_mark_complete_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) AcademyGold else AcademyPrimaryGreen
            )
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isCompleted) AcademyRoyalEmerald else Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCompleted) "পাঠ সম্পন্ন হয়েছে" else "পাঠ সম্পন্ন চিহ্নিত করুন",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isCompleted) AcademyRoyalEmerald else Color.White
            )
        }

        // আগের পাঠ / পরের পাঠ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { prevLesson?.let(onNavigateToLesson) },
                enabled = prevLesson != null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quran_lesson_prev_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (prevLesson != null) palette.accentGreen else palette.dividerColor)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "আগের পাঠ",
                    tint = if (prevLesson != null) palette.accentGreen else palette.mutedColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "আগের পাঠ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (prevLesson != null) palette.accentGreen else palette.mutedColor
                )
            }

            Button(
                onClick = { nextLesson?.let(onNavigateToLesson) },
                enabled = nextLesson != null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quran_lesson_next_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcademyPrimaryGreen,
                    disabledContainerColor = palette.dividerColor
                )
            ) {
                Text(
                    text = "পরের পাঠ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "পরের পাঠ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * ৪.১ শুনে শিখুন (Listen & Learn Interactive Phonetic Lab)
 */
@Composable
fun QuranListenAndLearnScreen(
    palette: QuranAcademyPalette
) {
    val context = LocalContext.current
    var activeItemId by remember { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsStatusMessage by remember { mutableStateOf<String?>(null) }

    val ttsEngine = remember {
        var engine: TextToSpeech? = null
        try {
            engine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = engine?.setLanguage(Locale.forLanguageTag("ar-SA"))
                    ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                    engine?.setSpeechRate(0.75f)
                }
            }
        } catch (_: Throwable) {
            ttsReady = false
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                ttsEngine?.stop()
                ttsEngine?.shutdown()
            } catch (_: Throwable) {
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_listen_learn_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoftEmeraldInfoCard(title = "শুনে শিখুন — উচ্চারণ ও মাখরাজ অনুশীলন", palette = palette) {
                Text(
                    text = "প্রতিটি আরবি উদাহরণের স্পিকার বাটনে চাপ দিয়ে উচ্চারণ শুনুন এবং নিচের তাজবীদ নির্দেশনা অনুযায়ী মিলিয়ে অনুশীলন করুন।",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = palette.subtitleColor
                )
                if (!ttsStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ttsStatusMessage.orEmpty(),
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = palette.accentGreen
                    )
                }
            }
        }

        items(QuranLearningCatalog.listeningExercises, key = { it.id }) { item ->
            val isActive = activeItemId == item.id
            val cardShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.glassCardTop, palette.glassCardBottom)
                        )
                    )
                    .border(
                        width = if (isActive) 1.4.dp else 1.dp,
                        color = if (isActive) AcademyGold else palette.glassCardBorderGreen,
                        shape = cardShape
                    )
                    .padding(14.dp)
                    .testTag("quran_listen_card_${item.id}")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = palette.arabicChamberBg,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, palette.arabicChamberBorder)
                        ) {
                            Text(
                                text = "${item.level.titleBangla} • ${item.categoryBangla}",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = palette.titleColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                activeItemId = item.id
                                if (ttsReady && ttsEngine != null) {
                                    try {
                                        ttsEngine.speak(
                                            item.arabicText,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            item.id
                                        )
                                        ttsStatusMessage = "চালু হয়েছে: ${item.phoneticBangla}"
                                    } catch (_: Throwable) {
                                        ttsStatusMessage = "উচ্চারণ নির্দেশিকা: ${item.phoneticBangla}"
                                    }
                                } else {
                                    ttsStatusMessage = "ডিভাইসে আরবি ভয়েস ডেটা না থাকায় বাংলা উচ্চারণ নির্দেশিকা দেখানো হচ্ছে: ${item.phoneticBangla}"
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AcademyPrimaryGreen)
                                .testTag("quran_listen_play_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "শুনুন",
                                tint = AcademyGoldSheen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = item.arabicText,
                        fontFamily = QuranAcademyArabicFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 40.sp,
                        color = palette.arabicSampleColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.arabicChamberBg)
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                    )

                    Text(
                        text = "উচ্চারণ: ${item.phoneticBangla}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = palette.titleColor
                    )
                    Text(
                        text = item.makhrajRuleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = palette.subtitleColor
                    )
                }
            }
        }
    }
}

/**
 * ৪.২ অনুশীলন ও পরীক্ষা (Interactive Practice & Quiz)
 */
@Composable
fun QuranPracticeAndQuizScreen(
    bestScore: Int,
    palette: QuranAcademyPalette,
    onQuizCompleted: (Int) -> Unit
) {
    val questions = remember { QuranLearningCatalog.quizQuestions }
    var currentQuestionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedOptionIndex by rememberSaveable { mutableIntStateOf(-1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var quizFinished by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("quran_practice_quiz_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (quizFinished) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald)
                        )
                    )
                    .border(1.4.dp, AcademyGold, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AcademyGoldSheen,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "মাশাআল্লাহ! পরীক্ষা সম্পন্ন হয়েছে",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "আপনার স্কোর: ${toAcademyBanglaDigits(score)} / ${toAcademyBanglaDigits(questions.size)}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = AcademyGoldSheen,
                        modifier = Modifier.testTag("quran_quiz_final_score")
                    )
                    Text(
                        text = "সর্বোচ্চ সংরক্ষিত স্কোর: ${toAcademyBanglaDigits(maxOf(bestScore, score))} / ${toAcademyBanglaDigits(questions.size)}",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFFD5EFE5)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            currentQuestionIndex = 0
                            selectedOptionIndex = -1
                            score = 0
                            quizFinished = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AcademyGold),
                        modifier = Modifier.testTag("quran_quiz_restart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AcademyRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "পুনরায় অনুশীলন করুন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = AcademyRoyalEmerald
                        )
                    }
                }
            }
        } else {
            val question = questions[currentQuestionIndex]
            val hasAnswered = selectedOptionIndex != -1

            // প্রশ্ন অগ্রগতি বার
            SoftEmeraldInfoCard(
                title = "প্রশ্ন ${toAcademyBanglaDigits(currentQuestionIndex + 1)} / ${toAcademyBanglaDigits(questions.size)} • ${question.level.titleBangla}",
                palette = palette
            ) {
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = AcademyPrimaryGreen,
                    trackColor = palette.dividerColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (!question.arabicPrompt.isNullOrBlank()) {
                    Text(
                        text = question.arabicPrompt,
                        fontFamily = QuranAcademyArabicFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 42.sp,
                        color = palette.arabicSampleColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.arabicChamberBg)
                            .border(1.dp, palette.arabicChamberBorder, RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = question.questionBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = palette.titleColor
                )
            }

            // অপশনসমূহ
            question.options.forEachIndexed { idx, optionText ->
                val isSelected = selectedOptionIndex == idx
                val isCorrectOption = idx == question.correctOptionIndex
                val borderColor = when {
                    !hasAnswered -> palette.glassCardBorderGreen
                    isCorrectOption -> AcademyPrimaryGreen
                    isSelected -> Color(0xFFC62828)
                    else -> palette.dividerColor
                }
                val bgColor = when {
                    !hasAnswered -> palette.glassCardTop
                    isCorrectOption -> AcademyPrimaryGreen.copy(alpha = if (palette.isDark) 0.35f else 0.16f)
                    isSelected -> Color(0xFFC62828).copy(alpha = 0.12f)
                    else -> palette.secondarySurfaceBottom
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !hasAnswered) {
                            selectedOptionIndex = idx
                            if (idx == question.correctOptionIndex) {
                                score += 1
                            }
                        }
                        .testTag("quran_quiz_option_$idx"),
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected || (hasAnswered && isCorrectOption)) 1.5.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${toAcademyBanglaDigits(idx + 1)}.",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.accentGreen
                        )
                        Text(
                            text = optionText,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = palette.titleColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (hasAnswered) {
                SoftEmeraldInfoCard(
                    title = if (selectedOptionIndex == question.correctOptionIndex) "সঠিক উত্তর!" else "সঠিক ব্যাখ্যা",
                    palette = palette
                ) {
                    Text(
                        text = question.explanationBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = palette.titleColor
                    )
                }

                Button(
                    onClick = {
                        if (currentQuestionIndex < questions.lastIndex) {
                            currentQuestionIndex += 1
                            selectedOptionIndex = -1
                        } else {
                            onQuizCompleted(score)
                            quizFinished = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quran_quiz_next_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcademyPrimaryGreen)
                ) {
                    Text(
                        text = if (currentQuestionIndex < questions.lastIndex) "পরবর্তী প্রশ্ন" else "ফলাফল দেখুন",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * ৪.৩ আমার শেখার অগ্রগতি (My Learning Progress Dashboard)
 */
@Composable
fun QuranLearningProgressScreen(
    completedLessonIds: Set<String>,
    bestQuizScore: Int,
    totalQuizAttempts: Int,
    lastReadLesson: QuranLesson,
    palette: QuranAcademyPalette,
    onOpenLevel: (QuranLearningLevel) -> Unit,
    onOpenLastLesson: () -> Unit
) {
    val totalLessons = QuranLearningCatalog.allLessons.size
    val totalCompleted = completedLessonIds.size.coerceAtMost(totalLessons)
    val overallPercent = (totalCompleted * 100) / totalLessons.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("quran_learning_progress_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // সামগ্রিক অগ্রগতি ব্যানার
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald)
                    )
                )
                .border(1.2.dp, AcademyGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "আমার শেখার সামগ্রিক অগ্রগতি",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট সম্পন্ন পাঠ: ${toAcademyBanglaDigits(totalCompleted)} / ${toAcademyBanglaDigits(totalLessons)}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = AcademyGoldSheen
                    )
                    Text(
                        text = "${toAcademyBanglaDigits(overallPercent)}%",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { (totalCompleted.toFloat() / totalLessons.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = AcademyGold,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text(
                    text = "অনুশীলন পরীক্ষায় সর্বোচ্চ স্কোর: ${toAcademyBanglaDigits(bestQuizScore)} / ${toAcademyBanglaDigits(QuranLearningCatalog.quizQuestions.size)} (অংশগ্রহণ: ${toAcademyBanglaDigits(totalQuizAttempts)} বার)",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 12.5.sp,
                    color = Color(0xFFD5EFE5)
                )
            }
        }

        // তিনটি স্তরের আলাদা অগ্রগতি
        QuranLearningLevel.entries.forEach { level ->
            val levelLessons = QuranLearningCatalog.getLessonsForLevel(level)
            val done = levelLessons.count { completedLessonIds.contains(it.id) }
            val shape = RoundedCornerShape(15.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(palette.glassCardTop, palette.glassCardBottom)))
                    .border(1.dp, palette.glassCardBorderGreen, shape)
                    .clickable { onOpenLevel(level) }
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = level.titleBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = palette.titleColor
                        )
                        Text(
                            text = "${toAcademyBanglaDigits(done)} / ${toAcademyBanglaDigits(levelLessons.size)} পাঠ",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.accentGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (done.toFloat() / levelLessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AcademyGold,
                        trackColor = palette.dividerColor
                    )
                }
            }
        }

        // সর্বশেষ পড়া পাঠ থেকে পুনরায় শুরু
        SoftEmeraldInfoCard(title = "সর্বশেষ পড়া পাঠ", palette = palette) {
            Text(
                text = "${lastReadLesson.level.titleBangla} • ${lastReadLesson.lessonNumberBangla}",
                fontFamily = HindSiliguriFontFamily,
                fontSize = 12.sp,
                color = palette.accentGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = lastReadLesson.titleBangla,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = palette.titleColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenLastLesson,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcademyPrimaryGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "এই পাঠ থেকে চালিয়ে যান",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SoftEmeraldInfoCard(
    title: String,
    palette: QuranAcademyPalette,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(palette.glassCardTop, palette.glassCardBottom)
                )
            )
            .border(1.dp, palette.glassCardBorderGreen, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.accentGreen
        )
        content()
    }
}
