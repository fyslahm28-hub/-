package com.example.ui.quranlearning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HindSiliguriFontFamily

/**
 * কুরআন শিক্ষা (تَعْلِيمُ الْقُرْآنِ) — Ultra-Premium Islamic Learning Academy Interface
 */
@Composable
fun QuranLearningAcademyScreen(
    registerBackInterceptor: ((() -> Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val storage = remember { QuranLearningStorageManager.getInstance(context) }
    val palette = rememberQuranAcademyPalette()

    val completedLessonIds by storage.completedLessonIds.collectAsState()
    val lastReadLessonId by storage.lastReadLessonId.collectAsState()
    val bestQuizScore by storage.bestQuizScore.collectAsState()
    val totalQuizAttempts by storage.totalQuizAttempts.collectAsState()

    var destination by remember {
        mutableStateOf<QuranLearningDestination>(QuranLearningDestination.AcademyHome)
    }
    var previousLevelForLesson by remember { mutableStateOf<QuranLearningLevel?>(null) }

    val lastReadLesson = remember(lastReadLessonId) {
        storage.getLastReadLessonOrDefault()
    }

    fun navigateUpInsideAcademy(): Boolean {
        return when (val current = destination) {
            QuranLearningDestination.AcademyHome -> false
            is QuranLearningDestination.LessonReader -> {
                val parentLevel = previousLevelForLesson
                destination = if (parentLevel != null) {
                    QuranLearningDestination.LevelLessons(parentLevel)
                } else {
                    QuranLearningDestination.AcademyHome
                }
                true
            }
            is QuranLearningDestination.LevelLessons,
            QuranLearningDestination.ListenAndLearn,
            QuranLearningDestination.PracticeAndQuiz,
            QuranLearningDestination.LearningProgress -> {
                previousLevelForLesson = null
                destination = QuranLearningDestination.AcademyHome
                true
            }
        }
    }

    DisposableEffect(destination) {
        registerBackInterceptor?.invoke { navigateUpInsideAcademy() }
        onDispose {
            registerBackInterceptor?.invoke { false }
        }
    }

    BackHandler(enabled = destination != QuranLearningDestination.AcademyHome) {
        navigateUpInsideAcademy()
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val isForward = targetState != QuranLearningDestination.AcademyHome
            if (isForward) {
                (slideInHorizontally(tween(240)) { it / 6 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 8 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(240)) { -it / 6 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { it / 8 } + fadeOut(tween(180)))
            }
        },
        label = "quran_academy_navigation_transition"
    ) { currentDest ->
        when (currentDest) {
            QuranLearningDestination.AcademyHome -> {
                QuranAcademyHomeContent(
                    palette = palette,
                    completedCount = completedLessonIds.size,
                    totalLessonsCount = QuranLearningCatalog.allLessons.size,
                    lastReadLesson = lastReadLesson,
                    onOpenLevel = { level ->
                        previousLevelForLesson = level
                        destination = QuranLearningDestination.LevelLessons(level)
                    },
                    onOpenListenAndLearn = {
                        destination = QuranLearningDestination.ListenAndLearn
                    },
                    onOpenPracticeAndQuiz = {
                        destination = QuranLearningDestination.PracticeAndQuiz
                    },
                    onOpenLearningProgress = {
                        destination = QuranLearningDestination.LearningProgress
                    },
                    onOpenLastReadLesson = {
                        previousLevelForLesson = null
                        storage.recordLastReadLesson(lastReadLesson.id)
                        destination = QuranLearningDestination.LessonReader(lastReadLesson.id)
                    }
                )
            }

            is QuranLearningDestination.LevelLessons -> {
                QuranLevelLessonsScreen(
                    level = currentDest.level,
                    completedLessonIds = completedLessonIds,
                    palette = palette,
                    onOpenLesson = { lesson ->
                        previousLevelForLesson = currentDest.level
                        storage.recordLastReadLesson(lesson.id)
                        destination = QuranLearningDestination.LessonReader(lesson.id)
                    }
                )
            }

            is QuranLearningDestination.LessonReader -> {
                val activeLesson = remember(currentDest.lessonId) {
                    QuranLearningCatalog.getLessonById(currentDest.lessonId)
                        ?: QuranLearningCatalog.allLessons.first()
                }
                QuranLessonReaderScreen(
                    lesson = activeLesson,
                    isCompleted = completedLessonIds.contains(activeLesson.id),
                    palette = palette,
                    onToggleCompleted = { storage.toggleLessonCompleted(activeLesson.id) },
                    onNavigateToLesson = { targetLesson ->
                        storage.recordLastReadLesson(targetLesson.id)
                        destination = QuranLearningDestination.LessonReader(targetLesson.id)
                    }
                )
            }

            QuranLearningDestination.ListenAndLearn -> {
                QuranListenAndLearnScreen(palette = palette)
            }

            QuranLearningDestination.PracticeAndQuiz -> {
                QuranPracticeAndQuizScreen(
                    bestScore = bestQuizScore,
                    palette = palette,
                    onQuizCompleted = { score -> storage.recordQuizScore(score) }
                )
            }

            QuranLearningDestination.LearningProgress -> {
                QuranLearningProgressScreen(
                    completedLessonIds = completedLessonIds,
                    bestQuizScore = bestQuizScore,
                    totalQuizAttempts = totalQuizAttempts,
                    lastReadLesson = lastReadLesson,
                    palette = palette,
                    onOpenLevel = { level ->
                        previousLevelForLesson = level
                        destination = QuranLearningDestination.LevelLessons(level)
                    },
                    onOpenLastLesson = {
                        previousLevelForLesson = null
                        storage.recordLastReadLesson(lastReadLesson.id)
                        destination = QuranLearningDestination.LessonReader(lastReadLesson.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuranAcademyHomeContent(
    palette: QuranAcademyPalette,
    completedCount: Int,
    totalLessonsCount: Int,
    lastReadLesson: QuranLesson,
    onOpenLevel: (QuranLearningLevel) -> Unit,
    onOpenListenAndLearn: () -> Unit,
    onOpenPracticeAndQuiz: () -> Unit,
    onOpenLearningProgress: () -> Unit,
    onOpenLastReadLesson: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_learning_academy_home")
    ) {
        // স্ক্রিন উচ্চতার ১৮–২০% (মাঝারি, ভারসাম্যপূর্ণ উচ্চতা)
        val calculatedHeaderHeight: Dp = (maxHeight * 0.19f).coerceIn(138.dp, 176.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ২. Royal Emerald Geometric Header
            QuranAcademyRoyalHeader(headerHeight = calculatedHeaderHeight)

            // মূল কনটেন্ট (Adaptive width for phones, foldables & tablets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ৩. তিনটি প্রধান শিক্ষার স্তর (মূল আকর্ষণ — অপরিবর্তিত নাম, ক্রম ও কাঠামো)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_primary_levels_section"),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    QuranLearningLevel.entries.forEach { level ->
                        QuranPrimaryLevelGlassCard(
                            level = level,
                            palette = palette,
                            onClick = { onOpenLevel(level) }
                        )
                    }
                }

                // সূক্ষ্ম সোনালি-সবুজ বিভাজক
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, palette.dividerColor)
                                )
                            )
                    )
                    Text(
                        text = "অন্যান্য শিক্ষামূলক সুবিধা",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = palette.mutedColor
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.dividerColor, Color.Transparent)
                                )
                            )
                    )
                }

                // ৪. অন্যান্য শিক্ষামূলক সুবিধা (ছোট ও পরিপাটি ২x২ সেকশন — প্রধান তিনটি স্তরের গুরুত্ব না কমিয়ে)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_secondary_tools_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryAcademyToolTile(
                            title = "শুনে শিখুন",
                            caption = "মাখরাজ ও উচ্চারণ",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            palette = palette,
                            onClick = onOpenListenAndLearn,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_listen_learn")
                        )
                        SecondaryAcademyToolTile(
                            title = "অনুশীলন ও পরীক্ষা",
                            caption = "তাজবীদ যাচাই কুইজ",
                            icon = Icons.Default.Quiz,
                            palette = palette,
                            onClick = onOpenPracticeAndQuiz,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_practice_quiz")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryAcademyToolTile(
                            title = "আমার শেখার অগ্রগতি",
                            caption = "${toAcademyBanglaDigits(completedCount)} / ${toAcademyBanglaDigits(totalLessonsCount)} পাঠ সম্পন্ন",
                            icon = Icons.Default.BookmarkAdded,
                            palette = palette,
                            onClick = onOpenLearningProgress,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_learning_progress")
                        )
                        SecondaryAcademyToolTile(
                            title = "সর্বশেষ পড়া পাঠ",
                            caption = lastReadLesson.titleBangla,
                            icon = Icons.Default.HistoryEdu,
                            palette = palette,
                            onClick = onOpenLastReadLesson,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_last_read")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SecondaryAcademyToolTile(
    title: String,
    caption: String,
    icon: ImageVector,
    palette: QuranAcademyPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "secondary_tile_scale"
    )
    val tileShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 64.dp)
            .clip(tileShape)
            .background(
                Brush.linearGradient(
                    listOf(palette.secondarySurfaceTop, palette.secondarySurfaceBottom)
                )
            )
            .border(1.dp, palette.glassCardBorderGreen, tileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AcademyGold.copy(alpha = 0.22f)),
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(palette.arabicChamberBg)
                .border(0.8.dp, palette.arabicChamberBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = palette.accentGreen,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = palette.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = caption,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = palette.mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
