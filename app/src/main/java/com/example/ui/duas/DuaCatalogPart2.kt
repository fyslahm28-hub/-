package com.example.ui.duas

/**
 * যাচাইকৃত দোয়া ও যিকির ক্যাটালগ (অংশ ২ — ক্যাটাগরি ৭ থেকে ১২)।
 * প্রতিটি দোয়া পবিত্র কুরআন ও গ্রহণযোগ্য হাদিসগ্রন্থের নির্দিষ্ট সূত্রসহ সংকলিত।
 */
internal object DuaCatalogPart2 {

    val duas: List<VerifiedDua> = listOf(
        // ==================== ৭. সফরের দোয়া ====================
        VerifiedDua(
            id = "travel_vehicle_dua",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "যানবাহনে আরোহণ ও সফরের দোয়া",
            occasionBangla = "গাড়ি, ট্রেন, নৌযান বা বিমানে ওঠার পর",
            arabicText = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
            banglaPronunciation = "সুবহা-নাল্লাযী সাখখারা লানা হা-যা ওয়ামা কুন্না লাহূ মুক্বরিনীন, ওয়া ইন্না ইলা রব্বিনা লামুনক্বালিবূন।",
            banglaMeaning = "পবিত্র মহান সেই সত্তা, যিনি এ বাহনকে আমাদের অধীন করে দিয়েছেন, অথচ আমরা একে বশীভূত করতে সক্ষম ছিলাম না। আর নিশ্চয়ই আমরা আমাদের রবের কাছেই প্রত্যাবর্তনকারী।",
            referenceBangla = "সূরা আয-যুখরুফ (৪৩), আয়াত: ১৩–১৪; সহীহ মুসলিম, হাদিস নং: ১৩৪২",
            gradingBangla = "পবিত্র কুরআন ও সহীহ মুসলিম",
            sourceType = DuaSourceType.QURAN
        ),
        VerifiedDua(
            id = "travel_full_safety",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "সফরে কল্যাণ, সহজতা ও নিরাপত্তার দোয়া",
            occasionBangla = "দীর্ঘ সফরের সূচনায় বাহনে বসার পর",
            arabicText = "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَلِيفَةُ فِي الْأَهْلِ",
            banglaPronunciation = "আল্লা-হুম্মা ইন্না নাসআলুকা ফী সাফারিনা হা-যাল বিররা ওয়াত-তাক্বওয়া ওয়া মিনাল ‘আমালি মা তারদ্বা। আল্লা-হুম্মা হাওউইন ‘আলাইনা সাফারানা হা-যা ওয়াত্ববি ‘আন্না বু‘দাহূ। আল্লা-হুম্মা আনতাস সা-হিবু ফিস-সাফারি ওয়াল খালীফাতু ফিল আহল।",
            banglaMeaning = "হে আল্লাহ! আমাদের এই সফরে আপনার কাছে পুণ্য, তাকওয়া এবং এমন আমল প্রার্থনা করি যাতে আপনি সন্তুষ্ট হন। হে আল্লাহ! আমাদের এই সফরকে সহজ করে দিন এবং এর দূরত্ব কমিয়ে দিন। হে আল্লাহ! আপনিই সফরের সাথী এবং পরিবার-পরিজনের অভিভাবক।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ১৩৪২",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "travel_returning_home",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "সফর থেকে ফিরে আসার দোয়া",
            occasionBangla = "সফর শেষ করে নিজ এলাকায় ফেরার সময়",
            arabicText = "آيِبُونَ، تَائِبُونَ، عَابِدُونَ، لِرَبِّنَا حَامِدُونَ",
            banglaPronunciation = "আ-য়িবূনা, তা-য়িবূনা, ‘আ-বিদূনা, লিরব্বিনা হা-মিদূন।",
            banglaMeaning = "আমরা প্রত্যাবর্তনকারী, তওবাকারী, ইবাদতকারী এবং আমাদের রবের প্রশংসাকারী অবস্থায় ফিরে আসছি।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ১৭৯৭; সহীহ মুসলিম, হাদিস নং: ১৩৪২",
            gradingBangla = "সহীহ (মুত্তাফাকুন আলাইহি)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "travel_farewell_musafir",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "মুসাফিরকে বিদায় দেওয়ার দোয়া",
            occasionBangla = "কেউ সফরে রওনা হলে তার জন্য পঠিতব্য দোয়া",
            arabicText = "أَسْتَوْدِعُ اللَّهَ دِينَكَ، وَأَمَانَتَكَ، وَخَوَاتِيمَ عَمَلِكَ",
            banglaPronunciation = "আস্তাওদি‘উল্লা-হা দীনাকা, ওয়া আমা-নাতাকা, ওয়া খাওয়া-তীমা ‘আমালিক।",
            banglaMeaning = "আমি আপনার দ্বীন, আপনার আমানত এবং আপনার শেষ আমলসমূহকে আল্লাহর জিম্মায় সোপর্দ করছি।",
            referenceBangla = "সুনানে আবু দাউদ, হাদিস নং: ২৬০০; সুনানে তিরমিযী, হাদিস নং: ৩৪৪৩",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "travel_stopping_place",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "সফরে নতুন স্থানে অবতরণের দোয়া",
            occasionBangla = "সফরের মাঝে কোনো বাড়ি বা নতুন জায়গায় অবস্থান করলে",
            arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            banglaPronunciation = "আ‘ঊযু বিকালিমা-তিল্লা-হিত তা-ম্মা-তি মিন শাররি মা খালাক্ব।",
            banglaMeaning = "আমি আল্লাহর পরিপূর্ণ বাণীসমূহের মাধ্যমে তাঁর সৃষ্ট সকল কিছুর অনিষ্ট থেকে আশ্রয় প্রার্থনা করছি।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২৭০৮",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "travel_ascending_descending",
            category = DuaCategory.TRAVEL_DUAS,
            titleBangla = "সফরে উঁচুতে উঠলে ও নিচে নামলে যিকির",
            occasionBangla = "পাহাড়, সেতু বা উঁচু পথে ওঠা ও নামার সময়",
            arabicText = "اللَّهُ أَكْبَرُ (উঁচুতে ওঠার সময়) • سُبْحَانَ اللَّهِ (নিচে নামার সময়)",
            banglaPronunciation = "উঁচুতে ওঠার সময়: আল্লা-হু আকবার। নিচে নামার সময়: সুবহা-নাল্লা-হ।",
            banglaMeaning = "আল্লাহ সর্বশ্রেষ্ঠ (উঁচু স্থানে ওঠার সময়) এবং আল্লাহ অতি পবিত্র (নিচে নামার সময়)।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ২৯৯৩",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH
        ),

        // ==================== ৮. বিপদ ও অসুস্থতার দোয়া ====================
        VerifiedDua(
            id = "distress_musibah",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "বিপদ-আপদ ও মুসিবতের দোয়া",
            occasionBangla = "যেকোনো দুঃসংবাদ, ক্ষতি বা বিপদে পতিত হলে",
            arabicText = "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
            banglaPronunciation = "ইন্না লিল্লা-হি ওয়া ইন্না ইলাইহি রা-জি‘ঊন। আল্লা-হুম্মা’জুরনী ফী মুসীবাতী ওয়া আখলিফ লী খাইরাম মিনহা।",
            banglaMeaning = "নিশ্চয়ই আমরা আল্লাহরই জন্য এবং তাঁরই কাছে ফিরে যাব। হে আল্লাহ! আমার এই বিপদে আমাকে উত্তম প্রতিদান দিন এবং এর পরিবর্তে এর চেয়ে উত্তম কিছু দান করুন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ৯১৮",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "distress_anxiety_debt",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "দুশ্চিন্তা, শোক ও ঋণমুক্তির দোয়া",
            occasionBangla = "মানসিক অস্থিরতা, অলসতা ও ঋণের চাপে পড়লে",
            arabicText = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নী আ‘ঊযু বিকা মিনাল হাম্মি ওয়াল হাযান, ওয়াল ‘আজযি ওয়াল কাসাল, ওয়াল বুখলি ওয়াল জুবন, ওয়া দ্বালা‘ইদ দাইনি ওয়া গালাবাতির রিজা-ল।",
            banglaMeaning = "হে আল্লাহ! আমি আপনার কাছে আশ্রয় চাই দুশ্চিন্তা ও শোক থেকে, অক্ষমতা ও অলসতা থেকে, কৃপণতা ও কাপুরুষতা থেকে এবং ঋণের বোঝা ও মানুষের আধিপত্য (জুলুম) থেকে।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬৩৬৯",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "distress_karb",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "কঠিন সংকট ও পেরেশানির দোয়া",
            occasionBangla = "ভয়াবহ দুঃখ-কষ্ট ও সংকটময় মুহূর্তে",
            arabicText = "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
            banglaPronunciation = "লা ইলা-হা ইল্লাল্লা-হুল ‘আযীমুল হালীম, লা ইলা-হা ইল্লাল্লা-হু রব্বুল ‘আরশিল ‘আযীম, লা ইলা-হা ইল্লাল্লা-হু রব্বুস সামা-ওয়া-তি ওয়া রব্বুল আরদ্বি ওয়া রব্বুল ‘আরশিল কারীম।",
            banglaMeaning = "মহান ও পরম সহনশীল আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই। মহান আরশের অধিপতি আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই। আসমানসমূহ, জমিন ও সম্মানিত আরশের রব আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬৩৪৬; সহীহ মুসলিম, হাদিস নং: ২৭৩০",
            gradingBangla = "সহীহ (মুত্তাফাকুন আলাইহি)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "distress_shifa_illness",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "অসুস্থ ব্যক্তির শিফা ও আরোগ্যের দোয়া",
            occasionBangla = "নিজের বা অসুস্থ ব্যক্তির রোগমুক্তির জন্য",
            arabicText = "أَذْهِبِ الْبَاسَ رَبَّ النَّاسِ، اشْفِ وَأَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            banglaPronunciation = "আযহিবিল বা’সা রব্বান না-স, ইশফি ওয়া আনতাশ শা-ফী, লা শিফা-আ ইল্লা শিফা-উকা, শিফা-আল লা ইউগা-দিরু সাক্বামা।",
            banglaMeaning = "হে মানুষের রব! কষ্ট দূর করে দিন এবং আরোগ্য দান করুন; আপনিই একমাত্র আরোগ্যদানকারী। আপনার আরোগ্য ছাড়া কোনো আরোগ্য নেই—এমন আরোগ্য দান করুন যা কোনো রোগ অবশিষ্ট রাখে না।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৫৭৪৩; সহীহ মুসলিম, হাদিস নং: ২১৯১",
            gradingBangla = "সহীহ (মুত্তাফাকুন আলাইহি)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "distress_visiting_sick",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "রোগী দেখার সময় পঠিতব্য দোয়া",
            occasionBangla = "অসুস্থ ব্যক্তিকে দেখতে গেলে তাঁকে সান্ত্বনা ও দোয়া",
            arabicText = "لَا بَأْسَ، طَهُورٌ إِنْ شَاءَ اللَّهُ",
            banglaPronunciation = "লা বা’সা, ত্বাহূরুন ইন শা-আল্লা-হ।",
            banglaMeaning = "কোনো ভয় নেই; ইনশাআল্লাহ এই অসুস্থতা গুনাহ থেকে পবিত্রকারী হবে।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৩৬১৬",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "distress_body_pain",
            category = DuaCategory.DISTRESS_ILLNESS_DUAS,
            titleBangla = "শরীরের ব্যথা স্থানে হাত রেখে দোয়া",
            occasionBangla = "ব্যথা স্থানে ডান হাত রেখে ৩ বার বিসমিল্লাহ ও ৭ বার এই দোয়া",
            arabicText = "أَعُوذُ بِاللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ",
            banglaPronunciation = "আ‘ঊযু বিল্লা-হি ওয়া ক্বুদরাতিহী মিন শাররি মা আজিদু ওয়া উহা-যির। (শুরুতে ৩ বার ‘বিসমিল্লা-হ’ পড়তে হবে।)",
            banglaMeaning = "আমি যে ব্যথা অনুভব করছি এবং যে ক্ষতির আশঙ্কা করছি, তার অনিষ্ট থেকে আমি আল্লাহ ও তাঁর কুদরতের আশ্রয় প্রার্থনা করছি।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২২০২",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH,
            recommendedCount = 7
        ),

        // ==================== ৯. ক্ষমা ও তওবার দোয়া ====================
        VerifiedDua(
            id = "tawbah_sayyidul_istighfar",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "সাইয়্যিদুল ইস্তিগফার (ক্ষমা প্রার্থনার শ্রেষ্ঠ দোয়া)",
            occasionBangla = "সকাল ও সন্ধ্যায় দৃঢ় বিশ্বাস (একিন) সহকারে পাঠ",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            banglaPronunciation = "আল্লা-হুম্মা আনতা রব্বী লা ইলা-হা ইল্লা আনতা, খালাক্বতানী ওয়া আনা ‘আবদুকা, ওয়া আনা ‘আলা ‘আহদিকা ওয়া ওয়া‘দিকা মাস্তাত্বা‘তু, আ‘ঊযু বিকা মিন শাররি মা সানা‘তু, আবূউ লাকা বিনি‘মাতিকা ‘আলাইয়্যা, ওয়া আবূউ বিযাম্বী ফাগফির লী, ফাইন্নাহূ লা ইয়াগফিরুয যুনূবা ইল্লা আনতা।",
            banglaMeaning = "হে আল্লাহ! আপনি আমার রব, আপনি ছাড়া কোনো সত্য উপাস্য নেই। আপনি আমাকে সৃষ্টি করেছেন এবং আমি আপনার বান্দা। আমি সাধ্যমতো আপনার অঙ্গীকার ও ওয়াদার ওপর প্রতিষ্ঠিত আছি। আমি আমার কৃতকর্মের অনিষ্ট থেকে আপনার কাছে আশ্রয় চাই। আমার ওপর আপনার নেয়ামতসমূহ স্বীকার করছি এবং আমার গুনাহও স্বীকার করছি। অতএব আমাকে ক্ষমা করুন; কেননা আপনি ছাড়া গুনাহ ক্ষমা করার কেউ নেই।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬৩০৬",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.MORNING
        ),
        VerifiedDua(
            id = "tawbah_short_istighfar",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "সংক্ষিপ্ত ইস্তিগফার ও তওবা",
            occasionBangla = "দিনে-রাতে অধিক পরিমাণে পাঠ",
            arabicText = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            banglaPronunciation = "আস্তাগফিরুল্লা-হা ওয়া আতূবু ইলাইহ।",
            banglaMeaning = "আমি আল্লাহর কাছে ক্ষমা প্রার্থনা করছি এবং তাঁর দিকেই ফিরে আসছি (তওবা করছি)।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬৩০৭",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.AFTER_SALAH
        ),
        VerifiedDua(
            id = "tawbah_astaghfirullahal_azim",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "মহাক্ষমার ইস্তিগফার",
            occasionBangla = "অন্তর থেকে গুনাহ মাফের আশায়",
            arabicText = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            banglaPronunciation = "আস্তাগফিরুল্লা-হাল ‘আযীমাল্লাযী লা ইলা-হা ইল্লা হুওয়াল হাইয়্যুল ক্বাইয়্যূমু ওয়া আতূবু ইলাইহ।",
            banglaMeaning = "আমি সেই মহান আল্লাহর কাছে ক্ষমা প্রার্থনা করছি, যিনি ছাড়া কোনো সত্য উপাস্য নেই, যিনি চিরঞ্জীব ও সর্বসত্তার ধারক; আর আমি তাঁর কাছেই তওবা করছি।",
            referenceBangla = "সুনানে আবু দাউদ, হাদিস নং: ১৫১৭; সুনানে তিরমিযী, হাদিস নং: ৩৫৭৭",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "tawbah_afuw_qadr",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "মার্জনা প্রার্থনার দোয়া (শবে কদর ও সর্বদা)",
            occasionBangla = "রমজানের শেষ দশক ও যেকোনো সময় মার্জনা চাওয়ার জন্য",
            arabicText = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নাকা ‘আফুউয়ুন তুহিব্বুল ‘আফওয়া ফা‘ফু ‘আন্নী।",
            banglaMeaning = "হে আল্লাহ! নিশ্চয়ই আপনি অতিশয় ক্ষমাকারী, ক্ষমা করতে ভালোবাসেন; অতএব আমাকে ক্ষমা করে দিন।",
            referenceBangla = "সুনানে তিরমিযী, হাদিস নং: ৩৫১৩; সুনানে ইবনে মাজাহ, হাদিস নং: ৩৮৫০",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "tawbah_all_sins_sujood",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "ছোট-বড়, গোপন-প্রকাশ্য সকল গুনাহ মাফের দোয়া",
            occasionBangla = "নফল নামাজের সিজদায় ও মুনাজাতে",
            arabicText = "اللَّهُمَّ اغْفِرْ لِي ذَنْبِي كُلَّهُ، دِقَّهُ وَجِلَّهُ، وَأَوَّلَهُ وَآخِرَهُ، وَعَلَانِيَتَهُ وَسِرَّهُ",
            banglaPronunciation = "আল্লা-হুম্মাগফির লী যাম্বী কুল্লাহূ, দিক্বক্বাহূ ওয়া জিল্লাহূ, ওয়া আওওয়ালাহূ ওয়া আ-খিরাহূ, ওয়া ‘আলা-নিয়াতাহূ ওয়া সিররাহূ।",
            banglaMeaning = "হে আল্লাহ! আমার সকল গুনাহ ক্ষমা করে দিন—ক্ষুদ্র ও বৃহৎ, প্রথম ও শেষ, প্রকাশ্য ও গোপন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ৪৮৩",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "tawbah_rabbighfirli",
            category = DuaCategory.FORGIVENESS_TAWBAH_DUAS,
            titleBangla = "তওবা কবুলের মাসনুন দোয়া",
            occasionBangla = "মজলিসে ও একাকী যিকিরের সময়",
            arabicText = "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
            banglaPronunciation = "রব্বিগফির লী ওয়া তুব ‘আলাইয়্যা ইন্নাকা আনতাত তাওওয়া-বুর রাহীম।",
            banglaMeaning = "হে আমার রব! আমাকে ক্ষমা করুন এবং আমার তওবা কবুল করুন। নিশ্চয়ই আপনি অতিশয় তওবা কবুলকারী, পরম দয়ালু।",
            referenceBangla = "সুনানে আবু দাউদ, হাদিস নং: ১৫১৬; সুনানে তিরমিযী, হাদিস নং: ৩৪৩৪",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),

        // ==================== ১০. রিজিক ও বরকতের দোয়া ====================
        VerifiedDua(
            id = "rizq_fajr_ilm_rizq",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "উপকারী ইলম, পবিত্র রিজিক ও কবুল আমলের দোয়া",
            occasionBangla = "ফজরের সালাতের সালাম ফিরানোর পর",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নী আসআলুকা ‘ইলমান না-ফি‘আও, ওয়া রিযক্বান ত্বাইয়্যিবাও, ওয়া ‘আমালাম মুতাক্বাব্বালা।",
            banglaMeaning = "হে আল্লাহ! আমি আপনার কাছে উপকারী জ্ঞান, পবিত্র (হালাল) রিজিক এবং কবুলযোগ্য নেক আমল প্রার্থনা করছি।",
            referenceBangla = "সুনানে ইবনে মাজাহ, হাদিস নং: ৯২৫",
            gradingBangla = "হাসান / সহীহ",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.MORNING
        ),
        VerifiedDua(
            id = "rizq_halal_sufficiency",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "হালাল রিজিকে তুষ্টি ও ঋণমুক্তির দোয়া",
            occasionBangla = "হালাল উপার্জন ও পরমুখাপেক্ষিতা থেকে মুক্তির জন্য",
            arabicText = "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ",
            banglaPronunciation = "আল্লা-হুম্মাকফিনী বিহালা-লিকা ‘আন হারা-মিকা ওয়া আগনিনী বিফাদ্বলিকা ‘আম্মান সিওয়া-ক।",
            banglaMeaning = "হে আল্লাহ! আপনার হালাল রিজিক দিয়ে আমাকে হারাম থেকে যথেষ্ট করুন এবং আপনার অনুগ্রহ দিয়ে আমাকে আপনি ছাড়া অন্য সকলের মুখাপেক্ষী হওয়া থেকে মুক্ত রাখুন।",
            referenceBangla = "সুনানে তিরমিযী, হাদিস নং: ৩৫৬৩",
            gradingBangla = "হাসান (তিরমিযী)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "rizq_huda_tuqa_ghina",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "হিদায়াত, তাকওয়া, পবিত্রতা ও প্রাচুর্যের দোয়া",
            occasionBangla = "অন্তরের প্রাচুর্য ও আত্মসম্মান রক্ষার জন্য",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى، وَالتُّقَى، وَالْعَفَافَ، وَالْغِنَى",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নী আসআলুকাল হুদা ওয়াত-তুক্বা ওয়াল ‘আফা-ফা ওয়াল গিনা।",
            banglaMeaning = "হে আল্লাহ! আমি আপনার কাছে হিদায়াত, তাকওয়া, চারিত্রিক পবিত্রতা এবং প্রাচুর্য (অন্তরের সচ্ছলতা) প্রার্থনা করছি।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২৭২১",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "rizq_comprehensive_five",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "ক্ষমা, রহমত, হিদায়াত, সুস্থতা ও রিজিকের দোয়া",
            occasionBangla = "দুনিয়া ও আখিরাতের সামগ্রিক কল্যাণের জন্য",
            arabicText = "اللَّهُمَّ اغْفِرْ لِي، وَارْحَمْنِي، وَاهْدِنِي، وَعَافِنِي، وَارْزُقْنِي",
            banglaPronunciation = "আল্লা-হুম্মাগফির লী, ওয়ারহামনী, ওয়াহদিনী, ওয়া ‘আ-ফিনী, ওয়ারযুক্বনী।",
            banglaMeaning = "হে আল্লাহ! আমাকে ক্ষমা করুন, আমার ওপর দয়া করুন, আমাকে হিদায়াত দিন, আমাকে সুস্থতা দান করুন এবং আমাকে রিজিক দান করুন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২৬৯৭",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "rizq_musa_faqir",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "কল্যাণ ও রিজিক প্রার্থনার কুরআনী দোয়া",
            occasionBangla = "কাজ, রিজিক ও কল্যাণের প্রত্যাশায় (হযরত মূসা আ.-এর দোয়া)",
            arabicText = "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            banglaPronunciation = "রব্বি ইন্নী লিমা আনযালতা ইলাইয়্যা মিন খাইরিন ফাক্বীর।",
            banglaMeaning = "হে আমার রব! আপনি আমার প্রতি যে কল্যাণই অবতীর্ণ করবেন, আমি তারই মুখাপেক্ষী।",
            referenceBangla = "সূরা আল-কাসাস (২৮), আয়াত: ২৪",
            gradingBangla = "পবিত্র কুরআন",
            sourceType = DuaSourceType.QURAN
        ),
        VerifiedDua(
            id = "rizq_barakah_protection",
            category = DuaCategory.RIZQ_BARAKAH_DUAS,
            titleBangla = "নেয়ামত ও বরকত স্থায়ী থাকার দোয়া",
            occasionBangla = "প্রাপ্ত নেয়ামত ও সুস্থতা অক্ষুণ্ণ রাখার জন্য",
            arabicText = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নী আ‘ঊযু বিকা মিন যাওয়া-লি নি‘মাতিকা, ওয়া তাহাওউলি ‘আ-ফিয়াতিকা, ওয়া ফুজা-আতি নিক্বমাতিকা, ওয়া জামী‘ই সাখাত্বিক।",
            banglaMeaning = "হে আল্লাহ! আমি আপনার কাছে আশ্রয় চাই আপনার নেয়ামত বিলুপ্ত হওয়া থেকে, আপনার দেওয়া সুস্থতা বদলে যাওয়া থেকে, আপনার আকস্মিক শাস্তি থেকে এবং আপনার সমস্ত অসন্তুষ্টি থেকে।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২৭৩৯",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),

        // ==================== ১১. মসজিদে প্রবেশ ও বের হওয়ার দোয়া ====================
        VerifiedDua(
            id = "masjid_entering",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "মসজিদে প্রবেশের দোয়া",
            occasionBangla = "ডান পা দিয়ে মসজিদে প্রবেশ করার সময় (দরূদ পাঠ করে)",
            arabicText = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            banglaPronunciation = "আল্লা-হুম্মাফতাহ লী আবওয়া-বা রাহমাতিক।",
            banglaMeaning = "হে আল্লাহ! আমার জন্য আপনার রহমতের দরজাসমূহ খুলে দিন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ৭১৩",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.TIMED_MASNUN
        ),
        VerifiedDua(
            id = "masjid_exiting",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "মসজিদ থেকে বের হওয়ার দোয়া",
            occasionBangla = "বাম পা দিয়ে মসজিদ থেকে বের হওয়ার সময়",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            banglaPronunciation = "আল্লা-হুম্মা ইন্নী আসআলুকা মিন ফাদ্বলিক।",
            banglaMeaning = "হে আল্লাহ! আমি আপনার কাছে আপনার অনুগ্রহ প্রার্থনা করছি।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ৭১৩",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "masjid_enter_refuge",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "মসজিদে প্রবেশে শয়তান থেকে আশ্রয় প্রার্থনা",
            occasionBangla = "মসজিদে প্রবেশের সময় পাঠ করা সুন্নত",
            arabicText = "أَعُوذُ بِاللَّهِ الْعَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ",
            banglaPronunciation = "আ‘ঊযু বিল্লা-হিল ‘আযীম, ওয়া বিওয়াজহিহিল কারীম, ওয়া সুলত্বা-নিহিল ক্বাদীম, মিনাশ শাইত্বা-নির রাজীম।",
            banglaMeaning = "আমি মহান আল্লাহর কাছে, তাঁর সম্মানিত সত্তার ওসিলায় এবং তাঁর চিরন্তন ক্ষমতার মাধ্যমে বিতাড়িত শয়তান থেকে আশ্রয় প্রার্থনা করছি।",
            referenceBangla = "সুনানে আবু দাউদ, হাদিস নং: ৪৬৬",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "masjid_going_noor",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "মসজিদে যাওয়ার পথে নূরের দোয়া",
            occasionBangla = "নামাজের উদ্দেশ্যে মসজিদে যাওয়ার সময়",
            arabicText = "اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُورًا، وَفِي لِسَانِي نُورًا، وَاجْعَلْ فِي سَمْعِي نُورًا، وَاجْعَلْ فِي بَصَرِي نُورًا",
            banglaPronunciation = "আল্লা-হুম্মাজ‘আল ফী ক্বালবী নূরা, ওয়া ফী লিসা-নী নূরা, ওয়াজ‘আল ফী সাম‘ঈ নূরা, ওয়াজ‘আল ফী বাসারী নূরা।",
            banglaMeaning = "হে আল্লাহ! আমার অন্তরে নূর (আলো) দান করুন, আমার জিহ্বায় নূর দান করুন, আমার শ্রবণশক্তিতে নূর দান করুন এবং আমার দৃষ্টিশক্তিতে নূর দান করুন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ৭৬৩",
            gradingBangla = "সহীহ (মুসলিম)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "masjid_after_azan",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "আজানের পরের মাসনুন দোয়া",
            occasionBangla = "আজানের জবাব ও দরূদ শরীফ পাঠের পর",
            arabicText = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            banglaPronunciation = "আল্লা-হুম্মা রব্বা হা-যিহিদ দা‘ওয়াতিল তা-ম্মাতি ওয়াস সালা-তিল ক্বা-ইমাহ, আ-তি মুহাম্মাদানিল ওয়াসীলাতা ওয়াল ফাদ্বীলাহ, ওয়াব‘আছহু মাক্বা-মাম মাহমূদানিল্লাযী ওয়া‘আত্তাহ।",
            banglaMeaning = "হে আল্লাহ! এই পরিপূর্ণ আহ্বান ও প্রতিষ্ঠিতব্য সালাতের রব! আপনি মুহাম্মদ ﷺ-কে ওয়াসীলা ও সর্বোচ্চ মর্যাদা দান করুন এবং তাঁকে সেই প্রশংসিত স্থানে (মাকামে মাহমুদে) অধিষ্ঠিত করুন, যার অঙ্গীকার আপনি করেছেন।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬১৪",
            gradingBangla = "সহীহ (বুখারী)",
            scholarlyNoteBangla = "হাদিসের মূল সহীহ বর্ণনায় শেষে ‘ইন্নাকা লা তুখলিফুল মী‘আদ’ অংশটি নেই; এটি বাইহাকীর বর্ণনায় এসেছে এবং এর সনদ নিয়ে মুহাদ্দিসদের মতভেদ রয়েছে।",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.TIMED_MASNUN
        ),
        VerifiedDua(
            id = "masjid_after_wudu",
            category = DuaCategory.MASJID_DUAS,
            titleBangla = "ওযু শেষের দোয়া",
            occasionBangla = "মসজিদে যাওয়ার আগে বা নামাজের জন্য ওযু সম্পন্ন করে",
            arabicText = "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ",
            banglaPronunciation = "আশহাদু আল্লা ইলা-হা ইল্লাল্লা-হু ওয়াহদাহূ লা শারীকা লাহূ ওয়া আশহাদু আন্না মুহাম্মাদান ‘আবদুহূ ওয়া রাসূলুহূ। আল্লা-হুম্মাজ‘আলনী মিনাত তাওওয়া-বীনা ওয়াজ‘আলনী মিনাল মুতাত্বাহহিরীন।",
            banglaMeaning = "আমি সাক্ষ্য দিচ্ছি যে, আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই, তিনি এক ও অদ্বিতীয় এবং মুহাম্মদ ﷺ তাঁর বান্দা ও রাসূল। হে আল্লাহ! আমাকে তওবাকারী ও পবিত্রতা অর্জনকারীদের অন্তর্ভুক্ত করুন।",
            referenceBangla = "সহীহ মুসলিম, হাদিস নং: ২৩৪; দ্বিতীয় অংশ: সুনানে তিরমিযী, হাদিস নং: ৫৫",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),

        // ==================== ১২. বিবিধ মাসনুন দোয়া ====================
        VerifiedDua(
            id = "misc_rain_dua",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "বৃষ্টি দেখলে পঠিতব্য দোয়া",
            occasionBangla = "বৃষ্টি শুরু হলে উপকারী বৃষ্টির জন্য",
            arabicText = "اللَّهُمَّ صَيِّبًا نَافِعًا",
            banglaPronunciation = "আল্লা-হুম্মা সাইয়্যিবান না-ফি‘আ।",
            banglaMeaning = "হে আল্লাহ! উপকারী ও কল্যাণকর বৃষ্টি বর্ষণ করুন।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ১০৩২",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "misc_sneezing_dua",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "হাঁচির পর দোয়া ও তার জবাব",
            occasionBangla = "হাঁচি দিলে এবং অন্যের হাঁচির উত্তর দিতে",
            arabicText = "الْحَمْدُ لِلَّهِ • يَرْحَمُكَ اللَّهُ • يَهْدِيكُمُ اللَّهُ وَيُصْلِحُ بَالَكُمْ",
            banglaPronunciation = "হাঁচিদাতা বলবে: আল-হামদু লিল্লা-হ। শ্রোতা বলবে: ইয়ারহামুকাল্লা-হ। হাঁচিদাতা উত্তরে বলবে: ইয়াহদীকুমুল্লা-হু ওয়া ইউসলিহু বা-লাকুম।",
            banglaMeaning = "সমস্ত প্রশংসা আল্লাহর। আল্লাহ আপনার প্রতি দয়া করুন। আল্লাহ আপনাদের হিদায়াত দান করুন এবং আপনাদের অবস্থা সংশোধন করে দিন।",
            referenceBangla = "সহীহ বুখারী, হাদিস নং: ৬২২৪",
            gradingBangla = "সহীহ (বুখারী)",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "misc_kaffarah_majlis",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "মজলিস বা বৈঠক শেষের দোয়া (কাফফারাতুল মজলিস)",
            occasionBangla = "যেকোনো বৈঠক বা আলোচনা শেষে ওঠার আগে",
            arabicText = "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ",
            banglaPronunciation = "সুবহা-নাকাল্লা-হুম্মা ওয়া বিহামদিকা, আশহাদু আল্লা ইলা-হা ইল্লা আনতা, আস্তাগফিরুকা ওয়া আতূবু ইলাইক।",
            banglaMeaning = "হে আল্লাহ! আপনি পবিত্র এবং সমস্ত প্রশংসা আপনারই। আমি সাক্ষ্য দিচ্ছি যে, আপনি ছাড়া কোনো সত্য উপাস্য নেই। আমি আপনার কাছে ক্ষমা প্রার্থনা করছি এবং আপনার কাছেই তওবা করছি।",
            referenceBangla = "সুনানে তিরমিযী, হাদিস নং: ৩৪৩৩; সুনানে আবু দাউদ, হাদিস নং: ৪৮৫৯",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "misc_jazakallah",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "উপকারকারীর প্রতি কৃতজ্ঞতার দোয়া",
            occasionBangla = "কেউ কোনো উপকার বা সদাচরণ করলে",
            arabicText = "جَزَاكَ اللَّهُ خَيْرًا",
            banglaPronunciation = "জাযা-কাল্লা-হু খাইরা।",
            banglaMeaning = "আল্লাহ আপনাকে উত্তম প্রতিদান দান করুন।",
            referenceBangla = "সুনানে তিরমিযী, হাদিস নং: ২০৩৫",
            gradingBangla = "হাসান সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "misc_ease_difficulty",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "কঠিন কাজ সহজ হওয়ার দোয়া",
            occasionBangla = "কোনো দায়িত্ব বা কাজ কঠিন মনে হলে",
            arabicText = "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا، وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
            banglaPronunciation = "আল্লা-হুম্মা লা সাহলা ইল্লা মা জা‘আলতাহূ সাহলা, ওয়া আনতা তাজ‘আলুল হাযনা ইযা শি’তা সাহলা।",
            banglaMeaning = "হে আল্লাহ! আপনি যা সহজ করেন তা ছাড়া কোনো কিছুই সহজ নয়; আর আপনি চাইলে কঠিন কাজকেও সহজ করে দিতে পারেন।",
            referenceBangla = "সহীহ ইবনে হিব্বান, হাদিস নং: ৯৭৪",
            gradingBangla = "সহীহ",
            sourceType = DuaSourceType.HADITH
        ),
        VerifiedDua(
            id = "misc_steadfast_heart",
            category = DuaCategory.MISC_MASNUN_DUAS,
            titleBangla = "দ্বীনের ওপর অন্তর অবিচল থাকার দোয়া",
            occasionBangla = "ঈমান ও আমলে দৃঢ়তা চেয়ে প্রতিদিন পাঠ",
            arabicText = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            banglaPronunciation = "ইয়া মুক্বাল্লিবাল ক্বুলূবি ছাব্বিত ক্বালবী ‘আলা দীনিক।",
            banglaMeaning = "হে অন্তরসমূহের পরিবর্তনকারী! আমার অন্তরকে আপনার দ্বীনের ওপর দৃঢ় রাখুন।",
            referenceBangla = "সুনানে তিরমিযী, হাদিস নং: ২১৪০",
            gradingBangla = "হাসান সহীহ",
            sourceType = DuaSourceType.HADITH,
            dailyAmalSlot = DailyAmalSlot.EVENING
        )
    )
}
