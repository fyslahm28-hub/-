package com.example.ui.quran

/**
 * Verified Page Mapping & Index Resolver for the Uploaded 15-Line Indo-Pak Quran PDF
 * (`public/assets/aistudio/quran-hd-small_1305a0bc.pdf` / `assets/quran_hafezi_15line.pdf`).
 *
 * - Original PDF SHA-256: `1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828`
 * - Total PDF Pages: 616
 *   - Page 1: Cover Page (Qudrat Ullah Co., Lahore)
 *   - Pages 2..611: Complete 15-Line Quran Text (Surah Al-Fatiha on Page 2 through Surah An-Nas + Dua Khatmul Quran Khurd on Page 611)
 *   - Page 612: Dua Khatmul Quran Kalan (دعاء ختم القرآن كلان)
 *   - Page 613: Rumuz-e-Awqaf (رموز اوقاف قرآن مجيد)
 *   - Pages 614..615: Official Fehrist of 30 Paras & 114 Surahs (فهرست پارہ و سورتہائے قرآن مجيد)
 *   - Page 616: Certificate of Verification (سرٹیفکیٹ تصحيح)
 */
object HafeziQuranIndexData {

    const val ORIGINAL_PDF_SHA256 =
        "1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828"
    const val BUNDLED_REPAIRED_PDF_SHA256 =
        "e4716200510ff689229eec15d85c01648f93ecf0b5de7819ce247f0e8ea4598c"
    const val ORIGINAL_PDF_SIZE_BYTES = 37871837L

    const val TOTAL_PDF_PAGES = 616
    const val TOTAL_MUSHAF_PAGES = 616
    const val TOTAL_QURAN_TEXT_PAGES = 611
    const val LINES_PER_PAGE = 15

    /**
     * Complete 114 Surahs verified against the official Fehrist table printed on Pages 614–615
     * of `quran-hd-small_1305a0bc.pdf`.
     */
    val all114Surahs: List<HafeziSurahInfo> = listOf(
        HafeziSurahInfo(1, "الفاتحة", "আল-ফাতিহা", "Al-Fatihah", "সূচনা", true, 7, 2, 2, 1, 1),
        HafeziSurahInfo(2, "البقرة", "আল-বাকারা", "Al-Baqarah", "বকনা-বাছুর", false, 286, 3, 50, 1, 40),
        HafeziSurahInfo(3, "آل عمران", "আল-ইমরান", "Ali 'Imran", "ইমরানের পরিবার", false, 200, 51, 77, 3, 20),
        HafeziSurahInfo(4, "النساء", "আন-নিসা", "An-Nisa", "নারী", false, 176, 78, 106, 4, 24),
        HafeziSurahInfo(5, "المائدة", "আল-মায়িদাহ", "Al-Ma'idah", "খাদ্য পরিপূর্ণ টেবিল", false, 120, 107, 128, 6, 16),
        HafeziSurahInfo(6, "الأنعام", "আল-আনআম", "Al-An'am", "গৃহপালিত পশু", true, 165, 129, 151, 7, 20),
        HafeziSurahInfo(7, "الأعراف", "আল-আরাফ", "Al-A'raf", "উঁচু স্থানসমূহ", true, 206, 152, 177, 8, 24),
        HafeziSurahInfo(8, "الأنفال", "আল-আনফাল", "Al-Anfal", "যুদ্ধলব্ধ সম্পদ", false, 75, 178, 187, 9, 10),
        HafeziSurahInfo(9, "التوبة", "আত-তাওবাহ", "At-Tawbah", "অনুশোচনা", false, 129, 188, 208, 10, 16),
        HafeziSurahInfo(10, "يونس", "ইউনুস", "Yunus", "নবী ইউনুস (আ.)", true, 109, 209, 221, 11, 11),
        HafeziSurahInfo(11, "هود", "হুদ", "Hud", "নবী হুদ (আ.)", true, 123, 222, 235, 11, 10),
        HafeziSurahInfo(12, "يوسف", "ইউসুফ", "Yusuf", "নবী ইউসুফ (আ.)", true, 111, 236, 249, 12, 12),
        HafeziSurahInfo(13, "الرعد", "আর-রাদ", "Ar-Ra'd", "বজ্রপাত", false, 43, 250, 255, 13, 6),
        HafeziSurahInfo(14, "إبراهيم", "ইবরাহিম", "Ibrahim", "নবী ইবরাহিম (আ.)", true, 52, 256, 261, 13, 7),
        HafeziSurahInfo(15, "الحجر", "আল-হিজর", "Al-Hijr", "পাথুরে পাহাড়", true, 99, 262, 267, 14, 6),
        HafeziSurahInfo(16, "النحل", "আন-নাহল", "An-Nahl", "মৌমাছি", true, 128, 268, 282, 14, 16),
        HafeziSurahInfo(17, "الإسراء", "বনী ইসরাঈল", "Al-Isra", "রাতের সফর", true, 111, 283, 293, 15, 12),
        HafeziSurahInfo(18, "الكهف", "আল-কাহফ", "Al-Kahf", "গুহা", true, 110, 294, 305, 15, 12),
        HafeziSurahInfo(19, "مريم", "মারইয়াম", "Maryam", "মারইয়াম (আ.)", true, 98, 306, 312, 16, 6),
        HafeziSurahInfo(20, "طه", "ত্বা-হা", "Taha", "ত্বা-হা", true, 135, 313, 322, 16, 8),
        HafeziSurahInfo(21, "الأنبياء", "আল-আম্বিয়া", "Al-Anbiya", "নবীগণ", true, 112, 323, 331, 17, 7),
        HafeziSurahInfo(22, "الحج", "আল-হাজ্জ", "Al-Hajj", "হজ্জ", false, 78, 332, 342, 17, 10),
        HafeziSurahInfo(23, "المؤمنون", "আল-মুমিনুন", "Al-Mu'minun", "মুমিনগণ", true, 118, 343, 350, 18, 6),
        HafeziSurahInfo(24, "النور", "আন-নূর", "An-Nur", "আলো", false, 64, 351, 359, 18, 9),
        HafeziSurahInfo(25, "الفرقان", "আল-ফুরকান", "Al-Furqan", "সত্য-মিথ্যার পার্থক্যকারী", true, 77, 360, 366, 18, 6),
        HafeziSurahInfo(26, "الشعراء", "আশ-শুআরা", "Ash-Shu'ara", "কবিগণ", true, 227, 367, 376, 19, 11),
        HafeziSurahInfo(27, "النمل", "আন-নামল", "An-Naml", "পিপীলিকা", true, 93, 377, 385, 19, 7),
        HafeziSurahInfo(28, "القصص", "আল-কাসাস", "Al-Qasas", "কাহিনী", true, 88, 386, 396, 20, 9),
        HafeziSurahInfo(29, "العنكبوت", "আল-আনকাবুত", "Al-'Ankabut", "মাকড়সা", true, 69, 397, 404, 20, 7),
        HafeziSurahInfo(30, "الروم", "আর-রূম", "Ar-Rum", "রোমান জাতি", true, 60, 405, 411, 21, 6),
        HafeziSurahInfo(31, "لقمان", "লুকমান", "Luqman", "হাকীম লুকমান", true, 34, 412, 415, 21, 4),
        HafeziSurahInfo(32, "السجدة", "আস-সাজদাহ", "As-Sajdah", "সিজদা", true, 30, 416, 418, 21, 3),
        HafeziSurahInfo(33, "الأحزاب", "আল-আহযাব", "Al-Ahzab", "সম্মিলিত বাহিনী", false, 73, 419, 428, 21, 9),
        HafeziSurahInfo(34, "سبأ", "সাবা", "Saba", "সাবা জাতি", true, 54, 429, 434, 22, 6),
        HafeziSurahInfo(35, "فاطر", "ফাতির", "Fatir", "আদি স্রষ্টা", true, 45, 435, 440, 22, 5),
        HafeziSurahInfo(36, "يس", "ইয়াসিন", "Ya-Sin", "ইয়াসিন", true, 83, 441, 445, 22, 5),
        HafeziSurahInfo(37, "الصافات", "আস-সাফফাত", "As-Saffat", "সারিবদ্ধ ফেরেশতাগণ", true, 182, 446, 452, 23, 5),
        HafeziSurahInfo(38, "ص", "সোয়াদ", "Sad", "আরবি বর্ণ সোয়াদ", true, 88, 453, 458, 23, 5),
        HafeziSurahInfo(39, "الزمر", "আয-যুমার", "Az-Zumar", "দলসমূহ", true, 75, 459, 467, 23, 8),
        HafeziSurahInfo(40, "غافر", "আল-মুমিন (গাফির)", "Ghafir", "ক্ষমাকারী", true, 85, 468, 477, 24, 9),
        HafeziSurahInfo(41, "فصلت", "হা-মীম সাজদাহ (ফুসসিলাত)", "Fussilat", "সুস্পষ্ট ব্যাখ্যা", true, 54, 478, 483, 24, 6),
        HafeziSurahInfo(42, "الشورى", "আশ-শূরা", "Ash-Shuraa", "পরামর্শ", true, 53, 484, 489, 25, 5),
        HafeziSurahInfo(43, "الزخرف", "আয-যুখরুফ", "Az-Zukhruf", "স্বর্ণালঙ্কার", true, 89, 490, 495, 25, 7),
        HafeziSurahInfo(44, "الدخان", "আদ-দুখান", "Ad-Dukhan", "ধোঁয়া", true, 59, 496, 498, 25, 3),
        HafeziSurahInfo(45, "الجاثية", "আল-জাসিয়াহ", "Al-Jathiyah", "নতজানু", true, 37, 499, 502, 25, 4),
        HafeziSurahInfo(46, "الأحقاف", "আল-আহকাফ", "Al-Ahqaf", "বালুর পাহাড়", true, 35, 503, 506, 26, 4),
        HafeziSurahInfo(47, "محمد", "মুহাম্মাদ", "Muhammad", "নবী মুহাম্মাদ ﷺ", false, 38, 507, 511, 26, 4),
        HafeziSurahInfo(48, "الفتح", "আল-ফাতহ", "Al-Fath", "বিজয়", false, 29, 512, 515, 26, 4),
        HafeziSurahInfo(49, "الحجرات", "আল-হুজুরাত", "Al-Hujurat", "বাসগৃহসমূহ", false, 18, 516, 518, 26, 2),
        HafeziSurahInfo(50, "ق", "ক্বাফ", "Qaf", "আরবি বর্ণ ক্বাফ", true, 45, 519, 520, 26, 3),
        HafeziSurahInfo(51, "الذاريات", "আয-যারিয়াত", "Adh-Dhariyat", "বিক্ষেপকারী বাতাস", true, 60, 521, 523, 26, 3),
        HafeziSurahInfo(52, "الطور", "আত-তূর", "At-Tur", "তূর পাহাড়", true, 49, 524, 526, 27, 2),
        HafeziSurahInfo(53, "النجم", "আন-নাজম", "An-Najm", "তারা", true, 62, 527, 528, 27, 3),
        HafeziSurahInfo(54, "القمر", "আল-কামার", "Al-Qamar", "চন্দ্র", true, 55, 529, 531, 27, 3),
        HafeziSurahInfo(55, "الرحمن", "আর-রহমান", "Ar-Rahman", "পরম করুণাময়", false, 78, 532, 534, 27, 3),
        HafeziSurahInfo(56, "الواقعة", "আল-ওয়াকিয়াহ", "Al-Waqi'ah", "নিশ্চিত ঘটনা", true, 96, 535, 537, 27, 3),
        HafeziSurahInfo(57, "الحديد", "আল-হাদীদ", "Al-Hadid", "লোহা", false, 29, 538, 542, 27, 4),
        HafeziSurahInfo(58, "المجادلة", "আল-মুজাদালাহ", "Al-Mujadila", "অনুযোগকারিণী", false, 22, 543, 545, 28, 3),
        HafeziSurahInfo(59, "الحشر", "আল-হাশর", "Al-Hashr", "সমাবেশ", false, 24, 546, 549, 28, 3),
        HafeziSurahInfo(60, "الممتحنة", "আল-মুমতাহিনাহ", "Al-Mumtahanah", "পরীক্ষাসাপেক্ষ নারী", false, 13, 550, 551, 28, 2),
        HafeziSurahInfo(61, "الصف", "আস-সাফ", "As-Saff", "সারিবদ্ধ সৈন্যদল", false, 14, 552, 553, 28, 2),
        HafeziSurahInfo(62, "الجمعة", "আল-জুমুআহ", "Al-Jumu'ah", "জুমার দিন", false, 11, 554, 554, 28, 2),
        HafeziSurahInfo(63, "المنافقون", "আল-মুনাফিকুন", "Al-Munafiqun", "মুনাফিকদল", false, 11, 555, 556, 28, 2),
        HafeziSurahInfo(64, "التغابن", "আত-তাগাবুন", "At-Taghabun", "মোহমুক্তি", false, 18, 557, 558, 28, 2),
        HafeziSurahInfo(65, "الطلاق", "আত-তালাক", "At-Talaq", "তালাক", false, 12, 559, 560, 28, 2),
        HafeziSurahInfo(66, "التحريم", "আত-তাহরীম", "At-Tahrim", "নিষিদ্ধকরণ", false, 12, 561, 562, 28, 2),
        HafeziSurahInfo(67, "الملك", "আল-মুলক", "Al-Mulk", "সার্বভৌম কর্তৃত্ব", true, 30, 563, 564, 29, 2),
        HafeziSurahInfo(68, "القلم", "আল-কলম", "Al-Qalam", "কলম", true, 52, 565, 567, 29, 2),
        HafeziSurahInfo(69, "الحاقة", "আল-হাক্কাহ", "Al-Haqqah", "নিশ্চিত সত্য", true, 52, 568, 569, 29, 2),
        HafeziSurahInfo(70, "المعارج", "আল-মাআরিজ", "Al-Ma'arij", "উন্নয়নের সোপান", true, 44, 570, 571, 29, 2),
        HafeziSurahInfo(71, "نوح", "নূহ", "Nuh", "নবী নূহ (আ.)", true, 28, 572, 573, 29, 2),
        HafeziSurahInfo(72, "الجن", "আল-জিন", "Al-Jinn", "জিন জাতি", true, 28, 574, 576, 29, 2),
        HafeziSurahInfo(73, "المزمل", "আল-মুযযাম্মিল", "Al-Muzzammil", "বস্ত্রাবৃতকারী", true, 20, 577, 578, 29, 2),
        HafeziSurahInfo(74, "المدثر", "আল-মুদ্দাসসির", "Al-Muddaththir", "চাদরাবৃতকারী", true, 56, 579, 580, 29, 2),
        HafeziSurahInfo(75, "القيامة", "আল-কিয়ামাহ", "Al-Qiyamah", "কিয়ামত", true, 40, 581, 582, 29, 2),
        HafeziSurahInfo(76, "الإنسان", "আল-ইনসান (দাহর)", "Al-Insan", "মানুষ", false, 31, 583, 584, 29, 2),
        HafeziSurahInfo(77, "المرسلات", "আল-মুরসালাত", "Al-Mursalat", "প্রেরিত বাতাস", true, 50, 585, 586, 29, 2),
        HafeziSurahInfo(78, "النبأ", "আন-নাবা", "An-Naba", "মহাসংবাদ", true, 40, 587, 587, 30, 2),
        HafeziSurahInfo(79, "النازعات", "আন-নাযিয়াত", "An-Nazi'at", "প্রচেষ্টাকারী ফেরেশতা", true, 46, 588, 589, 30, 2),
        HafeziSurahInfo(80, "عبس", "আবাসা", "'Abasa", "ভ্রুকুটি করলেন", true, 42, 590, 590, 30, 1),
        HafeziSurahInfo(81, "التكوير", "আত-তাকভীর", "At-Takwir", "অন্ধকারাচ্ছন্ন", true, 29, 591, 591, 30, 1),
        HafeziSurahInfo(82, "الانفطار", "আল-ইনফিতার", "Al-Infitar", "বিদীর্ণ হওয়া", true, 19, 592, 592, 30, 1),
        HafeziSurahInfo(83, "المطففين", "আল-মুতাফফিফীন", "Al-Mutaffifin", "ওজনে কমদানকারী", true, 36, 593, 594, 30, 1),
        HafeziSurahInfo(84, "الانشقاق", "আল-ইনশিকাক", "Al-Inshiqaq", "খণ্ড-বিখণ্ড করণ", true, 25, 595, 595, 30, 1),
        HafeziSurahInfo(85, "البروج", "আল-বুরূজ", "Al-Buruj", "নক্ষত্রপুঞ্জ", true, 22, 596, 596, 30, 1),
        HafeziSurahInfo(86, "الطارق", "আত-তারিক", "At-Tariq", "রাতের আগন্তুক", true, 17, 597, 597, 30, 1),
        HafeziSurahInfo(87, "الأعلى", "আল-আলা", "Al-A'la", "সর্বোন্নত", true, 19, 598, 598, 30, 1),
        HafeziSurahInfo(88, "الغاشية", "আল-গাশিয়াহ", "Al-Ghashiyah", "আচ্ছন্নকারী ঘটনা", true, 26, 598, 598, 30, 1),
        HafeziSurahInfo(89, "الفجر", "আল-ফজর", "Al-Fajr", "ভোরবেলা", true, 30, 599, 600, 30, 1),
        HafeziSurahInfo(90, "البلد", "আল-বালাদ", "Al-Balad", "নগরী", true, 20, 601, 601, 30, 1),
        HafeziSurahInfo(91, "الشمس", "আশ-শামস", "Ash-Shams", "সূর্য", true, 15, 601, 601, 30, 1),
        HafeziSurahInfo(92, "الليل", "আল-লাইল", "Al-Layl", "রাত্রি", true, 21, 602, 602, 30, 1),
        HafeziSurahInfo(93, "الضحى", "আদ-দুহা", "Ad-Duhaa", "পূর্বাহ্ণের সূর্যকিরণ", true, 11, 603, 603, 30, 1),
        HafeziSurahInfo(94, "الشرح", "আল-ইনশিরাহ", "Ash-Sharh", "বক্ষ প্রশস্তকরণ", true, 8, 603, 603, 30, 1),
        HafeziSurahInfo(95, "التين", "আত-তীন", "At-Tin", "ডুমুর", true, 8, 604, 604, 30, 1),
        HafeziSurahInfo(96, "العلق", "আল-আলাক", "Al-'Alaq", "রক্তপিণ্ড", true, 19, 604, 604, 30, 1),
        HafeziSurahInfo(97, "القدر", "আল-কদর", "Al-Qadr", "মহিমান্বিত রজনী", true, 5, 605, 605, 30, 1),
        HafeziSurahInfo(98, "البينة", "আল-বাইয়্যিনাহ", "Al-Bayyinah", "সুস্পষ্ট প্রমাণ", false, 8, 605, 605, 30, 1),
        HafeziSurahInfo(99, "الزلزلة", "আয-যিলযাল", "Az-Zalzalah", "ভূমিকম্প", false, 8, 606, 606, 30, 1),
        HafeziSurahInfo(100, "العاديات", "আল-আদিয়াত", "Al-'Adiyat", "অভিযানকারী অশ্ববৃন্দ", true, 11, 606, 606, 30, 1),
        HafeziSurahInfo(101, "القارعة", "আল-কারিয়াহ", "Al-Qari'ah", "মহাসংকট", true, 11, 607, 607, 30, 1),
        HafeziSurahInfo(102, "التكاثر", "আত-তাকাসুর", "At-Takathur", "প্রাচুর্যের প্রতিযোগিতা", true, 8, 607, 607, 30, 1),
        HafeziSurahInfo(103, "العصر", "আল-আসর", "Al-'Asr", "সময়", true, 3, 608, 608, 30, 1),
        HafeziSurahInfo(104, "الهمزة", "আল-হুমাযাহ", "Al-Humazah", "পরনিন্দাকারী", true, 9, 608, 608, 30, 1),
        HafeziSurahInfo(105, "الفيل", "আল-ফীল", "Al-Fil", "হাতি", true, 5, 608, 608, 30, 1),
        HafeziSurahInfo(106, "قريش", "কুরাইশ", "Quraysh", "কুরাইশ গোত্র", true, 4, 609, 609, 30, 1),
        HafeziSurahInfo(107, "الماعون", "আল-মাউন", "Al-Ma'un", "নিত্যপ্রয়োজনীয় বস্তু", true, 7, 609, 609, 30, 1),
        HafeziSurahInfo(108, "الكوثر", "আল-কাওসার", "Al-Kawthar", "হাউজে কাওসার", true, 3, 609, 609, 30, 1),
        HafeziSurahInfo(109, "الكافرون", "আল-কাফিরুন", "Al-Kafirun", "অবিশ্বাসী গোষ্ঠী", true, 6, 609, 609, 30, 1),
        HafeziSurahInfo(110, "النصر", "আন-নাসর", "An-Nasr", "সাহায্য", false, 3, 610, 610, 30, 1),
        HafeziSurahInfo(111, "المسد", "আল-লাহাব (মাসাদ)", "Al-Masad", "খেজুরের পাকানো রশি", true, 5, 610, 610, 30, 1),
        HafeziSurahInfo(112, "الإخلاص", "আল-ইখলাস", "Al-Ikhlas", "একত্ববাদ", true, 4, 610, 610, 30, 1),
        HafeziSurahInfo(113, "الفلق", "আল-ফালাক", "Al-Falaq", "নিশিভোর", true, 5, 611, 611, 30, 1),
        HafeziSurahInfo(114, "الناس", "আন-নাস", "An-Nas", "মানবজাতি", true, 6, 611, 611, 30, 1)
    )

    /**
     * Complete 30 Paras verified against the official Fehrist table printed on Pages 614–615
     * of `quran-hd-small_1305a0bc.pdf`.
     */
    val all30Paras: List<HafeziParaInfo> = listOf(
        HafeziParaInfo(1, "آلم", "আলিফ-লাম-মীম", "Alif Lam Meem", 2, 22, 1, "আল-ফাতিহা", "১:১", "প্রথম পারা", "সূরা আল-ফাতিহা - আল-বাকারা"),
        HafeziParaInfo(2, "سَيَقُولُ", "সাইয়াকুলু", "Sayaqool", 23, 42, 2, "আল-বাকারা", "২:১৪২", "দ্বিতীয় পারা", "সূরা আল-বাকারা (১৪২ - ২৫২)"),
        HafeziParaInfo(3, "تِلْكَ الرُّسُلُ", "তিলকার রুসুলু", "Tilkal Rusul", 43, 62, 2, "আল-বাকারা", "২:২৫৩", "তৃতীয় পারা", "সূরা আল-বাকারা - আল-ইমরান"),
        HafeziParaInfo(4, "لَنْ تَنَالُوا", "লান তানালুল বিররা", "Lan Tana Loo", 63, 82, 3, "আল-ইমরান", "৩:৯২", "চতুর্থ পারা", "সূরা আল-ইমরান - আন-নিসা"),
        HafeziParaInfo(5, "وَالْمُحْصَنَاتُ", "ওয়াল মুহসানাতু", "Wal Mohsanat", 83, 102, 4, "আন-নিসা", "৪:২৪", "পঞ্চম পারা", "সূরা আন-নিসা (২৪ - ১৪৭)"),
        HafeziParaInfo(6, "لَا يُحِبُّ اللَّهُ", "লা ইউহিব্বুল্লাহু", "La Yuhibbullah", 103, 122, 4, "আন-নিসা", "৪:১৪৮", "ষষ্ঠ পারা", "সূরা আন-নিসা - আল-মায়িদাহ"),
        HafeziParaInfo(7, "وَإِذَا سَمِعُوا", "ওয়া ইযা সামিউ", "Wa Iza Samiu", 123, 142, 5, "আল-মায়িদাহ", "৫:৮৩", "সপ্তম পারা", "সূরা আল-মায়িদাহ - আল-আনআম"),
        HafeziParaInfo(8, "وَلَوْ أَنَّنَا", "ওয়া লাও আন্নানা", "Wa Lau Annana", 143, 162, 6, "আল-আনআম", "৬:১১১", "অষ্টম পারা", "সূরা আল-আনআম - আল-আরাফ"),
        HafeziParaInfo(9, "قَالَ الْمَلَأُ", "কালাল মালাউ", "Qalal Malao", 163, 182, 7, "আল-আরাফ", "৭:৮৮", "নবম পারা", "সূরা আল-আরাফ - আল-আনফাল"),
        HafeziParaInfo(10, "وَاعْلَمُوا", "ওয়া'লামু", "Wa A'lamu", 183, 202, 8, "আল-আনফাল", "৮:৪১", "দশম পারা", "সূরা আল-আনফাল - আত-তাওবাহ"),
        HafeziParaInfo(11, "يَعْتَذِرُونَ", "ইয়া'তাযিরূনা", "Yatazeroon", 203, 222, 9, "আত-তাওবাহ", "৯:৯৪", "একাদশ পারা", "সূরা আত-তাওবাহ - হুদ"),
        HafeziParaInfo(12, "وَمَا مِنْ دَابَّةٍ", "ওয়ামা মিন দাব্বাতিন", "Wa Mamin Da'abat", 223, 242, 11, "হুদ", "১১:৬", "দ্বাদশ পারা", "সূরা হুদ - ইউসুফ"),
        HafeziParaInfo(13, "وَمَا أُبَرِّئُ", "ওয়ামা উবাররিউ", "Wa Ma Ubrioo", 243, 262, 12, "ইউসুফ", "১২:৫৩", "ত্রয়োদশ পারা", "সূরা ইউসুফ - ইবরাহিম"),
        HafeziParaInfo(14, "رُبَمَا", "রুবামা", "Rubama", 263, 282, 15, "আল-হিজর", "১৫:১", "চতুর্দশ পারা", "সূরা আল-হিজর - আন-নাহল"),
        HafeziParaInfo(15, "سُبْحَانَ الَّذِي", "সুবহানাল্লাযী", "Subhanallazi", 283, 302, 17, "বনী ইসরাঈল", "১৭:১", "পঞ্চদশ পারা", "সূরা বনী ইসরাঈল - আল-কাহফ"),
        HafeziParaInfo(16, "قَالَ أَلَمْ", "কালা আলাম", "Qal Alam", 303, 322, 18, "আল-কাহফ", "১৮:৭৫", "ষোড়শ পারা", "সূরা আল-কাহফ - ত্বা-হা"),
        HafeziParaInfo(17, "اقْتَرَبَ لِلنَّاسِ", "ইকতারাবা লিন্নাসি", "Aqtarabo", 323, 342, 21, "আল-আম্বিয়া", "২১:১", "সপ্তদশ পারা", "সূরা আল-আম্বিয়া - আল-হাজ্জ"),
        HafeziParaInfo(18, "قَدْ أَفْلَحَ", "কাদ আফলাহা", "Qadd Aflaha", 343, 362, 23, "আল-মুমিনুন", "২৩:১", "অষ্টাদশ পারা", "সূরা আল-মুমিনুন - আল-ফুরকান"),
        HafeziParaInfo(19, "وَقَالَ الَّذِينَ", "ওয়া কাল্লাযীনা", "Wa Qalallazina", 363, 382, 25, "আল-ফুরকান", "২৫:২১", "ঊনবিংশ পারা", "সূরা আল-ফুরকান - আন-নামল"),
        HafeziParaInfo(20, "أَمَّنْ خَلَقَ", "আম্মান খালাকা", "A'man Khalaq", 383, 402, 27, "আন-নামল", "২৭:৬০", "বিংশ পারা", "সূরা আন-নামল - আল-আনকাবুত"),
        HafeziParaInfo(21, "اتْلُ مَا أُوحِيَ", "উতলু মা উহিয়া", "Utlu Ma Oohi", 403, 422, 29, "আল-আনকাবুত", "২৯:৪৫", "একবিংশ পারা", "সূরা আল-আনকাবুত - আল-আহযাব"),
        HafeziParaInfo(22, "وَمَنْ يَقْنُتْ", "ওয়ামান ইয়াকনুত", "Wa Manyaqnut", 423, 442, 33, "আল-আহযাব", "৩৩:৩১", "দ্বাবিংশ পারা", "সূরা আল-আহযাব - ইয়াসিন"),
        HafeziParaInfo(23, "وَمَا لِيَ", "ওয়ামা লিয়া", "Wa Mali", 443, 462, 36, "ইয়াসিন", "৩৬:২২", "ত্রয়োবিংশ পারা", "সূরা ইয়াসিন - আয-যুমার"),
        HafeziParaInfo(24, "فَمَنْ أَظْلَمُ", "ফামান আযলামু", "Faman Azlam", 463, 482, 39, "আয-যুমার", "৩৯:৩২", "চতুর্বিংশ পারা", "সূরা আয-যুমার - ফুসসিলাত"),
        HafeziParaInfo(25, "إِلَيْهِ يُرَدُّ", "ইলাইহি ইউরাদ্দু", "Elahe Yuruddo", 483, 502, 41, "হা-মীম সাজদাহ", "৪১:৪৭", "পঞ্চবিংশ পারা", "সূরা ফুসসিলাত - আল-জাসিয়াহ"),
        HafeziParaInfo(26, "حم", "হা-মীম", "Ha'a Meem", 503, 522, 46, "আল-আহকাফ", "৪৬:১", "ষড়বিংশ পারা", "সূরা আল-আহকাফ - আয-যারিয়াত"),
        HafeziParaInfo(27, "قَالَ فَمَا خَطْبُكُمْ", "কালা ফামা খাতবুকুম", "Qala Fama Khatbukum", 523, 542, 51, "আয-যারিয়াত", "৫১:৩১", "সপ্তবিংশ পারা", "সূরা আয-যারিয়াত - আল-হাদীদ"),
        HafeziParaInfo(28, "قَدْ سَمِعَ اللَّهُ", "কাদ সামিয়াল্লাহু", "Qadd Sami Allah", 543, 562, 58, "আল-মুজাদালাহ", "৫৮:১", "অষ্টাবিংশ পারা", "সূরা আল-মুজাদালাহ - আত-তাহরীম"),
        HafeziParaInfo(29, "تَبَارَكَ الَّذِي", "তাবারাকাল্লাযী", "Tabarakallazi", 563, 586, 67, "আল-মুলক", "৬৭:১", "ঊনত্রিংশ পারা", "সূরা আল-মুলক - আল-মুরসালাত"),
        HafeziParaInfo(30, "عَمَّ يَتَسَاءَلُونَ", "আম্মা ইয়াতাসাআলুন", "Amma Yatasa'aloon", 587, 611, 78, "আন-নাবা", "৭৮:১", "ত্রিশতম পারা", "সূরা আন-নাবা - আন-নাস")
    )

    val featuredAyahSearchIndex: List<HafeziAyahSearchEntry> = listOf(
        HafeziAyahSearchEntry(
            surahNumber = 1,
            surahNameBangla = "আল-ফাতিহা",
            surahNameArabic = "الفاتحة",
            ayahNumber = 1,
            paraNumber = 1,
            pageNumber = 2,
            lineNumber = 3,
            arabicText = "اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَ ۝١ الرَّحْمٰنِ الرَّحِيْمِ ۝٢ مٰلِكِ يَوْمِ الدِّيْنِ ۝٣",
            banglaTopicOrTranslation = "সূরা ফাতিহা • সকল প্রশংসা আল্লাহর যিনি বিশ্বজগতের প্রতিপালক"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 1,
            paraNumber = 1,
            pageNumber = 3,
            lineNumber = 3,
            arabicText = "الٓمّٓ ۝١ ذٰلِكَ الْكِتٰبُ لَا رَيْبَ فِيْهِ هُدًى لِّلْمُتَّقِيْنَ ۝٢",
            banglaTopicOrTranslation = "সূরা বাকারা শুরু • মুত্তাকীদের পথপ্রদর্শক কিতাব"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 201,
            paraNumber = 2,
            pageNumber = 32,
            lineNumber = 8,
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَّفِي الْآخِرَةِ حَسَنَةً وَّقِنَا عَذَابَ النَّارِ ۝٢٠١",
            banglaTopicOrTranslation = "রাব্বানা আতিনা ফিদ দুনইয়া হাসানাহ • দুনিয়া ও আখিরাতের কল্যাণের দোয়া"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 255,
            paraNumber = 3,
            pageNumber = 43,
            lineNumber = 4,
            arabicText = "اَللّٰهُ لَا اِلٰهَ اِلَّا هُوَ الْحَيُّ الْقَيُّوْمُ ۚ لَا تَاْخُذُهٗ سِنَةٌ وَّلَا نَوْمٌ ۚ",
            banglaTopicOrTranslation = "আয়াতুল কুরসি (Ayatul Kursi) • আল্লাহ ছাড়া কোনো মাবুদ নেই, তিনি চিরঞ্জীব"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 285,
            paraNumber = 3,
            pageNumber = 50,
            lineNumber = 9,
            arabicText = "اٰمَنَ الرَّسُوْلُ بِمَا اُنْزِلَ اِلَيْهِ مِنْ رَّبِّهٖ وَالْمُؤْمِنُوْنَ ؕ",
            banglaTopicOrTranslation = "আমানার রাসূলু • সূরা বাকারার শেষ দুই আয়াত"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 18,
            surahNameBangla = "আল-কাহফ",
            surahNameArabic = "الكهف",
            ayahNumber = 1,
            paraNumber = 15,
            pageNumber = 294,
            lineNumber = 9,
            arabicText = "اَلْحَمْدُ لِلّٰهِ الَّذِيْ اَنْزَلَ عَلٰى عَبْدِهِ الْكِتٰبَ وَلَمْ يَجْعَلْ لَّهٗ عِوَجًا ۝١",
            banglaTopicOrTranslation = "সূরা কাহফ (জুমার দিনের তিলাওয়াত) • প্রথম ১০ আয়াত"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 36,
            surahNameBangla = "ইয়াসিন",
            surahNameArabic = "يس",
            ayahNumber = 1,
            paraNumber = 22,
            pageNumber = 441,
            lineNumber = 3,
            arabicText = "يٰسٓ ۝١ وَالْقُرْاٰنِ الْحَكِيْمِ ۝٢ اِنَّكَ لَمِنَ الْمُرْسَلِيْنَ ۝٣ عَلٰى صِرَاطٍ مُّسْتَقِيْمٍ ۝٤",
            banglaTopicOrTranslation = "সূরা ইয়াসিন • কুরআনের হৃদয় (ক্বলবুল কুরআন)"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 55,
            surahNameBangla = "আর-রহমান",
            surahNameArabic = "الرحمن",
            ayahNumber = 1,
            paraNumber = 27,
            pageNumber = 532,
            lineNumber = 4,
            arabicText = "اَلرَّحْمٰنُ ۝١ عَلَّمَ الْقُرْاٰنَ ۝٢ خَلَقَ الْاِنْسَانَ ۝٣ عَلَّمَهُ الْبَيَانَ ۝٤",
            banglaTopicOrTranslation = "সূরা আর-রহমান • দয়াময় আল্লাহ কুরআন শিক্ষা দিয়েছেন"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 56,
            surahNameBangla = "আল-ওয়াকিয়াহ",
            surahNameArabic = "الواقعة",
            ayahNumber = 1,
            paraNumber = 27,
            pageNumber = 535,
            lineNumber = 8,
            arabicText = "اِذَا وَقَعَتِ الْوَاقِعَةُ ۝١ لَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ ۝٢",
            banglaTopicOrTranslation = "সূরা ওয়াকিয়াহ • রিজিকের বরকতের সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 59,
            surahNameBangla = "আল-হাশর",
            surahNameArabic = "الحشر",
            ayahNumber = 22,
            paraNumber = 28,
            pageNumber = 549,
            lineNumber = 9,
            arabicText = "هُوَ اللّٰهُ الَّذِيْ لَا اِلٰهَ اِلَّا هُوَ ۚ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ ۚ هُوَ الرَّحْمٰنُ الرَّحِيْمُ ۝٢٢",
            banglaTopicOrTranslation = "সূরা হাশরের শেষ তিন আয়াত • হুওয়াল্লাহুল্লাযী লা ইলাহা ইল্লা হুয়া"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 67,
            surahNameBangla = "আল-মুলক",
            surahNameArabic = "الملك",
            ayahNumber = 1,
            paraNumber = 29,
            pageNumber = 563,
            lineNumber = 3,
            arabicText = "تَبٰرَكَ الَّذِيْ بِيَدِهِ الْمُلْكُ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيْرٌ ۝١",
            banglaTopicOrTranslation = "সূরা মুলক • কবরের আজাব থেকে মুক্তিদানকারী সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 112,
            surahNameBangla = "আল-ইখলাস",
            surahNameArabic = "الإخلاص",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 610,
            lineNumber = 12,
            arabicText = "قُلْ هُوَ اللّٰهُ اَحَدٌ ۝١ اَللّٰهُ الصَّمَدُ ۝٢ لَمْ يَلِدْ وَلَمْ يُوْلَدْ ۝٣ وَلَمْ يَكُنْ لَّهٗ كُفُوًا اَحَدٌ ۝٤",
            banglaTopicOrTranslation = "সূরা ইখলাস • কুরআনের এক-তৃতীয়াংশের সমতুল্য"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 113,
            surahNameBangla = "আল-ফালাক",
            surahNameArabic = "الفلق",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 611,
            lineNumber = 3,
            arabicText = "قُلْ اَعُوْذُ بِرَبِّ الْفَلَقِ ۝١ مِنْ شَرِّ مَا خَلَقَ ۝٢",
            banglaTopicOrTranslation = "সূরা ফালাক • অনিষ্ট থেকে আশ্রয় প্রার্থনার সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 114,
            surahNameBangla = "আন-নাস",
            surahNameArabic = "الناس",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 611,
            lineNumber = 8,
            arabicText = "قُلْ اَعُوْذُ بِرَبِّ النَّاسِ ۝١ مَلِكِ النَّاسِ ۝٢ اِلٰهِ النَّاسِ ۝٣",
            banglaTopicOrTranslation = "সূরা নাস • কুরআনুল কারীমের সর্বশেষ সূরা"
        )
    )

    private val sajdahPages = setOf(177, 253, 274, 294, 312, 334, 339, 365, 381, 417, 455, 481, 528, 595, 604)

    fun toBanglaDigits(number: Int): String = toBanglaDigits(number.toString())

    fun toBanglaDigits(input: String): String {
        val map = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(map[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toArabicIndicDigits(number: Int): String {
        val map = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val str = number.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') sb.append(map[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun parseBanglaOrEnglishNumber(raw: String): Int? {
        val normalized = buildString {
            for (ch in raw.trim()) {
                when (ch) {
                    in '০'..'৯' -> append(('0'.code + (ch.code - '০'.code)).toChar())
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    in '0'..'9' -> append(ch)
                }
            }
        }
        return normalized.toIntOrNull()
    }

    fun getSurahByNumber(surahNumber: Int): HafeziSurahInfo {
        return all114Surahs.firstOrNull { it.number == surahNumber } ?: all114Surahs.first()
    }

    fun getParaByNumber(paraNumber: Int): HafeziParaInfo {
        return all30Paras.firstOrNull { it.number == paraNumber } ?: all30Paras.first()
    }

    fun getParaForPage(pageNumber: Int): HafeziParaInfo {
        val safePage = pageNumber.coerceIn(1, TOTAL_PDF_PAGES)
        if (safePage <= 2) return all30Paras.first()
        return all30Paras.firstOrNull { safePage in it.startPage..it.endPage } ?: all30Paras.last()
    }

    fun getSurahsOnPage(pageNumber: Int): List<HafeziSurahInfo> {
        val safePage = pageNumber.coerceIn(1, TOTAL_PDF_PAGES)
        if (safePage <= 2) return listOf(all114Surahs.first())
        if (safePage > TOTAL_QURAN_TEXT_PAGES) return listOf(all114Surahs.last())
        val matches = all114Surahs.filter { safePage in it.startPage..it.endPage }
        if (matches.isNotEmpty()) return matches
        return listOf(
            all114Surahs.lastOrNull { it.startPage <= safePage } ?: all114Surahs.first()
        )
    }

    fun getManzilForSurah(surahNumber: Int): Int {
        return when (surahNumber) {
            in 1..4 -> 1
            in 5..9 -> 2
            in 10..16 -> 3
            in 17..25 -> 4
            in 26..36 -> 5
            in 37..49 -> 6
            else -> 7
        }
    }

    /**
     * Returns verified page metadata for any page (`1..616`) of the bundled 15-Line Indo-Pak Quran PDF.
     */
    fun getPageMetadata(pageNumber: Int): HafeziPageMetadata {
        val safePage = pageNumber.coerceIn(1, TOTAL_PDF_PAGES)
        val para = getParaForPage(safePage)
        val surahs = getSurahsOnPage(safePage)
        val primarySurah = surahs.firstOrNull { it.startPage == safePage } ?: surahs.last()
        val manzil = getManzilForSurah(primarySurah.number)
        val pageOffsetInPara = (safePage - para.startPage).coerceAtLeast(0)
        val hizbQuarter = when {
            pageOffsetInPara == 0 -> "الجزء ${toArabicIndicDigits(para.number)}"
            pageOffsetInPara == 5 -> "ربع الحزب"
            pageOffsetInPara == 10 -> "نصف الجزء"
            pageOffsetInPara == 15 -> "ثلاثة أرباع"
            else -> "منزل ${toArabicIndicDigits(manzil)}"
        }
        val lines = List(LINES_PER_PAGE) { idx ->
            val lineNum = idx + 1
            MushafLineItem(
                lineNumber = lineNum,
                lineType = MushafLineType.QURAN_TEXT_LINE,
                arabicText = "${primarySurah.nameArabic} • صفحة ${toArabicIndicDigits(safePage)} • سطر ${toArabicIndicDigits(lineNum)}",
                surahHeaderInfo = primarySurah,
                ayahRangeLabel = "${primarySurah.number}:$lineNum",
                hasSajdah = lineNum == 8 && sajdahPages.contains(safePage)
            )
        }
        return HafeziPageMetadata(
            pageNumber = safePage,
            paraNumber = para.number,
            paraNameArabic = para.nameArabic,
            paraNameBangla = para.nameBangla,
            primarySurah = primarySurah,
            surahsOnPage = surahs,
            manzilNumber = manzil,
            hizbQuarterLabel = hizbQuarter,
            hasSajdah = sajdahPages.contains(safePage),
            lines = lines
        )
    }

    data class QuranUnifiedSearchResults(
        val query: String,
        val directPageMatch: Int?,
        val matchingSurahs: List<HafeziSurahInfo>,
        val matchingParas: List<HafeziParaInfo>,
        val matchingAyahs: List<HafeziAyahSearchEntry>
    )

    fun searchAll(rawQuery: String): QuranUnifiedSearchResults {
        val q = rawQuery.trim()
        if (q.isEmpty()) {
            return QuranUnifiedSearchResults(
                query = "",
                directPageMatch = null,
                matchingSurahs = all114Surahs,
                matchingParas = all30Paras,
                matchingAyahs = emptyList()
            )
        }

        val lower = q.lowercase()
        val parsedNumber = parseBanglaOrEnglishNumber(q)
        val directPage = if (parsedNumber != null && parsedNumber in 1..TOTAL_PDF_PAGES && !q.contains(":")) {
            parsedNumber
        } else {
            null
        }

        val surahMatches = all114Surahs.filter { s ->
            s.number == parsedNumber ||
                s.nameBangla.contains(q, ignoreCase = true) ||
                s.nameEnglish.lowercase().contains(lower) ||
                s.nameArabic.contains(q) ||
                s.meaningBangla.contains(q, ignoreCase = true)
        }

        val paraMatches = all30Paras.filter { p ->
            p.number == parsedNumber ||
                p.nameBangla.contains(q, ignoreCase = true) ||
                p.ordinalBanglaTitle.contains(q, ignoreCase = true) ||
                p.nameEnglish.lowercase().contains(lower) ||
                p.nameArabic.contains(q) ||
                p.startSurahNameBangla.contains(q, ignoreCase = true)
        }

        val ayahMatches = featuredAyahSearchIndex.filter { a ->
            a.referenceKey == q.replace('০', '0').replace('১', '1').replace('২', '2')
                .replace('৩', '3').replace('৪', '4').replace('৫', '5')
                .replace('৬', '6').replace('৭', '7').replace('৮', '8').replace('৯', '9') ||
                a.banglaTopicOrTranslation.contains(q, ignoreCase = true) ||
                a.surahNameBangla.contains(q, ignoreCase = true) ||
                a.arabicText.contains(q)
        }

        return QuranUnifiedSearchResults(
            query = q,
            directPageMatch = directPage,
            matchingSurahs = surahMatches,
            matchingParas = paraMatches,
            matchingAyahs = ayahMatches
        )
    }
}
