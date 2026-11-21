package com.example.ui.quranlearning

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HindSiliguriFontFamily
import kotlin.math.cos
import kotlin.math.sin

// অ্যাপের মূল ইসলামিক কালার প্যালেট ও গাঢ় সবুজের পরিমিত শেড
val AcademyPrimaryGreen = Color(0xFF0F6B4B)
val AcademyDeepEmerald = Color(0xFF094933)
val AcademyRoyalEmerald = Color(0xFF063324)
val AcademySoftEmerald = Color(0xFF16805B)
val AcademyGold = Color(0xFFD9A82E)
val AcademyGoldSheen = Color(0xFFECD07B)
val AcademyLightGreenBg = Color(0xFFF6FAF8)

val QuranAcademyArabicFont = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

@Immutable
data class QuranAcademyPalette(
    val isDark: Boolean,
    val pageBackground: Color,
    val glassCardTop: Color,
    val glassCardBottom: Color,
    val glassCardSheen: Color,
    val glassCardBorderGold: Color,
    val glassCardBorderGreen: Color,
    val secondarySurfaceTop: Color,
    val secondarySurfaceBottom: Color,
    val arabicChamberBg: Color,
    val arabicChamberBorder: Color,
    val arabicSampleColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val mutedColor: Color,
    val navIndicatorBg: Color,
    val navIndicatorBorder: Color,
    val navIndicatorIcon: Color,
    val accentGreen: Color,
    val accentGold: Color,
    val dividerColor: Color,
    val cardShadowColor: Color
)

@Composable
fun rememberQuranAcademyPalette(
    darkTheme: Boolean = isSystemInDarkTheme()
): QuranAcademyPalette {
    return remember(darkTheme) {
        if (darkTheme) {
            QuranAcademyPalette(
                isDark = true,
                pageBackground = Color(0xFF061B14),
                glassCardTop = Color(0xFF103226),
                glassCardBottom = Color(0xFF0B251C),
                glassCardSheen = Color(0xFF1C5240).copy(alpha = 0.24f),
                glassCardBorderGold = AcademyGold.copy(alpha = 0.44f),
                glassCardBorderGreen = Color(0xFF2B6F56).copy(alpha = 0.45f),
                secondarySurfaceTop = Color(0xFF0E2B21),
                secondarySurfaceBottom = Color(0xFF0A2119),
                arabicChamberBg = Color(0xFF071E16),
                arabicChamberBorder = AcademyGold.copy(alpha = 0.42f),
                arabicSampleColor = AcademyGoldSheen,
                titleColor = Color(0xFFEFF8F4),
                subtitleColor = Color(0xFFABC7BC),
                mutedColor = Color(0xFF81A497),
                navIndicatorBg = AcademyGold.copy(alpha = 0.14f),
                navIndicatorBorder = AcademyGold.copy(alpha = 0.45f),
                navIndicatorIcon = AcademyGoldSheen,
                accentGreen = Color(0xFF209068),
                accentGold = AcademyGold,
                dividerColor = Color(0xFF1C4637),
                cardShadowColor = Color.Black.copy(alpha = 0.45f)
            )
        } else {
            QuranAcademyPalette(
                isDark = false,
                pageBackground = AcademyLightGreenBg,
                // সাদা বা অতিরিক্ত উজ্জ্বল নয় — প্রিমিয়াম ইসলামিক সফট এমেরাল্ড গ্লাস সারফেস
                glassCardTop = Color(0xFFECF6F1),
                glassCardBottom = Color(0xFFE0EFE8),
                glassCardSheen = Color(0xFFF4FBF8).copy(alpha = 0.65f),
                glassCardBorderGold = AcademyGold.copy(alpha = 0.48f),
                glassCardBorderGreen = AcademyPrimaryGreen.copy(alpha = 0.20f),
                secondarySurfaceTop = Color(0xFFEDF6F2),
                secondarySurfaceBottom = Color(0xFFE4F1EB),
                arabicChamberBg = AcademyPrimaryGreen.copy(alpha = 0.10f),
                arabicChamberBorder = AcademyGold.copy(alpha = 0.48f),
                arabicSampleColor = AcademyDeepEmerald,
                titleColor = Color(0xFF0A3D2B),
                subtitleColor = Color(0xFF395D50),
                mutedColor = Color(0xFF5A7A6E),
                navIndicatorBg = AcademyPrimaryGreen.copy(alpha = 0.09f),
                navIndicatorBorder = AcademyGold.copy(alpha = 0.50f),
                navIndicatorIcon = AcademyPrimaryGreen,
                accentGreen = AcademyPrimaryGreen,
                accentGold = AcademyGold,
                dividerColor = Color(0xFFC9E2D7),
                cardShadowColor = AcademyPrimaryGreen.copy(alpha = 0.16f)
            )
        }
    }
}

fun toAcademyBanglaDigits(number: Int): String {
    val digits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return number.toString().map { ch ->
        if (ch in '0'..'9') digits[ch - '0'] else ch
    }.joinToString("")
}

/**
 * সূক্ষ্ম ইসলামিক জ্যামিতিক প্যাটার্ন (৮-কোনা রুব-উল-হিযব ও সূক্ষ্ম ল্যাটিস) — Canvas ভিত্তিক।
 */
fun DrawScope.drawIslamicGeometricPattern(
    lineColor: Color,
    accentColor: Color,
    cellSizeDp: Float = 54f
) {
    val cellPx = cellSizeDp * density
    if (cellPx <= 0f) return
    val stroke = Stroke(width = 0.85f * density)
    val accentStroke = Stroke(width = 0.7f * density)

    val cols = (size.width / cellPx).toInt() + 2
    val rows = (size.height / cellPx).toInt() + 2

    for (r in 0..rows) {
        for (c in 0..cols) {
            val cx = c * cellPx + (if (r % 2 == 1) cellPx * 0.5f else 0f)
            val cy = r * cellPx * 0.86f
            val radius = cellPx * 0.34f

            // সূক্ষ্ম ৮-কোনা ইসলামিক তারা (দুটি ৪৫° ঘোরানো বর্গক্ষেত্র)
            drawRotatedSquare(cx, cy, radius, 0.0, lineColor, stroke)
            drawRotatedSquare(cx, cy, radius, Math.PI / 4.0, accentColor, accentStroke)

            // কেন্দ্রীয় ক্ষুদ্র সোনালি বিন্দু
            drawCircle(
                color = accentColor.copy(alpha = accentColor.alpha * 0.9f),
                radius = 1.4f * density,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawRotatedSquare(
    cx: Float,
    cy: Float,
    radius: Float,
    angleOffsetRad: Double,
    color: Color,
    stroke: Stroke
) {
    val path = Path()
    for (i in 0 until 4) {
        val angle = angleOffsetRad + i * (Math.PI / 2.0)
        val x = cx + (radius * cos(angle)).toFloat()
        val y = cy + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = stroke)
}

/**
 * ২. Royal Academy Header Design (স্ক্রিন উচ্চতার ~১৮-২০%, গাঢ় Emerald Green, সূক্ষ্ম Geometric Pattern ও Gold Accent)
 */
@Composable
fun QuranAcademyRoyalHeader(
    headerHeight: Dp,
    modifier: Modifier = Modifier
) {
    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .shadow(
                elevation = 6.dp,
                shape = headerShape,
                ambientColor = AcademyRoyalEmerald.copy(alpha = 0.35f),
                spotColor = AcademyRoyalEmerald.copy(alpha = 0.35f)
            )
            .clip(headerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AcademyDeepEmerald,
                        AcademyPrimaryGreen,
                        AcademyRoyalEmerald
                    )
                )
            )
            .drawBehind {
                // নরম গোল্ডেন রেডিয়াল গ্লো (ক্যালিগ্রাফির পেছনে)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AcademyGold.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.38f),
                        radius = size.width * 0.62f
                    )
                )

                // সূক্ষ্ম ইসলামিক জ্যামিতিক প্যাটার্ন
                drawIslamicGeometricPattern(
                    lineColor = Color.White.copy(alpha = 0.055f),
                    accentColor = AcademyGold.copy(alpha = 0.085f),
                    cellSizeDp = 48f
                )

                // নিচের প্রান্তে সূক্ষ্ম সোনালি বর্ডার রেখা
                val bottomY = size.height - (1.2f * density)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AcademyGold.copy(alpha = 0.65f),
                            AcademyGoldSheen.copy(alpha = 0.90f),
                            AcademyGold.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.12f, bottomY),
                    end = Offset(size.width * 0.88f, bottomY),
                    strokeWidth = 1.6f * density
                )
            }
            .testTag("quran_academy_header"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // আরবি শিরোনাম: تَعْلِيمُ الْقُرْآنِ (Amiri ফন্ট, হারাকাত অক্ষুণ্ণ)
            Text(
                text = QuranLearningCatalog.HEADER_ARABIC_TITLE,
                fontFamily = QuranAcademyArabicFont,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 38.sp,
                color = AcademyGoldSheen,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag("quran_academy_header_arabic")
            )

            // সূক্ষ্ম সোনালি অলংকরণ বিভাজক
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AcademyGold.copy(alpha = 0.85f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AcademyGoldSheen)
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AcademyGold.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )
            }

            // বাংলা শিরোনাম: কুরআন শিক্ষা
            Text(
                text = QuranLearningCatalog.HEADER_BANGLA_TITLE,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = Color(0xFFF6FBF8),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag("quran_academy_header_bangla")
            )

            Spacer(modifier = Modifier.height(2.dp))

            // ছোট সাবটাইটেল: শিখুন ধাপে ধাপে
            Text(
                text = QuranLearningCatalog.HEADER_SUBTITLE,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Color(0xFFD0ECE1),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag("quran_academy_header_subtitle")
            )
        }
    }
}

/**
 * ৩. প্রধান তিনটি শিক্ষার স্তর কার্ড (প্রাথমিক শিক্ষা, মধ্যবর্তী শিক্ষা, উচ্চ শিক্ষা)
 * - Premium Islamic Glass / Soft Surface Style (সাদা বা অতিরিক্ত উজ্জ্বল নয়)
 * - নিখুঁত আরবি Typography, স্পষ্ট বাংলা শিরোনাম, সংক্ষিপ্ত বিবরণ ও ডান পাশে মার্জিত Navigation Indicator
 * - অপ্রয়োজনীয় আইকন বা অতিরিক্ত লেখা মুক্ত
 * - Touch Feedback (Ripple + Spring Press Scale)
 */
@Composable
fun QuranPrimaryLevelGlassCard(
    level: QuranLearningLevel,
    palette: QuranAcademyPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "level_card_press_scale"
    )

    val cardShape = RoundedCornerShape(16.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            palette.glassCardBorderGold,
            palette.glassCardBorderGreen,
            palette.glassCardBorderGold.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = cardShape,
                ambientColor = palette.cardShadowColor,
                spotColor = palette.cardShadowColor
            )
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.glassCardTop, palette.glassCardBottom)
                )
            )
            .border(width = 1.dp, brush = borderBrush, shape = cardShape)
            .drawBehind {
                // উপরের প্রান্তে সূক্ষ্ম ফ্রস্টেড গ্লাস শীন
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.glassCardSheen, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
                // বাম প্রান্তে সূক্ষ্ম রাজকীয় সোনালি অ্যাকসেন্ট লাইন
                val accentHeight = size.height * 0.46f
                val topY = (size.height - accentHeight) / 2f
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AcademyGold.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startY = topY,
                        endY = topY + accentHeight
                    ),
                    start = Offset(1.8f * density, topY),
                    end = Offset(1.8f * density, topY + accentHeight),
                    strokeWidth = 2.4f * density
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AcademyGold.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .testTag("quran_level_card_${level.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // আরবি নমুনা চেম্বার (সমানুপাতিক ও হারাকাত-অক্ষুণ্ণ)
            Surface(
                modifier = Modifier
                    .width(98.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(13.dp),
                color = palette.arabicChamberBg,
                border = BorderStroke(1.dp, palette.arabicChamberBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.arabicSample,
                        fontFamily = QuranAcademyArabicFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (level == QuranLearningLevel.PRIMARY) 19.sp else 22.sp,
                        lineHeight = 32.sp,
                        color = palette.arabicSampleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.testTag("quran_level_arabic_${level.id}")
                    )
                }
            }

            // বাংলা শিরোনাম ও সংক্ষিপ্ত বিবরণ
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = level.titleBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    color = palette.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("quran_level_title_${level.id}")
                )
                Text(
                    text = level.descriptionBangla,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = palette.subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("quran_level_desc_${level.id}")
                )
            }

            // ডান পাশে মার্জিত Navigation Indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.navIndicatorBg)
                    .border(1.dp, palette.navIndicatorBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "${level.titleBangla} খুলুন",
                    tint = palette.navIndicatorIcon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
