package com.example.ui.islamicarticles

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EditorialDeepGreen = Color(0xFF0A4A35)
private val EditorialMidGreen = Color(0xFF0E5E46)
private val EditorialSoftGreenBg = Color(0xFFEAF5EF)
private val EditorialGold = Color(0xFFD4AF37)
private val EditorialGoldDark = Color(0xFF9A6F00)
private val EditorialWarmCream = Color(0xFFFFFDF8)

/**
 * ৫. আলাদা Full-Screen প্রবন্ধ জমা দেওয়ার পেজ (“আপনার প্রবন্ধ জমা দিন”)
 * - ৮টি নির্ধারিত ফিল্ড + Heading, Bold, Paragraph ও List ফরম্যাটিং এডিটর
 * - নতুন প্রবন্ধ জমা এবং লেখক ড্যাশবোর্ড থেকে সংশোধন (Revise & Resubmit) উভয়ই সমর্থন করে
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IslamicArticleSubmissionFullScreen(
    currentUserId: String,
    defaultAuthorName: String,
    defaultAuthorBio: String,
    editingSubmission: IslamicArticleSubmission?,
    onCancelAndBack: () -> Unit,
    onSubmitSuccess: (IslamicArticleSubmission) -> Unit,
    articleManager: IslamicArticleManager
) {
    var title by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.title ?: "")
    }
    var selectedCategory by remember(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.category ?: IslamicArticleCategory.QURAN_TAFSIR)
    }
    var content by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.content ?: "")
    }
    var authorName by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.authorName?.ifBlank { defaultAuthorName } ?: defaultAuthorName)
    }
    var authorBio by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.authorBio?.ifBlank { defaultAuthorBio } ?: defaultAuthorBio)
    }
    var coverImageUrl by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.coverImageUrl ?: "preset_cover_quran_tadabbur")
    }
    var quranReferences by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.quranReferences ?: "")
    }
    var hadithReferences by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.hadithReferences ?: "")
    }
    var otherSources by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.otherSourcesAndBibliography ?: "")
    }
    var authorReplyNote by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(editingSubmission?.authorReplyNote ?: "")
    }
    var isAuthenticityConfirmed by rememberSaveable(editingSubmission?.submissionId) {
        mutableStateOf(true)
    }
    var showFormattedPreview by rememberSaveable { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverImageUrl = uri.toString()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialWarmCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("article_submission_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialDeepGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "আপনার প্রবন্ধ জমা দিন",
                        tint = EditorialGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (editingSubmission == null) "আপনার প্রবন্ধ জমা দিন" else "প্রবন্ধ সংশোধন ও পুনরায় জমা দিন",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "কুরআন-সুন্নাহর বিশুদ্ধ রেফারেন্সসহ আপনার গবেষণাধর্মী প্রবন্ধ জমা দিন। সম্পাদনা পরিষদের যাচাই ও অনুমোদনের পর তা মূল প্রবন্ধ বিভাগে প্রকাশিত হবে।",
                    color = Color(0xFFDDF3EC),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                if (editingSubmission != null) {
                    Surface(
                        color = EditorialGold.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "সাবমিশন আইডি: ${editingSubmission.submissionId} • বর্তমান অবস্থা: ${editingSubmission.status.banglaLabel}",
                            color = EditorialGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (editingSubmission.adminComment.isNotBlank()) {
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "সম্পাদনা পরিষদের মন্তব্য: ${editingSubmission.adminComment}",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // ১. প্রবন্ধের শিরোনাম
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("১. প্রবন্ধের শিরোনাম *") },
            placeholder = { Text("যেমন: কুরআনুল কারীমের তাদাব্বুর ও ঈমান বৃদ্ধির উপায়") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_title"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        // ২. ক্যাটাগরি নির্বাচন (১২টি নির্ধারিত ক্যাটাগরি)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD5E6DD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "২. প্রবন্ধের ক্যাটাগরি নির্বাচন করুন *",
                    color = EditorialDeepGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IslamicArticleCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.titleBangla,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EditorialDeepGreen,
                                selectedLabelColor = Color.White,
                                containerColor = EditorialSoftGreenBg,
                                labelColor = EditorialDeepGreen
                            ),
                            modifier = Modifier.testTag("article_input_category_${category.id}")
                        )
                    }
                }
            }
        }

        // ৩. সম্পূর্ণ প্রবন্ধের লেখা + ফরম্যাটিং এডিটর (Heading, Bold, Paragraph, List)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD5E6DD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "৩. সম্পূর্ণ প্রবন্ধের লেখা (রচনা এডিটর) *",
                        color = EditorialDeepGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (showFormattedPreview) EditorialDeepGreen else EditorialSoftGreenBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFormattedPreview = !showFormattedPreview }
                            .testTag("editor_toggle_preview_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "প্রিভিউ",
                                tint = if (showFormattedPreview) Color.White else EditorialDeepGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (showFormattedPreview) "এডিট মোড" else "সাজানো প্রিভিউ",
                                color = if (showFormattedPreview) Color.White else EditorialDeepGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Formatting Toolbar: Heading, Bold, Paragraph, List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorFormatChip(
                        label = "Heading (শিরোনাম)",
                        icon = Icons.Default.Title,
                        testTag = "editor_btn_heading",
                        onClick = {
                            val prefix = if (content.isEmpty() || content.endsWith("\n")) "## উপশিরোনাম\n" else "\n\n## উপশিরোনাম\n"
                            content += prefix
                        }
                    )
                    EditorFormatChip(
                        label = "Bold (গাঢ় লেখা)",
                        icon = Icons.Default.FormatBold,
                        testTag = "editor_btn_bold",
                        onClick = {
                            val snippet = if (content.isEmpty() || content.endsWith(" ") || content.endsWith("\n")) "**গুরুত্বপূর্ণ অংশ** " else " **গুরুত্বপূর্ণ অংশ** "
                            content += snippet
                        }
                    )
                    EditorFormatChip(
                        label = "Paragraph (অনুচ্ছেদ)",
                        icon = Icons.Default.Subject,
                        testTag = "editor_btn_paragraph",
                        onClick = {
                            content = content.trimEnd() + "\n\n"
                        }
                    )
                    EditorFormatChip(
                        label = "List (তালিকা)",
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        testTag = "editor_btn_list",
                        onClick = {
                            val item = if (content.isEmpty() || content.endsWith("\n")) "• প্রথম পয়েন্ট\n• দ্বিতীয় পয়েন্ট\n" else "\n• প্রথম পয়েন্ট\n• দ্বিতীয় পয়েন্ট\n"
                            content += item
                        }
                    )
                }

                if (showFormattedPreview) {
                    Surface(
                        color = EditorialWarmCream,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EditorialGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_formatted_preview_box")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "প্রবন্ধের প্রিভিউ:",
                                color = EditorialGoldDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FormattedIslamicArticleBody(
                                content = content.ifBlank { "## নমুনা উপশিরোনাম\nএখানে আপনার সাজানো প্রবন্ধ দেখা যাবে।" },
                                fontSizeSp = 17
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("প্রবন্ধের মূল আলোচনা লিখুন *") },
                        placeholder = {
                            Text("## ভূমিকা\nআপনার প্রবন্ধের মূল আলোচনা সুন্দর অনুচ্ছেদে সাজিয়ে লিখুন...\n\n• প্রথম মূলনীতি\n• দ্বিতীয় মূলনীতি")
                        },
                        minLines = 8,
                        maxLines = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("article_input_content"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EditorialDeepGreen,
                            focusedLabelColor = EditorialDeepGreen
                        )
                    )
                }
            }
        }

        // ৪. লেখকের নাম ও ৫. লেখকের সংক্ষিপ্ত পরিচিতি
        OutlinedTextField(
            value = authorName,
            onValueChange = { authorName = it },
            label = { Text("৪. লেখকের নাম *") },
            placeholder = { Text("যেমন: মাওলানা মুফতি আব্দুল্লাহ") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_author_name"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        OutlinedTextField(
            value = authorBio,
            onValueChange = { authorBio = it },
            label = { Text("৫. লেখকের সংক্ষিপ্ত পরিচিতি *") },
            placeholder = { Text("যেমন: মুহাদ্দিস ও ফিকহ গবেষক, জামিয়া আরাবিয়া ঢাকা") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_author_bio"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        // ৬. প্রাসঙ্গিক ছবি (ঐচ্ছিক)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD5E6DD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "৬. প্রাসঙ্গিক ছবি বা প্রচ্ছদ (ঐচ্ছিক)",
                    color = EditorialDeepGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.dp, EditorialDeepGreen),
                        modifier = Modifier.testTag("article_input_image_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "ছবি নির্বাচন",
                            tint = EditorialDeepGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("গ্যালারি থেকে ছবি নিন", color = EditorialDeepGreen, fontSize = 12.sp)
                    }

                    Text(
                        text = if (coverImageUrl.startsWith("content://") || coverImageUrl.startsWith("file://")) {
                            "কাস্টম ছবি নির্বাচিত হয়েছে"
                        } else {
                            "ইসলামিক ম্যাগাজিন কভার নির্বাচিত"
                        },
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ৭. কুরআন-হাদিসের রেফারেন্স
        OutlinedTextField(
            value = quranReferences,
            onValueChange = { quranReferences = it },
            label = { Text("৭(ক). কুরআনুল কারীমের আয়াতের রেফারেন্স (সূরা ও আয়াত নম্বরসহ)") },
            placeholder = { Text("যেমন: সূরা সোয়াদ (৩৮), আয়াত: ২৯; সূরা হুজুরাত (৪৯), আয়াত: ৬") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_quran_refs"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        OutlinedTextField(
            value = hadithReferences,
            onValueChange = { hadithReferences = it },
            label = { Text("৭(খ). হাদিসের রেফারেন্স (মূল কিতাব ও হাদিস নম্বরসহ)") },
            placeholder = { Text("যেমন: সহীহ বুখারী, হাদিস নং: ১; সহীহ মুসলিম, হাদিস নং: ১৯০৭") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_hadith_refs"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        // ৮. অন্যান্য তথ্যসূত্র ও গ্রন্থপঞ্জি
        OutlinedTextField(
            value = otherSources,
            onValueChange = { otherSources = it },
            label = { Text("৮. অন্যান্য তথ্যসূত্র ও নির্ভরযোগ্য গ্রন্থপঞ্জি *") },
            placeholder = { Text("যেমন: তাফসিরে ইবনে কাসির, খণ্ড ৪; ফাতহুল বারী, খণ্ড ১; রদ্দুল মুহতার, খণ্ড ৬") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_input_other_sources"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EditorialDeepGreen,
                focusedLabelColor = EditorialDeepGreen
            )
        )

        if (editingSubmission != null) {
            OutlinedTextField(
                value = authorReplyNote,
                onValueChange = { authorReplyNote = it },
                label = { Text("সংশোধনী নোট (অ্যাডমিনের অবগতির জন্য)") },
                placeholder = { Text("আপনি কী কী সংশোধন বা রেফারেন্স সংযোজন করেছেন তা সংক্ষেপে লিখুন") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("article_input_author_reply_note"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialDeepGreen,
                    focusedLabelColor = EditorialDeepGreen
                )
            )
        }

        // ৮ নং নির্দেশনা: ইসলামিক তথ্যের নির্ভুলতা অঙ্গীকারনামা
        Surface(
            color = EditorialSoftGreenBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EditorialDeepGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAuthenticityConfirmed = !isAuthenticityConfirmed }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAuthenticityConfirmed,
                    onCheckedChange = { isAuthenticityConfirmed = it },
                    colors = CheckboxDefaults.colors(checkedColor = EditorialDeepGreen),
                    modifier = Modifier.testTag("article_authenticity_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "আমি সাক্ষ্য দিচ্ছি যে, এই প্রবন্ধে কোনো জাল বা বানোয়াট আয়াত/হাদিস ব্যবহার করা হয়নি এবং সকল কুরআন-হাদিস ও ফিকহি রেফারেন্স মূল কিতাব থেকে যাচাই করে উল্লেখ করা হয়েছে।",
                    color = EditorialDeepGreen,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!feedbackMessage.isNullOrBlank()) {
            Surface(
                color = if (isErrorFeedback) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("article_submission_feedback_banner")
            ) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    color = if (isErrorFeedback) Color(0xFFC62828) else EditorialDeepGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelAndBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("article_submission_cancel_btn"),
                border = BorderStroke(1.dp, EditorialDeepGreen)
            ) {
                Text("ফিরে যান", color = EditorialDeepGreen, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    val result = if (editingSubmission == null) {
                        articleManager.submitNewArticle(
                            authorId = currentUserId,
                            authorName = authorName,
                            authorBio = authorBio,
                            title = title,
                            category = selectedCategory,
                            content = content,
                            coverImageUrl = coverImageUrl,
                            quranReferences = quranReferences,
                            hadithReferences = hadithReferences,
                            otherSourcesAndBibliography = otherSources,
                            isAuthenticityConfirmed = isAuthenticityConfirmed
                        )
                    } else {
                        articleManager.resubmitUpdatedArticle(
                            submissionId = editingSubmission.submissionId,
                            authorId = currentUserId,
                            title = title,
                            category = selectedCategory,
                            content = content,
                            authorName = authorName,
                            authorBio = authorBio,
                            coverImageUrl = coverImageUrl,
                            quranReferences = quranReferences,
                            hadithReferences = hadithReferences,
                            otherSourcesAndBibliography = otherSources,
                            authorReplyNote = authorReplyNote
                        )
                    }

                    result.onSuccess { savedSubmission ->
                        isErrorFeedback = false
                        feedbackMessage = "আলহামদুলিল্লাহ! প্রবন্ধ সফলভাবে জমা হয়েছে। সাবমিশন আইডি: ${savedSubmission.submissionId}"
                        onSubmitSuccess(savedSubmission)
                    }.onFailure { err ->
                        isErrorFeedback = true
                        feedbackMessage = err.message ?: "প্রবন্ধ জমা দিতে সমস্যা হয়েছে।"
                    }
                },
                modifier = Modifier
                    .weight(1.6f)
                    .height(48.dp)
                    .testTag("article_submit_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialDeepGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "জমা দিন",
                    tint = EditorialGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingSubmission == null) "প্রবন্ধ জমা দিন" else "সংশোধিত প্রবন্ধ জমা দিন",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditorFormatChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = EditorialSoftGreenBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, EditorialDeepGreen.copy(alpha = 0.3f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = EditorialDeepGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = EditorialDeepGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * ৬. আমার জমা / লেখক Dashboard (Author Submissions Dashboard Section)
 * - ৬টি স্ট্যাটাস ব্যাজ: Submitted, Under Review, More Information Required, Approved, Published, Rejected
 * - অ্যাডমিনের মন্তব্য প্রদর্শন ও প্রয়োজন হলে প্রবন্ধ সংশোধন করে পুনরায় জমা দেওয়ার বাটন
 */
@Composable
fun AuthorSubmissionsDashboardSection(
    currentUserId: String,
    authorSubmissions: List<IslamicArticleSubmission>,
    lastSubmittedIdBanner: String?,
    onOpenNewSubmissionPage: () -> Unit,
    onReviseSubmission: (IslamicArticleSubmission) -> Unit,
    onOpenPublishedArticleById: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("article_author_dashboard_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!lastSubmittedIdBanner.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submission_success_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, EditorialDeepGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "সফল",
                        tint = EditorialDeepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "আপনার প্রবন্ধ সফলভাবে জমা হয়েছে!",
                            color = EditorialDeepGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Submission ID: $lastSubmittedIdBanner • অ্যাডমিন অনুমোদনের পর এটি পাবলিক প্রবন্ধ তালিকায় প্রকাশিত হবে।",
                            color = EditorialMidGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("submission_id_text")
                        )
                    }
                }
            }
        }

        // Status Overview Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD5E6DD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "আমার জমা / লেখক ড্যাশবোর্ড",
                            color = EditorialDeepGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "আপনার জমা দেওয়া প্রবন্ধের বর্তমান অবস্থা ও সম্পাদনা পরিষদের মন্তব্য",
                            color = Color(0xFF4B5563),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onOpenNewSubmissionPage,
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialDeepGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("author_dashboard_new_submit_btn")
                    ) {
                        Text("নতুন প্রবন্ধ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 6 Status Legend Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ArticleSubmissionStatus.entries.forEach { st ->
                        Surface(
                            color = st.badgeBgColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = st.englishLabel,
                                color = st.badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (authorSubmissions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD5E6DD))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "কোনো জমা নেই",
                        tint = EditorialMidGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "আপনি এখনো কোনো প্রবন্ধ জমা দেননি",
                        color = EditorialDeepGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "“আপনার প্রবন্ধ জমা দিন” বাটনে চাপ দিয়ে কুরআন-হাদিসের রেফারেন্সসহ আপনার প্রথম প্রবন্ধ জমা দিন।",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            authorSubmissions.forEach { sub ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("author_submission_card_${sub.submissionId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD5E6DD))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = EditorialSoftGreenBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ID: ${sub.submissionId}",
                                    color = EditorialDeepGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = sub.status.badgeBgColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("author_submission_status_${sub.submissionId}")
                            ) {
                                Text(
                                    text = sub.status.banglaLabel,
                                    color = sub.status.badgeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = sub.title,
                            color = Color(0xFF111827),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "বিভাগ: ${sub.category.titleBangla} • লেখক: ${sub.authorName}",
                            color = EditorialMidGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (sub.adminComment.isNotBlank()) {
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EditorialGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("author_submission_admin_comment_${sub.submissionId}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "অ্যাডমিন / সম্পাদনা পরিষদের মন্তব্য:",
                                        color = EditorialGoldDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sub.adminComment,
                                        color = Color(0xFF374151),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (sub.status == ArticleSubmissionStatus.SUBMITTED ||
                                sub.status == ArticleSubmissionStatus.MORE_INFO_REQUIRED ||
                                sub.status == ArticleSubmissionStatus.UNDER_REVIEW
                            ) {
                                OutlinedButton(
                                    onClick = { onReviseSubmission(sub) },
                                    border = BorderStroke(1.dp, EditorialDeepGreen),
                                    modifier = Modifier.testTag("article_revise_resubmit_btn_${sub.submissionId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "সংশোধন করুন",
                                        tint = EditorialDeepGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "সংশোধন ও পুনরায় জমা দিন",
                                        color = EditorialDeepGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (sub.status == ArticleSubmissionStatus.PUBLISHED && sub.publishedArticleId.isNotBlank()) {
                                Button(
                                    onClick = { onOpenPublishedArticleById(sub.publishedArticleId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EditorialDeepGreen),
                                    modifier = Modifier.testTag("author_view_published_btn_${sub.submissionId}")
                                ) {
                                    Text("প্রকাশিত প্রবন্ধ পড়ুন", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ৭. Admin Review ও প্রকাশনা সিস্টেম (Full-Screen Admin Review Dashboard)
 * - ৬টি ফিল্টার ট্যাব: নতুন জমা, যাচাইাধীন, সংশোধন প্রয়োজন, অনুমোদিত, প্রকাশিত, বাতিল
 * - রেফারেন্স যাচাই চেকবক্স, প্রবন্ধ সম্পাদনা, অনুমোদন, প্রকাশ ও বাতিল করার পূর্ণাঙ্গ ব্যবস্থা
 */
@Composable
fun IslamicArticleAdminReviewFullScreen(
    adminId: String,
    adminEmail: String?,
    adminPhone: String?,
    allSubmissions: List<IslamicArticleSubmission>,
    articleManager: IslamicArticleManager,
    onBackToMagazineHome: () -> Unit
) {
    var selectedStatusFilter by rememberSaveable { mutableStateOf<ArticleSubmissionStatus?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var isActionError by remember { mutableStateOf(false) }

    val isAuthorized = articleManager.isAuthorizedAdmin(adminEmail, adminPhone)

    val filteredList = remember(allSubmissions, selectedStatusFilter) {
        if (selectedStatusFilter == null) {
            allSubmissions.sortedByDescending { it.updatedAtMillis }
        } else {
            allSubmissions.filter { it.status == selectedStatusFilter }
                .sortedByDescending { it.updatedAtMillis }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialWarmCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("article_admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialDeepGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "অ্যাডমিন রিভিউ",
                            tint = EditorialGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "অ্যাডমিন প্রবন্ধ যাচাই ও প্রকাশনা প্যানেল",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onBackToMagazineHome,
                        border = BorderStroke(1.dp, EditorialGold),
                        modifier = Modifier.testTag("admin_dashboard_back_btn")
                    ) {
                        Text("মূল পাতায় ফিরুন", color = EditorialGold, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "কুরআন-হাদিস ও তথ্যসূত্র যাচাই করে প্রবন্ধ অনুমোদন ও প্রকাশ করুন। অনুমোদনহীন কোনো প্রবন্ধ পাবলিক পেজে প্রদর্শিত হবে না।",
                    color = Color(0xFFDDF3EC),
                    fontSize = 12.sp
                )
            }
        }

        if (!isAuthorized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = "শুধুমাত্র অনুমোদিত অ্যাডমিন প্রবন্ধ যাচাই ও প্রকাশ করতে পারবেন।",
                    color = Color(0xFFC62828),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }

        // 6 Admin Filter Tabs + "সকল জমা"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                label = { Text("সকল (${allSubmissions.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EditorialDeepGreen,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("admin_tab_ALL")
            )
            ArticleSubmissionStatus.entries.forEach { st ->
                val count = allSubmissions.count { it.status == st }
                val isSelected = selectedStatusFilter == st
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStatusFilter = st },
                    label = {
                        Text(
                            text = "${st.adminTabLabelBangla} ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EditorialDeepGreen,
                        selectedLabelColor = Color.White,
                        containerColor = EditorialSoftGreenBg,
                        labelColor = EditorialDeepGreen
                    ),
                    modifier = Modifier.testTag("admin_tab_${st.id}")
                )
            }
        }

        if (!actionFeedback.isNullOrBlank()) {
            Surface(
                color = if (isActionError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_action_feedback_banner")
            ) {
                Text(
                    text = actionFeedback.orEmpty(),
                    color = if (isActionError) Color(0xFFC62828) else EditorialDeepGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (filteredList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "এই বিভাগে বর্তমানে কোনো প্রবন্ধ সাবমিশন নেই।",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            filteredList.forEach { sub ->
                AdminSubmissionReviewCard(
                    submission = sub,
                    adminId = adminId,
                    adminEmail = adminEmail,
                    adminPhone = adminPhone,
                    articleManager = articleManager,
                    onFeedback = { msg, err ->
                        actionFeedback = msg
                        isActionError = err
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminSubmissionReviewCard(
    submission: IslamicArticleSubmission,
    adminId: String,
    adminEmail: String?,
    adminPhone: String?,
    articleManager: IslamicArticleManager,
    onFeedback: (String, Boolean) -> Unit
) {
    var adminComment by rememberSaveable(submission.submissionId, submission.adminComment) {
        mutableStateOf(submission.adminComment)
    }
    var isReferencesVerified by rememberSaveable(submission.submissionId, submission.isReferencesVerifiedByAdmin) {
        mutableStateOf(submission.isReferencesVerifiedByAdmin || submission.isAuthenticityConfirmed)
    }
    var markAsFeatured by rememberSaveable(submission.submissionId) {
        mutableStateOf(false)
    }
    var scholarlyNote by rememberSaveable(submission.submissionId) {
        mutableStateOf("")
    }
    var isEditMode by rememberSaveable(submission.submissionId) {
        mutableStateOf(false)
    }
    var editedTitle by rememberSaveable(submission.submissionId, submission.title) {
        mutableStateOf(submission.title)
    }
    var editedContent by rememberSaveable(submission.submissionId, submission.content) {
        mutableStateOf(submission.content)
    }
    var editedQuranRefs by rememberSaveable(submission.submissionId, submission.quranReferences) {
        mutableStateOf(submission.quranReferences)
    }
    var editedHadithRefs by rememberSaveable(submission.submissionId, submission.hadithReferences) {
        mutableStateOf(submission.hadithReferences)
    }
    var editedOtherSources by rememberSaveable(submission.submissionId, submission.otherSourcesAndBibliography) {
        mutableStateOf(submission.otherSourcesAndBibliography)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_submission_card_${submission.submissionId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD5E6DD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Submission ID: ${submission.submissionId}",
                    color = EditorialDeepGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = submission.status.badgeBgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = submission.status.banglaLabel,
                        color = submission.status.badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = submission.title,
                color = Color(0xFF111827),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "বিভাগ: ${submission.category.titleBangla} • লেখক: ${submission.authorName} (${submission.authorBio})",
                color = EditorialMidGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Reference summary box
            Surface(
                color = EditorialSoftGreenBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "প্রবন্ধের সংক্ষিপ্ত অংশ ও রেফারেন্স যাচাই:",
                        color = EditorialDeepGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = submission.content.take(260) + if (submission.content.length > 260) "..." else "",
                        color = Color(0xFF1F2937),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    if (submission.quranReferences.isNotBlank()) {
                        Text(
                            text = "কুরআন রেফারেন্স: ${submission.quranReferences}",
                            color = EditorialDeepGreen,
                            fontSize = 12.sp
                        )
                    }
                    if (submission.hadithReferences.isNotBlank()) {
                        Text(
                            text = "হাদিস রেফারেন্স: ${submission.hadithReferences}",
                            color = EditorialDeepGreen,
                            fontSize = 12.sp
                        )
                    }
                    if (submission.otherSourcesAndBibliography.isNotBlank()) {
                        Text(
                            text = "গ্রন্থপঞ্জি: ${submission.otherSourcesAndBibliography}",
                            color = Color(0xFF374151),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Admin Edit Content Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isReferencesVerified = !isReferencesVerified }
                ) {
                    Checkbox(
                        checked = isReferencesVerified,
                        onCheckedChange = { isReferencesVerified = it },
                        colors = CheckboxDefaults.colors(checkedColor = EditorialDeepGreen),
                        modifier = Modifier.testTag("admin_verify_refs_checkbox_${submission.submissionId}")
                    )
                    Text(
                        text = "কুরআন-হাদিস ও তথ্যসূত্র যাচাইকৃত",
                        color = EditorialDeepGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { isEditMode = !isEditMode },
                    border = BorderStroke(1.dp, EditorialDeepGreen),
                    modifier = Modifier.testTag("admin_edit_content_btn_${submission.submissionId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "সম্পাদনা",
                        tint = EditorialDeepGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEditMode) "সম্পাদনা বন্ধ করুন" else "প্রবন্ধ সম্পাদনা",
                        color = EditorialDeepGreen,
                        fontSize = 12.sp
                    )
                }
            }

            if (isEditMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorialWarmCream, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("শিরোনাম সম্পাদনা") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_edit_title_input_${submission.submissionId}")
                    )
                    OutlinedTextField(
                        value = editedContent,
                        onValueChange = { editedContent = it },
                        label = { Text("মূল প্রবন্ধ সম্পাদনা") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_edit_content_input_${submission.submissionId}")
                    )
                    OutlinedTextField(
                        value = editedQuranRefs,
                        onValueChange = { editedQuranRefs = it },
                        label = { Text("কুরআন রেফারেন্স সম্পাদনা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedHadithRefs,
                        onValueChange = { editedHadithRefs = it },
                        label = { Text("হাদিস রেফারেন্স সম্পাদনা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            articleManager.adminEditSubmissionContent(
                                adminId = adminId,
                                adminEmail = adminEmail,
                                adminPhone = adminPhone,
                                submissionId = submission.submissionId,
                                updatedTitle = editedTitle,
                                updatedCategory = submission.category,
                                updatedContent = editedContent,
                                updatedQuranRefs = editedQuranRefs,
                                updatedHadithRefs = editedHadithRefs,
                                updatedOtherSources = editedOtherSources,
                                adminComment = adminComment
                            ).onSuccess {
                                isEditMode = false
                                onFeedback("প্রবন্ধের তথ্য সফলভাবে সম্পাদিত হয়েছে।", false)
                            }.onFailure { e ->
                                onFeedback(e.message ?: "সম্পাদনা ব্যর্থ হয়েছে।", true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialDeepGreen),
                        modifier = Modifier.testTag("admin_save_edit_btn_${submission.submissionId}")
                    ) {
                        Text("সম্পাদনা সংরক্ষণ করুন", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { markAsFeatured = !markAsFeatured }
            ) {
                Checkbox(
                    checked = markAsFeatured,
                    onCheckedChange = { markAsFeatured = it },
                    colors = CheckboxDefaults.colors(checkedColor = EditorialGoldDark),
                    modifier = Modifier.testTag("admin_mark_featured_checkbox_${submission.submissionId}")
                )
                Text(
                    text = "নির্বাচিত প্রবন্ধ (Featured) হিসেবে চিহ্নিত করুন",
                    color = EditorialGoldDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = scholarlyNote,
                onValueChange = { scholarlyNote = it },
                label = { Text("আলেমদের মতভেদ নোট (প্রযোজ্য ক্ষেত্রে ঐচ্ছিক)") },
                placeholder = { Text("ফিকহি বা ইলমি মতভেদ থাকলে সংক্ষেপে উল্লেখ করুন") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_scholarly_note_input_${submission.submissionId}")
            )

            OutlinedTextField(
                value = adminComment,
                onValueChange = { adminComment = it },
                label = { Text("অ্যাডমিনের মন্তব্য / সংশোধনের নির্দেশনা") },
                placeholder = { Text("লেখকের জন্য মন্তব্য বা অনুমোদন নোট লিখুন") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_comment_input_${submission.submissionId}")
            )

            // Admin Status Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        articleManager.adminReviewSubmission(
                            adminId = adminId,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            submissionId = submission.submissionId,
                            newStatus = ArticleSubmissionStatus.UNDER_REVIEW,
                            adminComment = adminComment.ifBlank { "আপনার প্রবন্ধটি বর্তমানে সম্পাদনা পরিষদের যাচাইয়ের অধীনে রয়েছে।" },
                            isReferencesVerified = isReferencesVerified,
                            scholarlyDifferenceNote = scholarlyNote,
                            markAsFeatured = markAsFeatured
                        ).onSuccess {
                            onFeedback("প্রবন্ধটি ‘যাচাইাধীন’ তালিকায় নেওয়া হয়েছে।", false)
                        }.onFailure { e ->
                            onFeedback(e.message ?: "ত্রুটি ঘটেছে।", true)
                        }
                    },
                    modifier = Modifier.testTag("admin_action_under_review_btn_${submission.submissionId}")
                ) {
                    Text("যাচাইাধীন", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        articleManager.adminReviewSubmission(
                            adminId = adminId,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            submissionId = submission.submissionId,
                            newStatus = ArticleSubmissionStatus.MORE_INFO_REQUIRED,
                            adminComment = adminComment.ifBlank { "রেফারেন্সের হাদিস নম্বর ও তথ্যসূত্র আরও স্পষ্ট করে পুনরায় জমা দিন।" },
                            isReferencesVerified = isReferencesVerified,
                            scholarlyDifferenceNote = scholarlyNote,
                            markAsFeatured = markAsFeatured
                        ).onSuccess {
                            onFeedback("লেখককে সংশোধনের জন্য অবহিত করা হয়েছে।", false)
                        }.onFailure { e ->
                            onFeedback(e.message ?: "ত্রুটি ঘটেছে।", true)
                        }
                    },
                    modifier = Modifier.testTag("admin_action_more_info_btn_${submission.submissionId}")
                ) {
                    Text("সংশোধন প্রয়োজন", fontSize = 12.sp, color = Color(0xFFD97706))
                }

                OutlinedButton(
                    onClick = {
                        articleManager.adminReviewSubmission(
                            adminId = adminId,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            submissionId = submission.submissionId,
                            newStatus = ArticleSubmissionStatus.APPROVED,
                            adminComment = adminComment.ifBlank { "রেফারেন্স যাচাই সম্পন্ন হয়েছে এবং প্রবন্ধটি অনুমোদিত হয়েছে।" },
                            isReferencesVerified = isReferencesVerified,
                            scholarlyDifferenceNote = scholarlyNote,
                            markAsFeatured = markAsFeatured
                        ).onSuccess {
                            onFeedback("প্রবন্ধটি অনুমোদিত (Approved) হয়েছে।", false)
                        }.onFailure { e ->
                            onFeedback(e.message ?: "অনুমোদন করতে সমস্যা হয়েছে।", true)
                        }
                    },
                    border = BorderStroke(1.dp, EditorialMidGreen),
                    modifier = Modifier.testTag("admin_action_approve_btn_${submission.submissionId}")
                ) {
                    Text("অনুমোদন করুন", fontSize = 12.sp, color = EditorialMidGreen, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        articleManager.adminReviewSubmission(
                            adminId = adminId,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            submissionId = submission.submissionId,
                            newStatus = ArticleSubmissionStatus.PUBLISHED,
                            adminComment = adminComment.ifBlank { "আলহামদুলিল্লাহ! আপনার প্রবন্ধটি যাচাই শেষে প্রকাশিত হয়েছে।" },
                            isReferencesVerified = isReferencesVerified,
                            scholarlyDifferenceNote = scholarlyNote,
                            markAsFeatured = markAsFeatured
                        ).onSuccess {
                            onFeedback("আলহামদুলিল্লাহ! প্রবন্ধটি পাবলিক প্রবন্ধ বিভাগে প্রকাশিত হয়েছে।", false)
                        }.onFailure { e ->
                            onFeedback(e.message ?: "প্রকাশ করতে সমস্যা হয়েছে।", true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialDeepGreen),
                    modifier = Modifier.testTag("admin_action_publish_btn_${submission.submissionId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Publish,
                        contentDescription = "প্রকাশ করুন",
                        tint = EditorialGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("প্রকাশ করুন (Publish)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        articleManager.adminReviewSubmission(
                            adminId = adminId,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            submissionId = submission.submissionId,
                            newStatus = ArticleSubmissionStatus.REJECTED,
                            adminComment = adminComment.ifBlank { "রেফারেন্সের অসামঞ্জস্যতার কারণে প্রবন্ধটি প্রকাশের জন্য গৃহীত হয়নি।" },
                            isReferencesVerified = false,
                            scholarlyDifferenceNote = scholarlyNote,
                            markAsFeatured = false
                        ).onSuccess {
                            onFeedback("প্রবন্ধটি বাতিল (Rejected) করা হয়েছে।", false)
                        }.onFailure { e ->
                            onFeedback(e.message ?: "ত্রুটি ঘটেছে।", true)
                        }
                    },
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    modifier = Modifier.testTag("admin_action_reject_btn_${submission.submissionId}")
                ) {
                    Text("বাতিল করুন", fontSize = 12.sp, color = Color(0xFFC62828))
                }
            }
        }
    }
}

/**
 * প্রবন্ধের মূল বডি রেন্ডারার (Heading `##`, Bold `**`, Paragraph, ও List `•` সমর্থনসহ)
 */
@Composable
fun FormattedIslamicArticleBody(
    content: String,
    fontSizeSp: Int,
    modifier: Modifier = Modifier
) {
    val lines = remember(content) { content.lines() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
            } else if (line.startsWith("##")) {
                val headingText = line.removePrefix("##").trim()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((fontSizeSp + 6).dp)
                            .background(EditorialGold, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = headingText,
                        color = EditorialDeepGreen,
                        fontSize = (fontSizeSp + 3).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (fontSizeSp + 10).sp
                    )
                }
            } else if (line.startsWith("•") || line.startsWith("- ")) {
                val bulletText = line.removePrefix("•").removePrefix("-").trim().replace("**", "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = (fontSizeSp / 2).dp)
                            .size(7.dp)
                            .background(EditorialDeepGreen, CircleShape)
                    )
                    Text(
                        text = bulletText,
                        color = Color(0xFF1F2937),
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp + 9).sp,
                        fontWeight = if (line.contains("**")) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            } else {
                val cleanParagraph = line.replace("**", "")
                Text(
                    text = cleanParagraph,
                    color = Color(0xFF1F2937),
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp + 10).sp,
                    fontWeight = if (line.startsWith("**") && line.endsWith("**")) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
