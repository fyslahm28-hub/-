package com.example.ui.quran

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import com.example.R

val QuranAmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

/**
 * Renders the authentic bundled Emdadia 15-Line Nurani Hafezi Quran Sharif PDF page
 * (`imdadia_nurani_hafezi_quran_v2.pdf` — নূরানী হাফেজী কোরআন শরীফ, এমদাদিয়া লাইব্রেরী, চকবাজার, ঢাকা)
 * natively via Android's `PdfRenderer` with zero network loading time.
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

    LaunchedEffect(pageMetadata.pageNumber) {
        val cached = HafeziQuranStorageManager.getCachedPageBitmap(pageMetadata.pageNumber)
        if (cached != null) {
            pageBitmap = cached
        } else {
            pageBitmap = HafeziQuranStorageManager.loadOrRenderQuranPdfPageBitmap(
                context = context,
                pageNumber = pageMetadata.pageNumber,
                targetWidthPx = 1080
            )
        }
    }

    val pdfColorFilter = remember(themeMode) {
        createPdfThemeColorFilter(themeMode)
    }

    val paperColor = if (themeMode == MushafThemeMode.CLASSIC_WHITE) {
        Color.White
    } else {
        themeMode.pageInnerBackground
    }

    val containerHeightModifier = if (isVerticalContinuousMode) {
        modifier
            .fillMaxWidth()
            .aspectRatio(0.686f)
    } else {
        modifier.fillMaxSize()
    }

    val linesContainerTag = if (isPrimaryActivePage) {
        "hafezi_mushaf_15_lines_container"
    } else {
        "hafezi_mushaf_15_lines_container_${pageMetadata.pageNumber}"
    }

    Box(
        modifier = containerHeightModifier
            .background(paperColor)
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
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "এমদাদিয়া নূরানী হাফেজী কুরআন শরীফ পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)}",
                    contentScale = if (isVerticalContinuousMode) ContentScale.FillWidth else ContentScale.Fit,
                    colorFilter = pdfColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hafezi_pdf_page_image_${pageMetadata.pageNumber}")
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0F6B4B),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = themeMode.textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Applies paper-tone or Night Mode color matrix filtering to the rendered PDF page bitmap
 * so the original Emdadia Nurani Hafezi calligraphy stays razor-sharp in all 4 reading themes.
 */
private fun createPdfThemeColorFilter(themeMode: MushafThemeMode): ColorFilter? {
    return when (themeMode) {
        MushafThemeMode.CLASSIC_WHITE -> null

        MushafThemeMode.EMDADIA_CREAM -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.99f, 0f, 0f, 0f, 0f,
                    0f, 0.97f, 0f, 0f, 0f,
                    0f, 0f, 0.90f, 0f, 0f,
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
