package com.example.ui.quranlearning

/**
 * কুরআন শিক্ষা একাডেমির কেন্দ্রীয় ক্যাটালগ ও কনটেন্ট ম্যানেজমেন্ট (CMS) লেয়ার।
 * - মোট ৩০টি মডিউল (প্রতি স্তরে ১০টি)
 * - মোট ৩০৬টি যাচাইকৃত ও প্রকাশিত স্বতন্ত্র পাঠ (প্রতি স্তরে ১০২টি)
 * - ৩টি অপ্রকাশিত/যাচাই অপেক্ষমাণ খসড়া পাঠ (অ্যাডমিন অনুমোদন ছাড়া শিক্ষার্থীর কাছে অদৃশ্য)
 */
object QuranLearningCatalog {

    const val HEADER_ARABIC_TITLE = "تَعْلِيمُ الْقُرْآنِ"
    const val HEADER_BANGLA_TITLE = "কুরআন শিক্ষা"
    const val HEADER_SUBTITLE = "শিখুন ধাপে ধাপে"

    const val SCHOLARLY_VERIFICATION_NOTE =
        "অডিও ও ডিজিটাল অনুশীলন কেবল সহায়ক মাধ্যম; কুরআন তিলাওয়াতের চূড়ান্ত বিশুদ্ধতা একজন যোগ্য ক্বারী বা শিক্ষকের নিকট শোনানোর মাধ্যমে যাচাই করা উত্তম।"

    val allModules: List<QuranLearningModule> by lazy {
        QuranPrimaryCurriculum.modules +
            QuranIntermediateCurriculum.modules +
            QuranAdvancedCurriculum.modules
    }

    val allVerifiedLessons: List<QuranLesson> by lazy {
        QuranPrimaryCurriculum.lessons +
            QuranIntermediateCurriculum.lessons +
            QuranAdvancedCurriculum.lessons
    }

    /**
     * অ্যাডমিন যাচাইয়ের জন্য অপেক্ষমাণ (PENDING_REVIEW / UNPUBLISHED) CMS পাঠসমূহ।
     * শিক্ষার্থীরা কোনোভাবেই এগুলো দেখতে পাবে না যতক্ষণ না অনুমোদিত অ্যাডমিন যাচাই ও প্রকাশ করেন।
     */
    val pendingCmsLessons: List<QuranLesson> by lazy {
        val rawSpecs = listOf(
            Triple(
                QuranLearningLevel.PRIMARY,
                "m10",
                LessonSpec(
                    title = "অতিরিক্ত অনুশীলন: সূরা মাঊন শব্দ বিশ্লেষণ",
                    arabic = "أَرَأَيْتَ الَّذِي يُكَذِّبُ بِالدِّينِ",
                    pronunciation = "আরাআইতাল্লাযী ইউকাযযিবু বিদ্দীন",
                    objective = "সূরা মাঊনের প্রথম আয়াতের হরফ ও যুক্তাক্ষর অনুশীলন।",
                    explanation = "أَرَأَيْتَ শব্দে দুটি হামযা স্পষ্ট এবং ي সাকিন লীন। يُكَذِّبُ-এ নরম যাল তাশদিদযুক্ত।",
                    keyRule = "ধাপে ধাপে প্রতিটি হরকত ও তাশদিদ ভেঙে পড়ুন।",
                    wrongStatement = "يُكَذِّبُ-এ যালকে শিসযুক্ত যা (ز) পড়া",
                    rightStatement = "يُكَذِّبُ-এ জিহ্বার আগা দাঁতের কিনারায় রেখে নরম যাল পড়া",
                    mcqQuestion = "يُكَذِّبُ শব্দে কোন হরফটির উপর তাশদিদ রয়েছে?",
                    mcqOptions = listOf("ذ (যাল)", "ك (কাফ)", "ب (বা)", "ي (ইয়া)"),
                    mcqCorrectIndex = 0,
                    pairs = listOf("أَرَأَيْتَ" to "তুমি কি দেখেছ", "يُكَذِّبُ" to "মিথ্যা প্রতিপন্ন করে", "بِالدِّينِ" to "প্রতিদান দিবসকে"),
                    fillSentence = "بِالدِّينِ শব্দে দাল শামসী হওয়ায় ___ বিলুপ্ত হয়েছে।",
                    fillChoices = listOf("লাম (ل)", "বা (ب)", "নূন (ن)"),
                    fillCorrectIndex = 0,
                    sourceRef = "নূরানী কায়দা — খসড়া সংযোজন (অ্যাডমিন যাচাই অপেক্ষমাণ)",
                    hasVerifiedAudio = false
                )
            ),
            Triple(
                QuranLearningLevel.INTERMEDIATE,
                "m10",
                LessonSpec(
                    title = "অতিরিক্ত অনুশীলন: সূরা আসর তাজবীদ ছক",
                    arabic = "وَالْعَصْرِ ۝ إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ",
                    pronunciation = "ওয়াল আসর — ইন্নাল ইনসানা লাফী খুসর",
                    objective = "সূরা আসরের সম্পূর্ণ তাজবীদ বিশ্লেষণ।",
                    explanation = "وَالْعَصْرِ (মোটা র ওয়াকফে), إِنَّ (ওয়াজিব গুন্নাহ), الْإِنْسَانَ (পাতলা ইখফা), خُسْرٍ (মোটা র ওয়াকফে)।",
                    keyRule = "দুই সাকিনের মাঝে ওয়াকফে র-এর তাফখীম পূর্বের পূর্ববর্তী হরকত দেখে নির্ধারিত হয়।",
                    wrongStatement = "خُسْرٍ-এ ওয়াকফ করলে র পাতলা পড়া",
                    rightStatement = "خ-এ পেশ থাকায় ওয়াকফে خُسْرٍ-এর র মোটা পড়া",
                    mcqQuestion = "إِنَّ الْإِنْسَانَ অংশে কোন দুটি তাজবীদ নিয়ম পরপর আছে?",
                    mcqOptions = listOf("ওয়াজিব গুন্নাহ ও ইখফায়ে হাকীকী", "ইকলাব ও সাকতাহ", "ইমালাহ ও তাসহীল", "দুটিই ইযহার"),
                    mcqCorrectIndex = 0,
                    pairs = listOf("إِنَّ" to "ওয়াজিব গুন্নাহ", "الْإِنْسَانَ" to "পাতলা ইখফা", "خُسْرٍ" to "ওয়াকফে মোটা র"),
                    fillSentence = "الْإِنْسَانَ শব্দে নূন সাকিনের পর ___ আসায় পাতলা ইখফা হয়েছে।",
                    fillChoices = listOf("সীন (س)", "সাদ (ص)", "কফ (ق)"),
                    fillCorrectIndex = 0,
                    sourceRef = "জামালুল কুরআন — খসড়া সংযোজন (অ্যাডমিন যাচাই অপেক্ষমাণ)",
                    hasVerifiedAudio = false
                )
            ),
            Triple(
                QuranLearningLevel.ADVANCED,
                "m10",
                LessonSpec(
                    title = "অতিরিক্ত অনুশীলন: সূরা তীনের ওয়াকফ ও কলকলা",
                    arabic = "لَقَدْ خَلَقْنَا الْإِنْسَانَ فِي أَحْسَنِ تَقْوِيمٍ",
                    pronunciation = "লাক্বাদ খালাক্বনাল ইনসানা ফী আহসানি তাক্বভীম",
                    objective = "একাধিক কলকলায়ে সুগরা, ইখফা ও মুনফাসিলের সমন্বিত চর্চা।",
                    explanation = "لَقَدْ (সুগরা দ), خَلَقْنَا (সুগরা কফ), الْإِنْسَانَ (পাতলা ইখফা), فِي أَحْسَنِ (মুনফাসিল ৪-৫), تَقْوِيمٍ (সুগরা কফ + মাদ্দে আরিয)।",
                    keyRule = "একই আয়াতে তিনটি কলকলায়ে সুগরাকে সুষম ও পরিমিত মাত্রায় আদায় করুন।",
                    wrongStatement = "فِي أَحْسَنِ-এ ১ আলিফ টেনে দ্রুত পড়া",
                    rightStatement = "দুই শব্দে হামযা আসায় فِي أَحْسَنِ-এ ৪-৫ হরকত মাদ্দে মুনফাসিল পড়া",
                    mcqQuestion = "لَقَدْ خَلَقْنَا الْإِنْسَانَ فِي أَحْسَنِ تَقْوِيمٍ আয়াতে মোট কয়টি কলকলায়ে সুগরা রয়েছে?",
                    mcqOptions = listOf("১টি", "২টি", "৩টি (د، ق، ق)", "৪টি"),
                    mcqCorrectIndex = 2,
                    pairs = listOf("لَقَدْ" to "দাল কলকলায়ে সুগরা", "خَلَقْنَا" to "কফ মোটা সুগরা", "فِي أَحْسَنِ" to "মাদ্দে মুনফাসিল"),
                    fillSentence = "خَلَقْنَا এবং تَقْوِيمٍ শব্দে কফ সাকিন হওয়ায় ___ কলকলায়ে সুগরা হবে।",
                    fillChoices = listOf("মোটা", "পাতলা", "গুন্নাহযুক্ত"),
                    fillCorrectIndex = 0,
                    sourceRef = "ফাওয়ায়েদে মাক্কিয়্যাহ — খসড়া সংযোজন (অ্যাডমিন যাচাই অপেক্ষমাণ)",
                    hasVerifiedAudio = false
                )
            )
        )

        rawSpecs.mapIndexed { index, (level, modId, spec) ->
            val built = buildLessonsFromSpecs(
                level = level,
                moduleId = modId,
                startGlobalOrder = 103,
                specs = listOf(spec)
            ).first()
            built.copy(
                id = "${level.id}_cms_pending_${index + 1}",
                lessonOrderInModule = 13,
                lessonNumberBangla = "খসড়া ${toAcademyBanglaDigits(index + 1)}",
                verificationStatus = VerificationStatus.PENDING_REVIEW,
                publicationStatus = PublicationStatus.UNPUBLISHED
            )
        }
    }

    /**
     * শিক্ষার্থীর জন্য দৃশ্যমান পাঠসমূহ।
     * কেবল VERIFIED + PUBLISHED পাঠই এখানে থাকবে (অ্যাডমিন কোনো খসড়া অনুমোদন করলে তবেই যুক্ত হবে)।
     */
    fun getStudentVisibleLessons(adminApprovedIds: Set<String> = emptySet()): List<QuranLesson> {
        val approvedPending = pendingCmsLessons
            .filter { adminApprovedIds.contains(it.id) }
            .map {
                it.copy(
                    verificationStatus = VerificationStatus.VERIFIED,
                    publicationStatus = PublicationStatus.PUBLISHED
                )
            }
        return (allVerifiedLessons + approvedPending).filter { it.isStudentVisible }
    }

    /**
     * অ্যাডমিন যাচাই প্যানেলের জন্য প্রকাশিত ও অপ্রকাশিত সকল CMS পাঠের সমন্বিত তালিকা।
     */
    fun getAllCmsManagedLessons(adminApprovedIds: Set<String> = emptySet()): List<QuranLesson> {
        val pendingWithOverride = pendingCmsLessons.map { lesson ->
            if (adminApprovedIds.contains(lesson.id)) {
                lesson.copy(
                    verificationStatus = VerificationStatus.VERIFIED,
                    publicationStatus = PublicationStatus.PUBLISHED
                )
            } else {
                lesson
            }
        }
        return pendingWithOverride + allVerifiedLessons
    }

    fun getModulesForLevel(level: QuranLearningLevel): List<QuranLearningModule> =
        allModules.filter { it.level == level }.sortedBy { it.moduleNumber }

    fun getModuleById(level: QuranLearningLevel, moduleId: String): QuranLearningModule? =
        allModules.firstOrNull { it.level == level && it.moduleId == moduleId }

    fun getLessonsForLevel(
        level: QuranLearningLevel,
        adminApprovedIds: Set<String> = emptySet()
    ): List<QuranLesson> =
        getStudentVisibleLessons(adminApprovedIds)
            .filter { it.level == level }
            .sortedBy { it.globalOrderInLevel }

    fun getLessonsForModule(
        level: QuranLearningLevel,
        moduleId: String,
        adminApprovedIds: Set<String> = emptySet()
    ): List<QuranLesson> =
        getLessonsForLevel(level, adminApprovedIds)
            .filter { it.moduleId == moduleId }
            .sortedBy { it.lessonOrderInModule }

    fun getLessonById(
        lessonId: String,
        adminApprovedIds: Set<String> = emptySet()
    ): QuranLesson? =
        getStudentVisibleLessons(adminApprovedIds).firstOrNull { it.id == lessonId }

    fun getNextLessonInLevel(
        currentLesson: QuranLesson,
        adminApprovedIds: Set<String> = emptySet()
    ): QuranLesson? {
        val levelLessons = getLessonsForLevel(currentLesson.level, adminApprovedIds)
        val idx = levelLessons.indexOfFirst { it.id == currentLesson.id }
        return if (idx in 0 until levelLessons.lastIndex) levelLessons[idx + 1] else null
    }

    fun getAllRevisionAndAssessmentLessons(
        adminApprovedIds: Set<String> = emptySet()
    ): List<QuranLesson> =
        getStudentVisibleLessons(adminApprovedIds)
            .filter { it.isRevisionLesson || it.isFinalLevelAssessment }
}
