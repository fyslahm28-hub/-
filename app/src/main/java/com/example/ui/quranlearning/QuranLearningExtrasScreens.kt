package com.example.ui.quranlearning

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HindSiliguriFontFamily

/**
 * ১. ভুল সংশোধন, মডিউল রিভিশন ও চূড়ান্ত মূল্যায়ন স্ক্রিন (Mistakes Practice + Revision & Level Tests)
 */
@Composable
fun QuranMistakesAndRevisionScreen(
    completedLessonIds: Set<String>,
    mistakeLessonIds: Set<String>,
    adminApprovedIds: Set<String>,
    storage: QuranLearningStorageManager,
    palette: QuranAcademyPalette,
    onPracticeMistakeLesson: (QuranLesson) -> Unit,
    onOpenRevisionLesson: (QuranLesson) -> Unit
) {
    val mistakeLessons = remember(mistakeLessonIds, adminApprovedIds) {
        mistakeLessonIds.mapNotNull { QuranLearningCatalog.getLessonById(it, adminApprovedIds) }
    }
    val revisionAndAssessments = remember(adminApprovedIds) {
        QuranLearningCatalog.getAllRevisionAndAssessmentLessons(adminApprovedIds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_mistakes_revision_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // শীর্ষ পরিচিতি কার্ড
            item(key = "revision_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen))
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "ভুল সংশোধন ও রিভিশন কেন্দ্র",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "পূর্বে ভুল হওয়া পাঠগুলো পুনরায় অনুশীলন করুন এবং প্রতিটি মডিউল ও স্তরের রিভিশন মূল্যায়ন সম্পন্ন করুন।",
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFD2EDE2)
                        )
                    }
                }
            }

            // ভুল অনুশীলন বিভাগ
            item(key = "mistakes_header") {
                Text(
                    text = "ভুল অনুশীলন তালিকা (${toAcademyBanglaDigits(mistakeLessons.size)})",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.titleColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (mistakeLessons.isEmpty()) {
                item(key = "no_mistakes_card") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.secondarySurfaceTop,
                        border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_mistakes_empty_state")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "কোনো ভুল নেই",
                                tint = AcademyPrimaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "আলহামদুলিল্লাহ! কোনো অমীমাংসিত ভুল নেই",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = palette.titleColor
                                )
                                Text(
                                    text = "যেকোনো পাঠে ভুল উত্তর হলে সেটি স্বয়ংক্রিয়ভাবে এখানে পুনঃঅনুশীলনের জন্য জমা হবে।",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = palette.mutedColor
                                )
                            }
                        }
                    }
                }
            } else {
                items(mistakeLessons, key = { "mistake_${it.id}" }) { lesson ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.secondarySurfaceTop,
                        border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mistake_lesson_card_${lesson.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${lesson.level.titleBangla} • ${lesson.lessonNumberBangla}",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = AcademyGold
                                )
                                Text(
                                    text = lesson.titleBangla,
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = palette.titleColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { onPracticeMistakeLesson(lesson) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcademyPrimaryGreen,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("practice_mistake_btn_${lesson.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "সংশোধন করুন",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "অনুশীলন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // মডিউল রিভিশন ও চূড়ান্ত স্তর মূল্যায়ন তালিকা
            item(key = "assessments_header") {
                Text(
                    text = "মডিউল রিভিশন ও চূড়ান্ত মূল্যায়ন (${toAcademyBanglaDigits(revisionAndAssessments.size)}টি)",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.titleColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(revisionAndAssessments, key = { "rev_${it.id}" }) { lesson ->
                val unlocked = storage.isLessonUnlocked(lesson, completedLessonIds, adminApprovedIds)
                val completed = completedLessonIds.contains(lesson.id)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.secondarySurfaceTop,
                    border = BorderStroke(
                        1.dp,
                        when {
                            completed -> AcademyPrimaryGreen.copy(alpha = 0.6f)
                            lesson.isFinalLevelAssessment -> AcademyGold.copy(alpha = 0.6f)
                            else -> palette.glassCardBorderGreen
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = unlocked) { onOpenRevisionLesson(lesson) }
                        .testTag("revision_test_card_${lesson.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completed -> AcademyPrimaryGreen
                                        unlocked -> AcademyGold.copy(alpha = 0.22f)
                                        else -> palette.secondarySurfaceBottom
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    completed -> Icons.Default.Verified
                                    !unlocked -> Icons.Default.Lock
                                    lesson.isFinalLevelAssessment -> Icons.Default.EmojiEvents
                                    else -> Icons.Default.Star
                                },
                                contentDescription = lesson.titleBangla,
                                tint = when {
                                    completed -> Color.White
                                    unlocked -> AcademyGold
                                    else -> palette.mutedColor
                                },
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${lesson.level.titleBangla} • ${if (lesson.isFinalLevelAssessment) "চূড়ান্ত মূল্যায়ন" else "মডিউল রিভিশন"}",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = if (lesson.isFinalLevelAssessment) AcademyGold else palette.accentGreen
                            )
                            Text(
                                text = lesson.titleBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (unlocked) palette.titleColor else palette.mutedColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!unlocked) {
                                Text(
                                    text = "পূর্ববর্তী পাঠগুলো সম্পন্ন হলে আনলক হবে",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontSize = 11.sp,
                                    color = palette.mutedColor
                                )
                            }
                        }

                        if (unlocked) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "শুরু করুন",
                                tint = palette.accentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            item(key = "rev_bottom_space") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/**
 * ২. অগ্রগতি, দৈনিক লক্ষ্য, স্ট্রিক, অর্জন ব্যাজ ও রিভিশন হিস্ট্রি স্ক্রিন
 */
@Composable
fun QuranProgressAndAchievementsScreen(
    completedLessonIds: Set<String>,
    totalXp: Int,
    dailyGoal: Int,
    todayCompletedCount: Int,
    currentStreak: Int,
    longestStreak: Int,
    revisionHistory: List<RevisionHistoryEntry>,
    adminApprovedIds: Set<String>,
    storage: QuranLearningStorageManager,
    palette: QuranAcademyPalette,
    onChangeDailyGoal: (Int) -> Unit,
    onOpenLevel: (QuranLearningLevel) -> Unit
) {
    val allVisible = remember(adminApprovedIds) {
        QuranLearningCatalog.getStudentVisibleLessons(adminApprovedIds)
    }
    val totalLessons = allVisible.size
    val completedTotal = completedLessonIds.count { id -> allVisible.any { it.id == id } }
    val overallPercent = if (totalLessons == 0) 0 else (completedTotal * 100) / totalLessons
    val activePair = remember(completedLessonIds, adminApprovedIds) {
        storage.getCurrentActiveLevelAndModule()
    }
    val badges = remember(completedLessonIds, currentStreak, totalXp, adminApprovedIds) {
        storage.getAchievementBadges(completedLessonIds, currentStreak, totalXp, adminApprovedIds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_progress_achievements_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // শীর্ষ সামগ্রিক অগ্রগতি ও স্ট্রিক কার্ড
            item(key = "progress_hero") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald))
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "আমার কুরআন শিক্ষার অগ্রগতি",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "বর্তমান ধাপ: ${activePair.first.titleBangla} • ${activePair.second.moduleNumberBangla}",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontSize = 12.sp,
                                    color = AcademyGoldSheen
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = AcademyGold.copy(alpha = 0.22f),
                                border = BorderStroke(1.2.dp, AcademyGold)
                            ) {
                                Text(
                                    text = "${toAcademyBanglaDigits(overallPercent)}%",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AcademyGoldSheen,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProgressStatMiniPill(
                                label = "সম্পন্ন পাঠ",
                                value = "${toAcademyBanglaDigits(completedTotal)}/${toAcademyBanglaDigits(totalLessons)}",
                                modifier = Modifier.weight(1f)
                            )
                            ProgressStatMiniPill(
                                label = "দৈনিক স্ট্রিক",
                                value = "${toAcademyBanglaDigits(currentStreak)} দিন",
                                modifier = Modifier.weight(1f)
                            )
                            ProgressStatMiniPill(
                                label = "মোট পয়েন্ট",
                                value = "${toAcademyBanglaDigits(totalXp)} XP",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // দৈনিক লক্ষ্য (Daily Goal Selector)
            item(key = "daily_goal_card") {
                val goalProgress = (todayCompletedCount.toFloat() / dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.secondarySurfaceTop,
                    border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_goal_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "দৈনিক লক্ষ্য",
                                    tint = AcademyGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "দৈনিক শিক্ষার লক্ষ্য",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = palette.titleColor
                                )
                            }
                            Text(
                                text = "সর্বোচ্চ স্ট্রিক: ${toAcademyBanglaDigits(longestStreak)} দিন",
                                fontFamily = HindSiliguriFontFamily,
                                fontSize = 11.5.sp,
                                color = palette.mutedColor
                            )
                        }

                        Text(
                            text = "আজ সম্পন্ন: ${toAcademyBanglaDigits(todayCompletedCount)} / ${toAcademyBanglaDigits(dailyGoal)} পাঠ",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = palette.accentGreen
                        )

                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = AcademyGold,
                            trackColor = palette.dividerColor,
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "সহজ (১ পাঠ)", 3 to "আদর্শ (৩ পাঠ)", 5 to "নিষ্ঠাবান (৫ পাঠ)").forEach { (goal, label) ->
                                val selected = dailyGoal == goal
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) AcademyPrimaryGreen else palette.secondarySurfaceBottom,
                                    border = BorderStroke(1.dp, if (selected) AcademyGold else palette.dividerColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onChangeDailyGoal(goal) }
                                        .testTag("daily_goal_option_$goal")
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = if (selected) Color.White else palette.titleColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // স্তরভিত্তিক অগ্রগতি
            item(key = "level_breakdown_header") {
                Text(
                    text = "স্তরভিত্তিক অগ্রগতি",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.titleColor
                )
            }

            items(QuranLearningLevel.entries, key = { "lvl_prog_${it.id}" }) { level ->
                val lvlLessons = QuranLearningCatalog.getLessonsForLevel(level, adminApprovedIds)
                val lvlDone = lvlLessons.count { completedLessonIds.contains(it.id) }
                val lvlProg = if (lvlLessons.isEmpty()) 0f else lvlDone.toFloat() / lvlLessons.size.toFloat()

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.secondarySurfaceTop,
                    border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLevel(level) }
                ) {
                    Column(
                        modifier = Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = level.titleBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = palette.titleColor
                            )
                            Text(
                                text = "${toAcademyBanglaDigits(lvlDone)} / ${toAcademyBanglaDigits(lvlLessons.size)} পাঠ (${toAcademyBanglaDigits((lvlProg * 100).toInt())}%)",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = palette.accentGreen
                            )
                        }
                        LinearProgressIndicator(
                            progress = { lvlProg },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AcademyPrimaryGreen,
                            trackColor = palette.dividerColor,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // অর্জন ব্যাজসমূহ (Achievement Badges)
            item(key = "badges_header") {
                val unlockedCount = badges.count { it.isUnlocked }
                Text(
                    text = "অর্জন ব্যাজসমূহ (${toAcademyBanglaDigits(unlockedCount)}/${toAcademyBanglaDigits(badges.size)})",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.titleColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "badges_grid") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("achievement_badges_grid")
                ) {
                    badges.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (badge.isUnlocked) {
                                        AcademyGold.copy(alpha = 0.14f)
                                    } else {
                                        palette.secondarySurfaceBottom
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (badge.isUnlocked) AcademyGold else palette.dividerColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("badge_card_${badge.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (badge.isUnlocked) AcademyPrimaryGreen else palette.dividerColor
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (badge.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                                contentDescription = badge.titleBangla,
                                                tint = if (badge.isUnlocked) AcademyGoldSheen else palette.mutedColor,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                        Text(
                                            text = badge.titleBangla,
                                            fontFamily = HindSiliguriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (badge.isUnlocked) palette.titleColor else palette.mutedColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = badge.descriptionBangla,
                                            fontFamily = HindSiliguriFontFamily,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = palette.mutedColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // রিভিশন হিস্ট্রি (Revision History)
            item(key = "history_header") {
                Text(
                    text = "সাম্প্রতিক পাঠ ও রিভিশন হিস্ট্রি",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.titleColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (revisionHistory.isEmpty()) {
                item(key = "history_empty") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.secondarySurfaceTop,
                        border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "পাঠ সম্পন্ন করলে আপনার অর্জিত পয়েন্ট ও স্কোরের ইতিহাস এখানে সংরক্ষিত থাকবে।",
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            color = palette.mutedColor,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(revisionHistory.take(15)) { entry ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.secondarySurfaceTop,
                        border = BorderStroke(0.8.dp, palette.dividerColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.lessonTitle,
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = palette.titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.dateLabel} • সঠিক: ${entry.scoreText}",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontSize = 11.5.sp,
                                    color = palette.mutedColor
                                )
                            }
                            Text(
                                text = "+${toAcademyBanglaDigits(entry.earnedPoints)} XP",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = AcademyGold
                            )
                        }
                    }
                }
            }

            item(key = "prog_bottom_space") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ProgressStatMiniPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, AcademyGold.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = label,
                fontFamily = HindSiliguriFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFD0ECE1),
                maxLines = 1
            )
        }
    }
}

/**
 * ৩. অ্যাডমিন কনটেন্ট যাচাই ও প্রকাশনা প্যানেল (Admin CMS Verification Panel)
 * অযাচাইকৃত/খসড়া পাঠসমূহ অনুমোদিত অ্যাডমিন যাচাই করে প্রকাশ না করা পর্যন্ত শিক্ষার্থীর কাছে অদৃশ্য থাকে।
 */
@Composable
fun QuranAdminVerificationScreen(
    adminApprovedIds: Set<String>,
    palette: QuranAcademyPalette,
    onToggleVerifyAndPublish: (String) -> Unit
) {
    val pendingList = remember { QuranLearningCatalog.pendingCmsLessons }
    val verifiedCount = QuranLearningCatalog.getStudentVisibleLessons(adminApprovedIds).size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_admin_verification_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "admin_cms_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen))
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "অ্যাডমিন প্যানেল",
                                tint = AcademyGoldSheen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "অ্যাডমিন পাঠ যাচাই ও প্রকাশনা (CMS)",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "মোট প্রকাশিত ও যাচাইকৃত পাঠ: ${toAcademyBanglaDigits(verifiedCount)}টি। অযাচাইকৃত খসড়া পাঠ অনুমোদন ছাড়া শিক্ষার্থীর কাছে প্রদর্শিত হয় না।",
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFD2EDE2)
                        )
                    }
                }
            }

            item(key = "pending_section_title") {
                Text(
                    text = "যাচাই অপেক্ষমাণ খসড়া পাঠসমূহ (${toAcademyBanglaDigits(pendingList.size)}টি)",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = palette.titleColor
                )
            }

            items(pendingList, key = { it.id }) { lesson ->
                val isApproved = adminApprovedIds.contains(lesson.id)
                val statusLabel = if (isApproved) {
                    "${VerificationStatus.VERIFIED.labelBangla} • ${PublicationStatus.PUBLISHED.labelBangla}"
                } else {
                    "${lesson.verificationStatus.labelBangla} • ${lesson.publicationStatus.labelBangla}"
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.secondarySurfaceTop,
                    border = BorderStroke(
                        1.dp,
                        if (isApproved) AcademyPrimaryGreen else AcademyGold.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_cms_card_${lesson.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lesson.level.titleBangla} • ${lesson.moduleId.uppercase()}",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = palette.accentGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isApproved) {
                                    AcademyPrimaryGreen.copy(alpha = 0.16f)
                                } else {
                                    AcademyGold.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isApproved) AcademyPrimaryGreen else palette.titleColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = lesson.titleBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = palette.titleColor
                        )

                        Text(
                            text = lesson.arabicContent,
                            fontFamily = QuranAcademyArabicFont,
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            color = palette.arabicSampleColor
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "সূত্র",
                                tint = palette.mutedColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "সূত্র: ${lesson.sourceReference}",
                                fontFamily = HindSiliguriFontFamily,
                                fontSize = 11.5.sp,
                                color = palette.mutedColor
                            )
                        }

                        if (isApproved) {
                            OutlinedButton(
                                onClick = { onToggleVerifyAndPublish(lesson.id) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, palette.dividerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_toggle_publish_${lesson.id}")
                            ) {
                                Text(
                                    text = "অপ্রকাশিত (খসড়া) অবস্থায় ফিরিয়ে নিন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = palette.titleColor
                                )
                            }
                        } else {
                            Button(
                                onClick = { onToggleVerifyAndPublish(lesson.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcademyPrimaryGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_toggle_publish_${lesson.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "যাচাই ও প্রকাশ",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "যাচাই ও প্রকাশ করুন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
