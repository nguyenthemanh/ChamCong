package app.chamcong.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppWidthClass { Compact, Medium, Expanded }

data class AdaptiveMetrics(
    val widthClass: AppWidthClass,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
    val isFoldExpanded: Boolean,
)

val LocalAdaptive = compositionLocalOf {
    AdaptiveMetrics(
        widthClass = AppWidthClass.Compact,
        maxContentWidth = 720.dp,
        horizontalPadding = 0.dp,
        isFoldExpanded = false,
    )
}

@Composable
fun AdaptiveShell(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> AppWidthClass.Compact
            maxWidth < 840.dp -> AppWidthClass.Medium
            else -> AppWidthClass.Expanded
        }
        // Fold main screen / tablets: cap readable width, center pane
        val maxContent = when (widthClass) {
            AppWidthClass.Compact -> maxWidth
            AppWidthClass.Medium -> 720.dp
            AppWidthClass.Expanded -> 960.dp
        }
        val pad = when (widthClass) {
            AppWidthClass.Compact -> 0.dp
            AppWidthClass.Medium -> 12.dp
            AppWidthClass.Expanded -> 24.dp
        }
        val metrics = AdaptiveMetrics(
            widthClass = widthClass,
            maxContentWidth = maxContent,
            horizontalPadding = pad,
            isFoldExpanded = widthClass != AppWidthClass.Compact,
        )
        androidx.compose.runtime.CompositionLocalProvider(LocalAdaptive provides metrics) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxContent)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {
                    content()
                }
            }
        }
    }
}
