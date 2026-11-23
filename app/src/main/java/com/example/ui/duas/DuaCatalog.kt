package com.example.ui.duas

import com.example.ui.quranlearning.QuranLearningAudioCatalog
import com.example.ui.quranlearning.VerifiedQuranAudioSource
import java.util.Calendar

/**
 * দোয়া ও যিকির বিভাগের কেন্দ্রীয় ক্যাটালগ, সার্চ ইঞ্জিন, দৈনিক আমল সূচি এবং যাচাইকৃত অডিও রেজলভার।
 */
object DuaCatalog {

    const val PRONUNCIATION_GUIDANCE_NOTE =
        "দ্রষ্টব্য: বাংলা উচ্চারণ কেবল প্রাথমিক সহায়ক; শুদ্ধ আরবি মাখরাজ ও তাজবীদ শেখার বিকল্প নয়।"

    val allCategories: List<DuaCategory> = DuaCategory.entries.sortedBy { it.order }

    val allDuas: List<VerifiedDua> by lazy {
        DuaCatalogPart1.duas + DuaCatalogPart2.duas
    }

    private val duasById: Map<String, VerifiedDua> by lazy {
        allDuas.associateBy { it.id }
    }

    private val duasByCategoryMap: Map<DuaCategory, List<VerifiedDua>> by lazy {
        allDuas.groupBy { it.category }
    }

    fun getDuasForCategory(category: DuaCategory): List<VerifiedDua> =
        duasByCategoryMap[category].orEmpty()

    fun getDuaById(id: String): VerifiedDua? = duasById[id]

    /**
     * আজকের দোয়া — বছরের দিনের সংখ্যা অনুযায়ী নির্ধারিত (প্রতিদিন পরিবর্তন হয়, অফলাইনে কাজ করে)।
     */
    fun getDuaOfTheDay(dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)): VerifiedDua {
        val list = allDuas
        val safeIdx = ((dayOfYear - 1).coerceAtLeast(0)) % list.size
        return list[safeIdx]
    }

    /**
     * দৈনিক আমলের জন্য নির্ধারিত মাসনুন দোয়া ও যিকিরসমূহ।
     */
    fun getDailyAmalDuas(): List<VerifiedDua> =
        allDuas.filter { it.dailyAmalSlot != null }

    fun getDailyAmalDuasBySlot(slot: DailyAmalSlot): List<VerifiedDua> =
        allDuas.filter { it.dailyAmalSlot == slot }

    /**
     * বাংলা শিরোনাম, উপলক্ষ, আরবি পাঠ, বাংলা উচ্চারণ, বাংলা অর্থ, রেফারেন্স ও ক্যাটাগরি জুড়ে তাৎক্ষণিক সার্চ।
     */
    fun searchDuas(
        query: String,
        sourceFilter: DuaSourceFilter = DuaSourceFilter.ALL
    ): List<VerifiedDua> {
        val base = when (sourceFilter) {
            DuaSourceFilter.ALL -> allDuas
            DuaSourceFilter.QURAN -> allDuas.filter { it.sourceType == DuaSourceType.QURAN }
            DuaSourceFilter.HADITH -> allDuas.filter { it.sourceType == DuaSourceType.HADITH }
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return base

        return base.filter { dua ->
            dua.titleBangla.contains(trimmed, ignoreCase = true) ||
                dua.occasionBangla.contains(trimmed, ignoreCase = true) ||
                dua.banglaPronunciation.contains(trimmed, ignoreCase = true) ||
                dua.banglaMeaning.contains(trimmed, ignoreCase = true) ||
                dua.referenceBangla.contains(trimmed, ignoreCase = true) ||
                dua.arabicText.contains(trimmed, ignoreCase = true) ||
                dua.category.titleBangla.contains(trimmed, ignoreCase = true)
        }
    }

    /**
     * দোয়ার জন্য যাচাইকৃত অডিও সোর্স নির্ণয় করে:
     * - পবিত্র কুরআনের পূর্ণ আয়াত (quranSurahAyah != null) হলে শাইখ আল-হুসারীর যাচাইকৃত MP3 (EveryAyah + Islamic Network ব্যাকআপ)।
     * - হাদিসের দোয়া বা আয়াতাংশ হলে পরিশোধিত আরবি উচ্চারণ MP3 (ক্যাশসহ) + অফলাইন আরবি TTS ফলাব্যাক।
     * - hasVerifiedAudio = false অথবা আরবি পাঠ খালি হলে null ফেরত দেয়, যাতে কোনো ভুয়া বাটন না দেখায়।
     */
    fun resolveAudioSource(dua: VerifiedDua): VerifiedQuranAudioSource? {
        if (!dua.hasVerifiedAudio) return null
        val sanitized = QuranLearningAudioCatalog.sanitizeArabicForSpeech(dua.arabicText)
        if (sanitized.isBlank()) return null

        val surahAyah = dua.quranSurahAyah
        if (surahAyah != null) {
            val (surah, ayah) = surahAyah
            val primary = QuranLearningAudioCatalog.buildEveryAyahHusaryUrl(surah, ayah)
            val fallback = QuranLearningAudioCatalog.buildIslamicNetworkHusaryUrl(surah, ayah)
            if (primary != null || fallback != null) {
                return VerifiedQuranAudioSource(
                    audioKey = "dua_ayah_${surah.toString().padStart(3, '0')}_${ayah.toString().padStart(3, '0')}",
                    primaryMp3Url = primary,
                    fallbackMp3Url = fallback,
                    sanitizedArabicSpeechText = sanitized,
                    isVerifiedAyahRecitation = true,
                    sourceLabelBangla = "শাইখ হুসারী • সূরা ${toDuaBanglaDigits(surah)}:${toDuaBanglaDigits(ayah)}"
                )
            }
        }

        val primarySpeech = QuranLearningAudioCatalog.buildArabicSpeechPrimaryUrl(sanitized)
        val backupSpeech = QuranLearningAudioCatalog.buildArabicSpeechBackupUrl(sanitized)
        return VerifiedQuranAudioSource(
            audioKey = "dua_speech_${dua.id}",
            primaryMp3Url = primarySpeech,
            fallbackMp3Url = backupSpeech,
            sanitizedArabicSpeechText = sanitized,
            isVerifiedAyahRecitation = false,
            sourceLabelBangla = "আরবি দোয়া অডিও"
        )
    }

    /**
     * শেয়ার করার জন্য গোছানো বাংলা ও আরবি টেক্সট তৈরি করে।
     */
    fun formatShareText(dua: VerifiedDua): String {
        return buildString {
            appendLine(dua.titleBangla)
            appendLine()
            appendLine(dua.arabicText)
            appendLine()
            appendLine("উচ্চারণ: ${dua.banglaPronunciation}")
            appendLine()
            appendLine("অর্থ: ${dua.banglaMeaning}")
            appendLine()
            appendLine("রেফারেন্স: ${dua.referenceBangla} (${dua.gradingBangla})")
        }.trim()
    }
}
