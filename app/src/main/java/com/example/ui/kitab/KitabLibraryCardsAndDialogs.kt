package com.example.ui.kitab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val KitabGreenPrimary = Color(0xFF074E35)
internal val KitabGreenDark = Color(0xFF053B27)
internal val KitabGoldAccent = Color(0xFFDDA83A)
internal val KitabGoldLight = Color(0xFFF6E7C1)
internal val KitabCreamBg = Color(0xFFF6F9F6)
internal val KitabCardBorder = Color(0xFFD5E4DC)

@Composable
fun KitabCatalogBookCard(
    book: KitabItemUiModel,
    onDownloadOrResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onReadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (book.isReadyToReadOffline) 1.5.dp else 1.dp,
            color = if (book.isReadyToReadOffline) KitabGreenPrimary.copy(alpha = 0.55f) else KitabCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kitab_catalog_card_${book.bookId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Cover Image + Core Metadata (Title, Author, Translator, Publisher, Volumes, Size, Language)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                KitabCoverThumbnail(book = book)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KitabGreenPrimary.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = book.category.shortBadgeBangla,
                                color = KitabGreenPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (book.isReadyToReadOffline) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE1F5EA)
                            ) {
                                Text(
                                    text = "✓ অফলাইনে প্রস্তুত",
                                    color = KitabGreenPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. কিতাবের নাম
                    Text(
                        text = book.titleBangla,
                        color = KitabGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        lineHeight = 20.sp
                    )

                    if (book.arabicTitle.isNotBlank()) {
                        Text(
                            text = book.arabicTitle,
                            color = Color(0xFF7B601E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. লেখকের নাম
                    Text(
                        text = "লেখক: ${book.authorBangla}",
                        color = Color(0xFF284338),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // 3. অনুবাদক বা সংকলকের নাম
                    Text(
                        text = "অনুবাদক/সংকলক: ${book.translatorOrCompilerBangla}",
                        color = Color(0xFF3E594E),
                        fontSize = 11.5.sp
                    )

                    // 4. প্রকাশনীর নাম
                    Text(
                        text = "প্রকাশনী: ${book.publisherBangla}",
                        color = Color(0xFF3E594E),
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Pills Row: 6. মোট খণ্ড সংখ্যা | 7. PDF ফাইলের আকার (MB) | 8. ভাষা
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F8F5), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "খণ্ড: ${book.formattedVolumeBadge}",
                    color = KitabGreenDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (book.fileSizeBytes > 0L) {
                    Text(
                        text = "আকার: ${book.formattedFileSizeMb}",
                        color = KitabGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "ভাষা: ${book.languageBangla}",
                    color = Color(0xFF3E594E),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 9. সংক্ষিপ্ত পরিচিতি
            Text(
                text = book.shortDescriptionBangla,
                color = Color(0xFF374E44),
                fontSize = 12.2.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 10. ডাউনলোডের অবস্থা (Download Status Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (book.downloadStatus) {
                        KitabDownloadStatus.DOWNLOADED_VERIFIED -> Icons.Default.CheckCircle
                        KitabDownloadStatus.FAILED_CORRUPTED -> Icons.Default.ErrorOutline
                        KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE -> Icons.Default.Lock
                        else -> Icons.Default.Info
                    }
                    val statusColor = when (book.downloadStatus) {
                        KitabDownloadStatus.DOWNLOADED_VERIFIED -> KitabGreenPrimary
                        KitabDownloadStatus.FAILED_CORRUPTED -> Color(0xFFB71C1C)
                        KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE -> Color(0xFF795548)
                        else -> Color(0xFF0D5C40)
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "অবস্থা: ${book.downloadStatus.labelBangla}",
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("kitab_status_label_${book.bookId}")
                    )
                }

                if (book.isReadyToReadOffline && book.totalPageCount > 0) {
                    Text(
                        text = "শেষ পড়া পৃষ্ঠা: ${KitabFormatUtils.toBanglaDigits(book.lastReadPage)}/${KitabFormatUtils.toBanglaDigits(book.totalPageCount)}",
                        color = KitabGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (book.errorMessageBangla.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.errorMessageBangla,
                    color = if (book.downloadStatus == KitabDownloadStatus.FAILED_CORRUPTED) {
                        Color(0xFFB71C1C)
                    } else {
                        Color(0xFF8D5B0A)
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("kitab_error_message_${book.bookId}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Controls based on Download Status
            when (book.downloadStatus) {
                KitabDownloadStatus.DOWNLOADING,
                KitabDownloadStatus.PAUSED,
                KitabDownloadStatus.VERIFYING_CHECKSUM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F7F4), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (book.downloadStatus == KitabDownloadStatus.VERIFYING_CHECKSUM) {
                                    "SHA-256 নিরাপত্তা যাচাই হচ্ছে..."
                                } else {
                                    "ডাউনলোড অগ্রগতি: ${KitabFormatUtils.toBanglaDigits(book.progressPercent)}%"
                                },
                                color = KitabGreenPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${book.formattedDownloadedMb} / ${book.formattedFileSizeMb}",
                                color = KitabGreenDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("kitab_progress_text_${book.bookId}")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { book.progressFraction },
                            color = KitabGreenPrimary,
                            trackColor = Color(0xFFD2E5DB),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .testTag("kitab_progress_bar_${book.bookId}")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (book.downloadStatus == KitabDownloadStatus.DOWNLOADING) {
                                OutlinedButton(
                                    onClick = onPauseClick,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, KitabGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kitab_pause_btn_${book.bookId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("বিরতি (Pause)", fontSize = 12.sp)
                                }
                            } else if (book.downloadStatus == KitabDownloadStatus.PAUSED) {
                                Button(
                                    onClick = onDownloadOrResumeClick,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kitab_resume_btn_${book.bookId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("পুনরায় শুরু (Resume)", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = onCancelClick,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kitab_cancel_btn_${book.bookId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বাতিল (Cancel)", fontSize = 12.sp)
                            }
                        }
                    }
                }

                KitabDownloadStatus.DOWNLOADED_VERIFIED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onReadClick,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KitabGoldAccent,
                                contentColor = KitabGreenDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("kitab_read_btn_${book.bookId}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (book.lastReadPage > 1) {
                                    "পড়ুন (পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(book.lastReadPage)} থেকে)"
                                } else {
                                    "পড়ুন (অফলাইন PDF রিডার)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onDeleteDownloadClick,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("kitab_delete_btn_${book.bookId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "ডাউনলোড মুছুন",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF8E7),
                        border = BorderStroke(1.dp, Color(0xFFE0C380)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kitab_reference_only_badge_${book.bookId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF8D5B0A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = book.licenseNoteBangla,
                                color = Color(0xFF6D4505),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                KitabDownloadStatus.NOT_DOWNLOADED,
                KitabDownloadStatus.FAILED_CORRUPTED -> {
                    Button(
                        onClick = onDownloadOrResumeClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KitabGreenPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("kitab_download_btn_${book.bookId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (book.downloadStatus == KitabDownloadStatus.FAILED_CORRUPTED) {
                                "পুনরায় ডাউনলোড করুন (${book.formattedFileSizeMb})"
                            } else {
                                "ডাউনলোড করুন (${book.formattedFileSizeMb})"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KitabDownloadedMyBookCard(
    book: KitabItemUiModel,
    onReadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, KitabGreenPrimary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kitab_my_books_card_${book.bookId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                KitabCoverThumbnail(book = book)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = KitabGreenPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "অফলাইনে সংরক্ষিত • ইন্টারনেট ছাড়াই পাঠযোগ্য",
                            color = KitabGreenPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = book.titleBangla,
                        color = KitabGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )

                    Text(
                        text = "লেখক: ${book.authorBangla}",
                        color = Color(0xFF3E594E),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ফাইলের আকার: ${book.formattedFileSizeMb} • ${book.formattedVolumeBadge}",
                        color = KitabGreenDark,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "ডাউনলোডের তারিখ: ${KitabFormatUtils.formatDateBangla(book.downloadedAtMillis)}",
                        color = Color(0xFF4A6358),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reading Progress & Last Read Page Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F7F4), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                val safeTotalPages = book.totalPageCount.coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "শেষ পড়া পৃষ্ঠা: ${KitabFormatUtils.toBanglaDigits(book.lastReadPage)} / ${KitabFormatUtils.toBanglaDigits(safeTotalPages)}",
                        color = KitabGreenDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("kitab_my_books_last_page_${book.bookId}")
                    )
                    Text(
                        text = "অগ্রগতি: ${KitabFormatUtils.toBanglaDigits(book.readingProgressPercent)}%",
                        color = KitabGreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { book.readingProgressFraction.coerceIn(0.02f, 1f) },
                    color = KitabGoldAccent,
                    trackColor = Color(0xFFD5E4DC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReadClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KitabGoldAccent,
                        contentColor = KitabGreenDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("kitab_my_books_read_btn_${book.bookId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "পড়া চালিয়ে যান",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDeleteDownloadClick,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("kitab_my_books_delete_btn_${book.bookId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডাউনলোড মুছুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun KitabCoverThumbnail(book: KitabItemUiModel) {
    val primaryColor = Color(book.coverPrimaryHex)
    val accentColor = Color(book.coverAccentHex)

    Box(
        modifier = Modifier
            .width(74.dp)
            .height(106.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.88f))
                )
            )
            .border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.8.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "۞",
                    color = accentColor,
                    fontSize = 10.sp
                )
                Text(
                    text = book.arabicTitle.ifBlank { "الحديث" },
                    color = accentColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.titleBangla.substringBefore("—").trim(),
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "${KitabFormatUtils.toBanglaDigits(book.volumeNumber)}ম খণ্ড",
                        color = accentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminPublishKitabDialog(
    onDismiss: () -> Unit,
    onPublished: (String) -> Unit
) {
    val context = LocalContext.current
    var titleBangla by remember { mutableStateOf("") }
    var arabicTitle by remember { mutableStateOf("") }
    var authorBangla by remember { mutableStateOf("") }
    var translatorBangla by remember { mutableStateOf("") }
    var publisherBangla by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var sha256Checksum by remember { mutableStateOf("") }
    var fileSizeMbInput by remember { mutableStateOf("10") }
    var descriptionBangla by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন অনুমোদিত কিতাব যুক্ত করুন (অ্যাডমিন)",
                fontWeight = FontWeight.Bold,
                color = KitabGreenPrimary,
                fontSize = 15.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = titleBangla,
                    onValueChange = { titleBangla = it },
                    label = { Text("কিতাবের নাম (বাংলা)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = arabicTitle,
                    onValueChange = { arabicTitle = it },
                    label = { Text("আরবি নাম (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authorBangla,
                    onValueChange = { authorBangla = it },
                    label = { Text("লেখকের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translatorBangla,
                    onValueChange = { translatorBangla = it },
                    label = { Text("অনুবাদক / সংকলক") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = publisherBangla,
                    onValueChange = { publisherBangla = it },
                    label = { Text("প্রকাশনীর নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("সরাসরি PDF লিংক (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sha256Checksum,
                    onValueChange = { sha256Checksum = it },
                    label = { Text("SHA-256 Checksum (৬৪ অক্ষর)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionBangla,
                    onValueChange = { descriptionBangla = it },
                    label = { Text("সংক্ষিপ্ত পরিচিতি") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = Color(0xFFB71C1C), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sizeMb = fileSizeMbInput.toDoubleOrNull() ?: 10.0
                    val bytes = (sizeMb * 1024 * 1024).toLong()
                    val generatedId = "admin_kitab_" + System.currentTimeMillis()
                    val entity = KitabMetadataEntity(
                        bookId = generatedId,
                        seriesId = generatedId,
                        titleBangla = titleBangla.trim(),
                        arabicTitle = arabicTitle.trim(),
                        authorBangla = authorBangla.trim(),
                        translatorOrCompilerBangla = translatorBangla.trim().ifBlank { authorBangla.trim() },
                        publisherBangla = publisherBangla.trim(),
                        categoryId = KitabCategory.QAWMI_TEXTBOOK_SHARAH.id,
                        volumeNumber = 1,
                        totalVolumes = 1,
                        fileSizeBytes = bytes,
                        languageBangla = "বাংলা ও আরবি",
                        shortDescriptionBangla = descriptionBangla.trim().ifBlank { "অ্যাডমিন কর্তৃক সংযোজিত কিতাব।" },
                        marhalaBangla = "দাওরায়ে হাদিস ও উচ্চতর মারহালা",
                        downloadUrl = downloadUrl.trim(),
                        sha256Checksum = sha256Checksum.trim(),
                        isOpenLicenseVerified = downloadUrl.trim().startsWith("https://") && sha256Checksum.trim().length == 64,
                        isAdminApproved = true,
                        licenseNoteBangla = "অ্যাডমিন অনুমোদিত ও যাচাইকৃত সংস্করণ",
                        coverPrimaryHex = 0xFF074E35,
                        coverAccentHex = 0xFFDDA83A
                    )
                    KitabDownloadAndStorageManager.publishApprovedKitabAsAdmin(context, entity) { ok, msg ->
                        if (ok) {
                            onPublished(msg)
                        } else {
                            errorMsg = msg
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary)
            ) {
                Text("প্রকাশ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
