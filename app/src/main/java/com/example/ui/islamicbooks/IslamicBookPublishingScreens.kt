package com.example.ui.islamicbooks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val PortalDeepGreen = Color(0xFF0E5A3E)
private val PortalDarkGreen = Color(0xFF093B28)
private val PortalMintBg = Color(0xFFF3F8F5)
private val PortalGold = Color(0xFFC99A2E)
private val PortalGoldLight = Color(0xFFFFF8E6)

enum class SelfPublishingPortalTab(val titleBangla: String) {
    MY_DASHBOARD("আমার জমা দেওয়া বই"),
    NEW_SUBMISSION("নতুন বই জমা দিন"),
    CONTRIBUTOR_PROFILE("লেখক/প্রকাশক প্রোফাইল")
}

/**
 * ৬, ৭, ৮ ও ১০ নং নির্দেশনা অনুযায়ী লেখক, অনুবাদক ও প্রকাশকদের স্বতন্ত্র সেলফ-পাবলিশিং পোর্টাল (Full-Screen)
 */
@Composable
fun AuthorPublisherSelfPublishingPortalScreen(
    bookManager: IslamicBookManager,
    currentUserId: String,
    currentUserName: String,
    currentUserPhone: String,
    currentUserEmail: String,
    onBackToLibrary: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(SelfPublishingPortalTab.MY_DASHBOARD) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    val allSubmissions by bookManager.allSubmissions.collectAsState()
    val mySubmissions = remember(allSubmissions, currentUserId) {
        bookManager.getSubmissionsForContributor(currentUserId)
    }
    val uploadProgress by bookManager.uploadProgress.collectAsState()

    // Contributor Profile State
    val existingProfile = remember(currentUserId, allSubmissions) {
        bookManager.getContributorProfile(currentUserId)
    }
    var profileFullName by remember(existingProfile, currentUserName) {
        mutableStateOf(existingProfile?.fullName?.ifBlank { currentUserName } ?: currentUserName)
    }
    var profileMobile by remember(existingProfile, currentUserPhone) {
        mutableStateOf(existingProfile?.mobileNumber?.ifBlank { currentUserPhone } ?: currentUserPhone.ifBlank { "01700000000" })
    }
    var profileEmail by remember(existingProfile, currentUserEmail) {
        mutableStateOf(existingProfile?.email?.ifBlank { currentUserEmail } ?: currentUserEmail)
    }
    var profileRole by remember(existingProfile) {
        mutableStateOf(existingProfile?.contributorRole ?: ContributorRoleType.AUTHOR)
    }
    var profileOrgName by remember(existingProfile) {
        mutableStateOf(existingProfile?.organizationOrPublisherName ?: "")
    }
    var profileBio by remember(existingProfile) {
        mutableStateOf(existingProfile?.shortBio ?: "")
    }
    var profileContactInfo by remember(existingProfile, currentUserPhone) {
        mutableStateOf(existingProfile?.contactInfo?.ifBlank { currentUserPhone } ?: currentUserPhone)
    }

    // 15-Field Book Submission Form State
    var bookTitleBangla by remember { mutableStateOf("") }
    var bookTitleArabic by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf(profileFullName) }
    var translatorName by remember { mutableStateOf("") }
    var publisherName by remember { mutableStateOf(profileOrgName.ifBlank { "স্বতন্ত্র প্রকাশনা" }) }
    var selectedCategory by remember { mutableStateOf(IslamicBookCategory.FIQH_MASAIL) }
    var selectedLanguage by remember { mutableStateOf("বাংলা") }
    var editionText by remember { mutableStateOf("প্রথম সংস্করণ") }
    var publicationYear by remember { mutableStateOf("২০২৫") }
    var isYearVerified by remember { mutableStateOf(true) }
    var pageCountText by remember { mutableStateOf("১৬০") }
    var isbnText by remember { mutableStateOf("") }
    var shortIntroText by remember { mutableStateOf("") }
    var tocText by remember { mutableStateOf("") }
    val selectedCoverFromManager by bookManager.selectedCoverImageUri.collectAsState()
    var coverImageUriString by remember { mutableStateOf(selectedCoverFromManager) }
    LaunchedEffect(selectedCoverFromManager) {
        if (selectedCoverFromManager.isNotBlank()) {
            coverImageUriString = selectedCoverFromManager
        }
    }

    // Copyright Verification State
    var copyrightOwnerName by remember { mutableStateOf(profileFullName) }
    var copyrightPermissionType by remember { mutableStateOf(CopyrightPermissionType.SELF_COPYRIGHT) }
    var permissionProofText by remember { mutableStateOf("") }
    var isTruthConfirmed by remember { mutableStateOf(false) }

    var lastCreatedSubmissionId by remember { mutableStateOf<String?>(null) }

    // Zero-permission Photo Picker for Cover Image
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coverImageUriString = bookManager.prepareCoverImageFromUri(uri)
            statusBannerMessage = "কভার ছবি নির্বাচিত ও সংরক্ষিত হয়েছে।"
            isStatusError = false
        }
    }

    // Document Picker for PDF Upload
    val pdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "uploaded_islamic_book.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val res = bookManager.validateAndPreparePdfForUpload(
                        fileName = displayName,
                        pdfBytes = bytes,
                        contributorId = currentUserId.ifBlank { "contributor_user" }
                    )
                    res.onSuccess {
                        pageCountText = it.detectedPageCount.coerceAtLeast(1).toString()
                        statusBannerMessage = "আলহামদুলিল্লাহ! পিডিএফ ফাইল যাচাই ও প্রিভিউ প্রস্তুত হয়েছে (${it.detectedPageCount} পৃষ্ঠা)।"
                        isStatusError = false
                    }.onFailure { err ->
                        statusBannerMessage = err.message
                        isStatusError = true
                    }
                }
            } catch (e: Exception) {
                statusBannerMessage = "পিডিএফ ফাইল পড়তে ত্রুটি: ${e.localizedMessage}"
                isStatusError = true
            }
        }
    }

    BackHandler {
        if (activeTab != SelfPublishingPortalTab.MY_DASHBOARD) {
            activeTab = SelfPublishingPortalTab.MY_DASHBOARD
        } else {
            onBackToLibrary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalMintBg)
            .testTag("author_publisher_portal_screen")
    ) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(PortalDarkGreen, PortalDeepGreen)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (activeTab != SelfPublishingPortalTab.MY_DASHBOARD) {
                                activeTab = SelfPublishingPortalTab.MY_DASHBOARD
                            } else {
                                onBackToLibrary()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("portal_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "লাইব্রেরিতে ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "লেখক, অনুবাদক ও প্রকাশক পোর্টাল",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "নিজের লেখা, অনূদিত বা প্রকাশিত বই যাচাইপূর্বক লাইব্রেরিতে যুক্ত করুন",
                            color = Color(0xFFD9F2E6),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Portal Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelfPublishingPortalTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        val tag = when (tab) {
                            SelfPublishingPortalTab.MY_DASHBOARD -> "contributor_dashboard_tab"
                            SelfPublishingPortalTab.NEW_SUBMISSION -> "new_book_submission_btn"
                            SelfPublishingPortalTab.CONTRIBUTOR_PROFILE -> "contributor_profile_tab"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeTab = tab },
                            label = {
                                Text(
                                    text = tab.titleBangla,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PortalDarkGreen else Color.White
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PortalGoldLight,
                                containerColor = Color.White.copy(alpha = 0.14f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = PortalGold
                            ),
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
            }
        }

        // Status Feedback Banner
        statusBannerMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("portal_status_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStatusError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isStatusError) Color(0xFFDC2626) else Color(0xFF15803D)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isStatusError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isStatusError) Color(0xFFB91C1C) else Color(0xFF15803D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = if (isStatusError) Color(0xFF7F1D1D) else Color(0xFF14532D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { statusBannerMessage = null },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }
            }
        }

        when (activeTab) {
            SelfPublishingPortalTab.MY_DASHBOARD -> {
                ContributorDashboardContent(
                    profile = existingProfile,
                    mySubmissions = mySubmissions,
                    lastCreatedSubmissionId = lastCreatedSubmissionId,
                    onOpenNewSubmission = { activeTab = SelfPublishingPortalTab.NEW_SUBMISSION },
                    onOpenProfile = { activeTab = SelfPublishingPortalTab.CONTRIBUTOR_PROFILE },
                    onReplyAdditionalInfo = { submissionId, replyText ->
                        val res = bookManager.submitContributorAdditionalInfo(
                            submissionId = submissionId,
                            contributorId = currentUserId,
                            replyNoteBangla = replyText
                        )
                        res.onSuccess {
                            statusBannerMessage = "অতিরিক্ত তথ্য সফলভাবে পাঠানো হয়েছে। স্ট্যাটাস: যাচাইাধীন।"
                            isStatusError = false
                        }.onFailure { err ->
                            statusBannerMessage = err.message
                            isStatusError = true
                        }
                    }
                )
            }

            SelfPublishingPortalTab.CONTRIBUTOR_PROFILE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("contributor_profile_form"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PortalGold.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "৬.১ লেখক / অনুবাদক / প্রকাশক প্রোফাইল নিবন্ধন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PortalDarkGreen
                                )
                                Text(
                                    text = "আপনার বর্তমান কওমি কর্মসংস্থান অ্যাকাউন্টের অধীনে লেখক/প্রকাশক তথ্য সংরক্ষণ করুন। আলাদা কোনো দ্বিতীয় লগইন প্রয়োজন নেই।",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = profileFullName,
                                    onValueChange = { profileFullName = it },
                                    label = { Text("১. পূর্ণ নাম *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_name_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = profileMobile,
                                    onValueChange = { profileMobile = it },
                                    label = { Text("২. মোবাইল নম্বর *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_phone_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = profileEmail,
                                    onValueChange = { profileEmail = it },
                                    label = { Text("৩. ইমেইল (ঐচ্ছিক)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_email_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "৪. আপনার পরিচয় নির্বাচন করুন:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PortalDarkGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ContributorRoleType.entries.forEach { role ->
                                        FilterChip(
                                            selected = profileRole == role,
                                            onClick = { profileRole = role },
                                            label = { Text(role.labelBangla) },
                                            modifier = Modifier.testTag("contributor_role_chip_${role.id}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = profileOrgName,
                                    onValueChange = { profileOrgName = it },
                                    label = { Text("৫. প্রকাশনী বা প্রতিষ্ঠানের নাম (থাকলে)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_org_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = profileBio,
                                    onValueChange = { profileBio = it },
                                    label = { Text("৬. সংক্ষিপ্ত পরিচিতি (শিক্ষাগত যোগ্যতা ও রচনাবলি)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_bio_input"),
                                    minLines = 2
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = profileContactInfo,
                                    onValueChange = { profileContactInfo = it },
                                    label = { Text("৭. যোগাযোগের ঠিকানা / হোয়াটসঅ্যাপ") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contributor_contact_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val res = bookManager.saveContributorProfile(
                                            BookContributorProfile(
                                                userId = currentUserId.ifBlank { "contributor_default_uid" },
                                                fullName = profileFullName,
                                                mobileNumber = profileMobile,
                                                email = profileEmail,
                                                contributorRole = profileRole,
                                                organizationOrPublisherName = profileOrgName,
                                                shortBio = profileBio,
                                                contactInfo = profileContactInfo
                                            )
                                        )
                                        res.onSuccess {
                                            statusBannerMessage = "আলহামদুলিল্লাহ! লেখক/প্রকাশক প্রোফাইল সফলভাবে সংরক্ষিত হয়েছে।"
                                            isStatusError = false
                                        }.onFailure { err ->
                                            statusBannerMessage = err.message
                                            isStatusError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("save_contributor_profile_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("প্রোফাইল সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            SelfPublishingPortalTab.NEW_SUBMISSION -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("book_submission_form_screen"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PortalGold.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "৬.২ নতুন বই জমা দিন (১৫টি আবশ্যকীয় তথ্য ও কপিরাইট যাচাই)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PortalDarkGreen
                                )
                                Text(
                                    text = "ফাইল নীতিমালা: শুধুমাত্র বৈধ PDF (.pdf) ফাইল • সর্বোচ্চ ফাইল সাইজ: ৫০ মেগাবাইট",
                                    fontSize = 12.sp,
                                    color = PortalDeepGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // 1. বইয়ের বাংলা নাম
                                OutlinedTextField(
                                    value = bookTitleBangla,
                                    onValueChange = { bookTitleBangla = it },
                                    label = { Text("১. বইয়ের বাংলা নাম *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_title_bangla_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 2. বইয়ের আরবি / মূল নাম
                                OutlinedTextField(
                                    value = bookTitleArabic,
                                    onValueChange = { bookTitleArabic = it },
                                    label = { Text("২. বইয়ের আরবি / মূল নাম (থাকলে)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_title_arabic_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 3. লেখকের নাম
                                OutlinedTextField(
                                    value = authorName,
                                    onValueChange = { authorName = it },
                                    label = { Text("৩. লেখকের নাম *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_author_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 4. অনুবাদক / সম্পাদকের নাম
                                OutlinedTextField(
                                    value = translatorName,
                                    onValueChange = { translatorName = it },
                                    label = { Text("৪. অনুবাদক / সম্পাদকের নাম (থাকলে)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_translator_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 5. প্রকাশনীর নাম
                                OutlinedTextField(
                                    value = publisherName,
                                    onValueChange = { publisherName = it },
                                    label = { Text("৫. প্রকাশনীর নাম *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_publisher_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // 6. বইয়ের বিভাগ
                                Text(
                                    text = "৬. বইয়ের বিভাগ নির্বাচন করুন: (${selectedCategory.titleBangla})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PortalDarkGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IslamicBookCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = cat },
                                            label = { Text(cat.titleBangla) },
                                            modifier = Modifier.testTag("submission_category_chip_${cat.id}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // 7. ভাষা
                                Text(
                                    text = "৭. বইয়ের ভাষা:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PortalDarkGreen
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("বাংলা", "আরবি", "আরবি-বাংলা", "উর্দু-বাংলা").forEach { lang ->
                                        FilterChip(
                                            selected = selectedLanguage == lang,
                                            onClick = { selectedLanguage = lang },
                                            label = { Text(lang) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // 8. সংস্করণ & 9. প্রকাশের বছর
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editionText,
                                        onValueChange = { editionText = it },
                                        label = { Text("৮. সংস্করণ") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("submission_edition_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = publicationYear,
                                        onValueChange = { publicationYear = it },
                                        label = { Text("৯. প্রকাশের বছর") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("submission_pub_year_input"),
                                        singleLine = true
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isYearVerified,
                                        onCheckedChange = { isYearVerified = it },
                                        modifier = Modifier.testTag("submission_year_verified_checkbox")
                                    )
                                    Text(
                                        text = "প্রকাশের বছরটি মুদ্রিত বই থেকে নিশ্চিতভাবে যাচাইকৃত",
                                        fontSize = 12.sp
                                    )
                                }

                                // 10. মোট পৃষ্ঠা সংখ্যা & 11. ISBN
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pageCountText,
                                        onValueChange = { pageCountText = it },
                                        label = { Text("১০. মোট পৃষ্ঠা সংখ্যা *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("submission_page_count_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = isbnText,
                                        onValueChange = { isbnText = it },
                                        label = { Text("১১. ISBN (থাকলে)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("submission_isbn_input"),
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // 12. বইয়ের সংক্ষিপ্ত পরিচিতি
                                OutlinedTextField(
                                    value = shortIntroText,
                                    onValueChange = { shortIntroText = it },
                                    label = { Text("১২. বইয়ের সংক্ষিপ্ত পরিচিতি (কমপক্ষে ১০ অক্ষর) *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_intro_input"),
                                    minLines = 3
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 13. সূচিপত্র বা অধ্যায়সমূহ
                                OutlinedTextField(
                                    value = tocText,
                                    onValueChange = { tocText = it },
                                    label = { Text("১৩. সূচিপত্র বা অধ্যায়সমূহ (প্রতি লাইনে একটি অধ্যায়)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_toc_input"),
                                    minLines = 3
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // 14. বইয়ের কভার ছবি / Thumbnail (600 × 800 pixels, 3:4 Portrait Ratio)
                                BookThumbnail3x4PreviewSection(
                                    coverImageUriString = coverImageUriString,
                                    onSelectOrChangeCover = {
                                        coverPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRemoveCover = {
                                        bookManager.clearSelectedCoverImage()
                                        coverImageUriString = ""
                                        statusBannerMessage = "কভার থাম্বনেইল মুছে ফেলা হয়েছে।"
                                        isStatusError = false
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // 15. সম্পূর্ণ বইয়ের PDF ফাইল আপলোড ও তাৎক্ষণিক ফর্ম প্রিভিউ
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PortalMintBg),
                                    border = BorderStroke(1.dp, PortalDeepGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "১৫. সম্পূর্ণ বইয়ের PDF ফাইল নির্বাচন ও তাৎক্ষণিক প্রিভিউ (সর্বোচ্চ ৫০ মেগাবাইট) *",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PortalDarkGreen
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                pdfDocumentLauncher.launch(arrayOf("application/pdf"))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("submission_pdf_picker_btn")
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (uploadProgress.state == IslamicBookUploadState.COMPLETED)
                                                    "অন্য PDF নির্বাচন করুন (${uploadProgress.fileName})"
                                                else
                                                    "ফোন থেকে PDF নির্বাচন করুন",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (uploadProgress.state != IslamicBookUploadState.IDLE) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { uploadProgress.progressPercent / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .testTag("pdf_upload_progress_bar"),
                                                color = PortalDeepGreen
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "ফাইল: ${uploadProgress.fileName} • পৃষ্ঠা: ${uploadProgress.detectedPageCount} • অবস্থা: ${uploadProgress.state} (${uploadProgress.progressPercent}%)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PortalDarkGreen
                                            )
                                            if (uploadProgress.sha256Hash.isNotBlank()) {
                                                Text(
                                                    text = "SHA-256: ${uploadProgress.sha256Hash.take(24)}...",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.testTag("pdf_sha256_label")
                                                )
                                            }
                                            if (uploadProgress.errorMessageBangla != null) {
                                                Text(
                                                    text = uploadProgress.errorMessageBangla!!,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFB91C1C),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.testTag("pdf_upload_error_text")
                                                )
                                            }

                                            // Pause / Resume / Cancel controls for upload
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { bookManager.pauseUpload() },
                                                    modifier = Modifier.testTag("upload_pause_btn")
                                                ) {
                                                    Text("Pause", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { bookManager.resumeUpload() },
                                                    modifier = Modifier.testTag("upload_resume_btn")
                                                ) {
                                                    Text("Resume", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { bookManager.cancelUpload() },
                                                    modifier = Modifier.testTag("upload_cancel_btn")
                                                ) {
                                                    Text("Cancel", fontSize = 11.sp)
                                                }
                                            }

                                            // তাৎক্ষণিক ইন-ফর্ম PDF Preview (ফাইল নির্বাচন করার সঙ্গে সঙ্গে ফর্মের মধ্যেই প্রিভিউ)
                                            if (uploadProgress.stagedLocalFilePath.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                InlineSelectedPdfPreviewSection(
                                                    uploadProgress = uploadProgress
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                // ৭. কপিরাইট ও অনুমতি যাচাই (অত্যন্ত গুরুত্বপূর্ণ)
                                Text(
                                    text = "৭. কপিরাইট ও প্রকাশের বৈধ অনুমতি যাচাই (বাধ্যতামূলক)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PortalDarkGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = copyrightOwnerName,
                                    onValueChange = { copyrightOwnerName = it },
                                    label = { Text("১. কপিরাইট মালিক বা স্বত্বাধিকারীর নাম *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_copyright_owner_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "২. অনুমতির ধরন নির্বাচন করুন:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PortalDarkGreen
                                )
                                listOf(
                                    CopyrightPermissionType.SELF_COPYRIGHT,
                                    CopyrightPermissionType.WRITTEN_PERMISSION,
                                    CopyrightPermissionType.OPEN_LICENSE
                                ).forEach { permType ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { copyrightPermissionType = permType }
                                            .padding(vertical = 4.dp)
                                            .testTag("copyright_type_chip_${permType.id}")
                                    ) {
                                        RadioButton(
                                            selected = copyrightPermissionType == permType,
                                            onClick = { copyrightPermissionType = permType }
                                        )
                                        Column {
                                            Text(permType.labelBangla, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(permType.detailBangla, fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = permissionProofText,
                                    onValueChange = { permissionProofText = it },
                                    label = { Text("৩. অনুমতিপত্রের রেফারেন্স বা প্রমাণপত্রের বিবরণ (প্রযোজ্য হলে)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submission_permission_proof_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // ৪. বাধ্যতামূলক অঙ্গীকারনামা চেকবক্স
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PortalGoldLight),
                                    border = BorderStroke(1.dp, PortalGold)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isTruthConfirmed = !isTruthConfirmed }
                                            .padding(10.dp)
                                    ) {
                                        Checkbox(
                                            checked = isTruthConfirmed,
                                            onCheckedChange = { isTruthConfirmed = it },
                                            modifier = Modifier.testTag("copyright_truth_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "আমি নিশ্চিত করছি যে এই বই প্রকাশের বৈধ অধিকার বা লিখিত অনুমতি আমার রয়েছে এবং প্রদত্ত সকল তথ্য সত্য।",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PortalDarkGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val submission = IslamicBookSubmission(
                                            contributorId = currentUserId.ifBlank { "contributor_default_uid" },
                                            contributorName = profileFullName.ifBlank { currentUserName.ifBlank { "লেখক" } },
                                            contributorRole = profileRole,
                                            contributorPhone = profileMobile.ifBlank { "01700000000" },
                                            contributorEmail = profileEmail,
                                            bookTitleBangla = bookTitleBangla,
                                            bookTitleArabicOrOriginal = bookTitleArabic,
                                            authorName = authorName,
                                            translatorOrEditorName = translatorName,
                                            publisherName = publisherName,
                                            category = selectedCategory,
                                            language = selectedLanguage,
                                            editionText = editionText,
                                            publicationYear = publicationYear,
                                            isPublicationYearVerified = isYearVerified,
                                            pageCount = pageCountText.toIntOrNull()?.coerceAtLeast(1) ?: 100,
                                            isbn = isbnText,
                                            shortIntroduction = shortIntroText,
                                            tableOfContentsText = tocText,
                                            coverImageUrl = coverImageUriString,
                                            pdfStorageUrl = uploadProgress.storageDownloadUrl,
                                            pdfFileName = uploadProgress.fileName,
                                            pdfFileSizeBytes = uploadProgress.fileSizeBytes,
                                            pdfSha256 = uploadProgress.sha256Hash,
                                            copyrightOwnerName = copyrightOwnerName,
                                            copyrightPermissionType = copyrightPermissionType,
                                            permissionProofUrl = permissionProofText,
                                            isTruthConfirmed = isTruthConfirmed
                                        )
                                        val result = bookManager.submitNewBookApplication(submission)
                                        result.onSuccess { saved ->
                                            lastCreatedSubmissionId = saved.submissionId
                                            statusBannerMessage = "আলহামদুলিল্লাহ! আবেদনটি জমা হয়েছে। সাবমিশন আইডি: ${saved.submissionId} • স্ট্যাটাস: Submitted (যাচাইয়ের অপেক্ষায়)।"
                                            isStatusError = false
                                            activeTab = SelfPublishingPortalTab.MY_DASHBOARD
                                        }.onFailure { err ->
                                            statusBannerMessage = err.message
                                            isStatusError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_book_application_btn")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("বই প্রকাশের আবেদন জমা দিন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorDashboardContent(
    profile: BookContributorProfile?,
    mySubmissions: List<IslamicBookSubmission>,
    lastCreatedSubmissionId: String?,
    onOpenNewSubmission: () -> Unit,
    onOpenProfile: () -> Unit,
    onReplyAdditionalInfo: (submissionId: String, replyText: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("contributor_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PortalGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.fullName?.ifBlank { "সম্মানিত লেখক / প্রকাশক" } ?: "সম্মানিত লেখক / প্রকাশক",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PortalDarkGreen
                            )
                            Text(
                                text = "পরিচয়: ${profile?.contributorRole?.labelBangla ?: "লেখক"} • প্রতিষ্ঠান: ${profile?.organizationOrPublisherName?.ifBlank { "স্বতন্ত্র" } ?: "স্বতন্ত্র"}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("edit_contributor_profile_btn")
                        ) {
                            Text("প্রোফাইল সম্পাদনা", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenNewSubmission,
                        colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dashboard_new_book_submission_btn")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন বই জমা দিন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (lastCreatedSubmissionId != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PortalGoldLight),
                    border = BorderStroke(1.dp, PortalGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submission_id_confirmation_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "সর্বশেষ জমাকৃত সাবমিশন আইডি: $lastCreatedSubmissionId",
                            fontWeight = FontWeight.Bold,
                            color = PortalDarkGreen,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "অ্যাডমিন অনুমোদনের আগ পর্যন্ত বইটি মূল লাইব্রেরিতে প্রদর্শিত হবে না।",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "আমার জমা দেওয়া বইয়ের তালিকা (${mySubmissions.size}টি)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PortalDarkGreen
            )
        }

        if (mySubmissions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = PortalDeepGreen,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "আপনি এখনো কোনো বই প্রকাশের আবেদন জমা দেননি।",
                            fontWeight = FontWeight.SemiBold,
                            color = PortalDarkGreen
                        )
                        Text(
                            text = "উপরের “নতুন বই জমা দিন” বাটনে চাপ দিয়ে আপনার বই জমা দিন।",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(mySubmissions, key = { it.submissionId }) { sub ->
                var replyInput by remember(sub.submissionId) { mutableStateOf(sub.contributorReplyNote) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PortalDeepGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contributor_submission_card_${sub.submissionId}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ID: ${sub.submissionId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PortalDeepGreen
                            )
                            Surface(
                                color = when (sub.status) {
                                    BookSubmissionStatus.PUBLISHED, BookSubmissionStatus.APPROVED -> Color(0xFFDCFCE7)
                                    BookSubmissionStatus.REJECTED -> Color(0xFFFEE2E2)
                                    BookSubmissionStatus.MORE_INFO_REQUIRED -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFE0F2FE)
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${sub.status.labelBangla} (${sub.status.labelEnglish})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PortalDarkGreen,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("submission_status_badge_${sub.submissionId}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sub.bookTitleBangla,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PortalDarkGreen
                        )
                        Text(
                            text = "লেখক: ${sub.authorName} • প্রকাশনী: ${sub.publisherName} • বিভাগ: ${sub.category.titleBangla}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub.status.descriptionBangla,
                            fontSize = 12.sp,
                            color = PortalDeepGreen
                        )

                        if (sub.adminComment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = PortalGoldLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PortalGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "অ্যাডমিনের মন্তব্য / নির্দেশনা:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PortalDarkGreen
                                    )
                                    Text(
                                        text = sub.adminComment,
                                        fontSize = 12.5.sp,
                                        color = Color.Black,
                                        modifier = Modifier.testTag("submission_admin_comment_${sub.submissionId}")
                                    )
                                }
                            }
                        }

                        if (sub.status == BookSubmissionStatus.MORE_INFO_REQUIRED) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = replyInput,
                                onValueChange = { replyInput = it },
                                label = { Text("অতিরিক্ত তথ্য বা সংশোধিত বিবরণ লিখুন") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contributor_reply_input_${sub.submissionId}")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onReplyAdditionalInfo(sub.submissionId, replyInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contributor_reply_submit_btn_${sub.submissionId}")
                            ) {
                                Text("অতিরিক্ত তথ্য জমা দিন")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ৯. অ্যাডমিন অনুমোদন ড্যাশবোর্ড (Admin Book Approval Dashboard)
 */
@Composable
fun AdminBookApprovalDashboardScreen(
    bookManager: IslamicBookManager,
    adminId: String,
    adminEmail: String?,
    adminPhone: String?,
    onBackToLibrary: () -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf<BookSubmissionStatus?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    val allSubmissions by bookManager.allSubmissions.collectAsState()
    val auditLogs by bookManager.auditLogs.collectAsState()
    val dynamicCatalog by bookManager.dynamicCatalogBooks.collectAsState()

    val filteredSubmissions = remember(allSubmissions, selectedStatusFilter) {
        if (selectedStatusFilter == null) allSubmissions
        else allSubmissions.filter { it.status == selectedStatusFilter }
    }

    BackHandler {
        onBackToLibrary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalMintBg)
            .testTag("admin_book_approval_dashboard")
    ) {
        // Admin Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PortalDarkGreen, PortalDeepGreen)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToLibrary,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_dashboard_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "অ্যাডমিন বুক অনুমোদন ড্যাশবোর্ড",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "নতুন আবেদন যাচাই, কপিরাইট পরীক্ষা, অনুমোদন, প্রত্যাখ্যান ও অডিট লগ",
                            color = Color(0xFFD9F2E6),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = {
                            Text(
                                "সকল আবেদন (${allSubmissions.size})",
                                color = if (selectedStatusFilter == null) PortalDarkGreen else Color.White
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PortalGoldLight,
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("admin_filter_tab_ALL")
                    )
                    BookSubmissionStatus.entries.forEach { st ->
                        val count = allSubmissions.count { it.status == st }
                        FilterChip(
                            selected = selectedStatusFilter == st,
                            onClick = { selectedStatusFilter = st },
                            label = {
                                Text(
                                    "${st.labelBangla} ($count)",
                                    color = if (selectedStatusFilter == st) PortalDarkGreen else Color.White
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PortalGoldLight,
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("admin_filter_tab_${st.id}")
                        )
                    }
                }
            }
        }

    val context = LocalContext.current
    val uploadProgress by bookManager.uploadProgress.collectAsState()
    var directBookTitle by remember { mutableStateOf("") }
    var directCategory by remember { mutableStateOf(IslamicBookCategory.AQIDAH_IMAN) }
    var directCoverImageUrl by remember { mutableStateOf("") }

    val adminCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            directCoverImageUrl = bookManager.prepareCoverImageFromUri(uri)
            feedbackBanner = "কিতাবের কভার ছবি নির্বাচিত হয়েছে।"
        }
    }

    val adminPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "admin_uploaded_book.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    bookManager.validateAndPreparePdfForUpload(
                        fileName = displayName,
                        pdfBytes = bytes,
                        contributorId = adminId.ifBlank { "admin_uploader" }
                    ).onSuccess {
                        feedbackBanner = "পিডিএফ ফাইল প্রস্তুত হয়েছে (${it.fileName})।"
                    }.onFailure { err ->
                        feedbackBanner = err.message
                    }
                }
            } catch (e: Exception) {
                feedbackBanner = "পিডিএফ পড়তে ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

        feedbackBanner?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("admin_feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = PortalDeepGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(msg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PortalDarkGreen)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Admin Direct Book Upload Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PortalDeepGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_direct_upload_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "অ্যাডমিন সরাসরি কিতাব আপলোড ও প্রকাশ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PortalDarkGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = directBookTitle,
                            onValueChange = { directBookTitle = it },
                            label = { Text("কিতাবের নাম (বাংলা)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_direct_book_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IslamicBookCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = directCategory == cat,
                                    onClick = { directCategory = cat },
                                    label = { Text(cat.titleBangla, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BookThumbnail3x4PreviewSection(
                            coverImageUriString = directCoverImageUrl,
                            onSelectOrChangeCover = {
                                adminCoverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveCover = {
                                directCoverImageUrl = ""
                                feedbackBanner = "কভার থাম্বনেইল মুছে ফেলা হয়েছে।"
                            },
                            pickerButtonTag = "admin_pick_cover_btn",
                            previewBoxTag = "admin_thumbnail_preview_box",
                            changeButtonTag = "admin_thumbnail_change_btn",
                            fullPreviewButtonTag = "admin_thumbnail_full_preview_btn",
                            removeButtonTag = "admin_thumbnail_remove_btn"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                adminPdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("admin_pick_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (uploadProgress.state == IslamicBookUploadState.COMPLETED)
                                    "PDF যুক্ত হয়েছে (${uploadProgress.fileName} • ${uploadProgress.detectedPageCount} পৃষ্ঠা)"
                                else
                                    "ফোন থেকে PDF ফাইল নির্বাচন করুন",
                                fontSize = 12.sp
                            )
                        }
                        if (uploadProgress.stagedLocalFilePath.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InlineSelectedPdfPreviewSection(
                                uploadProgress = uploadProgress,
                                previewBoxTag = "admin_pdf_preview_box"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                bookManager.adminDirectPublishBook(
                                    adminId = adminId,
                                    adminEmail = adminEmail,
                                    adminPhone = adminPhone,
                                    bookTitleBangla = directBookTitle,
                                    category = directCategory,
                                    coverImageUrl = directCoverImageUrl,
                                    pageCount = uploadProgress.detectedPageCount.coerceAtLeast(1)
                                ).onSuccess { published ->
                                    feedbackBanner = "আলহামদুলিল্লাহ! '${published.titleBangla}' সরাসরি লাইব্রেরিতে প্রকাশিত হয়েছে।"
                                    directBookTitle = ""
                                    directCoverImageUrl = ""
                                }.onFailure { err ->
                                    feedbackBanner = err.message
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_direct_publish_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সরাসরি লাইব্রেরিতে প্রকাশ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (filteredSubmissions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "এই বিভাগে বর্তমানে কোনো বইয়ের আবেদন নেই।",
                            modifier = Modifier.padding(20.dp),
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                items(filteredSubmissions, key = { it.submissionId }) { sub ->
                    var adminNoteInput by remember(sub.submissionId) {
                        mutableStateOf(sub.adminComment.ifBlank { "তথ্য ও কপিরাইট যাচাই সম্পন্ন।" })
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PortalGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_submission_card_${sub.submissionId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "সাবমিশন: ${sub.submissionId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PortalDeepGreen
                                )
                                Text(
                                    text = sub.status.labelBangla,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PortalDarkGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sub.bookTitleBangla,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PortalDarkGreen
                            )
                            Text(
                                text = "লেখক: ${sub.authorName} • প্রকাশনী: ${sub.publisherName} • বিভাগ: ${sub.category.titleBangla}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "আবেদনকারী: ${sub.contributorName} (${sub.contributorRole.labelBangla}) • ফোন: ${sub.contributorPhone}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "কপিরাইট মালিক: ${sub.copyrightOwnerName} • ধরন: ${sub.copyrightPermissionType.labelBangla}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PortalDeepGreen
                            )
                            Text(
                                text = "SHA-256: ${sub.pdfSha256.take(20)}... • পৃষ্ঠা: ${sub.pageCount}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            if (sub.contributorReplyNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "লেখকের অতিরিক্ত তথ্য: ${sub.contributorReplyNote}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PortalDeepGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminNoteInput,
                                onValueChange = { adminNoteInput = it },
                                label = { Text("অ্যাডমিন মন্তব্য / সংশোধনী নির্দেশনা") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_comment_input_${sub.submissionId}"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        bookManager.adminReviewSubmission(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            submissionId = sub.submissionId,
                                            newStatus = BookSubmissionStatus.UNDER_REVIEW,
                                            adminCommentBangla = adminNoteInput
                                        ).onSuccess {
                                            feedbackBanner = "'${sub.bookTitleBangla}' যাচাইাধীন (Under Review) তালিকায় রাখা হয়েছে।"
                                        }
                                    },
                                    modifier = Modifier.testTag("admin_action_under_review_${sub.submissionId}")
                                ) {
                                    Text("যাচাইাধীন", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        bookManager.adminReviewSubmission(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            submissionId = sub.submissionId,
                                            newStatus = BookSubmissionStatus.MORE_INFO_REQUIRED,
                                            adminCommentBangla = adminNoteInput
                                        ).onSuccess {
                                            feedbackBanner = "'${sub.bookTitleBangla}'-এর জন্য অতিরিক্ত তথ্য চাওয়া হয়েছে।"
                                        }
                                    },
                                    modifier = Modifier.testTag("admin_action_more_info_${sub.submissionId}")
                                ) {
                                    Text("অতিরিক্ত তথ্য চান", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        bookManager.adminReviewSubmission(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            submissionId = sub.submissionId,
                                            newStatus = BookSubmissionStatus.PUBLISHED,
                                            adminCommentBangla = adminNoteInput
                                        ).onSuccess {
                                            feedbackBanner = "আলহামদুলিল্লাহ! '${sub.bookTitleBangla}' অনুমোদিত ও মূল লাইব্রেরিতে প্রকাশিত হয়েছে।"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PortalDeepGreen),
                                    modifier = Modifier.testTag("admin_action_publish_${sub.submissionId}")
                                ) {
                                    Text("অনুমোদন ও প্রকাশ করুন", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        bookManager.adminReviewSubmission(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            submissionId = sub.submissionId,
                                            newStatus = BookSubmissionStatus.REJECTED,
                                            adminCommentBangla = adminNoteInput
                                        ).onSuccess {
                                            feedbackBanner = "'${sub.bookTitleBangla}' আবেদনটি প্রত্যাখ্যান করা হয়েছে।"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    modifier = Modifier.testTag("admin_action_reject_${sub.submissionId}")
                                ) {
                                    Text("প্রত্যাখ্যান করুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Published Books Management (Suspend / Remove / Restore)
            if (dynamicCatalog.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "প্রকাশিত বই ব্যবস্থাপনা (স্থগিত / অপসারণ / পুনরুদ্ধার)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PortalDarkGreen
                    )
                }
                items(dynamicCatalog, key = { "cat_${it.bookId}" }) { book ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_catalog_book_${book.bookId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.titleBangla, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "অবস্থা: ${book.approvalStatus.labelBangla}",
                                    fontSize = 12.sp,
                                    color = PortalDeepGreen
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        bookManager.adminUpdatePublishedBookState(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            bookId = book.bookId,
                                            newState = BookPublicationState.SUSPENDED,
                                            reasonBangla = "অ্যাডমিন কর্তৃক সাময়িক স্থগিত"
                                        )
                                    },
                                    modifier = Modifier.testTag("admin_suspend_book_${book.bookId}")
                                ) {
                                    Text("স্থগিত", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        bookManager.adminUpdatePublishedBookState(
                                            adminId = adminId,
                                            adminEmail = adminEmail,
                                            adminPhone = adminPhone,
                                            bookId = book.bookId,
                                            newState = BookPublicationState.REMOVED,
                                            reasonBangla = "অ্যাডমিন কর্তৃক অপসারণ"
                                        )
                                    },
                                    modifier = Modifier.testTag("admin_remove_book_${book.bookId}")
                                ) {
                                    Text("অপসারণ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Audit Logs Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "নিরাপত্তা ও প্রকাশনা অডিট লগ (${auditLogs.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PortalDarkGreen,
                    modifier = Modifier.testTag("admin_audit_logs_section")
                )
            }
            items(auditLogs.take(15), key = { it.logId }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${log.actionType} • ${log.actorName} (${log.actorRole})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PortalDeepGreen
                        )
                        Text(log.detailsBangla, fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

/**
 * ১. ইউজারের নির্বাচিত Book Thumbnail / Cover Image-এর 600 × 800 pixels (3:4 Portrait Ratio) প্রিভিউ বক্স,
 * যেখানে ছবিটি টেনে বিকৃত করা বা কেটে ফেলা ছাড়াই সম্পূর্ণ দেখা যায় এবং পরিবর্তন, বড় প্রিভিউ ও মুছে ফেলার সুবিধা থাকে।
 */
@Composable
fun BookThumbnail3x4PreviewSection(
    coverImageUriString: String,
    onSelectOrChangeCover: () -> Unit,
    onRemoveCover: () -> Unit,
    pickerButtonTag: String = "submission_cover_picker_btn",
    previewBoxTag: String = "submission_thumbnail_preview_box",
    changeButtonTag: String = "submission_thumbnail_change_btn",
    fullPreviewButtonTag: String = "submission_thumbnail_full_preview_btn",
    removeButtonTag: String = "submission_thumbnail_remove_btn"
) {
    var isExpandedPreview by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PortalMintBg),
        border = BorderStroke(1.dp, PortalDeepGreen.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "১৪. বইয়ের থাম্বনেইল / কভার ছবি (600 × 800 px • 3:4 অনুপাত)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PortalDarkGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (coverImageUriString.isBlank()) {
                OutlinedButton(
                    onClick = onSelectOrChangeCover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(pickerButtonTag)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("গ্যালারি থেকে কভার থাম্বনেইল নির্বাচন করুন (3:4)", fontSize = 12.5.sp)
                }
            } else {
                val cleanUrl = coverImageUriString.trim()
                val decodedBitmap = remember(cleanUrl) {
                    if (cleanUrl.startsWith("data:image", ignoreCase = true) && cleanUrl.contains("base64,")) {
                        try {
                            val base64Part = cleanUrl.substringAfter("base64,")
                            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 3:4 Portrait Preview Box (600 x 800 ratio -> 96.dp x 128.dp or expanded 165.dp x 220.dp)
                    val boxWidth = if (isExpandedPreview) 165.dp else 96.dp
                    val boxHeight = if (isExpandedPreview) 220.dp else 128.dp
                    Box(
                        modifier = Modifier
                            .width(boxWidth)
                            .height(boxHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF7FAF8))
                            .border(1.dp, PortalGold, RoundedCornerShape(8.dp))
                            .testTag(previewBoxTag),
                        contentAlignment = Alignment.Center
                    ) {
                        if (decodedBitmap != null) {
                            Image(
                                bitmap = decodedBitmap.asImageBitmap(),
                                contentDescription = "নির্বাচিত কভার থাম্বনেইল প্রিভিউ",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = cleanUrl,
                                contentDescription = "নির্বাচিত কভার থাম্বনেইল প্রিভিউ",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "মাপ: 600 × 800 px (3:4 Portrait)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PortalDeepGreen,
                            modifier = Modifier.testTag("submission_thumbnail_ratio_label")
                        )
                        Text(
                            text = "ছবিটি বিকৃত বা কাটা ছাড়াই সম্পূর্ণ প্রদর্শিত হচ্ছে।",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )

                        // পরিবর্তন (Change)
                        OutlinedButton(
                            onClick = onSelectOrChangeCover,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag(changeButtonTag)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("থাম্বনেইল পরিবর্তন", fontSize = 11.5.sp)
                        }

                        // প্রিভিউ (Preview Toggle)
                        OutlinedButton(
                            onClick = { isExpandedPreview = !isExpandedPreview },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag(fullPreviewButtonTag)
                        ) {
                            Icon(
                                imageVector = if (isExpandedPreview) Icons.Default.ZoomOutMap else Icons.Default.ZoomInMap,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isExpandedPreview) "ছোট প্রিভিউ" else "বড় প্রিভিউ দেখুন",
                                fontSize = 11.5.sp
                            )
                        }

                        // মুছে ফেলুন (Remove)
                        OutlinedButton(
                            onClick = {
                                isExpandedPreview = false
                                onRemoveCover()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag(removeButtonTag)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("থাম্বনেইল মুছুন", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ১. ইউজার PDF নির্বাচন করার সঙ্গে সঙ্গে ফর্মের মধ্যেই তাৎক্ষণিক PDF Preview দেখানোর কম্পোনেন্ট।
 */
@Composable
fun InlineSelectedPdfPreviewSection(
    uploadProgress: IslamicBookUploadProgress,
    previewBoxTag: String = "submission_pdf_preview_box"
) {
    var previewPage by remember(uploadProgress.stagedLocalFilePath) { mutableIntStateOf(1) }
    var totalPages by remember(uploadProgress.stagedLocalFilePath, uploadProgress.detectedPageCount) {
        mutableIntStateOf(uploadProgress.detectedPageCount.coerceAtLeast(1))
    }
    var pageBitmap by remember(uploadProgress.stagedLocalFilePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uploadProgress.stagedLocalFilePath, previewPage) {
        val path = uploadProgress.stagedLocalFilePath
        if (path.isBlank()) {
            pageBitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            var rendered: Bitmap? = null
            try {
                val file = File(path)
                if (file.exists() && file.length() > 100L) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            val count = renderer.pageCount
                            if (count > 0) {
                                totalPages = count
                                val idx = (previewPage - 1).coerceIn(0, count - 1)
                                renderer.openPage(idx).use { page ->
                                    val targetW = (page.width * 2).coerceIn(600, 1200)
                                    val targetH = (page.height * 2).coerceIn(800, 1600)
                                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    rendered = bmp
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            if (rendered == null) {
                totalPages = uploadProgress.detectedPageCount.coerceAtLeast(1)
                val fallbackBmp = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
                fallbackBmp.eraseColor(android.graphics.Color.WHITE)
                rendered = fallbackBmp
            }
            pageBitmap = rendered
        }
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PortalDeepGreen.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(previewBoxTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "নির্বাচিত PDF প্রিভিউ (${uploadProgress.fileName})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PortalDarkGreen,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "পৃষ্ঠা $previewPage / $totalPages",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortalDeepGreen,
                    modifier = Modifier.testTag("submission_pdf_preview_page_indicator")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(0.75.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = pageBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF পৃষ্ঠা প্রিভিউ $previewPage",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("submission_pdf_preview_page_image")
                    )
                } else {
                    Text(
                        text = "পিডিএফ পৃষ্ঠা লোড হচ্ছে...",
                        fontSize = 12.sp,
                        color = PortalDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (previewPage > 1) previewPage -= 1 },
                    enabled = previewPage > 1,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("submission_pdf_preview_prev_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("আগের পৃষ্ঠা", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { if (previewPage < totalPages) previewPage += 1 },
                    enabled = previewPage < totalPages,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("submission_pdf_preview_next_btn")
                ) {
                    Text("পরের পৃষ্ঠা", fontSize = 11.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

