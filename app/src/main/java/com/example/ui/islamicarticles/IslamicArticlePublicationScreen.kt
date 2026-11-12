package com.example.ui.islamicarticles

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.viewmodel.QawmiViewModel

private val MagazineDeepGreen = Color(0xFF0A4A35)
private val MagazineMidGreen = Color(0xFF0E5E46)
private val MagazineSoftGreen = Color(0xFFEAF5EF)
private val MagazineGold = Color(0xFFD4AF37)
private val MagazineGoldDark = Color(0xFF9A6F00)
private val MagazineCreamBg = Color(0xFFF9FBF9)

/**
 * ইসলামিক প্রবন্ধ ও জ্ঞানচর্চা বিভাগ — মূল ফুল-স্ক্রিন কম্পোজেবল
 * - প্রবন্ধের মূল পেজ (নির্বাচিত, সকল প্রবন্ধ, আমার জমা)
 * - আলাদা Full-Screen প্রবন্ধ পড়ার পেজ (Reading Page)
 * - আলাদা Full-Screen প্রবন্ধ জমা দেওয়ার পেজ (“আপনার প্রবন্ধ জমা দিন”)
 * - আলাদা Full-Screen অ্যাডমিন রিভিউ ও প্রকাশনা পেজ (Admin Review Dashboard)
 */
@Composable
fun IslamicArticlePublicationScreen(
    currentUserId: String = "user_author_kks",
    currentUserName: String = "মাওলানা লেখক",
    currentUserBio: String = "আলেম ও গবেষক • বাংলাদেশ",
    currentUserEmail: String = "",
    currentUserPhone: String = "",
    isAdmin: Boolean = false,
    registerBackInterceptor: (((() -> Boolean)?) -> Unit)? = null
) {
    val context = LocalContext.current
    val articleManager = remember(context) { IslamicArticleManager.getInstance(context) }

    val dynamicPublished by articleManager.dynamicPublishedArticles.collectAsState()
    val allSubmissions by articleManager.allSubmissions.collectAsState()
    val bookmarksMap by articleManager.bookmarkedIdsByUser.collectAsState()
    val readerFontSizeSp by articleManager.readerFontSizeSp.collectAsState()

    val resolvedUserId = currentUserId.ifBlank { "user_author_kks" }
    val resolvedUserName = currentUserName.ifBlank { "মাওলানা লেখক" }
    val resolvedUserBio = currentUserBio.ifBlank { "আলেম ও গবেষক • বাংলাদেশ" }

    val isCurrentUserAdmin = remember(currentUserEmail, currentUserPhone, isAdmin) {
        isAdmin || AdminSecurityHelper.isAuthorizedAdminIdentity(currentUserEmail, currentUserPhone)
    }

    var currentSubView by rememberSaveable { mutableStateOf(ArticleSubView.MAGAZINE_HOME) }
    var selectedMainTab by rememberSaveable { mutableStateOf(ArticleMainTab.FEATURED) }
    var selectedCategory by rememberSaveable { mutableStateOf<IslamicArticleCategory?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedArticleIdForReading by rememberSaveable { mutableStateOf<String?>(null) }
    var editingSubmissionId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSubmittedIdBanner by rememberSaveable { mutableStateOf<String?>(null) }

    // Register TopBar Back Button interceptor so single TopBar back arrow returns from sub-screens cleanly
    DisposableEffect(currentSubView, registerBackInterceptor) {
        if (currentSubView != ArticleSubView.MAGAZINE_HOME) {
            registerBackInterceptor?.invoke {
                currentSubView = ArticleSubView.MAGAZINE_HOME
                editingSubmissionId = null
                true
            }
        } else {
            registerBackInterceptor?.invoke(null)
        }
        onDispose {
            registerBackInterceptor?.invoke(null)
        }
    }

    BackHandler(enabled = currentSubView != ArticleSubView.MAGAZINE_HOME) {
        currentSubView = ArticleSubView.MAGAZINE_HOME
        editingSubmissionId = null
    }

    val allPublicArticles = remember(dynamicPublished) {
        articleManager.getPublicPublishedArticles()
    }

    val featuredArticles = remember(allPublicArticles) {
        allPublicArticles.filter { it.isFeatured }
    }

    val newlyPublishedArticles = remember(allPublicArticles) {
        val flagged = allPublicArticles.filter { it.isNewRelease }
        if (flagged.isNotEmpty()) flagged else allPublicArticles.take(4)
    }

    val popularArticles = remember(allPublicArticles) {
        val flagged = allPublicArticles.filter { it.isPopular }
        if (flagged.isNotEmpty()) flagged else allPublicArticles.take(4)
    }

    val filteredPublicArticles = remember(allPublicArticles, selectedCategory, searchQuery) {
        articleManager.searchPublicArticles(searchQuery, selectedCategory)
    }

    val authorSubmissions = remember(allSubmissions, resolvedUserId) {
        articleManager.getSubmissionsForAuthor(resolvedUserId)
    }

    val bookmarkedIds = remember(bookmarksMap, resolvedUserId) {
        articleManager.getBookmarkedArticleIds(resolvedUserId)
    }

    when (currentSubView) {
        ArticleSubView.READING_PAGE -> {
            val activeArticle = remember(selectedArticleIdForReading, allPublicArticles) {
                allPublicArticles.firstOrNull { it.articleId == selectedArticleIdForReading }
                    ?: allPublicArticles.firstOrNull()
            }
            if (activeArticle != null) {
                val isBookmarked = activeArticle.articleId in bookmarkedIds
                IslamicArticleReadingFullScreen(
                    article = activeArticle,
                    isBookmarked = isBookmarked,
                    readerFontSizeSp = readerFontSizeSp,
                    onDecreaseFont = { articleManager.decreaseReaderFontSize() },
                    onIncreaseFont = { articleManager.increaseReaderFontSize() },
                    onToggleBookmark = {
                        articleManager.toggleBookmark(resolvedUserId, activeArticle)
                    },
                    onShareArticle = {
                        val shareText = buildString {
                            append(activeArticle.title)
                            append("\nলেখক: ")
                            append(activeArticle.authorName)
                            append(" (")
                            append(activeArticle.authorBio)
                            append(")\nবিভাগ: ")
                            append(activeArticle.category.titleBangla)
                            append("\n\n")
                            append(activeArticle.summary)
                            append("\n\nকুরআন রেফারেন্স: ")
                            append(activeArticle.quranReferences)
                            append("\nহাদিস রেফারেন্স: ")
                            append(activeArticle.hadithReferences)
                            append("\n— কওমি কর্মসংস্থান বাংলাদেশ (ইসলামিক প্রবন্ধ ও জ্ঞানচর্চা)")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, activeArticle.title)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(sendIntent, "প্রবন্ধটি শেয়ার করুন")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            } else {
                currentSubView = ArticleSubView.MAGAZINE_HOME
            }
        }

        ArticleSubView.SUBMISSION_PAGE -> {
            val editingSub = remember(editingSubmissionId, allSubmissions) {
                allSubmissions.firstOrNull { it.submissionId == editingSubmissionId }
            }
            IslamicArticleSubmissionFullScreen(
                currentUserId = resolvedUserId,
                defaultAuthorName = resolvedUserName,
                defaultAuthorBio = resolvedUserBio,
                editingSubmission = editingSub,
                onCancelAndBack = {
                    editingSubmissionId = null
                    currentSubView = ArticleSubView.MAGAZINE_HOME
                },
                onSubmitSuccess = { saved ->
                    lastSubmittedIdBanner = saved.submissionId
                    editingSubmissionId = null
                    selectedMainTab = ArticleMainTab.MY_SUBMISSIONS
                    currentSubView = ArticleSubView.MAGAZINE_HOME
                },
                articleManager = articleManager
            )
        }

        ArticleSubView.ADMIN_REVIEW_DASHBOARD -> {
            IslamicArticleAdminReviewFullScreen(
                adminId = resolvedUserId,
                adminEmail = currentUserEmail.ifBlank { AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.first() },
                adminPhone = currentUserPhone.ifBlank { AdminSecurityHelper.ADMIN_MOBILE_NUMBER },
                allSubmissions = allSubmissions,
                articleManager = articleManager,
                onBackToMagazineHome = {
                    currentSubView = ArticleSubView.MAGAZINE_HOME
                }
            )
        }

        ArticleSubView.MAGAZINE_HOME -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MagazineCreamBg)
                    .testTag("islamic_article_magazine_home"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ১. ম্যাগাজিন হেডার ও প্রবন্ধ জমা / অ্যাডমিন অ্যাকশন বার
                item {
                    EditorialMagazineHeroBanner(
                        totalPublishedCount = allPublicArticles.size,
                        isCurrentUserAdmin = isCurrentUserAdmin,
                        pendingAdminCount = allSubmissions.count {
                            it.status == ArticleSubmissionStatus.SUBMITTED ||
                                it.status == ArticleSubmissionStatus.UNDER_REVIEW
                        },
                        onOpenSubmitArticlePage = {
                            editingSubmissionId = null
                            currentSubView = ArticleSubView.SUBMISSION_PAGE
                        },
                        onOpenAdminDashboard = {
                            currentSubView = ArticleSubView.ADMIN_REVIEW_DASHBOARD
                        }
                    )
                }

                // ২. প্রবন্ধ অনুসন্ধানের Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotBlank() && selectedMainTab == ArticleMainTab.MY_SUBMISSIONS) {
                                selectedMainTab = ArticleMainTab.ALL_ARTICLES
                            }
                        },
                        placeholder = {
                            Text(
                                text = "প্রবন্ধের শিরোনাম, লেখক, বিষয় বা রেফারেন্স খুঁজুন...",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "অনুসন্ধান",
                                tint = MagazineDeepGreen
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("article_search_clear_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "মুছুন",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagazineDeepGreen,
                            unfocusedBorderColor = Color(0xFFCCE2D6),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("article_search_input")
                    )
                }

                // ৩. প্রধান তিনটি বিভাগ ট্যাব: ১. নির্বাচিত, ২. সকল প্রবন্ধ, ৩. আমার জমা
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArticleMainTab.entries.forEach { tab ->
                            val isSelected = selectedMainTab == tab
                            val tag = when (tab) {
                                ArticleMainTab.FEATURED -> "article_main_tab_featured"
                                ArticleMainTab.ALL_ARTICLES -> "article_main_tab_all"
                                ArticleMainTab.MY_SUBMISSIONS -> "article_main_tab_my_submissions"
                            }
                            Surface(
                                color = if (isSelected) MagazineDeepGreen else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedMainTab = tab }
                                    .testTag(tag)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (tab == ArticleMainTab.MY_SUBMISSIONS && authorSubmissions.isNotEmpty()) {
                                            "${tab.titleBangla} (${authorSubmissions.size})"
                                        } else {
                                            tab.titleBangla
                                        },
                                        color = if (isSelected) Color.White else MagazineDeepGreen,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // ৪. ১২টি নির্ধারিত ক্যাটাগরি ফিল্টার বার (যখন নির্বাচিত বা সকল প্রবন্ধ ট্যাবে থাকে)
                if (selectedMainTab != ArticleMainTab.MY_SUBMISSIONS) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "বিষয়ভিত্তিক প্রবন্ধ (১২টি বিভাগ)",
                                color = MagazineDeepGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = {
                                        Text(
                                            text = "সব বিষয় (${allPublicArticles.size})",
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MagazineDeepGreen,
                                        selectedLabelColor = Color.White,
                                        containerColor = MagazineSoftGreen,
                                        labelColor = MagazineDeepGreen
                                    ),
                                    modifier = Modifier.testTag("article_category_chip_ALL")
                                )

                                IslamicArticleCategory.entries.forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCategory = if (selectedCategory == cat) null else cat
                                        },
                                        label = {
                                            Text(
                                                text = cat.titleBangla,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MagazineDeepGreen,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.White,
                                            labelColor = MagazineDeepGreen
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = Color(0xFFCCE2D6)
                                        ),
                                        modifier = Modifier.testTag("article_category_chip_${cat.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // ৫. ট্যাব অনুযায়ী কনটেন্ট প্রদর্শন
                when (selectedMainTab) {
                    ArticleMainTab.MY_SUBMISSIONS -> {
                        item {
                            AuthorSubmissionsDashboardSection(
                                currentUserId = currentUserId,
                                authorSubmissions = authorSubmissions,
                                lastSubmittedIdBanner = lastSubmittedIdBanner,
                                onOpenNewSubmissionPage = {
                                    editingSubmissionId = null
                                    currentSubView = ArticleSubView.SUBMISSION_PAGE
                                },
                                onReviseSubmission = { sub ->
                                    editingSubmissionId = sub.submissionId
                                    currentSubView = ArticleSubView.SUBMISSION_PAGE
                                },
                                onOpenPublishedArticleById = { pubId ->
                                    selectedArticleIdForReading = pubId
                                    currentSubView = ArticleSubView.READING_PAGE
                                }
                            )
                        }
                    }

                    ArticleMainTab.ALL_ARTICLES -> {
                        item {
                            SectionHeaderRow(
                                title = if (selectedCategory != null) {
                                    "বিষয়ভিত্তিক প্রবন্ধ: ${selectedCategory!!.titleBangla}"
                                } else {
                                    "সকল প্রকাশিত প্রবন্ধ (${filteredPublicArticles.size})"
                                },
                                subtitle = "সম্পাদনা পরিষদ কর্তৃক যাচাইকৃত ও প্রকাশিত প্রবন্ধসমূহ",
                                testTag = "category_wise_articles_section"
                            )
                        }
                        if (filteredPublicArticles.isEmpty()) {
                            item {
                                EmptyArticlesStateCard(query = searchQuery)
                            }
                        } else {
                            items(filteredPublicArticles, key = { it.articleId }) { article ->
                                EditorialArticleMagazineCard(
                                    article = article,
                                    isBookmarked = article.articleId in bookmarkedIds,
                                    onOpenArticle = {
                                        selectedArticleIdForReading = article.articleId
                                        currentSubView = ArticleSubView.READING_PAGE
                                    },
                                    onToggleBookmark = {
                                        articleManager.toggleBookmark(currentUserId, article)
                                    }
                                )
                            }
                        }
                    }

                    ArticleMainTab.FEATURED -> {
                        // যদি সার্চ বা ক্যাটাগরি ফিল্টার সক্রিয় থাকে, তবে সরাসরি ফিল্টারকৃত ফলাফল দেখাবে
                        if (searchQuery.isNotBlank() || selectedCategory != null) {
                            item {
                                SectionHeaderRow(
                                    title = selectedCategory?.let { "বিষয়ভিত্তিক প্রবন্ধ: ${it.titleBangla}" }
                                        ?: "অনুসন্ধানের ফলাফল (${filteredPublicArticles.size})",
                                    subtitle = "কুরআন ও সুন্নাহর দালিলিক প্রবন্ধ",
                                    testTag = "category_wise_articles_section"
                                )
                            }
                            if (filteredPublicArticles.isEmpty()) {
                                item {
                                    EmptyArticlesStateCard(query = searchQuery)
                                }
                            } else {
                                items(filteredPublicArticles, key = { it.articleId }) { article ->
                                    EditorialArticleMagazineCard(
                                        article = article,
                                        isBookmarked = article.articleId in bookmarkedIds,
                                        onOpenArticle = {
                                            selectedArticleIdForReading = article.articleId
                                            currentSubView = ArticleSubView.READING_PAGE
                                        },
                                        onToggleBookmark = {
                                            articleManager.toggleBookmark(currentUserId, article)
                                        }
                                    )
                                }
                            }
                        } else {
                            // ১. নির্বাচিত প্রবন্ধ (Featured Articles)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("featured_articles_section"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SectionHeaderRow(
                                        title = "নির্বাচিত প্রবন্ধ",
                                        subtitle = "সম্পাদনা পর্ষদের বিশেষ নির্বাচিত গবেষণাধর্মী রচনা",
                                        testTag = "header_featured_articles"
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(end = 4.dp)
                                    ) {
                                        items(featuredArticles, key = { "feat_${it.articleId}" }) { article ->
                                            FeaturedSpotlightArticleCard(
                                                article = article,
                                                isBookmarked = article.articleId in bookmarkedIds,
                                                onOpenArticle = {
                                                    selectedArticleIdForReading = article.articleId
                                                    currentSubView = ArticleSubView.READING_PAGE
                                                },
                                                onToggleBookmark = {
                                                    articleManager.toggleBookmark(currentUserId, article)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // ২. নতুন প্রকাশিত প্রবন্ধ (Newly Published Articles)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("newly_published_articles_section"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SectionHeaderRow(
                                        title = "নতুন প্রকাশিত প্রবন্ধ",
                                        subtitle = "সাম্প্রতিক যাচাইকৃত ও প্রকাশিত প্রবন্ধসমূহ",
                                        testTag = "header_newly_published_articles"
                                    )
                                    newlyPublishedArticles.forEach { article ->
                                        EditorialArticleMagazineCard(
                                            article = article,
                                            isBookmarked = article.articleId in bookmarkedIds,
                                            onOpenArticle = {
                                                selectedArticleIdForReading = article.articleId
                                                currentSubView = ArticleSubView.READING_PAGE
                                            },
                                            onToggleBookmark = {
                                                articleManager.toggleBookmark(currentUserId, article)
                                            }
                                        )
                                    }
                                }
                            }

                            // ৩. জনপ্রিয় প্রবন্ধ (Popular Articles)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("popular_articles_section"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SectionHeaderRow(
                                        title = "জনপ্রিয় প্রবন্ধ",
                                        subtitle = "পাঠক ও তালিবে ইলমদের সর্বাধিক পঠিত প্রবন্ধ",
                                        testTag = "header_popular_articles"
                                    )
                                    popularArticles.forEach { article ->
                                        EditorialArticleMagazineCard(
                                            article = article,
                                            isBookmarked = article.articleId in bookmarkedIds,
                                            onOpenArticle = {
                                                selectedArticleIdForReading = article.articleId
                                                currentSubView = ArticleSubView.READING_PAGE
                                            },
                                            onToggleBookmark = {
                                                articleManager.toggleBookmark(currentUserId, article)
                                            }
                                        )
                                    }
                                }
                            }

                            // ৪. বিষয়ভিত্তিক প্রবন্ধ (Category-wise Articles)
                            item {
                                SectionHeaderRow(
                                    title = "বিষয়ভিত্তিক প্রবন্ধ",
                                    subtitle = "১২টি বিভাগের সকল যাচাইকৃত প্রবন্ধ সংকলন",
                                    testTag = "category_wise_articles_section"
                                )
                            }
                            items(allPublicArticles, key = { "cat_all_${it.articleId}" }) { article ->
                                EditorialArticleMagazineCard(
                                    article = article,
                                    isBookmarked = article.articleId in bookmarkedIds,
                                    onOpenArticle = {
                                        selectedArticleIdForReading = article.articleId
                                        currentSubView = ArticleSubView.READING_PAGE
                                    },
                                    onToggleBookmark = {
                                        articleManager.toggleBookmark(currentUserId, article)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorialMagazineHeroBanner(
    totalPublishedCount: Int,
    isCurrentUserAdmin: Boolean,
    pendingAdminCount: Int,
    onOpenSubmitArticlePage: () -> Unit,
    onOpenAdminDashboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MagazineDeepGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(MagazineDeepGreen, MagazineMidGreen)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Surface(
                        color = MagazineGold.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "ইসলামিক প্রবন্ধ",
                            tint = MagazineGold,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ইসলামিক প্রবন্ধ ও জ্ঞানচর্চা",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "দালিলিক গবেষণা, কুরআন-হাদিস ও সমকালীন ইলমি সাময়িকী",
                            color = Color(0xFFDDF3EC),
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = MagazineGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$totalPublishedCount+ প্রবন্ধ",
                        color = MagazineGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenSubmitArticlePage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MagazineGold,
                        contentColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("open_submit_article_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "প্রবন্ধ জমা দিন",
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আপনার প্রবন্ধ জমা দিন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                OutlinedButton(
                    onClick = onOpenAdminDashboard,
                    border = BorderStroke(1.dp, MagazineGold.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_article_admin_dashboard_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "অ্যাডমিন প্যানেল",
                        tint = MagazineGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (pendingAdminCount > 0 || isCurrentUserAdmin) {
                            "অ্যাডমিন রিভিউ ($pendingAdminCount)"
                        } else {
                            "অ্যাডমিন রিভিউ"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    subtitle: String,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .background(MagazineGold, RoundedCornerShape(2.dp))
            )
            Text(
                text = title,
                color = MagazineDeepGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = subtitle,
            color = Color(0xFF4B5563),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun FeaturedSpotlightArticleCard(
    article: PublishedIslamicArticle,
    isBookmarked: Boolean,
    onOpenArticle: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(305.dp)
            .clickable(onClick = onOpenArticle)
            .testTag("featured_article_card_${article.articleId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MagazineGold.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Editorial Cover Art Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(article.category.badgeColorHex),
                                MagazineMidGreen
                            )
                        )
                    )
                    .padding(14.dp)
                    .testTag("article_cover_${article.articleId}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MagazineGold,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "নির্বাচিত • ${article.category.titleBangla}",
                            color = Color(0xFF111827),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("featured_bookmark_btn_${article.articleId}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = MagazineGold
                        )
                    }
                }

                Text(
                    text = "${article.publishedDateTextBangla} • ${article.estimatedReadMinutesBangla}",
                    color = Color(0xFFDDF3EC),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.title,
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )

                Text(
                    text = article.summary,
                    color = Color(0xFF4B5563),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MagazineSoftGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = article.authorName,
                                tint = MagazineDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = article.authorName,
                            color = MagazineDeepGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = article.authorBio,
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialArticleMagazineCard(
    article: PublishedIslamicArticle,
    isBookmarked: Boolean,
    onOpenArticle: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenArticle)
            .testTag("article_card_${article.articleId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD9EAE0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Editorial Cover Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(article.category.badgeColorHex),
                                MagazineMidGreen
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("article_card_cover_${article.articleId}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = article.category.titleBangla,
                            tint = MagazineGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = article.category.titleBangla,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${article.publishedDateTextBangla} • ${article.estimatedReadMinutesBangla}",
                        color = Color(0xFFDDF3EC),
                        fontSize = 11.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = article.title,
                        color = Color(0xFF111827),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 23.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("article_card_bookmark_btn_${article.articleId}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = if (isBookmarked) MagazineGoldDark else MagazineDeepGreen
                        )
                    }
                }

                Text(
                    text = article.summary,
                    color = Color(0xFF374151),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MagazineSoftGreen,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = article.authorName,
                                    tint = MagazineDeepGreen,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = article.authorName,
                                color = MagazineDeepGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = article.authorBio,
                                color = Color(0xFF6B7280),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = MagazineSoftGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "যাচাইকৃত",
                                tint = MagazineDeepGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "দালিলিক",
                                color = MagazineDeepGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArticlesStateCard(query: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("article_empty_search_state"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD5E6DD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = "খুঁজে পাওয়া যায়নি",
                tint = MagazineMidGreen,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "“$query” বিষয়ে কোনো প্রকাশিত প্রবন্ধ পাওয়া যায়নি",
                color = MagazineDeepGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ভিন্ন শব্দ বা ক্যাটাগরি নির্বাচন করে পুনরায় অনুসন্ধান করুন।",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * ৪. প্রবন্ধ পড়ার পেজ (Dedicated Full-Screen Article Reading Page)
 * - প্রবন্ধের পূর্ণ শিরোনাম, লেখকের নাম ও পরিচিতি, প্রকাশের তারিখ, মূল প্রবন্ধ, প্রাসঙ্গিক ছবি
 * - কুরআনের আয়াতের রেফারেন্স, হাদিসের কিতাব ও নম্বর, তথ্যসূত্র ও গ্রন্থপঞ্জি, আলেমদের মতভেদ নোট
 * - বুকমার্ক ও শেয়ার করার সুবিধা এবং বাংলা ফন্ট ছোট-বড় করার সুবিধা
 */
@Composable
fun IslamicArticleReadingFullScreen(
    article: PublishedIslamicArticle,
    isBookmarked: Boolean,
    readerFontSizeSp: Int,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShareArticle: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF8))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("article_reading_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ১. প্রবন্ধের প্রচ্ছদ ব্যানার, ক্যাটাগরি ও প্রকাশের তারিখ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_reading_cover_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MagazineDeepGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(article.category.badgeColorHex),
                                MagazineDeepGreen
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MagazineGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = article.category.titleBangla,
                            color = Color(0xFF111827),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "প্রকাশের তারিখ: ${article.publishedDateTextBangla} • ${article.estimatedReadMinutesBangla}",
                        color = Color(0xFFDDF3EC),
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("article_reading_date")
                    )
                }

                // প্রবন্ধের পূর্ণ শিরোনাম
                Text(
                    text = article.title,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp,
                    modifier = Modifier.testTag("article_reading_title")
                )

                // লেখকের নাম ও পরিচিতি কার্ড
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("article_reading_author_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MagazineGold,
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = article.authorName,
                                    tint = Color(0xFF111827),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = article.authorName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = article.authorBio,
                                color = Color(0xFFDDF3EC),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ২. রিডার কন্ট্রোল বার: বাংলা ফন্ট ছোট-বড় করার সুবিধা + বুকমার্ক + শেয়ার
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD5E6DD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bangla Font Size Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecreaseFont,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MagazineDeepGreen),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("article_font_decrease_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextDecrease,
                            contentDescription = "ফন্ট ছোট করুন",
                            tint = MagazineDeepGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অ-", color = MagazineDeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${readerFontSizeSp}sp",
                        color = MagazineDeepGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("article_font_size_label")
                    )

                    OutlinedButton(
                        onClick = onIncreaseFont,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MagazineDeepGreen),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("article_font_increase_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextIncrease,
                            contentDescription = "ফন্ট বড় করুন",
                            tint = MagazineDeepGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অ+", color = MagazineDeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Bookmark & Share Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("article_reading_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক করুন",
                            tint = if (isBookmarked) MagazineGoldDark else MagazineDeepGreen
                        )
                    }

                    IconButton(
                        onClick = onShareArticle,
                        modifier = Modifier.testTag("article_reading_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার করুন",
                            tint = MagazineDeepGreen
                        )
                    }
                }
            }
        }

        // ৩. মূল প্রবন্ধ (Editorial Body)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("article_reading_body"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE3EFE8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormattedIslamicArticleBody(
                    content = article.content,
                    fontSizeSp = readerFontSizeSp
                )
            }
        }

        // ৪. কুরআনের আয়াতের রেফারেন্স
        if (article.quranReferences.isNotBlank()) {
            ReferenceSectionCard(
                title = "কুরআনুল কারীমের আয়াতের রেফারেন্স",
                bodyText = article.quranReferences,
                accentColor = MagazineDeepGreen,
                bgColor = MagazineSoftGreen,
                testTag = "article_reading_quran_refs"
            )
        }

        // ৫. হাদিসের কিতাব ও নম্বর
        if (article.hadithReferences.isNotBlank()) {
            ReferenceSectionCard(
                title = "হাদিসের কিতাব ও নম্বর",
                bodyText = article.hadithReferences,
                accentColor = MagazineMidGreen,
                bgColor = Color(0xFFF0F9F4),
                testTag = "article_reading_hadith_refs"
            )
        }

        // ৬. আলেমদের মতভেদ নোট (প্রযোজ্য ক্ষেত্রে)
        if (article.scholarlyDifferenceNote.isNotBlank()) {
            ReferenceSectionCard(
                title = "ফুকাহা ও ইমামগণের মতভেদ সংক্রান্ত নোট",
                bodyText = article.scholarlyDifferenceNote,
                accentColor = MagazineGoldDark,
                bgColor = Color(0xFFFFFBEB),
                testTag = "article_reading_scholarly_note"
            )
        }

        // ৭. তথ্যসূত্র ও গ্রন্থপঞ্জি
        if (article.otherSourcesAndBibliography.isNotBlank()) {
            ReferenceSectionCard(
                title = "তথ্যসূত্র ও গ্রন্থপঞ্জি (Bibliography)",
                bodyText = article.otherSourcesAndBibliography,
                accentColor = Color(0xFF1F2937),
                bgColor = Color(0xFFF9FAFB),
                testTag = "article_reading_bibliography"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReferenceSectionCard(
    title: String,
    bodyText: String,
    accentColor: Color,
    bgColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = bodyText,
                color = Color(0xFF1F2937),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
