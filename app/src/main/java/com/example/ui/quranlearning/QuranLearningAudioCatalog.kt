package com.example.ui.quranlearning

/**
 * কুরআন শিক্ষা বিভাগের যাচাইকৃত অডিও সোর্স ক্যাটালগ।
 *
 * ১. আয়াত ও সূরাভিত্তিক পাঠের জন্য শাইখ মাহমুদ খলিল আল-হুসারী (রহ.)-এর যাচাইকৃত তিলাওয়াত MP3
 *    (প্রাথমিক: EveryAyah Husary 64kbps, ব্যাকআপ: Islamic Network CDN Husary 64kbps)।
 * ২. হরফ, হরকত, সুকুন, তাশদিদ ও তাজবীদ শব্দের জন্য পরিশোধিত বিশুদ্ধ আরবি উচ্চারণ (Arabic TTS)।
 * ৩. অডিও রেফারেন্স না থাকলে বা বিশুদ্ধ আরবি অক্ষর না থাকলে null ফেরত দেয়, যাতে কোনো ভুয়া অডিও বাটন না দেখায়।
 */
data class VerifiedQuranAudioSource(
    val audioKey: String,
    val primaryMp3Url: String?,
    val fallbackMp3Url: String?,
    val sanitizedArabicSpeechText: String,
    val isVerifiedAyahRecitation: Boolean,
    val sourceLabelBangla: String
) {
    val hasRemoteMp3: Boolean
        get() = !primaryMp3Url.isNullOrBlank() || !fallbackMp3Url.isNullOrBlank()
}

data class QuranAyahRef(
    val surah: Int,
    val ayah: Int,
    val labelBangla: String
)

object QuranLearningAudioCatalog {

    private const val EVERY_AYAH_HUSARY_BASE = "https://everyayah.com/data/Husary_64kbps/"
    private const val ISLAMIC_NETWORK_HUSARY_BASE = "https://cdn.islamic.network/quran/audio/64/ar.husary/"

    /**
     * পবিত্র কুরআনের ১১৪টি সূরার সুনির্দিষ্ট আয়াত সংখ্যা (মোট ৬,২৩৬ আয়াত — হাফস আন আসিম)।
     */
    internal val SURAH_AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    /**
     * সূরা ও আয়াত নম্বর থেকে বৈশ্বিক আয়াত নম্বর (১..৬২৩৬) নির্ণয় করে।
     */
    fun computeGlobalAyahNumber(surah: Int, ayah: Int): Int? {
        if (surah !in 1..SURAH_AYAH_COUNTS.size) return null
        val maxAyah = SURAH_AYAH_COUNTS[surah - 1]
        if (ayah !in 1..maxAyah) return null
        var offset = 0
        for (i in 0 until (surah - 1)) {
            offset += SURAH_AYAH_COUNTS[i]
        }
        return offset + ayah
    }

    /**
     * EveryAyah যাচাইকৃত URL তৈরি করে: যেমন সূরা ১ আয়াত ১ -> 001001.mp3
     */
    fun buildEveryAyahHusaryUrl(surah: Int, ayah: Int): String? {
        if (computeGlobalAyahNumber(surah, ayah) == null) return null
        val sss = surah.toString().padStart(3, '0')
        val aaa = ayah.toString().padStart(3, '0')
        return "$EVERY_AYAH_HUSARY_BASE$sss$aaa.mp3"
    }

    /**
     * Islamic Network CDN ব্যাকআপ যাচাইকৃত URL তৈরি করে: যেমন ১ -> 1.mp3
     */
    fun buildIslamicNetworkHusaryUrl(surah: Int, ayah: Int): String? {
        val globalNumber = computeGlobalAyahNumber(surah, ayah) ?: return null
        return "$ISLAMIC_NETWORK_HUSARY_BASE$globalNumber.mp3"
    }

    /**
     * সুনির্দিষ্ট আয়াত ও সূরাভিত্তিক পাঠসমূহের যাচাইকৃত সূরা:আয়াত ম্যাপিং।
     */
    private val VERIFIED_LESSON_AYAH_MAP: Map<String, QuranAyahRef> = mapOf(
        // প্রাথমিক স্তর — মডিউল ১০ (সহজ কুরআন পাঠ ও ছোট সূরা)
        "primary_m10_l02" to QuranAyahRef(1, 1, "শাইখ হুসারী • সূরা ফাতিহা ১:১"),
        "primary_m10_l03" to QuranAyahRef(1, 2, "শাইখ হুসারী • সূরা ফাতিহা ১:২"),
        "primary_m10_l04" to QuranAyahRef(1, 5, "শাইখ হুসারী • সূরা ফাতিহা ১:৫"),
        "primary_m10_l05" to QuranAyahRef(1, 6, "শাইখ হুসারী • সূরা ফাতিহা ১:৬"),
        "primary_m10_l06" to QuranAyahRef(108, 1, "শাইখ হুসারী • সূরা কাউসার ১০৮:১"),
        "primary_m10_l07" to QuranAyahRef(103, 1, "শাইখ হুসারী • সূরা আসর ১০৩:১"),
        "primary_m10_l08" to QuranAyahRef(112, 1, "শাইখ হুসারী • সূরা ইখলাস ১১২:১"),
        "primary_m10_l09" to QuranAyahRef(110, 1, "শাইখ হুসারী • সূরা নাসর ১১০:১"),
        "primary_m10_l10" to QuranAyahRef(113, 1, "শাইখ হুসারী • সূরা ফালাক ১১৩:১"),
        "primary_m10_l11" to QuranAyahRef(114, 1, "শাইখ হুসারী • সূরা নাস ১১৪:১"),

        // উচ্চ স্তর — মডিউল ৬ (হাফস আন আসিমের বিশেষ কিরাআত আয়াত)
        "advanced_m06_l01" to QuranAyahRef(11, 41, "শাইখ হুসারী • সূরা হূদ ১১:৪১"),
        "advanced_m06_l02" to QuranAyahRef(12, 11, "শাইখ হুসারী • সূরা ইউসুফ ১২:১১"),
        "advanced_m06_l03" to QuranAyahRef(41, 44, "শাইখ হুসারী • সূরা ফুসসিলাত ৪১:৪৪"),
        "advanced_m06_l05" to QuranAyahRef(30, 54, "শাইখ হুসারী • সূরা রূম ৩০:৫৪"),
        "advanced_m06_l06" to QuranAyahRef(49, 11, "শাইখ হুসারী • সূরা হুজুরাত ৪৯:১১"),
        "advanced_m06_l07" to QuranAyahRef(27, 36, "শাইখ হুসারী • সূরা নামল ২৭:৩৬"),
        "advanced_m06_l09" to QuranAyahRef(76, 15, "শাইখ হুসারী • সূরা দাহর ৭৬:১৫"),

        // উচ্চ স্তর — মডিউল ৭ (আয়াতভিত্তিক উন্নত তিলাওয়াত)
        "advanced_m07_l01" to QuranAyahRef(2, 2, "শাইখ হুসারী • সূরা বাকারা ২:২"),
        "advanced_m07_l02" to QuranAyahRef(3, 26, "শাইখ হুসারী • সূরা আলে ইমরান ৩:২৬"),
        "advanced_m07_l03" to QuranAyahRef(18, 1, "শাইখ হুসারী • সূরা কাহফ ১৮:১"),
        "advanced_m07_l04" to QuranAyahRef(25, 63, "শাইখ হুসারী • সূরা ফুরকান ২৫:৬৩"),
        "advanced_m07_l05" to QuranAyahRef(31, 13, "শাইখ হুসারী • সূরা লুকমান ৩১:১৩"),
        "advanced_m07_l06" to QuranAyahRef(49, 10, "শাইখ হুসারী • সূরা হুজুরাত ৪৯:১০"),
        "advanced_m07_l07" to QuranAyahRef(55, 1, "শাইখ হুসারী • সূরা আর-রাহমান ৫৫:১"),
        "advanced_m07_l08" to QuranAyahRef(73, 4, "শাইখ হুসারী • সূরা মুযযাম্মিল ৭৩:৪"),
        "advanced_m07_l09" to QuranAyahRef(93, 1, "শাইখ হুসারী • সূরা দুহা ৯৩:১"),

        // উচ্চ স্তর — মডিউল ১০ (বাস্তব কুরআন প্রয়োগ)
        "advanced_m10_l01" to QuranAyahRef(2, 255, "শাইখ হুসারী • আয়াতুল কুরসী ২:২৫৫"),
        "advanced_m10_l02" to QuranAyahRef(2, 255, "শাইখ হুসারী • আয়াতুল কুরসী ২:২৫৫"),
        "advanced_m10_l03" to QuranAyahRef(2, 285, "শাইখ হুসারী • সূরা বাকারা ২:২৮৫"),
        "advanced_m10_l04" to QuranAyahRef(2, 286, "শাইখ হুসারী • সূরা বাকারা ২:২৮৬"),
        "advanced_m10_l05" to QuranAyahRef(59, 22, "শাইখ হুসারী • সূরা হাশর ৫৯:২২"),
        "advanced_m10_l06" to QuranAyahRef(36, 1, "শাইখ হুসারী • সূরা ইয়াসীন ৩৬:১"),
        "advanced_m10_l07" to QuranAyahRef(67, 1, "শাইখ হুসারী • সূরা মুলক ৬৭:১"),
        "advanced_m10_l08" to QuranAyahRef(78, 1, "শাইখ হুসারী • সূরা নাবা ৭৮:১"),
        "advanced_m10_l09" to QuranAyahRef(97, 1, "শাইখ হুসারী • সূরা কদর ৯৭:১"),
        "advanced_m10_l12" to QuranAyahRef(54, 17, "শাইখ হুসারী • সূরা ক্বামার ৫৪:১৭")
    )

    /**
     * নির্দিষ্ট আরবি আয়াত অংশের সাথে সরাসরি যাচাইকৃত সূরা:আয়াত শনাক্তকরণ (যেমন: মধ্যবর্তী বা উচ্চ স্তরে
     * ব্যবহৃত কুরআনের বিখ্যাত আয়াতসমূহ)।
     */
    private val VERIFIED_ARABIC_PHRASE_AYAH_MAP: List<Pair<String, QuranAyahRef>> = listOf(
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" to QuranAyahRef(1, 1, "শাইখ হুসারী • বিসমিল্লাহ ১:১"),
        "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ" to QuranAyahRef(1, 2, "শাইখ হুসারী • সূরা ফাতিহা ১:২"),
        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ" to QuranAyahRef(1, 5, "শাইখ হুসারী • সূরা ফাতিহা ১:৫"),
        "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ" to QuranAyahRef(1, 6, "শাইখ হুসারী • সূরা ফাতিহা ১:৬"),
        "اللَّهُ لَا إِلٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ" to QuranAyahRef(2, 255, "শাইখ হুসারী • আয়াতুল কুরসী ২:২৫৫"),
        "آمَنَ الرَّسُولُ بِمَا أُنْزِلَ" to QuranAyahRef(2, 285, "শাইখ হুসারী • সূরা বাকারা ২:২৮৫"),
        "لَا يُكَلِّفُ اللَّهُ نَفْسًا" to QuranAyahRef(2, 286, "শাইখ হুসারী • সূরা বাকারা ২:২৮৬"),
        "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ" to QuranAyahRef(108, 1, "শাইখ হুসারী • সূরা কাউসার ১০৮:১"),
        "قُلْ هُوَ اللَّهُ أَحَدٌ" to QuranAyahRef(112, 1, "শাইখ হুসারী • সূরা ইখলাস ১১২:১"),
        "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ" to QuranAyahRef(113, 1, "শাইখ হুসারী • সূরা ফালাক ১১৩:১"),
        "قُلْ أَعُوذُ بِرَبِّ النَّاسِ" to QuranAyahRef(114, 1, "শাইখ হুসারী • সূরা নাস ১১৪:১"),
        "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ" to QuranAyahRef(110, 1, "শাইখ হুসারী • সূরা নাসর ১১০:১"),
        "إِنَّا أَنْزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ" to QuranAyahRef(97, 1, "শাইখ হুসারী • সূরা কদর ৯৭:১"),
        "وَلَقَدْ يَسَّرْنَا الْقُرْآنَ لِلذِّكْرِ" to QuranAyahRef(54, 17, "শাইখ হুসারী • সূরা ক্বামার ৫৪:১৭"),
        "مَجْرٖىهَا وَمُرْسَاهَا" to QuranAyahRef(11, 41, "শাইখ হুসারী • সূরা হূদ ১১:৪১"),
        "لَا تَأْمَنَّا عَلَىٰ يُوسُفَ" to QuranAyahRef(12, 11, "শাইখ হুসারী • সূরা ইউসুফ ১২:১১"),
        "ءَاعْجَمِيٌّ وَعَرَبِيٌّ" to QuranAyahRef(41, 44, "শাইখ হুসারী • সূরা ফুসসিলাত ৪১:৪৪"),
        "وَاسْجُدْ وَاقْتَرِبْ" to QuranAyahRef(96, 19, "শাইখ হুসারী • সূরা আলাক ৯৬:১৯"),
        "وَالْعَصْرِ" to QuranAyahRef(103, 1, "শাইখ হুসারী • সূরা আসর ১০৩:১")
    )

    /**
     * একক হরকতবিহীন আরবি হরফগুলোকে পূর্ণ বিশুদ্ধ আরবি নামে রূপান্তর করে,
     * যাতে Arabic TTS (যেমন "ا  ب  ت") চুপ না থেকে স্পষ্টভাবে "ألف، باء، تاء" উচ্চারণ করে।
     */
    private val ISOLATED_LETTER_NAMES = mapOf(
        "ا" to "أَلِف",
        "أ" to "أَلِف",
        "ب" to "بَاء",
        "ت" to "تَاء",
        "ث" to "ثَاء",
        "ج" to "جِيم",
        "ح" to "حَاء",
        "خ" to "خَاء",
        "د" to "دَال",
        "ذ" to "ذَال",
        "ر" to "رَاء",
        "ز" to "زَاي",
        "س" to "سِين",
        "ش" to "شِين",
        "ص" to "صَاد",
        "ض" to "ضَاد",
        "ط" to "طَاء",
        "ظ" to "ظَاء",
        "ع" to "عَيْن",
        "غ" to "غَيْن",
        "ف" to "فَاء",
        "ق" to "قَاف",
        "ك" to "كَاف",
        "ل" to "لَام",
        "م" to "مِيم",
        "ن" to "نُون",
        "و" to "وَاو",
        "ه" to "هَاء",
        "ء" to "هَمْزَة",
        "ي" to "يَاء",
        "ة" to "تَاء مَرْبُوطَة"
    )

    private val PAREN_CONTENT_REGEX = Regex("\\([^)]*\\)")
    private val NON_ARABIC_NOISE_REGEX = Regex("[^\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\s،]")
    private val MULTISPACE_REGEX = Regex("\\s+")
    private val HAS_ARABIC_LETTER_REGEX = Regex("[\\u0621-\\u064A\\u0671]")

    /**
     * পাঠের raw আরবি স্ট্রিং (যাতে বাংলা সংখ্যা, বন্ধনী, বুলেট •, তীর → বা তাতউইল ـ থাকতে পারে)
     * পরিষ্কার করে বিশুদ্ধ উচ্চারণযোগ্য আরবি টেক্সটে রূপান্তর করে।
     */
    fun sanitizeArabicForSpeech(raw: String): String {
        if (raw.isBlank()) return ""

        var cleaned = PAREN_CONTENT_REGEX.replace(raw, " ")
            .replace('•', '،')
            .replace('۝', '،')
            .replace('۩', ' ')
            .replace('ۜ', ' ')
            .replace('ۛ', ' ')
            .replace("→", "،")
            .replace("←", "،")
            .replace("...", "،")
            .replace("ـ", "") // Tatweel সরিয়ে প্রকৃত হরফ রাখা

        cleaned = NON_ARABIC_NOISE_REGEX.replace(cleaned, " ")
        cleaned = MULTISPACE_REGEX.replace(cleaned, " ").trim()

        if (cleaned.isEmpty() || !HAS_ARABIC_LETTER_REGEX.containsMatchIn(cleaned)) {
            return ""
        }

        // প্রতিটি টোকেন যদি একক হরকতবিহীন আরবি হরফ হয় তবে তার বিশুদ্ধ আরবি নাম ব্যবহার করি
        val tokens = cleaned.split(" ").filter { it.isNotBlank() && it != "،" }
        val expandedTokens = tokens.map { token ->
            val strippedPunct = token.trim('،')
            val trailingComma = token.endsWith('،')
            val replacement = ISOLATED_LETTER_NAMES[strippedPunct] ?: strippedPunct
            if (trailingComma) "$replacement،" else replacement
        }

        // যদি সবগুলোই একক হরফ ছিল, কমা বসিয়ে স্পষ্ট বিরতি দিই
        val allIsolated = tokens.isNotEmpty() && tokens.all { ISOLATED_LETTER_NAMES.containsKey(it.trim('،')) }
        return if (allIsolated) {
            expandedTokens.joinToString("، ") { it.trimEnd('،') }
        } else {
            expandedTokens.joinToString(" ")
                .replace(Regex("،\\s*،+"), "،")
                .trim(' ', '،')
        }
    }

    /**
     * নির্দিষ্ট পাঠ ও আরবি টেক্সটের জন্য যাচাইকৃত অডিও সোর্স নির্ণয় করে।
     * যদি rawArabicText null/খালি হয় বা কোনো বিশুদ্ধ আরবি অক্ষর না থাকে তবে null ফেরত দেয়।
     */
    fun resolveAudioSource(lessonId: String?, rawArabicText: String?): VerifiedQuranAudioSource? {
        if (rawArabicText.isNullOrBlank()) return null

        val sanitizedSpeech = sanitizeArabicForSpeech(rawArabicText)
        if (sanitizedSpeech.isBlank()) return null

        val ayahRef = (lessonId?.let { VERIFIED_LESSON_AYAH_MAP[it] })
            ?: VERIFIED_ARABIC_PHRASE_AYAH_MAP.firstOrNull { (phrase, _) ->
                rawArabicText.contains(phrase)
            }?.second

        return if (ayahRef != null) {
            val primaryUrl = buildEveryAyahHusaryUrl(ayahRef.surah, ayahRef.ayah)
            val fallbackUrl = buildIslamicNetworkHusaryUrl(ayahRef.surah, ayahRef.ayah)
            val key = "ayah_${ayahRef.surah.toString().padStart(3, '0')}_${ayahRef.ayah.toString().padStart(3, '0')}"
            VerifiedQuranAudioSource(
                audioKey = key,
                primaryMp3Url = primaryUrl,
                fallbackMp3Url = fallbackUrl,
                sanitizedArabicSpeechText = sanitizedSpeech,
                isVerifiedAyahRecitation = primaryUrl != null || fallbackUrl != null,
                sourceLabelBangla = ayahRef.labelBangla
            )
        } else {
            val stableHash = sanitizedSpeech.hashCode().toUInt().toString(16)
            val key = "speech_${lessonId ?: "step"}_$stableHash"
            VerifiedQuranAudioSource(
                audioKey = key,
                primaryMp3Url = null,
                fallbackMp3Url = null,
                sanitizedArabicSpeechText = sanitizedSpeech,
                isVerifiedAyahRecitation = false,
                sourceLabelBangla = "আরবি উচ্চারণ ভয়েস"
            )
        }
    }
}
