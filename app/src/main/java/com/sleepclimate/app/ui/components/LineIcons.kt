package com.sleepclimate.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleepclimate.app.ui.theme.SC

/** 24×24 그리드의 얇은 선 아이콘 */
object LineIcons {
    val Snowflake = icon("M12 2.5v19 M4 7l16 10 M20 7L4 17 M9.5 3.8L12 5.5l2.5-1.7 M9.5 20.2L12 18.5l2.5 1.7")
    val Flame = icon("M12 3c.8 3.2 5 5.2 5 10.2a5 5 0 0 1-10 0c0-2.6 1.4-4.2 2.6-5.2.2 1.6 1 2.6 2 3.1-.2-3.1 0-5.4.4-8.1z")
    val Cup = icon("M4.5 9.5h11v4.5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z M15.5 11h1.3a2.5 2.5 0 0 1 0 5h-1.6 M8.5 3.5v3 M11.5 3.5v3")
    val Sliders = icon("M4 7h9 M17 7h3 M4 17h3 M11 17h9 M15 5v4 M9 15v4")
    val Moon = icon("M19.5 14.6A8 8 0 1 1 9.4 4.5a6.3 6.3 0 0 0 10.1 10.1z")
    val Battery = icon("M5 7.5h11a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z M21 10.5v3")
    val Parking = icon("M6.5 3.5h11a3 3 0 0 1 3 3v11a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3v-11a3 3 0 0 1 3-3z M10 17V7.5h3a2.7 2.7 0 0 1 0 5.4h-3")
    val Link = icon("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1 M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val Bell = icon("M6.5 16v-5a5.5 5.5 0 0 1 11 0v5l1.5 2h-14z M10 20.5a2 2 0 0 0 4 0")
    val Back = icon("M15 5l-7 7 7 7")
    val Forward = icon("M9 5l7 7-7 7")
    val Warning = icon("M12 3.5l9.5 16.5h-19z M12 10v4.5 M12 17.3v.2")
    val Clock = icon("M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18z M12 7.5V12l3 2")
    val Thermometer = icon("M10 4.5a2 2 0 0 1 4 0v9.3a4 4 0 1 1-4 0z M12 9v7")
    val Paw = icon(
        "M7.5 10.5a1.8 1.8 0 1 1 0-3.6 1.8 1.8 0 0 1 0 3.6z M16.5 10.5a1.8 1.8 0 1 1 0-3.6 1.8 1.8 0 0 1 0 3.6z " +
            "M12 7.5a1.8 1.8 0 1 1 0-3.6 1.8 1.8 0 0 1 0 3.6z M7.5 17.5c0-2.8 2.2-4.8 4.5-4.8s4.5 2 4.5 4.8c0 1.3-1.1 1.9-2.2 1.9-1 0-1.3-.6-2.3-.6s-1.3.6-2.3.6c-1.1 0-2.2-.6-2.2-1.9z"
    )
    val Flask = icon("M9 3.5h6 M10 3.5v5.5l-5 8.7a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3L14 9V3.5 M7.5 15h9")
    val Fan = icon("M12 12c0-4 1-7.5 3.5-7.5S18 7 12 12z M12 12c4 0 7.5 1 7.5 3.5S17 18 12 12z M12 12c0 4-1 7.5-3.5 7.5S6 17 12 12z M12 12c-4 0-7.5-1-7.5-3.5S7 6 12 12z")

    private fun icon(path: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .build()
}

@Composable
fun LineIcon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = SC.TextPrimary, size: Dp = 24.dp) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
