package com.example.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
 * Renders a single page (`1..616`) of the Verified 15-Line Indo-Pak Hafezi Quran PDF
 * natively inside the app using `android.graphics.pdf.PdfRenderer`.
 *
 * - Preserves the PDF's original Arabic calligraphy, harakat, tajweed symbols, and 15-line layout untouched.
 * - Supports pinch-to-zoom, double-tap zoom, and +/- button zoom (`externalZoomScale`).
 * - Supports Full-Screen Reading, Vertical Continuous Scroll, and Horizontal Pager modes.
 */
@Composable
fun HafeziMushafPageView(
    pageMetadata: HafeziPageMetadata,
    themeMode: MushafThemeMode,
    externalZoomScale: Float = 1f,
    onZoomChanged: (Float) -> Unit = {},
    isVerticalContinuousMode: Boolean = false,
    isPrimaryActivePage: Boolean = true,
    onSingleTapToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember(pageMetadata.pageNumber) { mutableFloatStateOf(externalZoomScale) }
    var offset by remember(pageMetadata.pageNumber) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(externalZoomScale) {
        scale = externalZoomScale
        if (externalZoomScale <= 1.02f) {
            offset = Offset.Zero
        }
    }

    val effectiveScale = scale.coerceIn(1f, 3.5f)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (effectiveScale * zoomChange).coerceIn(1f, 3.5f)
        scale = newScale
        onZoomChanged(newScale)
        if (newScale > 1.02f) {
            val maxPan = 450f * (newScale - 1f)
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxPan, maxPan),
                y = (offset.y + panChange.y).coerceIn(-maxPan, maxPan)
            )
        } else {
            offset = Offset.Zero
        }
    }

    val paperColor = themeMode.pageInnerBackground
    val containerHeightModifier = if (isVerticalContinuousMode) {
        modifier
            .fillMaxWidth()
            .height(690.dp)
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
                    onTap = { onSingleTapToggleFullScreen() },
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
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("hafezi_mushaf_page_${pageMetadata.pageNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = effectiveScale,
                    scaleY = effectiveScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFD9A82E).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(10.dp)
                )
                .background(paperColor)
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(linesContainerTag)
            ) {
                HafeziDrivePdfPageSurface(
                    pageNumber = pageMetadata.pageNumber,
                    themeMode = themeMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
