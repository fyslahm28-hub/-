package com.example.ui.kitab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val KitabGreenPrimary = Color(0xFF074E35)
val KitabGreenDark = Color(0xFF053826)
val KitabGoldAccent = Color(0xFFDDA83A)
val KitabGoldLight = Color(0xFFF6E7BD)
val KitabCreamBg = Color(0xFFF6F9F6)
val KitabWarmCreamSurface = Color(0xFFFAF7F0)
val KitabCardBorder = Color(0xFFCDE2D6)

/**
 * Compact Book Cover Thumbnail (`48.dp x 68.dp` by default) with ornate Islamic gold frame,
 * volume number badge, and Arabic/Bangla title emblem.
 */
@Composable
fun KitabCoverThumbnail(
    book: KitabItemUiModel,
    width: Dp = 48.dp,
    height: Dp = 68.dp
) {
    val coverPrimary = Color(book.coverPrimaryHex)
    val coverAccent = Color(book.coverAccentHex)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        coverPrimary,
                        KitabGreenDark
                    )
                )
            )
            .border(1.2.dp, coverAccent, RoundedCornerShape(8.dp))
            .testTag("kitab_cover_thumbnail_${book.bookId}"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 3.5.dp.toPx()
            drawRect(
                color = coverAccent.copy(alpha = 0.42f),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width - inset * 2,
                    height = size.height - inset * 2
                ),
                style = Stroke(width = 0.8.dp.toPx())
            )
            drawCircle(
                color = coverAccent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.28f,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "খণ্ড ${KitabFormatUtils.toBanglaDigits(book.volumeNumber)}",
                color = coverAccent,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = book.category.shortBadgeBangla,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 9.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = coverAccent.copy(alpha = 0.22f)
            ) {
                Text(
                    text = "PDF",
                    color = coverAccent,
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Compact Hadith Kitab Card designed for 2-column mobile grid & 3–4 column tablet/landscape grid.
 * Zero excess whitespace, authentic database metadata, cover thumbnail, bookmark button, and download progress/actions.
 */
@Composable
fun KitabCatalogBookCard(
    book: KitabItemUiModel,
    isBookmarked: Boolean = false,
    onBookmarkToggleClick: () -> Unit = {},
    onDownloadOrResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onReadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (book.isReadyToReadOffline) 1.3.dp else 1.dp,
            color = if (book.isReadyToReadOffline) KitabGoldAccent else KitabCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("kitab_catalog_card_${book.bookId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Top Row: Category Pill + Marhala + Bookmark Action Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = KitabGreenPrimary.copy(alpha = 0.10f),
                    border = BorderStroke(0.7.dp, KitabGreenPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = book.category.shortBadgeBangla,
                        color = KitabGreenPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = CircleShape,
                    color = if (isBookmarked) KitabGoldLight else KitabWarmCreamSurface,
                    border = BorderStroke(
                        0.8.dp,
                        if (isBookmarked) KitabGoldAccent else KitabCardBorder
                    ),
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onBookmarkToggleClick() }
                        .testTag("kitab_card_bookmark_btn_${book.bookId}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "কিতাব বুকমার্ক করুন",
                            tint = if (isBookmarked) KitabGreenPrimary else Color(0xFF60786D),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 2. Book Cover Thumbnail + Title, Arabic Title, Author & Publisher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                KitabCoverThumbnail(
                    book = book,
                    width = 48.dp,
                    height = 68.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = book.titleBangla,
                        color = KitabGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        lineHeight = 15.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (book.arabicTitle.isNotBlank()) {
                        Text(
                            text = book.arabicTitle,
                            color = Color(0xFF8A691B),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = book.authorBangla,
                        color = Color(0xFF284338),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = book.publisherBangla,
                        color = Color(0xFF526B60),
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Compact Cream Metadata Strip (Volume • File Size MB)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KitabWarmCreamSurface, RoundedCornerShape(7.dp))
                    .border(0.6.dp, Color(0xFFE6DEC8), RoundedCornerShape(7.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (book.totalVolumes > 1) {
                        "${KitabFormatUtils.toBanglaDigits(book.volumeNumber)}ম/${KitabFormatUtils.toBanglaDigits(book.totalVolumes)} খণ্ড"
                    } else {
                        "এক খণ্ডে সমাপ্ত"
                    },
                    color = KitabGreenDark,
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                if (book.fileSizeBytes > 0L) {
                    Text(
                        text = book.formattedFileSizeMb,
                        color = KitabGreenPrimary,
                        fontSize = 9.8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // 4. Compact Status & Reading Progress Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (book.isReadyToReadOffline) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (book.isReadyToReadOffline) KitabGreenPrimary else Color(0xFF6B7F75),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = book.downloadStatus.labelBangla,
                        color = if (book.isReadyToReadOffline) KitabGreenPrimary else Color(0xFF42594E),
                        fontSize = 9.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("kitab_status_label_${book.bookId}")
                    )
                }

                if (book.isReadyToReadOffline && book.totalPageCount > 0) {
                    Text(
                        text = "পৃষ্ঠা ${KitabFormatUtils.toBanglaDigits(book.lastReadPage)}/${KitabFormatUtils.toBanglaDigits(book.totalPageCount)}",
                        color = KitabGreenDark,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (book.errorMessageBangla.isNotBlank()) {
                Text(
                    text = book.errorMessageBangla,
                    color = if (book.downloadStatus == KitabDownloadStatus.PAUSED) Color(0xFF9A6700) else Color(0xFFB71C1C),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("kitab_error_message_${book.bookId}")
                )
            }

            // 5. Action Buttons / Progress Bar Area
            when (book.downloadStatus) {
                KitabDownloadStatus.DOWNLOADING,
                KitabDownloadStatus.PAUSED,
                KitabDownloadStatus.VERIFYING_CHECKSUM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F7F3), RoundedCornerShape(8.dp))
                            .padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${KitabFormatUtils.toBanglaDigits(book.progressPercent)}%",
                                color = KitabGreenDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("kitab_progress_percent_${book.bookId}")
                            )
                            Text(
                                text = "${book.formattedDownloadedMb}/${book.formattedFileSizeMb}",
                                color = KitabGreenPrimary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.testTag("kitab_progress_mb_${book.bookId}")
                            )
                        }

                        LinearProgressIndicator(
                            progress = { book.progressFraction },
                            color = KitabGreenPrimary,
                            trackColor = Color(0xFFD5E6DC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (book.downloadStatus == KitabDownloadStatus.DOWNLOADING) {
                                OutlinedButton(
                                    onClick = onPauseClick,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, KitabGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .testTag("kitab_pause_btn_${book.bookId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("বিরতি", fontSize = 10.sp, maxLines = 1)
                                }
                            } else if (book.downloadStatus == KitabDownloadStatus.PAUSED) {
                                Button(
                                    onClick = onDownloadOrResumeClick,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .testTag("kitab_resume_btn_${book.bookId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("চালু", fontSize = 10.sp, maxLines = 1)
                                }
                            }

                            OutlinedButton(
                                onClick = onCancelClick,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .testTag("kitab_cancel_btn_${book.bookId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("বাতিল", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }

                KitabDownloadStatus.DOWNLOADED_VERIFIED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onReadClick,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KitabGoldAccent,
                                contentColor = KitabGreenDark
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("kitab_read_btn_${book.bookId}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "পড়ুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(0.8.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteDownloadClick() }
                                .testTag("kitab_delete_btn_${book.bookId}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "ডাউনলোড মুছে ফেলুন",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                KitabDownloadStatus.REFERENCE_ONLY_NO_LICENSE -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF8E7),
                        border = BorderStroke(0.8.dp, Color(0xFFE0C380)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kitab_reference_only_badge_${book.bookId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF8D5B0A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "প্রকাশকের কপিরাইট সংরক্ষিত",
                                color = Color(0xFF6D4505),
                                fontSize = 9.8.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("kitab_download_btn_${book.bookId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = KitabGoldLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (book.downloadStatus == KitabDownloadStatus.FAILED_CORRUPTED) {
                                "পুনরায় ডাউনলোড"
                            } else {
                                "ডাউনলোড (${book.formattedFileSizeMb})"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact My Books (`আমার কিতাব`) card designed for responsive 2–4 column grid layout.
 */
@Composable
fun KitabDownloadedMyBookCard(
    book: KitabItemUiModel,
    isBookmarked: Boolean = false,
    onBookmarkToggleClick: () -> Unit = {},
    onReadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, KitabGreenPrimary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("kitab_my_books_card_${book.bookId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = KitabGreenPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "অফলাইনে প্রস্তুত",
                        color = KitabGreenPrimary,
                        fontSize = 9.8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isBookmarked) KitabGoldLight else KitabWarmCreamSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onBookmarkToggleClick() }
                        .testTag("kitab_card_bookmark_btn_${book.bookId}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = if (isBookmarked) KitabGreenPrimary else Color(0xFF6B7F75),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                KitabCoverThumbnail(
                    book = book,
                    width = 48.dp,
                    height = 68.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = book.titleBangla,
                        color = KitabGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        lineHeight = 15.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = book.authorBangla,
                        color = Color(0xFF3E594E),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${book.formattedFileSizeMb} • ${book.formattedVolumeBadge}",
                        color = KitabGreenDark,
                        fontSize = 9.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Compact Reading Progress Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KitabWarmCreamSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                val safeTotalPages = book.totalPageCount.coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পৃষ্ঠা: ${KitabFormatUtils.toBanglaDigits(book.lastReadPage)} / ${KitabFormatUtils.toBanglaDigits(safeTotalPages)}",
                        color = KitabGreenDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${KitabFormatUtils.toBanglaDigits(book.readingProgressPercent)}%",
                        color = KitabGreenPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.readingProgressFraction.coerceIn(0.02f, 1f) },
                    color = KitabGreenPrimary,
                    trackColor = Color(0xFFD8E6DE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReadClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KitabGreenPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("kitab_my_books_read_btn_${book.bookId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = KitabGoldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পড়ুন", fontWeight = FontWeight.Bold, fontSize = 11.2.sp, maxLines = 1)
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(0.8.dp, Color(0xFFEF9A9A)),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onDeleteDownloadClick() }
                        .testTag("kitab_my_books_delete_btn_${book.bookId}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "কিতাব মুছুন",
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Admin Kitab Approval & Publishing Modal.
 */
@Composable
fun AdminPublishKitabDialog(
    onDismiss: () -> Unit,
    onPublishedMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var titleBangla by remember { mutableStateOf("") }
    var arabicTitle by remember { mutableStateOf("") }
    var authorBangla by remember { mutableStateOf("") }
    var translatorBangla by remember { mutableStateOf("") }
    var publisherBangla by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(KitabCategory.BUKHARI_SHARAH) }
    var volumeNumberStr by remember { mutableStateOf("1") }
    var totalVolumesStr by remember { mutableStateOf("1") }
    var fileSizeBytesStr by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var sha256Hex by remember { mutableStateOf("") }
    var shortDesc by remember { mutableStateOf("") }
    var isOpenLicense by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("kitab_admin_publish_dialog"),
        title = {
            Text(
                text = "নতুন হাদিস শরাহ কিতাব অনুমোদন (অ্যাডমিন)",
                fontWeight = FontWeight.Bold,
                color = KitabGreenPrimary,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    label = { Text("আরবি শিরোনাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authorBangla,
                    onValueChange = { authorBangla = it },
                    label = { Text("মূল লেখক / মুহাদ্দিস") },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = volumeNumberStr,
                        onValueChange = { volumeNumberStr = it },
                        label = { Text("খণ্ড নং") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalVolumesStr,
                        onValueChange = { totalVolumesStr = it },
                        label = { Text("মোট খণ্ড") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = fileSizeBytesStr,
                    onValueChange = { fileSizeBytesStr = it },
                    label = { Text("ফাইলের আকার (Bytes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("সরাসরি PDF Download URL (HTTPS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sha256Hex,
                    onValueChange = { sha256Hex = it },
                    label = { Text("SHA-256 Checksum (64 hex chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortDesc,
                    onValueChange = { shortDesc = it },
                    label = { Text("সংক্ষিপ্ত পরিচিতি") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOpenLicense,
                        onCheckedChange = { isOpenLicense = it }
                    )
                    Text(
                        text = "ওপেন-অ্যাক্সেস / প্রকাশকের বৈধ ডিজিটাল অনুমতি যাচাইকৃত",
                        fontSize = 12.sp
                    )
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFB71C1C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vol = KitabFormatUtils.parseBanglaOrEnglishInt(volumeNumberStr) ?: 1
                    val totalVol = KitabFormatUtils.parseBanglaOrEnglishInt(totalVolumesStr) ?: 1
                    val sizeBytes = fileSizeBytesStr.trim().toLongOrNull() ?: 0L
                    val generatedId = "custom_${System.currentTimeMillis()}"
                    val entity = KitabMetadataEntity(
                        bookId = generatedId,
                        seriesId = "series_$generatedId",
                        titleBangla = titleBangla.trim(),
                        arabicTitle = arabicTitle.trim(),
                        authorBangla = authorBangla.trim(),
                        translatorOrCompilerBangla = translatorBangla.trim().ifBlank { authorBangla.trim() },
                        publisherBangla = publisherBangla.trim(),
                        categoryId = selectedCategory.id,
                        volumeNumber = vol,
                        totalVolumes = totalVol.coerceAtLeast(vol),
                        fileSizeBytes = sizeBytes,
                        languageBangla = "বাংলা ও আরবি",
                        shortDescriptionBangla = shortDesc.trim().ifBlank { "প্রামাণ্য বাংলা হাদিস শরাহ।" },
                        marhalaBangla = "দাওরায়ে হাদিস (তাকমীল)",
                        downloadUrl = downloadUrl.trim(),
                        sha256Checksum = sha256Hex.trim().lowercase(),
                        isOpenLicenseVerified = isOpenLicense,
                        isAdminApproved = true,
                        licenseNoteBangla = if (isOpenLicense) "অ্যাডমিন ভেরিফায়েড ওপেন-অ্যাক্সেস সংস্করণ" else "শুধু গ্রন্থ-তথ্য",
                        coverPrimaryHex = 0xFF074E35,
                        coverAccentHex = 0xFFDDA83A
                    )
                    KitabDownloadAndStorageManager.publishApprovedKitabAsAdmin(context, entity) { ok, msg ->
                        if (ok) {
                            onPublishedMessage(msg)
                            onDismiss()
                        } else {
                            errorMsg = msg
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KitabGreenPrimary)
            ) {
                Text("অনুমোদন ও প্রকাশ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
