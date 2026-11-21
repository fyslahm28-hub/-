package com.example.ui.quranlearning

/**
 * কুরআন শিক্ষা (تَعْلِيمُ الْقُرْآنِ) একাডেমির ডেটা মডেল।
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

data class QuranLessonExample(
    val arabic: String,
    val pronunciationBangla: String,
    val noteBangla: String
)

data class QuranLesson(
    val id: String,
    val level: QuranLearningLevel,
    val lessonNumberBangla: String,
    val titleBangla: String,
    val arabicHeading: String,
    val summaryBangla: String,
    val explanationBangla: String,
    val examples: List<QuranLessonExample>,
    val keyRuleBangla: String,
    val practiceTipBangla: String
)

data class QuranListeningItem(
    val id: String,
    val categoryBangla: String,
    val arabicText: String,
    val phoneticBangla: String,
    val makhrajRuleBangla: String,
    val level: QuranLearningLevel
)

data class QuranQuizQuestion(
    val id: String,
    val level: QuranLearningLevel,
    val questionBangla: String,
    val arabicPrompt: String? = null,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanationBangla: String
)

sealed interface QuranLearningDestination {
    data object AcademyHome : QuranLearningDestination
    data class LevelLessons(val level: QuranLearningLevel) : QuranLearningDestination
    data class LessonReader(val lessonId: String) : QuranLearningDestination
    data object ListenAndLearn : QuranLearningDestination
    data object PracticeAndQuiz : QuranLearningDestination
    data object LearningProgress : QuranLearningDestination
}
