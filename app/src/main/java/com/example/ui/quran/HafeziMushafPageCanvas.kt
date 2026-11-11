package com.example.ui.quran

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val QuranAmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

/**
 * Renders the authentic scanned Emdadia 15-Line Hafezi Quran Sharif PDF page
 * (`imdadia-hafezi-quran3_text.pdf` / Archive.org `Emdadia-hafezi-quran`):
 * - Uses Android's native `PdfRenderer` when the full 30.68 MB PDF is downloaded on device.
 * - Automatically streams and caches individual high-resolution PDF pages on demand when reading online,
 *   and prefetches adjacent pages for instant page turning.
 * - Supports pinch-to-zoom, double-tap zoom, and paper color themes (Classic White, Emdadia Cream, Sepia, Night Mode).
 */
@Composable
fun HafeziMushafPageView(
    pageMetadata: HafeziPageMetadata,
    themeMode: MushafThemeMode,
    externalZoomScale: Float = 1f,
    onZoomChanged: (Float) -> Unit = {},
    isVerticalContinuousMode: Boolean = false,
    isPrimaryActivePage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember(pageMetadata.pageNumber) { mutableFloatStateOf(externalZoomScale) }
    var offset by remember(pageMetadata.pageNumber) { mutableStateOf(Offset.Zero) }

    val effectiveScale = if (externalZoomScale != 1f && scale == 1f) externalZoomScale else scale

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (effectiveScale * zoomChange).coerceIn(1f, 3.5f)
        scale = newScale
        onZoomChanged(newScale)
        if (newScale > 1.02f) {
            val maxPan = 480f * (newScale - 1f)
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxPan, maxPan),
                y = (offset.y + panChange.y).coerceIn(-maxPan, maxPan)
            )
        } else {
            offset = Offset.Zero
        }
    }

    var pageBitmap by remember(pageMetadata.pageNumber) {
        mutableStateOf<Bitmap?>(HafeziQuranStorageManager.getCachedPageBitmap(pageMetadata.pageNumber))
    }
    var isLoadingPage by remember(pageMetadata.pageNumber) {
        mutableStateOf(pageBitmap == null)
    }
    var retryCounter by remember(pageMetadata.pageNumber) { mutableIntStateOf(0) }

    val downloadState by HafeziQuranStorageManager.downloadStateFlow.collectAsState()

    LaunchedEffect(pageMetadata.pageNumber, retryCounter, downloadState.status) {
        val cached = HafeziQuranStorageManager.getCachedPageBitmap(pageMetadata.pageNumber)
        if (cached != null) {
            pageBitmap = cached
            isLoadingPage = false
        } else {
            isLoadingPage = true
            val loaded = HafeziQuranStorageManager.loadOrRenderQuranPdfPageBitmap(
                context = context,
                pageNumber = pageMetadata.pageNumber,
                targetWidthPx = 1280
            )
            pageBitmap = loaded
            isLoadingPage = false
        }
    }

    val pdfColorFilter = remember(themeMode) {
        createPdfThemeColorFilter(themeMode)
    }

    val paperColor = themeMode.pageInnerBackground
    val borderColor = themeMode.borderColor

    val containerHeightModifier = if (isVerticalContinuousMode) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .aspectRatio(0.675f)
    } else {
        modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    }

    val linesContainerTag = if (isPrimaryActivePage) {
        "hafezi_mushaf_15_lines_container"
    } else {
        "hafezi_mushaf_15_lines_container_${pageMetadata.pageNumber}"
    }

    Box(
        modifier = containerHeightModifier
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(paperColor)
            .border(0.8.dp, borderColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .pointerInput(pageMetadata.pageNumber) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomChanged(1f)
                        } else {
                            scale = 2.0f
                            onZoomChanged(2.0f)
                        }
                    }
                )
            }
            .let { baseMod ->
                if (effectiveScale > 1.01f) {
                    baseMod.transformable(state = transformableState)
                } else {
                    baseMod
                }
            }
            .testTag("hafezi_mushaf_page_${pageMetadata.pageNumber}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = effectiveScale,
                    scaleY = effectiveScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .background(paperColor)
                .testTag(linesContainerTag),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = pageBitmap
            when {
                currentBitmap != null -> {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "এমদাদিয়া হাফেজি কুরআন শরীফ পিডিএফ পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)}",
                        contentScale = ContentScale.Fit,
                        colorFilter = pdfColorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .testTag("hafezi_pdf_page_image_${pageMetadata.pageNumber}")
                    )
                }

                isLoadingPage -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0F6B4B),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "এমদাদিয়া হাফেজি কুরআন শরীফ (PDF)",
                            style = MaterialTheme.typography.titleSmall,
                            color = themeMode.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)} লোড হচ্ছে • সূরা ${pageMetadata.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.paraNumber)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeMode.textColor.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "এমদাদিয়া ১৫ লাইন হাফেজি কুরআন (PDF)",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeMode.textColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)} • সূরা ${pageMetadata.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.paraNumber)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeMode.textColor.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "পিডিএফ পৃষ্ঠাটি দেখতে ইন্টারনেট সংযোগ চালু করে পুনরায় চেষ্টা করুন অথবা সম্পূর্ণ পিডিএফ কুরআন শরীফ (৩০.৬৮ MB) অফলাইনে ডাউনলোড করে নিন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeMode.textColor.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { retryCounter++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6B4B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("quran_pdf_page_retry_button_${pageMetadata.pageNumber}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "পুনরায় লোড করুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFD9A82E),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        HafeziQuranStorageManager.startOrResumeOfflineDownload(context, scope)
                                    }
                                    .testTag("quran_pdf_full_download_button_${pageMetadata.pageNumber}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF0B1914),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "সম্পূর্ণ PDF ডাউনলোড",
                                        color = Color(0xFF0B1914),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
 * Applies paper-tone or Night Mode color matrix filtering to the scanned PDF page bitmap
 * so the original Emdadia Hafezi calligraphy stays razor-sharp in all 4 reading themes.
 */
private fun createPdfThemeColorFilter(themeMode: MushafThemeMode): ColorFilter? {
    return when (themeMode) {
        MushafThemeMode.CLASSIC_WHITE -> null

        MushafThemeMode.EMDADIA_CREAM -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.99f, 0f, 0f, 0f, 0f,
                    0f, 0.965f, 0f, 0f, 0f,
                    0f, 0f, 0.89f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        MushafThemeMode.SEPIA_HERITAGE -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.96f, 0f, 0f, 0f, 0f,
                    0f, 0.90f, 0f, 0f, 0f,
                    0f, 0f, 0.78f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        MushafThemeMode.NIGHT_MODE -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -0.88f, 0f, 0f, 0f, 240f,
                    0f, -0.85f, 0f, 0f, 235f,
                    0f, 0f, -0.78f, 0f, 218f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
}
