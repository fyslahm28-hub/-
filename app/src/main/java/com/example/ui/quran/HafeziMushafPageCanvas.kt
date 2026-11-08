package com.example.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val QuranAmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

/**
 * Renders a single authentic printed Indo-Pak / Emdadia / Nurani 15-Line Hafezi Mushaf Page
 * matching the classic black-on-white calligraphed Hafezi Quran layout:
 * - Top Arabic Manzil ("منزل ١")
 * - Centered bold "( 01 : 03 )" Para & Page-in-Para index
 * - Left Surah name & number ("البقرة ٢") and Right Para name & number ("الٓمّٓ ١")
 * - Intricate black-and-white floral/arabesque Hafezi ornamental border with double-ruled inner frame
 * - Strictly 15 horizontal ruled lines separated by crisp black dividers
 * - Outer margin Ruku ("ع") indicator and bottom-corner page numeral
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
    val drivePdfState by HafeziDrivePdfManager.stateFlow.collectAsState()
    val useOfflineDrivePdf = drivePdfState.status == DrivePdfStatus.READY_OFFLINE &&
        drivePdfState.renderSource == QuranMushafRenderSource.DRIVE_PDF_MUSHAF

    var scale by remember(pageMetadata.pageNumber) { mutableFloatStateOf(externalZoomScale) }
    var offset by remember(pageMetadata.pageNumber) { mutableStateOf(Offset.Zero) }

    val effectiveScale = if (externalZoomScale != 1f && scale == 1f) externalZoomScale else scale

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

    val inkColor = themeMode.textColor
    val frameColor = themeMode.borderColor
    val paperColor = themeMode.pageInnerBackground

    val paraInfo = remember(pageMetadata.paraNumber) {
        HafeziQuranIndexData.getParaByNumber(pageMetadata.paraNumber)
    }
    val pageInPara = remember(pageMetadata.pageNumber, paraInfo.startPage) {
        if (pageMetadata.pageNumber <= 1) 1 else (pageMetadata.pageNumber - paraInfo.startPage + 1).coerceAtLeast(1)
    }
    val formattedParaPageCode = remember(pageMetadata.paraNumber, pageInPara) {
        String.format(Locale.US, "( %02d : %02d )", pageMetadata.paraNumber, pageInPara)
    }

    val rukuLineIndex = remember(pageMetadata.lines) {
        pageMetadata.lines.indexOfFirst { it.marginalMarker != null && it.marginalMarker.contains("ركوع") }
    }

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
                .background(paperColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (useOfflineDrivePdf) {
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
            } else {
                // 1. Classic Printed Top Margin Header Outside Frame:
                //    - Centered "منزل ١"
                //    - Centered bold "( 01 : 03 )"
                //    - Left: "البقرة ٢" | Right: "الٓمّٓ ١"
                ClassicMushafTopMarginHeader(
                    pageMetadata = pageMetadata,
                    formattedParaPageCode = formattedParaPageCode,
                    inkColor = inkColor
                )

                // 2. Main Framed 15-Line Mushaf Area with Outer Margin Ruku & Ornamental Canvas Frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    // Traditional Intricate Black-and-White Floral/Arabesque Border Frame
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawTraditionalHafeziOrnamentalFrame(
                            frameColor = frameColor,
                            paperColor = paperColor
                        )
                    }

                    // Outer Left Margin Ruku ("ع") Indicator (matching screenshot left margin)
                    if (rukuLineIndex >= 0) {
                        val verticalFraction = ((rukuLineIndex + 0.5f) / 15f).coerceIn(0.08f, 0.92f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(18.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = (verticalFraction * 440f).dp.coerceIn(24.dp, 460.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = HafeziQuranIndexData.toArabicIndicDigits((pageMetadata.pageNumber % 3) + 1),
                                    color = inkColor,
                                    fontFamily = QuranAmiriFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 9.sp
                                )
                                Text(
                                    text = "ع",
                                    color = inkColor,
                                    fontFamily = QuranAmiriFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    text = HafeziQuranIndexData.toArabicIndicDigits((pageMetadata.pageNumber % 9) + 1),
                                    color = inkColor,
                                    fontFamily = QuranAmiriFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 9.sp
                                )
                            }
                        }
                    }

                    // Bottom-Right Outer Margin Page Numeral (matching screenshot bottom-right "٥")
                    Text(
                        text = HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber),
                        color = inkColor,
                        fontFamily = QuranAmiriFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 2.dp)
                    )

                    // Strictly 15 Ruled Lines Inside the Double-Ruled Inner Box
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 17.dp, vertical = 14.dp)
                            .testTag(linesContainerTag)
                    ) {
                        pageMetadata.lines.take(HafeziQuranIndexData.LINES_PER_PAGE).forEachIndexed { idx, lineItem ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("hafezi_mushaf_line_${lineItem.lineNumber}"),
                                contentAlignment = Alignment.Center
                            ) {
                                ClassicMushafSingleLineRow(
                                    lineItem = lineItem,
                                    inkColor = inkColor,
                                    paperColor = paperColor
                                )
                                if (idx < HafeziQuranIndexData.LINES_PER_PAGE - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        thickness = 1.dp,
                                        color = inkColor.copy(alpha = 0.88f)
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

@Composable
private fun ClassicMushafTopMarginHeader(
    pageMetadata: HafeziPageMetadata,
    formattedParaPageCode: String,
    inkColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Centered "منزل ١"
        Text(
            text = "منزل ${HafeziQuranIndexData.toArabicIndicDigits(pageMetadata.manzilNumber)}",
            color = inkColor,
            fontFamily = QuranAmiriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )

        // 2. Centered bold "( 01 : 03 )"
        Text(
            text = formattedParaPageCode,
            color = inkColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        // 3. Left: Surah Name & Number ("البقرة ٢") | Right: Para Name & Number ("الٓمّٓ ١")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${pageMetadata.primarySurah.nameArabic} ${HafeziQuranIndexData.toArabicIndicDigits(pageMetadata.primarySurah.number)}",
                color = inkColor,
                fontFamily = QuranAmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 16.sp
            )

            Text(
                text = "${pageMetadata.paraNameArabic} ${HafeziQuranIndexData.toArabicIndicDigits(pageMetadata.paraNumber)}",
                color = inkColor,
                fontFamily = QuranAmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ClassicMushafSingleLineRow(
    lineItem: MushafLineItem,
    inkColor: Color,
    paperColor: Color
) {
    when (lineItem.lineType) {
        MushafLineType.SURAH_HEADER_BANNER -> {
            val surah = lineItem.surahHeaderInfo
            // Classic Black-and-White Printed Hafezi Surah Cartouche Banner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Outer double border of Surah cartouche
                    drawRect(
                        color = inkColor,
                        topLeft = Offset(0f, 1f),
                        size = Size(w, (h - 2f).coerceAtLeast(2f)),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawRect(
                        color = inkColor,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size((w - 6.dp.toPx()).coerceAtLeast(2f), (h - 6.dp.toPx()).coerceAtLeast(2f)),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                    // Left & Right decorative floral medallions inside cartouche
                    val r = (h * 0.28f).coerceAtLeast(4f)
                    drawCircle(
                        color = inkColor,
                        radius = r,
                        center = Offset(w * 0.16f, h / 2f),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                    drawCircle(
                        color = inkColor,
                        radius = r,
                        center = Offset(w * 0.84f, h / 2f),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (surah != null) "ركوعاتها ${HafeziQuranIndexData.toArabicIndicDigits(surah.rukuCount)}" else "",
                        color = inkColor,
                        fontFamily = QuranAmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = lineItem.arabicText,
                        color = inkColor,
                        fontFamily = QuranAmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (surah != null) "آياتها ${HafeziQuranIndexData.toArabicIndicDigits(surah.ayahCount)}" else "",
                        color = inkColor,
                        fontFamily = QuranAmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        MushafLineType.BISMILLAH_BANNER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(paperColor)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lineItem.arabicText,
                    color = inkColor,
                    fontFamily = QuranAmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        MushafLineType.QURAN_TEXT_LINE -> {
            // Format ayah markers so they look like crisp traditional Hafezi circular ayah medallions
            val formattedText = remember(lineItem.arabicText) {
                formatClassicalIndoPakLine(lineItem.arabicText)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dynamically scale font size slightly based on character count so every line
                // fills the width cleanly without clipping or ellipsis, matching the printed Mushaf.
                val charLen = formattedText.length
                val adaptiveFontSize = when {
                    charLen > 92 -> 18.5.sp
                    charLen > 82 -> 19.5.sp
                    charLen > 72 -> 20.5.sp
                    else -> 21.5.sp
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = formattedText,
                        modifier = Modifier.fillMaxWidth(),
                        color = inkColor,
                        fontFamily = QuranAmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = adaptiveFontSize,
                        lineHeight = (adaptiveFontSize.value * 1.15f).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            textDirection = TextDirection.Rtl
                        )
                    )
                }
            }
        }
    }
}

/**
 * Formats ayah end markers (e.g. "۝١٦") into ornate classic Hafezi circular ayah medallions
 * so they render clearly inside the Amiri Quranic calligraphy.
 */
private fun formatClassicalIndoPakLine(raw: String): String {
    if (!raw.contains('۝')) return raw
    val sb = StringBuilder(raw.length + 8)
    var i = 0
    while (i < raw.length) {
        val ch = raw[i]
        if (ch == '۝') {
            val digits = StringBuilder()
            var j = i + 1
            while (j < raw.length && (raw[j] in '٠'..'٩' || raw[j] in '0'..'9')) {
                digits.append(raw[j])
                j++
            }
            if (digits.isNotEmpty()) {
                sb.append("﴿").append(digits).append("﴾")
                i = j
                continue
            }
        }
        sb.append(ch)
        i++
    }
    return sb.toString()
}

/**
 * Draws the traditional black-and-white Indo-Pak / Emdadia Hafezi Mushaf ornamental border:
 * - Scalloped botanical outer vine border along left, right, top, and bottom
 * - Bold black horizontal center bar at top and bottom flanked by S-scroll floral brackets
 * - Ornate corner flourishes
 * - Crisp double-ruled inner rectangular frame enclosing the 15 horizontal lines
 */
private fun DrawScope.drawTraditionalHafeziOrnamentalFrame(
    frameColor: Color,
    paperColor: Color
) {
    val w = size.width
    val h = size.height

    val outerPadX = 15.dp.toPx()
    val outerPadY = 12.dp.toPx()
    val innerPadX = 17.dp.toPx()
    val innerPadY = 14.dp.toPx()

    val outerLeft = outerPadX
    val outerTop = outerPadY
    val outerRight = w - outerPadX
    val outerBottom = h - outerPadY
    val outerWidth = (outerRight - outerLeft).coerceAtLeast(10f)
    val outerHeight = (outerBottom - outerTop).coerceAtLeast(10f)

    // 1. Scalloped floral vine border along Left & Right outer margins
    val verticalLobes = 28
    val stepY = outerHeight / verticalLobes
    val vineOutreach = 6.5.dp.toPx()
    for (i in 0 until verticalLobes) {
        val y1 = outerTop + i * stepY
        val yMid = y1 + stepY * 0.5f
        val y2 = y1 + stepY

        // Left botanical scallop
        val leftPath = Path().apply {
            moveTo(outerLeft, y1)
            quadraticTo(outerLeft - vineOutreach, yMid, outerLeft, y2)
        }
        drawPath(leftPath, color = frameColor, style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(
            color = frameColor,
            radius = 1.3.dp.toPx(),
            center = Offset(outerLeft - vineOutreach * 0.45f, yMid)
        )

        // Right botanical scallop
        val rightPath = Path().apply {
            moveTo(outerRight, y1)
            quadraticTo(outerRight + vineOutreach, yMid, outerRight, y2)
        }
        drawPath(rightPath, color = frameColor, style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(
            color = frameColor,
            radius = 1.3.dp.toPx(),
            center = Offset(outerRight + vineOutreach * 0.45f, yMid)
        )
    }

    // 2. Top & Bottom Center Solid Black Bar + Symmetrical Floral Scrollwork (matching screenshot)
    val centerBarWidth = outerWidth * 0.54f
    val centerBarLeft = outerLeft + (outerWidth - centerBarWidth) / 2f
    val centerBarHeight = 4.2.dp.toPx()

    // Top solid black bar
    drawRoundRect(
        color = frameColor,
        topLeft = Offset(centerBarLeft, outerTop - centerBarHeight - 2.dp.toPx()),
        size = Size(centerBarWidth, centerBarHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    // Bottom solid black bar
    drawRoundRect(
        color = frameColor,
        topLeft = Offset(centerBarLeft, outerBottom + 2.dp.toPx()),
        size = Size(centerBarWidth, centerBarHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Top & Bottom side floral scrolls between corners and center bar
    val sideScrollCount = 6
    val leftSegmentWidth = (centerBarLeft - outerLeft) / sideScrollCount
    val scrollReachY = 5.5.dp.toPx()
    for (i in 0 until sideScrollCount) {
        // Top-Left & Top-Right scrolls
        val lx1 = outerLeft + i * leftSegmentWidth
        val lxMid = lx1 + leftSegmentWidth * 0.5f
        val lx2 = lx1 + leftSegmentWidth

        val rx1 = centerBarLeft + centerBarWidth + i * leftSegmentWidth
        val rxMid = rx1 + leftSegmentWidth * 0.5f
        val rx2 = rx1 + leftSegmentWidth

        val topScrollPath = Path().apply {
            moveTo(lx1, outerTop)
            quadraticTo(lxMid, outerTop - scrollReachY, lx2, outerTop)
            moveTo(rx1, outerTop)
            quadraticTo(rxMid, outerTop - scrollReachY, rx2, outerTop)
        }
        drawPath(topScrollPath, color = frameColor, style = Stroke(width = 1.25.dp.toPx()))

        val bottomScrollPath = Path().apply {
            moveTo(lx1, outerBottom)
            quadraticTo(lxMid, outerBottom + scrollReachY, lx2, outerBottom)
            moveTo(rx1, outerBottom)
            quadraticTo(rxMid, outerBottom + scrollReachY, rx2, outerBottom)
        }
        drawPath(bottomScrollPath, color = frameColor, style = Stroke(width = 1.25.dp.toPx()))
    }

    // 3. Four Corner Floral/Arabesque Medallion Flourishes
    val corners = listOf(
        Offset(outerLeft, outerTop) to Offset(-1f, -1f),
        Offset(outerRight, outerTop) to Offset(1f, -1f),
        Offset(outerLeft, outerBottom) to Offset(-1f, 1f),
        Offset(outerRight, outerBottom) to Offset(1f, 1f)
    )
    val cornerSize = 9.dp.toPx()
    for ((corner, dir) in corners) {
        val cx = corner.x + dir.x * (cornerSize * 0.35f)
        val cy = corner.y + dir.y * (cornerSize * 0.35f)
        drawCircle(
            color = paperColor,
            radius = cornerSize * 0.55f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = frameColor,
            radius = cornerSize * 0.55f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Inner 4-petal rosette in each corner
        for (angleDeg in 0 until 360 step 90) {
            val rad = angleDeg * (PI / 180.0)
            val px = cx + (cos(rad) * cornerSize * 0.25f).toFloat()
            val py = cy + (sin(rad) * cornerSize * 0.25f).toFloat()
            drawCircle(
                color = frameColor,
                radius = 1.3.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }

    // 4. Crisp Outer & Inner Double-Ruled Rectangular Frame Around the 15 Lines
    drawRect(
        color = frameColor,
        topLeft = Offset(outerLeft, outerTop),
        size = Size(outerWidth, outerHeight),
        style = Stroke(width = 2.0.dp.toPx())
    )

    drawRect(
        color = frameColor,
        topLeft = Offset(innerPadX, innerPadY),
        size = Size(
            width = (w - innerPadX * 2f).coerceAtLeast(10f),
            height = (h - innerPadY * 2f).coerceAtLeast(10f)
        ),
        style = Stroke(width = 1.0.dp.toPx())
    )
}
