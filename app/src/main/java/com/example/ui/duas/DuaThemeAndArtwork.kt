package com.example.ui.duas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HindSiliguriFontFamily

val DuaPrimaryGreen = Color(0xFF0F6B4B)
val DuaDeepEmerald = Color(0xFF094933)
val DuaRoyalEmerald = Color(0xFF063324)
val DuaGold = Color(0xFFD9A82E)
val DuaGoldSheen = Color(0xFFF3D57C)
val DuaLightBackground = Color(0xFFF6FAF8)

val DuaArabicFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

@Immutable
data class DuaPalette(
    val isDark: Boolean,
    val pageBackground: Color,
    val cardSurface: Color,
    val cardSurfaceSoft: Color,
    val cardBorder: Color,
    val goldBorder: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val mutedColor: Color,
    val accentGreen: Color,
    val accentGold: Color,
    val arabicBoxTop: Color,
    val arabicBoxBottom: Color,
    val arabicTextColor: Color,
    val chipBackground: Color,
    val dividerColor: Color,
    val tabInactiveBg: Color
)

@Composable
fun rememberDuaPalette(darkTheme: Boolean = isSystemInDarkTheme()): DuaPalette {
    return remember(darkTheme) {
        if (darkTheme) {
            DuaPalette(
                isDark = true,
                pageBackground = Color(0xFF061B14),
                cardSurface = Color(0xFF0D2A20),
                cardSurfaceSoft = Color(0xFF113528),
                cardBorder = Color(0xFF1E4F3D),
                goldBorder = DuaGold.copy(alpha = 0.45f),
                titleColor = Color(0xFFEFF8F4),
                subtitleColor = Color(0xFFB3CEC4),
                mutedColor = Color(0xFF83A89B),
                accentGreen = Color(0xFF28A377),
                accentGold = DuaGoldSheen,
                arabicBoxTop = Color(0xFF0A2E22),
                arabicBoxBottom = Color(0xFF072219),
                arabicTextColor = Color(0xFFF7E4A6),
                chipBackground = Color(0xFF133B2D),
                dividerColor = Color(0xFF1B4536),
                tabInactiveBg = Color(0xFF0E2E23)
            )
        } else {
            DuaPalette(
                isDark = false,
                pageBackground = DuaLightBackground,
                cardSurface = Color(0xFFFFFFFF),
                cardSurfaceSoft = Color(0xFFEBF5F0),
                cardBorder = Color(0xFFD2E6DC),
                goldBorder = DuaGold.copy(alpha = 0.50f),
                titleColor = Color(0xFF0B3023),
                subtitleColor = Color(0xFF395D4F),
                mutedColor = Color(0xFF5D7B6F),
                accentGreen = DuaPrimaryGreen,
                accentGold = Color(0xFFB8860B),
                arabicBoxTop = Color(0xFF0C583E),
                arabicBoxBottom = Color(0xFF073A28),
                arabicTextColor = Color(0xFFFFF4CE),
                chipBackground = Color(0xFFE6F3EC),
                dividerColor = Color(0xFFD9EAE1),
                tabInactiveBg = Color(0xFFE9F4EE)
            )
        }
    }
}

fun toDuaBanglaDigits(number: Int): String {
    val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return number.toString().map { ch ->
        if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
    }.joinToString("")
}

data class DuaCategoryVisualStyle(
    val icon: ImageVector,
    val tintLight: Color,
    val tintDark: Color,
    val haloLight: Color,
    val haloDark: Color
)

fun categoryVisualStyle(category: DuaCategory): DuaCategoryVisualStyle = when (category) {
    DuaCategory.DAILY_DUA -> DuaCategoryVisualStyle(
        icon = Icons.Default.Bedtime,
        tintLight = Color(0xFF1565C0),
        tintDark = Color(0xFF90CAF9),
        haloLight = Color(0xFFE3F2FD),
        haloDark = Color(0xFF142E47)
    )
    DuaCategory.MORNING_EVENING_AZKAR -> DuaCategoryVisualStyle(
        icon = Icons.Default.WbTwilight,
        tintLight = Color(0xFFD97706),
        tintDark = Color(0xFFFCD34D),
        haloLight = Color(0xFFFFF7E0),
        haloDark = Color(0xFF3A2C0E)
    )
    DuaCategory.SALAH_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.Mosque,
        tintLight = DuaPrimaryGreen,
        tintDark = Color(0xFF6EE7B7),
        haloLight = Color(0xFFE4F5ED),
        haloDark = Color(0xFF103A2B)
    )
    DuaCategory.QURANIC_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        tintLight = Color(0xFF0B6E4F),
        tintDark = Color(0xFF86EFAC),
        haloLight = Color(0xFFE5F6EE),
        haloDark = Color(0xFF113B2B)
    )
    DuaCategory.PROPHETS_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.AccountBalance,
        tintLight = Color(0xFFB45309),
        tintDark = DuaGoldSheen,
        haloLight = Color(0xFFFEF3DC),
        haloDark = Color(0xFF36280E)
    )
    DuaCategory.HOME_FAMILY_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.Home,
        tintLight = Color(0xFF2E7D32),
        tintDark = Color(0xFFA5D6A7),
        haloLight = Color(0xFFE8F5E9),
        haloDark = Color(0xFF16381B)
    )
    DuaCategory.TRAVEL_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.FlightTakeoff,
        tintLight = Color(0xFF0277BD),
        tintDark = Color(0xFF81D4FA),
        haloLight = Color(0xFFE1F5FE),
        haloDark = Color(0xFF10344A)
    )
    DuaCategory.DISTRESS_ILLNESS_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.Healing,
        tintLight = Color(0xFFC62828),
        tintDark = Color(0xFFEF9A9A),
        haloLight = Color(0xFFFFEBEE),
        haloDark = Color(0xFF3E181B)
    )
    DuaCategory.FORGIVENESS_TAWBAH_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.VolunteerActivism,
        tintLight = Color(0xFF0F6B4B),
        tintDark = Color(0xFF6EE7B7),
        haloLight = Color(0xFFE3F4EC),
        haloDark = Color(0xFF123B2C)
    )
    DuaCategory.RIZQ_BARAKAH_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.Savings,
        tintLight = Color(0xFFB7791F),
        tintDark = DuaGoldSheen,
        haloLight = Color(0xFFFFF8E1),
        haloDark = Color(0xFF382A0E)
    )
    DuaCategory.MASJID_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.AutoAwesome,
        tintLight = Color(0xFF0E6251),
        tintDark = Color(0xFF76D7C4),
        haloLight = Color(0xFFE4F4F0),
        haloDark = Color(0xFF0F3830)
    )
    DuaCategory.MISC_MASNUN_DUAS -> DuaCategoryVisualStyle(
        icon = Icons.Default.LocalFlorist,
        tintLight = Color(0xFFAD1457),
        tintDark = Color(0xFFF48FB1),
        haloLight = Color(0xFFFCE4EC),
        haloDark = Color(0xFF3B1526)
    )
}

/**
 * মাঝারি উচ্চতার প্রিমিয়াম ইসলামিক হেডার ("দোয়া ও যিকির")।
 * গাঢ় এমেরাল্ড ব্যাকগ্রাউন্ড, সূক্ষ্ম মসজিদ সিলুয়েট, স্বর্ণালী ফানুস ও আরবি অলংকরণ।
 */
@Composable
fun DuaSectionHeader(
    totalDuasCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DuaRoyalEmerald, DuaDeepEmerald, DuaPrimaryGreen)
                )
            )
            .testTag("dua_main_header")
    ) {
        // সূক্ষ্ম ইসলামিক জ্যামিতিক প্যাটার্ন, মসজিদ সিলুয়েট ও সোনালি ফানুসের ছোঁয়া
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // সূক্ষ্ম ডায়মন্ড জালি প্যাটার্ন
            val step = 36f
            var x = 0f
            while (x < w) {
                var y = 0f
                while (y < h) {
                    drawCircle(
                        color = DuaGold.copy(alpha = 0.06f),
                        radius = 12f,
                        center = Offset(x, y),
                        style = Stroke(width = 1f)
                    )
                    y += step
                }
                x += step
            }

            // ডানদিকে সূক্ষ্ম মসজিদ গম্বুজ ও মিনার সিলুয়েট
            val domeCenterX = w * 0.84f
            val baseY = h
            val domeColor = Color(0xFF16835D).copy(alpha = 0.34f)
            drawArc(
                color = domeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(domeCenterX - 52f, baseY - 64f),
                size = Size(104f, 92f)
            )
            // বাম ও ডান ছোট মিনার
            drawRect(
                color = domeColor,
                topLeft = Offset(domeCenterX - 72f, baseY - 74f),
                size = Size(12f, 74f)
            )
            drawRect(
                color = domeColor,
                topLeft = Offset(domeCenterX + 60f, baseY - 74f),
                size = Size(12f, 74f)
            )

            // ডান কোণায় ঝুলন্ত সোনালি ফানুসের সূক্ষ্ম আভা
            val lanternX = w * 0.90f
            drawLine(
                color = DuaGoldSheen.copy(alpha = 0.55f),
                start = Offset(lanternX, 0f),
                end = Offset(lanternX, 26f),
                strokeWidth = 2f
            )
            drawCircle(
                color = DuaGoldSheen.copy(alpha = 0.16f),
                radius = 22f,
                center = Offset(lanternX, 36f)
            )
            drawOval(
                color = DuaGold.copy(alpha = 0.80f),
                topLeft = Offset(lanternX - 7f, 26f),
                size = Size(14f, 20f)
            )

            // নিচে সূক্ষ্ম সোনালি বর্ডার আর্চ
            val trimPath = Path().apply {
                moveTo(0f, h - 2f)
                lineTo(w * 0.44f, h - 2f)
                quadraticTo(w * 0.50f, h - 10f, w * 0.56f, h - 2f)
                lineTo(w, h - 2f)
            }
            drawPath(
                path = trimPath,
                color = DuaGold.copy(alpha = 0.75f),
                style = Stroke(width = 2.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "দোয়া ও যিকির",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("dua_header_title")
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DuaGold.copy(alpha = 0.22f))
                        .border(0.8.dp, DuaGoldSheen.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${toDuaBanglaDigits(totalDuasCount)}টি যাচাইকৃত",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = DuaGoldSheen
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "প্রতিদিনের জীবনে প্রয়োজনীয় মাসনুন দোয়া ও যিকির।",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = Color(0xFFDDF3EA),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .testTag("dua_header_subtitle")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "الدُّعَاءُ وَالْأَذْكَارُ الْمَسْنُونَةُ",
                fontFamily = DuaArabicFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DuaGoldSheen.copy(alpha = 0.92f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun DuaCategoryIconBadge(
    category: DuaCategory,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val style = categoryVisualStyle(category)
    val haloColor = if (isDark) style.haloDark else style.haloLight
    val iconColor = if (isDark) style.tintDark else style.tintLight
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(haloColor)
            .border(1.dp, iconColor.copy(alpha = 0.24f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = category.titleBangla,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
