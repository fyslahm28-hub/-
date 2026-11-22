package com.example.ui.quranlearning

/**
 * প্রতিটি স্বতন্ত্র পাঠের ব্লুপ্রিন্ট থেকে Duolingo-ধাঁচের বহু-ধাপবিশিষ্ট ইন্টারঅ্যাক্টিভ পাঠ ও কুইজ তৈরি করে।
 * প্রতিটি পাঠে স্বতন্ত্র আরবি বিষয়বস্তু, বাংলা ব্যাখ্যা, প্রামাণ্য কিতাবের সূত্র এবং ৫ ধরনের অনুশীলন ধাপ থাকে।
 */
internal data class LessonSpec(
    val title: String,
    val arabic: String,
    val pronunciation: String,
    val objective: String,
    val explanation: String,
    val keyRule: String,
    val wrongStatement: String,
    val rightStatement: String,
    val mcqQuestion: String,
    val mcqOptions: List<String>,
    val mcqCorrectIndex: Int,
    val pairs: List<Pair<String, String>>,
    val fillSentence: String,
    val fillChoices: List<String>,
    val fillCorrectIndex: Int,
    val sequencePrompt: String? = null,
    val sequenceCorrect: List<String>? = null,
    val sourceRef: String,
    val isRevision: Boolean = false,
    val isFinalAssessment: Boolean = false,
    val hasVerifiedAudio: Boolean = true
)

internal fun buildLessonsFromSpecs(
    level: QuranLearningLevel,
    moduleId: String,
    startGlobalOrder: Int,
    specs: List<LessonSpec>
): List<QuranLesson> {
    return specs.mapIndexed { idx, spec ->
        val orderInModule = idx + 1
        val globalOrder = startGlobalOrder + idx
        val lessonId = "${level.id}_${moduleId}_l${orderInModule.toString().padStart(2, '0')}"
        val lessonNumBangla = "পাঠ ${toAcademyBanglaDigits(globalOrder)}"
        val audioRef = if (spec.hasVerifiedAudio) spec.arabic else null

        val steps = mutableListOf<QuranLessonStep>()

        // ধাপ ১: শিক্ষামূলক কার্ড (আরবি প্রদর্শন, বাংলা ব্যাখ্যা ও অডিও অনুশীলন)
        steps += QuranLessonStep.TeachingCard(
            stepId = "${lessonId}_s1",
            headingBangla = if (spec.isFinalAssessment) {
                "চূড়ান্ত মূল্যায়ন নির্দেশিকা — ${spec.title}"
            } else if (spec.isRevision) {
                "মডিউল রিভিশন — ${spec.title}"
            } else {
                spec.title
            },
            arabicDisplay = spec.arabic,
            pronunciationBangla = spec.pronunciation,
            explanationBangla = spec.explanation,
            keyRuleBangla = spec.keyRule,
            verifiedAudioText = audioRef
        )

        // ধাপ ২: সঠিক উত্তর / উচ্চারণ নির্বাচন (MCQ + Audio option if available)
        steps += QuranLessonStep.SelectOption(
            stepId = "${lessonId}_s2",
            promptBangla = spec.mcqQuestion,
            arabicDisplay = spec.arabic,
            options = spec.mcqOptions,
            correctIndex = spec.mcqCorrectIndex,
            feedbackExplanationBangla = spec.keyRule,
            verifiedAudioText = audioRef,
            isAudioQuestion = audioRef != null && orderInModule % 2 == 1
        )

        // ধাপ ৩: হরফ/শব্দের সঙ্গে উচ্চারণ বা নিয়ম মেলানো (Match Pairs)
        if (spec.pairs.size >= 2) {
            steps += QuranLessonStep.MatchPairs(
                stepId = "${lessonId}_s3",
                promptBangla = "বাম পাশের আরবি অংশের সাথে ডান পাশের সঠিক বাংলা অর্থ বা উচ্চারণ মেলান:",
                pairs = spec.pairs.mapIndexed { pIdx, (ar, bn) ->
                    MatchPairItem(
                        id = "${lessonId}_p$pIdx",
                        leftArabic = ar,
                        rightBangla = bn
                    )
                },
                feedbackExplanationBangla = "সঠিক মিলগুলো মনে রাখুন: ${spec.pairs.joinToString(" • ") { "${it.first} = ${it.second}" }}"
            )
        }

        // ধাপ ৪: শূন্যস্থান পূরণ অথবা সঠিক ক্রম সাজানো
        if (spec.sequenceCorrect != null && spec.sequenceCorrect.size >= 3) {
            val shuffled = spec.sequenceCorrect.reversed().let { rev ->
                if (rev == spec.sequenceCorrect) spec.sequenceCorrect.drop(1) + spec.sequenceCorrect.first() else rev
            }
            steps += QuranLessonStep.OrderSequence(
                stepId = "${lessonId}_s4",
                promptBangla = spec.sequencePrompt ?: "নিচের অংশগুলো সঠিক ক্রমে সাজান:",
                arabicHint = spec.arabic,
                correctOrder = spec.sequenceCorrect,
                shuffledOrder = shuffled,
                feedbackExplanationBangla = "সঠিক ক্রম: ${spec.sequenceCorrect.joinToString(" ← ")}"
            )
        } else {
            steps += QuranLessonStep.FillBlank(
                stepId = "${lessonId}_s4",
                promptBangla = "সঠিক শব্দ বা নিয়ম নির্বাচন করে শূন্যস্থান পূরণ করুন:",
                sentenceWithBlank = spec.fillSentence,
                arabicCue = spec.arabic,
                choices = spec.fillChoices,
                correctChoiceIndex = spec.fillCorrectIndex,
                feedbackExplanationBangla = spec.keyRule
            )
        }

        // ধাপ ৫: ভুল ও সঠিক উদাহরণ তুলনা
        val rightFirst = globalOrder % 2 == 0
        steps += QuranLessonStep.CompareExample(
            stepId = "${lessonId}_s5",
            promptBangla = "কোনটি সঠিক কুরআনী / তাজবীদি পদ্ধতি? নির্বাচন করুন:",
            arabicSubject = spec.arabic,
            optionA = if (rightFirst) spec.rightStatement else spec.wrongStatement,
            optionB = if (rightFirst) spec.wrongStatement else spec.rightStatement,
            correctIsOptionA = rightFirst,
            feedbackExplanationBangla = "সঠিক পদ্ধতি: ${spec.rightStatement}"
        )

        val quizList = listOf(
            QuranQuizQuestion(
                id = "${lessonId}_q1",
                level = level,
                questionBangla = spec.mcqQuestion,
                arabicPrompt = spec.arabic,
                options = spec.mcqOptions,
                correctOptionIndex = spec.mcqCorrectIndex,
                explanationBangla = spec.keyRule
            )
        )

        QuranLesson(
            id = lessonId,
            level = level,
            moduleId = moduleId,
            lessonOrderInModule = orderInModule,
            globalOrderInLevel = globalOrder,
            lessonNumberBangla = lessonNumBangla,
            titleBangla = spec.title,
            descriptionBangla = spec.objective,
            learningObjectiveBangla = spec.objective,
            arabicContent = spec.arabic,
            pronunciationGuideBangla = spec.pronunciation,
            bengaliExplanation = spec.explanation,
            keyRuleBangla = spec.keyRule,
            wrongVsRightNoteBangla = "সঠিক: ${spec.rightStatement} | বর্জনীয় ভুল: ${spec.wrongStatement}",
            audioReference = audioRef,
            practiceSteps = steps,
            quizData = quizList,
            sourceReference = spec.sourceRef,
            verificationStatus = VerificationStatus.VERIFIED,
            publicationStatus = PublicationStatus.PUBLISHED,
            isRevisionLesson = spec.isRevision,
            isFinalLevelAssessment = spec.isFinalAssessment
        )
    }
}
