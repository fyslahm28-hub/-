package com.example.ui.quranlearning

/**
 * প্রিমিয়াম ইন্টারঅ্যাক্টিভ কুরআন শিক্ষা একাডেমির ডেটা মডেল ও CMS আর্কিটেকচার।
 */
enum class QuranLearningLevel(
    val id: String,
    val titleBangla: String,
    val arabicSample: String,
    val descriptionBangla: String,
    val levelOrder: Int
) {
    PRIMARY(
        id = "primary",
        titleBangla = "প্রাথমিক শিক্ষা",
        arabicSample = "أ ب ت ث",
        descriptionBangla = "আরবি হরফ, হরকত ও প্রাথমিক পাঠ।",
        levelOrder = 1
    ),
    INTERMEDIATE(
        id = "intermediate",
        titleBangla = "মধ্যবর্তী শিক্ষা",
        arabicSample = "تَجْوِيد",
        descriptionBangla = "মাখরাজ, সিফাত ও তাজবিদের নিয়ম।",
        levelOrder = 2
    ),
    ADVANCED(
        id = "advanced",
        titleBangla = "উচ্চ শিক্ষা",
        arabicSample = "التِّلَاوَة",
        descriptionBangla = "উন্নত তাজবিদ ও শুদ্ধ তিলাওয়াত।",
        levelOrder = 3
    )
}

enum class VerificationStatus(val labelBangla: String) {
    VERIFIED("যাচাইকৃত"),
    PENDING_REVIEW("যাচাই অপেক্ষমাণ"),
    DRAFT("খসড়া")
}

enum class PublicationStatus(val labelBangla: String) {
    PUBLISHED("প্রকাশিত"),
    UNPUBLISHED("অপ্রকাশিত")
}

enum class LessonNodeState {
    COMPLETED,
    CURRENT,
    LOCKED
}

data class QuranLearningModule(
    val moduleId: String,
    val level: QuranLearningLevel,
    val moduleNumber: Int,
    val moduleNumberBangla: String,
    val titleBangla: String,
    val arabicBadge: String,
    val shortDescriptionBangla: String,
    val learningObjectiveBangla: String
)

data class MatchPairItem(
    val id: String,
    val leftArabic: String,
    val rightBangla: String
)

/**
 * Duolingo-ধাঁচের ইন্টারঅ্যাক্টিভ পাঠের ধাপসমূহ (Steps)।
 */
sealed interface QuranLessonStep {
    val stepId: String

    /** ১. শিক্ষামূলক কার্ড (আরবি প্রদর্শন, বাংলা ব্যাখ্যা ও প্রযোজ্য ক্ষেত্রে যাচাইকৃত অডিও) */
    data class TeachingCard(
        override val stepId: String,
        val headingBangla: String,
        val arabicDisplay: String,
        val pronunciationBangla: String,
        val explanationBangla: String,
        val keyRuleBangla: String,
        val verifiedAudioText: String? = null
    ) : QuranLessonStep

    /** ২. সঠিক হরফ / নিয়ম / অডিও শুনে উত্তর নির্বাচন (MCQ) */
    data class SelectOption(
        override val stepId: String,
        val promptBangla: String,
        val arabicDisplay: String?,
        val options: List<String>,
        val correctIndex: Int,
        val feedbackExplanationBangla: String,
        val verifiedAudioText: String? = null,
        val isAudioQuestion: Boolean = false
    ) : QuranLessonStep

    /** ৩. হরফের সঙ্গে উচ্চারণ বা নিয়ম মেলানো (Pair Matching) */
    data class MatchPairs(
        override val stepId: String,
        val promptBangla: String,
        val pairs: List<MatchPairItem>,
        val feedbackExplanationBangla: String
    ) : QuranLessonStep

    /** ৪. শূন্যস্থান পূরণ (Fill in the Blank) */
    data class FillBlank(
        override val stepId: String,
        val promptBangla: String,
        val sentenceWithBlank: String,
        val arabicCue: String?,
        val choices: List<String>,
        val correctChoiceIndex: Int,
        val feedbackExplanationBangla: String
    ) : QuranLessonStep

    /** ৫. সঠিক ক্রম সাজানো (Arrange Sequence) */
    data class OrderSequence(
        override val stepId: String,
        val promptBangla: String,
        val arabicHint: String?,
        val correctOrder: List<String>,
        val shuffledOrder: List<String>,
        val feedbackExplanationBangla: String
    ) : QuranLessonStep

    /** ৬. ভুল ও সঠিক উদাহরণ তুলনা (Compare Right vs Wrong) */
    data class CompareExample(
        override val stepId: String,
        val promptBangla: String,
        val arabicSubject: String,
        val optionA: String,
        val optionB: String,
        val correctIsOptionA: Boolean,
        val feedbackExplanationBangla: String
    ) : QuranLessonStep
}

data class QuranQuizQuestion(
    val id: String,
    val level: QuranLearningLevel,
    val questionBangla: String,
    val arabicPrompt: String? = null,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanationBangla: String
)

/**
 * প্রতিটি পাঠের পূর্ণাঙ্গ CMS ডেটা স্ট্রাকচার।
 */
data class QuranLesson(
    val id: String,
    val level: QuranLearningLevel,
    val moduleId: String,
    val lessonOrderInModule: Int,
    val globalOrderInLevel: Int,
    val lessonNumberBangla: String,
    val titleBangla: String,
    val descriptionBangla: String,
    val learningObjectiveBangla: String,
    val arabicContent: String,
    val pronunciationGuideBangla: String,
    val bengaliExplanation: String,
    val keyRuleBangla: String,
    val wrongVsRightNoteBangla: String,
    val audioReference: String?,
    val practiceSteps: List<QuranLessonStep>,
    val quizData: List<QuranQuizQuestion>,
    val sourceReference: String,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
    val isRevisionLesson: Boolean = false,
    val isFinalLevelAssessment: Boolean = false
) {
    val isStudentVisible: Boolean
        get() = verificationStatus == VerificationStatus.VERIFIED &&
            publicationStatus == PublicationStatus.PUBLISHED
}

data class LessonCompletionResult(
    val lesson: QuranLesson,
    val earnedPoints: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val mistakeStepIndices: List<Int>,
    val nextLessonId: String?
)

data class RevisionHistoryEntry(
    val lessonId: String,
    val lessonTitle: String,
    val scoreText: String,
    val earnedPoints: Int,
    val dateLabel: String
)

data class AcademyAchievementBadge(
    val id: String,
    val titleBangla: String,
    val descriptionBangla: String,
    val isUnlocked: Boolean
)

sealed interface QuranLearningDestination {
    data object AcademyHome : QuranLearningDestination
    data class LevelLessonMap(val level: QuranLearningLevel) : QuranLearningDestination
    data class InteractiveLesson(val lessonId: String, val retryOnlyMistakes: Boolean = false) : QuranLearningDestination
    data object MistakesAndRevision : QuranLearningDestination
    data object ProgressAndAchievements : QuranLearningDestination
    data object AdminContentVerification : QuranLearningDestination
}
