package com.example.ui.quranlearning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HindSiliguriFontFamily

private val FeedbackCorrectGreen = Color(0xFF0F6B4B)
private val FeedbackWrongCoral = Color(0xFFC0392B)

/**
 * Duolingo-ধাঁচের ইন্টারঅ্যাক্টিভ পাঠ ইঞ্জিন, যাচাইকৃত অডিও সিস্টেম ও পাঠ সমাপ্তি পুরস্কার স্ক্রিন।
 */
@Composable
fun QuranInteractiveLessonScreen(
    lesson: QuranLesson,
    retryStepIndices: Set<Int> = emptySet(),
    palette: QuranAcademyPalette,
    storage: QuranLearningStorageManager,
    onNavigateToNextLesson: (String) -> Unit,
    onRetryMistakes: (QuranLesson, Set<Int>) -> Unit,
    onExitToPath: () -> Unit,
    audioControllerOverride: QuranLearningAudioController? = null
) {
    val context = LocalContext.current

    // যদি কেবল ভুল ধাপগুলো পুনরায় অনুশীলন করতে চায়
    val activeSteps = remember(lesson.id, retryStepIndices) {
        if (retryStepIndices.isNotEmpty()) {
            val filtered = lesson.practiceSteps.filterIndexed { idx, _ -> retryStepIndices.contains(idx) }
            if (filtered.isNotEmpty()) filtered else lesson.practiceSteps
        } else {
            lesson.practiceSteps
        }
    }

    var currentStepIndex by remember(lesson.id, retryStepIndices) { mutableIntStateOf(0) }
    var correctCount by remember(lesson.id, retryStepIndices) { mutableIntStateOf(0) }
    var totalGradedQuestions by remember(lesson.id, retryStepIndices) { mutableIntStateOf(0) }
    val mistakeStepIndices = remember(lesson.id, retryStepIndices) { mutableStateListOf<Int>() }
    var completionResult by remember(lesson.id, retryStepIndices) { mutableStateOf<LessonCompletionResult?>(null) }

    // নির্ভরযোগ্য একক-ইনস্ট্যান্স অডিও কন্ট্রোলার (MediaPlayer + AudioFocus + Offline Cache + Arabic TTS)
    val audioController = remember(context, audioControllerOverride) {
        audioControllerOverride ?: QuranLearningAudioController.create(context)
    }
    val playbackState by audioController.playbackState.collectAsState()
    val volumeMutedHint by audioController.volumeMutedHint.collectAsState()

    // পাঠ, ধাপ বা সমাপ্তি স্ক্রিনে গেলে আগের অডিও স্বয়ংক্রিয়ভাবে বন্ধ হবে
    LaunchedEffect(lesson.id, currentStepIndex, completionResult != null) {
        audioController.stop()
    }

    // স্ক্রিন থেকে বের হলে অডিও রিসোর্স নিরাপদে মুক্ত হবে
    DisposableEffect(audioController, audioControllerOverride) {
        onDispose {
            if (audioControllerOverride == null) {
                audioController.release()
            } else {
                audioController.stop()
            }
        }
    }

    // সমাপ্তি পুরস্কার স্ক্রিন দেখাও
    val completed = completionResult
    if (completed != null) {
        LessonCompletionRewardsScreen(
            result = completed,
            palette = palette,
            onNextLesson = { nextId -> onNavigateToNextLesson(nextId) },
            onRetryMistakes = { onRetryMistakes(lesson, completed.mistakeStepIndices.toSet()) },
            onBackToPath = onExitToPath
        )
        return
    }

    val currentStep = activeSteps[currentStepIndex]
    val progress = (currentStepIndex + 1).toFloat() / activeSteps.size.toFloat()

    // বর্তমান ধাপের ফিডব্যাক স্টেট
    var stepEvaluated by remember(currentStep.stepId) { mutableStateOf(false) }
    var stepWasCorrect by remember(currentStep.stepId) { mutableStateOf(true) }
    var stepFeedbackText by remember(currentStep.stepId) { mutableStateOf("") }

    fun submitEvaluation(isCorrect: Boolean, explanation: String) {
        if (stepEvaluated) return
        stepEvaluated = true
        stepWasCorrect = isCorrect
        stepFeedbackText = explanation
        totalGradedQuestions += 1
        if (isCorrect) {
            correctCount += 1
        } else {
            val originalIndex = lesson.practiceSteps.indexOfFirst { it.stepId == currentStep.stepId }
            if (originalIndex >= 0 && !mistakeStepIndices.contains(originalIndex)) {
                mistakeStepIndices.add(originalIndex)
            }
        }
    }

    fun advanceToNextStep() {
        audioController.stop()
        if (currentStepIndex < activeSteps.lastIndex) {
            currentStepIndex += 1
        } else {
            completionResult = storage.completeLesson(
                lesson = lesson,
                correctAnswers = correctCount,
                totalQuestions = totalGradedQuestions,
                mistakeStepIndices = mistakeStepIndices.toList()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("quran_interactive_lesson_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // শীর্ষ প্রোগ্রেস হেডার
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        audioController.stop()
                        onExitToPath()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("interactive_lesson_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "পাঠ বন্ধ করুন",
                        tint = palette.titleColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${lesson.lessonNumberBangla} • ${lesson.titleBangla}",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = palette.subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ধাপ ${toAcademyBanglaDigits(currentStepIndex + 1)}/${toAcademyBanglaDigits(activeSteps.size)}",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = palette.accentGreen,
                            modifier = Modifier.testTag("lesson_step_counter")
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = AcademyPrimaryGreen,
                        trackColor = palette.dividerColor,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // ইন্টারঅ্যাক্টিভ ধাপের মূল কনটেন্ট
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "interactive_step_transition"
                ) { step ->
                    when (step) {
                        is QuranLessonStep.TeachingCard -> {
                            TeachingStepContent(
                                lessonId = lesson.id,
                                step = step,
                                sourceReference = lesson.sourceReference,
                                palette = palette,
                                playbackState = playbackState,
                                volumeMutedHint = volumeMutedHint,
                                onPlayOrPause = { source -> audioController.onPlayOrPauseTapped(source) },
                                onReplay = { source -> audioController.onReplayTapped(source) },
                                onRetry = { source -> audioController.onRetryTapped(source) },
                                onContinue = { advanceToNextStep() }
                            )
                        }

                        is QuranLessonStep.SelectOption -> {
                            SelectOptionStepContent(
                                lessonId = lesson.id,
                                step = step,
                                evaluated = stepEvaluated,
                                palette = palette,
                                playbackState = playbackState,
                                volumeMutedHint = volumeMutedHint,
                                onPlayOrPause = { source -> audioController.onPlayOrPauseTapped(source) },
                                onReplay = { source -> audioController.onReplayTapped(source) },
                                onRetry = { source -> audioController.onRetryTapped(source) },
                                onSubmit = { isCorrect ->
                                    submitEvaluation(isCorrect, step.feedbackExplanationBangla)
                                }
                            )
                        }

                        is QuranLessonStep.MatchPairs -> {
                            MatchPairsStepContent(
                                step = step,
                                evaluated = stepEvaluated,
                                palette = palette,
                                onCompleted = { hadMistake ->
                                    submitEvaluation(!hadMistake, step.feedbackExplanationBangla)
                                }
                            )
                        }

                        is QuranLessonStep.FillBlank -> {
                            FillBlankStepContent(
                                step = step,
                                evaluated = stepEvaluated,
                                palette = palette,
                                onSubmit = { isCorrect ->
                                    submitEvaluation(isCorrect, step.feedbackExplanationBangla)
                                }
                            )
                        }

                        is QuranLessonStep.OrderSequence -> {
                            OrderSequenceStepContent(
                                step = step,
                                evaluated = stepEvaluated,
                                palette = palette,
                                onSubmit = { isCorrect ->
                                    submitEvaluation(isCorrect, step.feedbackExplanationBangla)
                                }
                            )
                        }

                        is QuranLessonStep.CompareExample -> {
                            CompareExampleStepContent(
                                step = step,
                                evaluated = stepEvaluated,
                                palette = palette,
                                onSubmit = { isCorrect ->
                                    submitEvaluation(isCorrect, step.feedbackExplanationBangla)
                                }
                            )
                        }
                    }
                }
            }

            // তাৎক্ষণিক ফিডব্যাক কার্ড (মূল্যায়নকৃত ধাপের পর)
            AnimatedVisibility(visible = stepEvaluated) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (stepWasCorrect) {
                        FeedbackCorrectGreen.copy(alpha = 0.14f)
                    } else {
                        FeedbackWrongCoral.copy(alpha = 0.13f)
                    },
                    border = BorderStroke(
                        1.2.dp,
                        if (stepWasCorrect) FeedbackCorrectGreen else FeedbackWrongCoral
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_instant_feedback_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (stepWasCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = if (stepWasCorrect) "সঠিক" else "সংশোধন",
                                tint = if (stepWasCorrect) FeedbackCorrectGreen else FeedbackWrongCoral,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (stepWasCorrect) "মাশাআল্লাহ! সঠিক উত্তর" else "সঠিক নিয়মটি জেনে নিন",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (stepWasCorrect) FeedbackCorrectGreen else FeedbackWrongCoral
                            )
                        }

                        Text(
                            text = stepFeedbackText,
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = palette.titleColor
                        )

                        Button(
                            onClick = { advanceToNextStep() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stepWasCorrect) AcademyPrimaryGreen else FeedbackWrongCoral,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lesson_next_step_btn")
                        ) {
                            Text(
                                text = if (currentStepIndex == activeSteps.lastIndex) "পাঠ সম্পন্ন করুন" else "পরবর্তী ধাপ",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "পরবর্তী",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedAudioControlBar(
    audioSource: VerifiedQuranAudioSource,
    playbackState: QuranAudioPlaybackState,
    volumeMutedHint: Boolean,
    palette: QuranAcademyPalette,
    onPlayOrPause: (VerifiedQuranAudioSource) -> Unit,
    onReplay: (VerifiedQuranAudioSource) -> Unit,
    onRetry: (VerifiedQuranAudioSource) -> Unit
) {
    val isLoading = playbackState is QuranAudioPlaybackState.Loading &&
        playbackState.activeKey == audioSource.audioKey
    val isPlaying = playbackState is QuranAudioPlaybackState.Playing &&
        playbackState.activeKey == audioSource.audioKey
    val isPaused = playbackState is QuranAudioPlaybackState.Paused &&
        playbackState.activeKey == audioSource.audioKey
    val activeError = (playbackState as? QuranAudioPlaybackState.Error)
        ?.takeIf { it.activeKey == audioSource.audioKey }

    val primaryButtonLabel = when {
        isLoading -> "লোড হচ্ছে..."
        isPlaying -> {
            val canPause = (playbackState as QuranAudioPlaybackState.Playing).canPauseResume
            if (canPause) "বিরতি (Pause)" else "থামান"
        }
        isPaused -> "চালিয়ে যান (Resume)"
        audioSource.isVerifiedAyahRecitation -> "তিলাওয়াত শুনুন"
        else -> "উচ্চারণ শুনুন"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verified_audio_control_bar"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onPlayOrPause(audioSource) },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcademyPrimaryGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("audio_play_pause_btn")
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .testTag("audio_loading_indicator"),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                    isPlaying -> {
                        val canPause = (playbackState as QuranAudioPlaybackState.Playing).canPauseResume
                        Icon(
                            imageVector = if (canPause) Icons.Default.Pause else Icons.Default.Stop,
                            contentDescription = if (canPause) "বিরতি" else "থামান",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    isPaused -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "চালিয়ে যান",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "শুনুন",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryButtonLabel,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = { onReplay(audioSource) },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AcademyGold),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("audio_replay_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "পুনরায় শুনুন",
                    tint = palette.titleColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "পুনরায়",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = palette.titleColor
                )
            }
        }

        // অডিও চালু বা বিরতিতে থাকলে সোর্স ও প্লেয়িং স্ট্যাটাস নির্দেশক
        if (isPlaying || isPaused) {
            val statusLabel = when (playbackState) {
                is QuranAudioPlaybackState.Playing -> "চলছে: ${playbackState.sourceLabelBangla}"
                is QuranAudioPlaybackState.Paused -> "বিরতিতে: ${playbackState.sourceLabelBangla}"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_playing_indicator"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AcademyPrimaryGreen else AcademyGold)
                )
                Text(
                    text = statusLabel,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                    color = palette.accentGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ডিভাইসের মিডিয়া ভলিউম শূন্য থাকলে সহায়ক সতর্কতা
        if (volumeMutedHint && (isPlaying || isLoading)) {
            Text(
                text = "ডিভাইসের মিডিয়া ভলিউম বন্ধ আছে — শুনতে ভলিউম বাড়ান।",
                fontFamily = HindSiliguriFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = AcademyGold,
                modifier = Modifier.testTag("audio_volume_muted_hint")
            )
        }

        // নেটওয়ার্ক / সার্ভার / ভয়েস ডেটা ত্রুটি বার্তা ও পুনরায় চেষ্টা (Retry) বাটন
        if (activeError != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FeedbackWrongCoral.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, FeedbackWrongCoral.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_error_banner")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "অডিও ত্রুটি",
                            tint = FeedbackWrongCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = activeError.messageBangla,
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = palette.titleColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (activeError.canRetry) {
                        OutlinedButton(
                            onClick = { onRetry(audioSource) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FeedbackWrongCoral),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("audio_retry_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "আবার চেষ্টা করুন",
                                tint = FeedbackWrongCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আবার চেষ্টা করুন",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = FeedbackWrongCoral
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachingStepContent(
    lessonId: String,
    step: QuranLessonStep.TeachingCard,
    sourceReference: String,
    palette: QuranAcademyPalette,
    playbackState: QuranAudioPlaybackState,
    volumeMutedHint: Boolean,
    onPlayOrPause: (VerifiedQuranAudioSource) -> Unit,
    onReplay: (VerifiedQuranAudioSource) -> Unit,
    onRetry: (VerifiedQuranAudioSource) -> Unit,
    onContinue: () -> Unit
) {
    val resolvedAudioSource = remember(lessonId, step.verifiedAudioText) {
        QuranLearningAudioCatalog.resolveAudioSource(lessonId, step.verifiedAudioText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_teaching_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AcademyPrimaryGreen.copy(alpha = 0.12f)
        ) {
            Text(
                text = "নতুন পাঠ শিখুন",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.accentGreen,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Text(
            text = step.headingBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            color = palette.titleColor
        )

        // আরবি ডিসপ্লে চেম্বার (হারাকাত সম্পূর্ণ স্পষ্ট)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen))
                )
                .border(1.2.dp, AcademyGold.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = step.arabicDisplay,
                    fontFamily = QuranAcademyArabicFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 48.sp,
                    color = AcademyGoldSheen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("teaching_arabic_display")
                )
                Text(
                    text = step.pronunciationBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFE0F4EC),
                    textAlign = TextAlign.Center
                )
            }
        }

        // যাচাইকৃত অডিও থাকলেই কেবল অডিও প্লেয়ার দেখানো হবে (কোনো ভুয়া অডিও বাটন নয়)
        if (resolvedAudioSource != null) {
            VerifiedAudioControlBar(
                audioSource = resolvedAudioSource,
                playbackState = playbackState,
                volumeMutedHint = volumeMutedHint,
                palette = palette,
                onPlayOrPause = onPlayOrPause,
                onReplay = onReplay,
                onRetry = onRetry
            )
        }

        // বিস্তারিত বাংলা ব্যাখ্যা
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.secondarySurfaceTop,
            border = BorderStroke(1.dp, palette.glassCardBorderGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "বিস্তারিত ব্যাখ্যা",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = palette.accentGreen
                )
                Text(
                    text = step.explanationBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = palette.titleColor
                )
            }
        }

        // মূল তাজবীদ/পাঠের সূত্র
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AcademyGold.copy(alpha = 0.13f),
            border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "মনে রাখার মূল নিয়ম",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = palette.titleColor
                )
                Text(
                    text = step.keyRuleBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = palette.subtitleColor
                )
            }
        }

        // প্রামাণ্য কিতাবের সূত্র ও উস্তাদ যাচাই নোট
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.secondarySurfaceBottom,
            border = BorderStroke(0.8.dp, palette.dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "প্রামাণ্য সূত্র",
                        tint = palette.accentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "সূত্র: $sourceReference",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = palette.titleColor
                    )
                }
                Text(
                    text = QuranLearningCatalog.SCHOLARLY_VERIFICATION_NOTE,
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = palette.mutedColor
                )
            }
        }

        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AcademyPrimaryGreen,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("teaching_continue_btn")
        ) {
            Text(
                text = "বুঝেছি — অনুশীলন শুরু করুন",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "এগিয়ে যান",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SelectOptionStepContent(
    lessonId: String,
    step: QuranLessonStep.SelectOption,
    evaluated: Boolean,
    palette: QuranAcademyPalette,
    playbackState: QuranAudioPlaybackState,
    volumeMutedHint: Boolean,
    onPlayOrPause: (VerifiedQuranAudioSource) -> Unit,
    onReplay: (VerifiedQuranAudioSource) -> Unit,
    onRetry: (VerifiedQuranAudioSource) -> Unit,
    onSubmit: (Boolean) -> Unit
) {
    var selectedIdx by remember(step.stepId) { mutableIntStateOf(-1) }
    val resolvedAudioSource = remember(lessonId, step.verifiedAudioText) {
        QuranLearningAudioCatalog.resolveAudioSource(lessonId, step.verifiedAudioText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_select_option"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (step.isAudioQuestion) "শুনুন ও সঠিক উত্তর নির্বাচন করুন" else "সঠিক উত্তর নির্বাচন করুন",
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = palette.accentGreen
        )

        Text(
            text = step.promptBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = palette.titleColor
        )

        if (!step.arabicDisplay.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.arabicChamberBg,
                border = BorderStroke(1.dp, palette.arabicChamberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = step.arabicDisplay,
                    fontFamily = QuranAcademyArabicFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 42.sp,
                    color = palette.arabicSampleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }

        if (resolvedAudioSource != null) {
            VerifiedAudioControlBar(
                audioSource = resolvedAudioSource,
                playbackState = playbackState,
                volumeMutedHint = volumeMutedHint,
                palette = palette,
                onPlayOrPause = onPlayOrPause,
                onReplay = onReplay,
                onRetry = onRetry
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            step.options.forEachIndexed { idx, option ->
                val isSelected = selectedIdx == idx
                val isCorrectOption = idx == step.correctIndex
                val borderColor = when {
                    evaluated && isCorrectOption -> FeedbackCorrectGreen
                    evaluated && isSelected && !isCorrectOption -> FeedbackWrongCoral
                    isSelected -> AcademyGold
                    else -> palette.glassCardBorderGreen
                }
                val bgColor = when {
                    evaluated && isCorrectOption -> FeedbackCorrectGreen.copy(alpha = 0.14f)
                    evaluated && isSelected && !isCorrectOption -> FeedbackWrongCoral.copy(alpha = 0.12f)
                    isSelected -> AcademyGold.copy(alpha = 0.16f)
                    else -> palette.secondarySurfaceTop
                }

                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected || (evaluated && isCorrectOption)) 1.6.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !evaluated) { selectedIdx = idx }
                        .testTag("select_option_item_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AcademyPrimaryGreen else palette.arabicChamberBg
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toAcademyBanglaDigits(idx + 1),
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else palette.titleColor
                            )
                        }
                        Text(
                            text = option,
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                            color = palette.titleColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (!evaluated) {
            Button(
                onClick = { onSubmit(selectedIdx == step.correctIndex) },
                enabled = selectedIdx >= 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcademyPrimaryGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step_check_answer_btn")
            ) {
                Text(
                    text = "উত্তর যাচাই করুন",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun MatchPairsStepContent(
    step: QuranLessonStep.MatchPairs,
    evaluated: Boolean,
    palette: QuranAcademyPalette,
    onCompleted: (hadMistake: Boolean) -> Unit
) {
    val shuffledRight = remember(step.stepId) {
        if (step.pairs.size > 1) step.pairs.reversed() else step.pairs
    }
    var selectedLeftId by remember(step.stepId) { mutableStateOf<String?>(null) }
    val matchedIds = remember(step.stepId) { mutableStateListOf<String>() }
    var wrongFlashText by remember(step.stepId) { mutableStateOf<String?>(null) }
    var hadMistake by remember(step.stepId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_match_pairs"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "জোড়া মেলান (Pair Matching)",
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = palette.accentGreen
        )
        Text(
            text = step.promptBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = palette.titleColor
        )

        if (wrongFlashText != null && !evaluated) {
            Text(
                text = wrongFlashText.orEmpty(),
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = FeedbackWrongCoral
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // বাম কলাম: আরবি অংশ
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                step.pairs.forEachIndexed { idx, pair ->
                    val isMatched = matchedIds.contains(pair.id)
                    val isSelected = selectedLeftId == pair.id
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = when {
                            isMatched -> FeedbackCorrectGreen.copy(alpha = 0.16f)
                            isSelected -> AcademyGold.copy(alpha = 0.22f)
                            else -> palette.secondarySurfaceTop
                        },
                        border = BorderStroke(
                            if (isSelected || isMatched) 1.5.dp else 1.dp,
                            when {
                                isMatched -> FeedbackCorrectGreen
                                isSelected -> AcademyGold
                                else -> palette.glassCardBorderGreen
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatched && !evaluated) {
                                selectedLeftId = pair.id
                                wrongFlashText = null
                            }
                            .testTag("match_left_$idx")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.leftArabic,
                                fontFamily = QuranAcademyArabicFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 32.sp,
                                color = if (isMatched) FeedbackCorrectGreen else palette.arabicSampleColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ডান কলাম: বাংলা অর্থ বা উচ্চারণ
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                shuffledRight.forEachIndexed { idx, pair ->
                    val isMatched = matchedIds.contains(pair.id)
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (isMatched) FeedbackCorrectGreen.copy(alpha = 0.16f) else palette.secondarySurfaceTop,
                        border = BorderStroke(
                            if (isMatched) 1.5.dp else 1.dp,
                            if (isMatched) FeedbackCorrectGreen else palette.glassCardBorderGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatched && !evaluated && selectedLeftId != null) {
                                if (selectedLeftId == pair.id) {
                                    matchedIds.add(pair.id)
                                    selectedLeftId = null
                                    wrongFlashText = null
                                    if (matchedIds.size == step.pairs.size) {
                                        onCompleted(hadMistake)
                                    }
                                } else {
                                    hadMistake = true
                                    wrongFlashText = "জোড়াটি সঠিক হয়নি—আবার চেষ্টা করুন!"
                                }
                            }
                            .testTag("match_right_$idx")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.rightBangla,
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = if (isMatched) FeedbackCorrectGreen else palette.titleColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillBlankStepContent(
    step: QuranLessonStep.FillBlank,
    evaluated: Boolean,
    palette: QuranAcademyPalette,
    onSubmit: (Boolean) -> Unit
) {
    var selectedIdx by remember(step.stepId) { mutableIntStateOf(-1) }
    val filledPreview = if (selectedIdx in step.choices.indices) {
        step.sentenceWithBlank.replace("___", "【${step.choices[selectedIdx]}】")
    } else {
        step.sentenceWithBlank
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_fill_blank"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "শূন্যস্থান পূরণ করুন",
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = palette.accentGreen
        )
        Text(
            text = step.promptBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = palette.subtitleColor
        )

        if (!step.arabicCue.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.arabicChamberBg,
                border = BorderStroke(1.dp, palette.arabicChamberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = step.arabicCue,
                    fontFamily = QuranAcademyArabicFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 38.sp,
                    color = palette.arabicSampleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.secondarySurfaceTop,
            border = BorderStroke(1.dp, AcademyGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = filledPreview,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 25.sp,
                color = palette.titleColor,
                modifier = Modifier.padding(16.dp)
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            step.choices.forEachIndexed { idx, choice ->
                val isSelected = selectedIdx == idx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AcademyPrimaryGreen else palette.secondarySurfaceBottom,
                    border = BorderStroke(1.2.dp, if (isSelected) AcademyGold else palette.glassCardBorderGreen),
                    modifier = Modifier
                        .clickable(enabled = !evaluated) { selectedIdx = idx }
                        .testTag("fill_choice_$idx")
                ) {
                    Text(
                        text = choice,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else palette.titleColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }

        if (!evaluated) {
            Button(
                onClick = { onSubmit(selectedIdx == step.correctChoiceIndex) },
                enabled = selectedIdx >= 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcademyPrimaryGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step_check_answer_btn")
            ) {
                Text(
                    text = "উত্তর যাচাই করুন",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderSequenceStepContent(
    step: QuranLessonStep.OrderSequence,
    evaluated: Boolean,
    palette: QuranAcademyPalette,
    onSubmit: (Boolean) -> Unit
) {
    val selectedSequence = remember(step.stepId) { mutableStateListOf<String>() }
    val remainingPool = step.shuffledOrder.filter { !selectedSequence.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_order_sequence"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "সঠিক ক্রম সাজান",
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = palette.accentGreen
        )
        Text(
            text = step.promptBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = palette.titleColor
        )

        // সাজানো ক্রম বাক্স
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.secondarySurfaceTop,
            border = BorderStroke(1.2.dp, AcademyGold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = "আপনার সাজানো ক্রম (সরাতে ট্যাপ করুন):",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 12.sp,
                        color = palette.mutedColor
                    )
                    if (selectedSequence.isNotEmpty() && !evaluated) {
                        Text(
                            text = "রিসেট",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = palette.accentGreen,
                            modifier = Modifier.clickable { selectedSequence.clear() }
                        )
                    }
                }

                if (selectedSequence.isEmpty()) {
                    Text(
                        text = "নিচের অংশগুলোতে একে একে ট্যাপ করে সঠিক ক্রমে সাজান...",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.sp,
                        color = palette.mutedColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedSequence.forEachIndexed { idx, item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AcademyPrimaryGreen,
                                modifier = Modifier.clickable(enabled = !evaluated) {
                                    selectedSequence.remove(item)
                                }
                            ) {
                                Text(
                                    text = "${toAcademyBanglaDigits(idx + 1)}. $item",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // বাছাই করার পুল
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            remainingPool.forEachIndexed { idx, item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.secondarySurfaceBottom,
                    border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                    modifier = Modifier
                        .clickable(enabled = !evaluated) { selectedSequence.add(item) }
                        .testTag("order_pool_item_$idx")
                ) {
                    Text(
                        text = item,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = palette.titleColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    )
                }
            }
        }

        if (!evaluated) {
            Button(
                onClick = { onSubmit(selectedSequence.toList() == step.correctOrder) },
                enabled = selectedSequence.size == step.correctOrder.size,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcademyPrimaryGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step_check_answer_btn")
            ) {
                Text(
                    text = "ক্রম যাচাই করুন",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CompareExampleStepContent(
    step: QuranLessonStep.CompareExample,
    evaluated: Boolean,
    palette: QuranAcademyPalette,
    onSubmit: (Boolean) -> Unit
) {
    var chosenOptionA by remember(step.stepId) { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_compare_example"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ভুল ও সঠিক তুলনা করুন",
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = palette.accentGreen
        )
        Text(
            text = step.promptBangla,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.5.sp,
            lineHeight = 23.sp,
            color = palette.titleColor
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.arabicChamberBg,
            border = BorderStroke(1.dp, palette.arabicChamberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = step.arabicSubject,
                fontFamily = QuranAcademyArabicFont,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                lineHeight = 40.sp,
                color = palette.arabicSampleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        val options = listOf(true to step.optionA, false to step.optionB)
        options.forEachIndexed { idx, (isOptionA, label) ->
            val isSelected = chosenOptionA == isOptionA
            val isRightCard = isOptionA == step.correctIsOptionA
            val borderColor = when {
                evaluated && isRightCard -> FeedbackCorrectGreen
                evaluated && isSelected && !isRightCard -> FeedbackWrongCoral
                isSelected -> AcademyGold
                else -> palette.glassCardBorderGreen
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when {
                    evaluated && isRightCard -> FeedbackCorrectGreen.copy(alpha = 0.14f)
                    evaluated && isSelected && !isRightCard -> FeedbackWrongCoral.copy(alpha = 0.12f)
                    isSelected -> AcademyGold.copy(alpha = 0.18f)
                    else -> palette.secondarySurfaceTop
                },
                border = BorderStroke(if (isSelected || (evaluated && isRightCard)) 1.6.dp else 1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !evaluated) { chosenOptionA = isOptionA }
                    .testTag("compare_option_$idx")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) AcademyPrimaryGreen else palette.arabicChamberBg,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isOptionA) "ক" else "খ",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else palette.titleColor
                            )
                        }
                    }
                    Text(
                        text = label,
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = palette.titleColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (!evaluated) {
            Button(
                onClick = { onSubmit(chosenOptionA == step.correctIsOptionA) },
                enabled = chosenOptionA != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcademyPrimaryGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step_check_answer_btn")
            ) {
                Text(
                    text = "উত্তর যাচাই করুন",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * ৫. পাঠ সমাপ্তি ও পুরস্কার স্ক্রিন (Completion Message, Earned Points, Correct/Total, Next Lesson, Retry Mistakes)
 */
@Composable
private fun LessonCompletionRewardsScreen(
    result: LessonCompletionResult,
    palette: QuranAcademyPalette,
    onNextLesson: (String) -> Unit,
    onRetryMistakes: () -> Unit,
    onBackToPath: () -> Unit
) {
    val accuracy = if (result.totalQuestions == 0) 100 else {
        ((result.correctAnswers * 100f) / result.totalQuestions).toInt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .testTag("lesson_completion_rewards_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // রাজকীয় ট্রফি ব্যাজ
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(AcademyDeepEmerald, AcademyPrimaryGreen))
                    )
                    .border(3.dp, AcademyGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "পাঠ সম্পন্ন পুরস্কার",
                    tint = AcademyGoldSheen,
                    modifier = Modifier.size(46.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "আলহামদুলিল্লাহ! পাঠ সম্পন্ন হয়েছে",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = palette.titleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("completion_congrats_title")
                )
                Text(
                    text = "${result.lesson.lessonNumberBangla} — ${result.lesson.titleBangla}",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 14.sp,
                    color = palette.subtitleColor,
                    textAlign = TextAlign.Center
                )
            }

            // ৩টি স্ট্যাট কার্ড: অর্জিত পয়েন্ট, সঠিক উত্তর, নির্ভুলতা
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RewardMetricCard(
                    title = "অর্জিত পয়েন্ট",
                    value = "+${toAcademyBanglaDigits(result.earnedPoints)} XP",
                    accentColor = AcademyGold,
                    palette = palette,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reward_earned_xp")
                )
                RewardMetricCard(
                    title = "সঠিক উত্তর",
                    value = "${toAcademyBanglaDigits(result.correctAnswers)}/${toAcademyBanglaDigits(result.totalQuestions)}",
                    accentColor = AcademyPrimaryGreen,
                    palette = palette,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reward_correct_count")
                )
                RewardMetricCard(
                    title = "নির্ভুলতা",
                    value = "${toAcademyBanglaDigits(accuracy)}%",
                    accentColor = AcademySoftEmerald,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            // শেখার সারসংক্ষেপ ও প্রামাণ্য সূত্র
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.secondarySurfaceTop,
                border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "এই পাঠের সারসংক্ষেপ",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = palette.accentGreen
                    )
                    Text(
                        text = result.lesson.keyRuleBangla,
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = palette.titleColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "যাচাই নোট",
                            tint = AcademyGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = QuranLearningCatalog.SCHOLARLY_VERIFICATION_NOTE,
                            fontFamily = HindSiliguriFontFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = palette.mutedColor
                        )
                    }
                }
            }

            // অ্যাকশন বাটনসমূহ
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (result.nextLessonId != null) {
                    Button(
                        onClick = { onNextLesson(result.nextLessonId) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcademyPrimaryGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("completion_next_lesson_btn")
                    ) {
                        Text(
                            text = "পরবর্তী পাঠ শুরু করুন",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরবর্তী পাঠ",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (result.mistakeStepIndices.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onRetryMistakes,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, AcademyGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("completion_retry_mistakes_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ভুলগুলো আবার অনুশীলন",
                            tint = palette.titleColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ভুলগুলো আবার অনুশীলন করুন (${toAcademyBanglaDigits(result.mistakeStepIndices.size)}টি)",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.titleColor
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBackToPath,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.glassCardBorderGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("completion_back_to_path_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "মানচিত্রে ফিরুন",
                        tint = palette.accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "পাঠের মানচিত্রে ফিরে যান",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = palette.titleColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    palette: QuranAcademyPalette,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.secondarySurfaceTop,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = title,
                fontFamily = HindSiliguriFontFamily,
                fontSize = 11.5.sp,
                color = palette.mutedColor,
                maxLines = 1
            )
        }
    }
}
