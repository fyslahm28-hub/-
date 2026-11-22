package com.example.ui.quranlearning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.launch

private sealed interface PathListRow {
    data class ModuleHeader(
        val module: QuranLearningModule,
        val totalLessons: Int,
        val completedLessons: Int,
        val isCompleted: Boolean
    ) : PathListRow

    data class LessonNodeRow(
        val lesson: QuranLesson,
        val nodeState: LessonNodeState,
        val indexInModule: Int,
        val isLastInModule: Boolean
    ) : PathListRow
}

/**
 * Duolingo-ধাঁচের ইন্টারঅ্যাক্টিভ লার্নিং পাথ (Lesson Map)।
 * প্রতিটি স্তরের ১০টি মডিউল ও ১০২+ স্বতন্ত্র পাঠ ধাপে ধাপে আনলক সিস্টেমে উপস্থাপিত।
 */
@Composable
fun QuranLevelLearningPathScreen(
    level: QuranLearningLevel,
    completedLessonIds: Set<String>,
    adminApprovedIds: Set<String>,
    storage: QuranLearningStorageManager,
    palette: QuranAcademyPalette,
    onOpenLesson: (QuranLesson) -> Unit
) {
    val modules = remember(level) { QuranLearningCatalog.getModulesForLevel(level) }
    val levelLessons = remember(level, adminApprovedIds) {
        QuranLearningCatalog.getLessonsForLevel(level, adminApprovedIds)
    }
    val completedInLevel = remember(levelLessons, completedLessonIds) {
        levelLessons.count { completedLessonIds.contains(it.id) }
    }
    val currentActiveLesson = remember(levelLessons, completedLessonIds, adminApprovedIds) {
        levelLessons.firstOrNull { !completedLessonIds.contains(it.id) && storage.isLessonUnlocked(it, completedLessonIds, adminApprovedIds) }
            ?: levelLessons.lastOrNull()
    }

    val pathRows = remember(modules, levelLessons, completedLessonIds, adminApprovedIds) {
        val rows = mutableListOf<PathListRow>()
        val lessonsByMod = levelLessons.groupBy { it.moduleId }
        modules.forEach { mod ->
            val modLessons = lessonsByMod[mod.moduleId].orEmpty().sortedBy { it.lessonOrderInModule }
            val modCompleted = modLessons.count { completedLessonIds.contains(it.id) }
            rows += PathListRow.ModuleHeader(
                module = mod,
                totalLessons = modLessons.size,
                completedLessons = modCompleted,
                isCompleted = modLessons.isNotEmpty() && modCompleted == modLessons.size
            )
            modLessons.forEachIndexed { idx, lesson ->
                rows += PathListRow.LessonNodeRow(
                    lesson = lesson,
                    nodeState = storage.getNodeState(lesson, completedLessonIds, adminApprovedIds),
                    indexInModule = idx,
                    isLastInModule = idx == modLessons.lastIndex
                )
            }
        }
        rows
    }

    val moduleRowIndexMap = remember(pathRows) {
        buildMap<String, Int> {
            pathRows.forEachIndexed { idx, row ->
                if (row is PathListRow.ModuleHeader) {
                    // +2 for top summary + jump bar items in LazyColumn
                    put(row.module.moduleId, idx + 2)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lockedNoticeText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_level_path_screen_${level.id}"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            AnimatedVisibility(visible = lockedNoticeText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AcademyGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, AcademyGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("locked_lesson_notice")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "লক করা পাঠ",
                            tint = AcademyGold,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = lockedNoticeText.orEmpty(),
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.5.sp,
                            color = palette.titleColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("quran_level_path_lazy_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ০. লেভেল সারসংক্ষেপ ও বর্তমান পাঠ শুরু বাটন
                item(key = "level_summary_banner") {
                    LevelPathOverviewCard(
                        level = level,
                        totalModules = modules.size,
                        completedLessons = completedInLevel,
                        totalLessons = levelLessons.size,
                        currentLesson = currentActiveLesson,
                        onContinueCurrent = {
                            currentActiveLesson?.let { onOpenLesson(it) }
                        }
                    )
                }

                // ১. দ্রুত মডিউল জাম্প চিপস (মডিউল ১..১০)
                item(key = "module_jump_bar") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.testTag("quran_module_jump_row")
                    ) {
                        items(modules, key = { "${level.id}_chip_${it.moduleId}" }) { mod ->
                            val modDone = storage.isModuleCompleted(mod, completedLessonIds, adminApprovedIds)
                            val isCurrentMod = currentActiveLesson?.moduleId == mod.moduleId
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when {
                                    modDone -> AcademyPrimaryGreen
                                    isCurrentMod -> AcademyGold.copy(alpha = 0.22f)
                                    else -> palette.secondarySurfaceTop
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        modDone -> AcademyPrimaryGreen
                                        isCurrentMod -> AcademyGold
                                        else -> palette.glassCardBorderGreen
                                    }
                                ),
                                modifier = Modifier
                                    .clickable {
                                        val targetIdx = moduleRowIndexMap[mod.moduleId] ?: 0
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIdx)
                                        }
                                    }
                                    .testTag("module_jump_chip_${mod.moduleId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (modDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "সম্পন্ন",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = mod.moduleNumberBangla,
                                        fontFamily = HindSiliguriFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (modDone) Color.White else palette.titleColor
                                    )
                                }
                            }
                        }
                    }
                }

            // ২. প্রতিটি মডিউল ও তার অধীনে আঁকাবাঁকা লার্নিং পাথ নোডসমূহ
            items(
                items = pathRows,
                key = { row ->
                    when (row) {
                        is PathListRow.ModuleHeader -> "mod_${row.module.level.id}_${row.module.moduleId}"
                        is PathListRow.LessonNodeRow -> "node_${row.lesson.id}"
                    }
                }
            ) { row ->
                when (row) {
                    is PathListRow.ModuleHeader -> {
                        ModuleHeaderBannerCard(
                            module = row.module,
                            completedLessons = row.completedLessons,
                            totalLessons = row.totalLessons,
                            isCompleted = row.isCompleted,
                            palette = palette
                        )
                    }

                    is PathListRow.LessonNodeRow -> {
                        WindingPathLessonNodeItem(
                            lesson = row.lesson,
                            nodeState = row.nodeState,
                            indexInModule = row.indexInModule,
                            showConnectorBelow = !row.isLastInModule,
                            palette = palette,
                            onClick = {
                                if (row.nodeState == LessonNodeState.LOCKED) {
                                    lockedNoticeText =
                                        "\"${row.lesson.lessonNumberBangla}\" আনলক করতে ঠিক আগের পাঠটি সম্পন্ন করুন।"
                                } else {
                                    lockedNoticeText = null
                                    onOpenLesson(row.lesson)
                                }
                            }
                        )
                    }
                }
            }

            item(key = "path_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }
    }
}

@Composable
private fun LevelPathOverviewCard(
    level: QuranLearningLevel,
    totalModules: Int,
    completedLessons: Int,
    totalLessons: Int,
    currentLesson: QuranLesson?,
    onContinueCurrent: () -> Unit
) {
    val progress = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons.toFloat()
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen, AcademyRoyalEmerald))
            )
            .drawBehind {
                drawIslamicGeometricPattern(
                    lineColor = Color.White.copy(alpha = 0.05f),
                    accentColor = AcademyGold.copy(alpha = 0.08f),
                    cellSizeDp = 46f
                )
            }
            .border(1.dp, AcademyGold.copy(alpha = 0.45f), cardShape)
            .padding(16.dp)
            .testTag("level_path_overview_${level.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = level.titleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${toAcademyBanglaDigits(totalModules)}টি মডিউল • ${toAcademyBanglaDigits(totalLessons)}টি যাচাইকৃত পাঠ",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.5.sp,
                        color = Color(0xFFD2EDE2)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = level.arabicSample,
                        fontFamily = QuranAcademyArabicFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        lineHeight = 29.sp,
                        color = AcademyGoldSheen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "অগ্রগতি: ${toAcademyBanglaDigits(completedLessons)} / ${toAcademyBanglaDigits(totalLessons)} পাঠ",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AcademyGoldSheen
                    )
                    Text(
                        text = "${toAcademyBanglaDigits((progress * 100).toInt())}%",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = AcademyGold,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    strokeCap = StrokeCap.Round
                )
            }

            if (currentLesson != null) {
                Button(
                    onClick = onContinueCurrent,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcademyGold,
                        contentColor = AcademyRoyalEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("level_continue_current_btn_${level.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "চালু করুন",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${currentLesson.lessonNumberBangla} শুরু করুন: ${currentLesson.titleBangla}",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleHeaderBannerCard(
    module: QuranLearningModule,
    completedLessons: Int,
    totalLessons: Int,
    isCompleted: Boolean,
    palette: QuranAcademyPalette
) {
    val cardShape = RoundedCornerShape(16.dp)
    val progress = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons.toFloat()

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isCompleted) 1.4.dp else 1.dp,
            color = if (isCompleted) AcademyGold else palette.glassCardBorderGreen
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(cardShape)
            .background(
                Brush.linearGradient(listOf(palette.glassCardTop, palette.glassCardBottom))
            )
            .testTag("module_card_${module.level.id}_${module.moduleId}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCompleted) AcademyGold else AcademyPrimaryGreen
                    ) {
                        Text(
                            text = module.moduleNumberBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (isCompleted) AcademyRoyalEmerald else Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AcademyGold.copy(alpha = 0.20f),
                            border = BorderStroke(0.8.dp, AcademyGold),
                            modifier = Modifier.testTag("module_completed_badge_${module.moduleId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "মডিউল সম্পন্ন ব্যাজ",
                                    tint = AcademyGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "মডিউল সম্পন্ন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = palette.titleColor
                                )
                            }
                        }
                    }
                }

                Text(
                    text = module.arabicBadge,
                    fontFamily = QuranAcademyArabicFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = palette.arabicSampleColor,
                    maxLines = 1
                )
            }

            Text(
                text = module.titleBangla,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = palette.titleColor
            )

            Text(
                text = module.shortDescriptionBangla,
                fontFamily = HindSiliguriFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = palette.subtitleColor
            )

            Text(
                text = "লক্ষ্য: ${module.learningObjectiveBangla}",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = palette.accentGreen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) AcademyGold else AcademyPrimaryGreen,
                    trackColor = palette.dividerColor,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${toAcademyBanglaDigits(completedLessons)}/${toAcademyBanglaDigits(totalLessons)} পাঠ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = palette.mutedColor
                )
            }
        }
    }
}

/**
 * Duolingo-ধাঁচের আঁকাবাঁকা লার্নিং পাথ নোড:
 * - Completed = Emerald Check
 * - Current = Gold Highlight Ring + শুরু করুন ট্যাগ
 * - Locked = Muted Lock
 * - Revision / Final Assessment = Star / Trophy আইকন
 */
@Composable
private fun WindingPathLessonNodeItem(
    lesson: QuranLesson,
    nodeState: LessonNodeState,
    indexInModule: Int,
    showConnectorBelow: Boolean,
    palette: QuranAcademyPalette,
    onClick: () -> Unit
) {
    // আঁকাবাঁকা পথের জন্য বাম–মধ্য–ডান–মধ্য অফসেট প্যাটার্ন
    val alignment = when (indexInModule % 4) {
        0 -> Alignment.CenterHorizontally
        1 -> Alignment.Start
        2 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    val horizontalSidePadding = when (indexInModule % 4) {
        1 -> 18.dp
        3 -> 18.dp
        else -> 4.dp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lesson_node_scale"
    )

    val isSpecial = lesson.isRevisionLesson || lesson.isFinalLevelAssessment
    val nodeBgColor = when (nodeState) {
        LessonNodeState.COMPLETED -> AcademyPrimaryGreen
        LessonNodeState.CURRENT -> AcademyGold
        LessonNodeState.LOCKED -> palette.secondarySurfaceBottom
    }
    val nodeBorderColor = when (nodeState) {
        LessonNodeState.COMPLETED -> AcademyGold.copy(alpha = 0.85f)
        LessonNodeState.CURRENT -> AcademyGoldSheen
        LessonNodeState.LOCKED -> palette.dividerColor
    }
    val iconTint = when (nodeState) {
        LessonNodeState.COMPLETED -> Color.White
        LessonNodeState.CURRENT -> AcademyRoyalEmerald
        LessonNodeState.LOCKED -> palette.mutedColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalSidePadding),
        horizontalAlignment = alignment
    ) {
        // বর্তমান পাঠের উপরে হাইলাইট পিল
        if (nodeState == LessonNodeState.CURRENT) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AcademyGold,
                modifier = Modifier.padding(bottom = 5.dp)
            ) {
                Text(
                    text = if (lesson.isFinalLevelAssessment) "চূড়ান্ত মূল্যায়ন" else "শুরু করুন",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AcademyRoyalEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        // নোড + সংক্ষিপ্ত পাঠ তথ্য কার্ড
        Row(
            modifier = Modifier
                .scale(scale)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (nodeState == LessonNodeState.CURRENT) {
                        Brush.linearGradient(listOf(palette.glassCardTop, AcademyGold.copy(alpha = 0.14f)))
                    } else {
                        Brush.linearGradient(listOf(palette.secondarySurfaceTop, palette.secondarySurfaceBottom))
                    }
                )
                .border(
                    width = if (nodeState == LessonNodeState.CURRENT) 1.5.dp else 1.dp,
                    color = when (nodeState) {
                        LessonNodeState.COMPLETED -> AcademyPrimaryGreen.copy(alpha = 0.45f)
                        LessonNodeState.CURRENT -> AcademyGold
                        LessonNodeState.LOCKED -> palette.dividerColor
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = AcademyGold.copy(alpha = 0.25f)),
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("lesson_path_node_${lesson.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // গোলাকার ইন্টারঅ্যাক্টিভ নোড ব্যাজ (নূন্যতম 52dp টাচ ডিস্ক)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(
                        elevation = if (nodeState == LessonNodeState.CURRENT) 6.dp else 2.dp,
                        shape = CircleShape,
                        ambientColor = nodeBgColor,
                        spotColor = nodeBgColor
                    )
                    .clip(CircleShape)
                    .background(nodeBgColor)
                    .border(
                        width = if (nodeState == LessonNodeState.CURRENT) 3.dp else 2.dp,
                        color = nodeBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconVector = when {
                    nodeState == LessonNodeState.COMPLETED -> Icons.Default.Check
                    nodeState == LessonNodeState.LOCKED -> Icons.Default.Lock
                    lesson.isFinalLevelAssessment -> Icons.Default.EmojiEvents
                    lesson.isRevisionLesson -> Icons.Default.Star
                    else -> Icons.Default.PlayArrow
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = "${lesson.lessonNumberBangla} অবস্থা",
                    tint = iconTint,
                    modifier = Modifier.size(if (isSpecial) 26.dp else 24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = lesson.lessonNumberBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = when (nodeState) {
                            LessonNodeState.COMPLETED -> palette.accentGreen
                            LessonNodeState.CURRENT -> AcademyGold
                            LessonNodeState.LOCKED -> palette.mutedColor
                        }
                    )
                    if (lesson.isFinalLevelAssessment) {
                        Text(
                            text = "• মূল্যায়ন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AcademyGold
                        )
                    } else if (lesson.isRevisionLesson) {
                        Text(
                            text = "• রিভিশন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = palette.accentGreen
                        )
                    }
                }

                Text(
                    text = lesson.titleBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    color = if (nodeState == LessonNodeState.LOCKED) palette.mutedColor else palette.titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = lesson.arabicContent,
                    fontFamily = QuranAcademyArabicFont,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    color = if (nodeState == LessonNodeState.LOCKED) {
                        palette.mutedColor.copy(alpha = 0.7f)
                    } else {
                        palette.arabicSampleColor
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }

        // নোডের নিচে সংযোগকারী পাথ রেখা
        if (showConnectorBelow) {
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (nodeState == LessonNodeState.COMPLETED) {
                            AcademyPrimaryGreen.copy(alpha = 0.55f)
                        } else {
                            palette.dividerColor
                        }
                    )
            )
        }
    }
}
