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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
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
 * কুরআন শিক্ষা (تَعْلِيمُ الْقُرْآنِ) — Premium Interactive & Gamified Quran Learning Platform
 */
@Composable
fun QuranLearningAcademyScreen(
    isAdmin: Boolean = false,
    registerBackInterceptor: ((() -> Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val storage = remember { QuranLearningStorageManager.getInstance(context) }
    val palette = rememberQuranAcademyPalette()

    val completedLessonIds by storage.completedLessonIds.collectAsState()
    val totalXp by storage.totalXpPoints.collectAsState()
    val dailyGoal by storage.dailyGoalLessons.collectAsState()
    val todayCompletedCount by storage.todayCompletedCount.collectAsState()
    val currentStreak by storage.currentStreakDays.collectAsState()
    val longestStreak by storage.longestStreakDays.collectAsState()
    val lastReadLessonId by storage.lastReadLessonId.collectAsState()
    val mistakeLessonIds by storage.mistakeLessonIds.collectAsState()
    val mistakeStepsMap by storage.mistakeStepsByLesson.collectAsState()
    val revisionHistory by storage.revisionHistory.collectAsState()
    val adminApprovedIds by storage.adminApprovedLessonIds.collectAsState()

    var destination by remember {
        mutableStateOf<QuranLearningDestination>(QuranLearningDestination.AcademyHome)
    }
    var parentLevelForLesson by remember { mutableStateOf<QuranLearningLevel?>(null) }

    val lastReadLesson = remember(lastReadLessonId, adminApprovedIds) {
        storage.getLastReadLessonOrDefault()
    }
    val totalVisibleLessonsCount = remember(adminApprovedIds) {
        QuranLearningCatalog.getStudentVisibleLessons(adminApprovedIds).size
    }

    fun navigateUpInsideAcademy(): Boolean {
        return when (destination) {
            QuranLearningDestination.AcademyHome -> false
            is QuranLearningDestination.InteractiveLesson -> {
                val parentLevel = parentLevelForLesson
                destination = if (parentLevel != null) {
                    QuranLearningDestination.LevelLessonMap(parentLevel)
                } else {
                    QuranLearningDestination.AcademyHome
                }
                true
            }
            is QuranLearningDestination.LevelLessonMap,
            QuranLearningDestination.MistakesAndRevision,
            QuranLearningDestination.ProgressAndAchievements,
            QuranLearningDestination.AdminContentVerification -> {
                parentLevelForLesson = null
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
                    isAdmin = isAdmin,
                    completedCount = completedLessonIds.size,
                    totalLessonsCount = totalVisibleLessonsCount,
                    todayCompletedCount = todayCompletedCount,
                    dailyGoal = dailyGoal,
                    currentStreak = currentStreak,
                    totalXp = totalXp,
                    mistakesCount = mistakeLessonIds.size,
                    lastReadLesson = lastReadLesson,
                    onOpenLevel = { level ->
                        parentLevelForLesson = level
                        destination = QuranLearningDestination.LevelLessonMap(level)
                    },
                    onOpenMistakesAndRevision = {
                        destination = QuranLearningDestination.MistakesAndRevision
                    },
                    onOpenProgressAndAchievements = {
                        destination = QuranLearningDestination.ProgressAndAchievements
                    },
                    onOpenLastReadLesson = {
                        parentLevelForLesson = lastReadLesson.level
                        storage.recordLastReadLesson(lastReadLesson.id)
                        destination = QuranLearningDestination.InteractiveLesson(lastReadLesson.id)
                    },
                    onOpenAdminVerification = {
                        destination = QuranLearningDestination.AdminContentVerification
                    }
                )
            }

            is QuranLearningDestination.LevelLessonMap -> {
                QuranLevelLearningPathScreen(
                    level = currentDest.level,
                    completedLessonIds = completedLessonIds,
                    adminApprovedIds = adminApprovedIds,
                    storage = storage,
                    palette = palette,
                    onOpenLesson = { lesson ->
                        parentLevelForLesson = currentDest.level
                        storage.recordLastReadLesson(lesson.id)
                        destination = QuranLearningDestination.InteractiveLesson(lesson.id)
                    }
                )
            }

            is QuranLearningDestination.InteractiveLesson -> {
                val activeLesson = remember(currentDest.lessonId, adminApprovedIds) {
                    QuranLearningCatalog.getLessonById(currentDest.lessonId, adminApprovedIds)
                        ?: QuranLearningCatalog.getStudentVisibleLessons(adminApprovedIds).first()
                }
                val retryIndices = remember(currentDest.lessonId, currentDest.retryOnlyMistakes, mistakeStepsMap) {
                    if (currentDest.retryOnlyMistakes) {
                        mistakeStepsMap[currentDest.lessonId].orEmpty()
                    } else {
                        emptySet()
                    }
                }
                QuranInteractiveLessonScreen(
                    lesson = activeLesson,
                    retryStepIndices = retryIndices,
                    palette = palette,
                    storage = storage,
                    onNavigateToNextLesson = { nextId ->
                        storage.recordLastReadLesson(nextId)
                        destination = QuranLearningDestination.InteractiveLesson(nextId, retryOnlyMistakes = false)
                    },
                    onRetryMistakes = { targetLesson, _ ->
                        destination = QuranLearningDestination.InteractiveLesson(
                            lessonId = targetLesson.id,
                            retryOnlyMistakes = true
                        )
                    },
                    onExitToPath = { navigateUpInsideAcademy() }
                )
            }

            QuranLearningDestination.MistakesAndRevision -> {
                QuranMistakesAndRevisionScreen(
                    completedLessonIds = completedLessonIds,
                    mistakeLessonIds = mistakeLessonIds,
                    adminApprovedIds = adminApprovedIds,
                    storage = storage,
                    palette = palette,
                    onPracticeMistakeLesson = { lesson ->
                        parentLevelForLesson = lesson.level
                        storage.recordLastReadLesson(lesson.id)
                        destination = QuranLearningDestination.InteractiveLesson(
                            lessonId = lesson.id,
                            retryOnlyMistakes = true
                        )
                    },
                    onOpenRevisionLesson = { lesson ->
                        parentLevelForLesson = lesson.level
                        storage.recordLastReadLesson(lesson.id)
                        destination = QuranLearningDestination.InteractiveLesson(
                            lessonId = lesson.id,
                            retryOnlyMistakes = false
                        )
                    }
                )
            }

            QuranLearningDestination.ProgressAndAchievements -> {
                QuranProgressAndAchievementsScreen(
                    completedLessonIds = completedLessonIds,
                    totalXp = totalXp,
                    dailyGoal = dailyGoal,
                    todayCompletedCount = todayCompletedCount,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    revisionHistory = revisionHistory,
                    adminApprovedIds = adminApprovedIds,
                    storage = storage,
                    palette = palette,
                    onChangeDailyGoal = { newGoal -> storage.setDailyGoal(newGoal) },
                    onOpenLevel = { level ->
                        parentLevelForLesson = level
                        destination = QuranLearningDestination.LevelLessonMap(level)
                    }
                )
            }

            QuranLearningDestination.AdminContentVerification -> {
                QuranAdminVerificationScreen(
                    adminApprovedIds = adminApprovedIds,
                    palette = palette,
                    onToggleVerifyAndPublish = { lessonId ->
                        storage.toggleAdminVerifyAndPublish(lessonId)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuranAcademyHomeContent(
    palette: QuranAcademyPalette,
    isAdmin: Boolean,
    completedCount: Int,
    totalLessonsCount: Int,
    todayCompletedCount: Int,
    dailyGoal: Int,
    currentStreak: Int,
    totalXp: Int,
    mistakesCount: Int,
    lastReadLesson: QuranLesson,
    onOpenLevel: (QuranLearningLevel) -> Unit,
    onOpenMistakesAndRevision: () -> Unit,
    onOpenProgressAndAchievements: () -> Unit,
    onOpenLastReadLesson: () -> Unit,
    onOpenAdminVerification: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_learning_academy_home")
    ) {
        // স্ক্রিন উচ্চতার ~১৭-১৯% (মাঝারি, ভারসাম্যপূর্ণ Royal Header — বড় ব্যানার নয়)
        val calculatedHeaderHeight: Dp = (maxHeight * 0.18f).coerceIn(132.dp, 168.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ১. মাঝারি আকারের Royal Emerald Header
            QuranAcademyRoyalHeader(headerHeight = calculatedHeaderHeight)

            // মূল কনটেন্ট (Adaptive width)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // ২. Daily Progress, Streak ও XP ইন্ডিকেটর স্ট্রিপ
                DailyStreakAndProgressStrip(
                    todayCompletedCount = todayCompletedCount,
                    dailyGoal = dailyGoal,
                    currentStreak = currentStreak,
                    totalXp = totalXp,
                    palette = palette,
                    onClick = onOpenProgressAndAchievements
                )

                // ৩. প্রথম স্ক্রিনেই স্পষ্ট ৩টি প্রধান শিক্ষার স্তর (অপরিবর্তিত নাম, ক্রম ও আরবি নমুনা)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_primary_levels_section"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .padding(top = 4.dp),
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
                        text = "অনুশীলন ও অগ্রগতি",
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

                // ৪. সেকেন্ডারি শিক্ষামূলক সুবিধাসমূহ
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
                            title = "ভুল সংশোধন ও রিভিশন",
                            caption = if (mistakesCount > 0) {
                                "${toAcademyBanglaDigits(mistakesCount)}টি ভুল অনুশীলন"
                            } else {
                                "মডিউল ও স্তর মূল্যায়ন"
                            },
                            icon = Icons.Default.Refresh,
                            palette = palette,
                            onClick = onOpenMistakesAndRevision,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_mistakes_revision")
                        )
                        SecondaryAcademyToolTile(
                            title = "অগ্রগতি ও ব্যাজ",
                            caption = "${toAcademyBanglaDigits(completedCount)}/${toAcademyBanglaDigits(totalLessonsCount)} পাঠ সম্পন্ন",
                            icon = Icons.Default.EmojiEvents,
                            palette = palette,
                            onClick = onOpenProgressAndAchievements,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quran_tool_learning_progress")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                        if (isAdmin) {
                            SecondaryAcademyToolTile(
                                title = "পাঠ যাচাই (CMS)",
                                caption = "অ্যাডমিন প্রকাশনা প্যানেল",
                                icon = Icons.Default.AdminPanelSettings,
                                palette = palette,
                                onClick = onOpenAdminVerification,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quran_tool_admin_verify")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DailyStreakAndProgressStrip(
    todayCompletedCount: Int,
    dailyGoal: Int,
    currentStreak: Int,
    totalXp: Int,
    palette: QuranAcademyPalette,
    onClick: () -> Unit
) {
    val goalProgress = (todayCompletedCount.toFloat() / dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val stripShape = RoundedCornerShape(14.dp)

    Surface(
        shape = stripShape,
        color = palette.secondarySurfaceTop,
        border = BorderStroke(1.dp, palette.glassCardBorderGold.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("quran_daily_progress_streak_strip")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "দৈনিক স্ট্রিক",
                        tint = AcademyGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${toAcademyBanglaDigits(currentStreak)} দিন স্ট্রিক",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = palette.titleColor,
                        modifier = Modifier.testTag("home_streak_text")
                    )
                }

                Text(
                    text = "আজ: ${toAcademyBanglaDigits(todayCompletedCount)}/${toAcademyBanglaDigits(dailyGoal)} পাঠ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.accentGreen,
                    modifier = Modifier.testTag("home_daily_goal_text")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "পয়েন্ট",
                        tint = AcademyGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${toAcademyBanglaDigits(totalXp)} XP",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = palette.titleColor,
                        modifier = Modifier.testTag("home_xp_text")
                    )
                }
            }

            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AcademyGold,
                trackColor = palette.dividerColor,
                strokeCap = StrokeCap.Round
            )
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
