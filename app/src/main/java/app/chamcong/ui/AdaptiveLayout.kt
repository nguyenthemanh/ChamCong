package app.chamcong.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import app.chamcong.ui.theme.SheetLine
import kotlinx.coroutines.flow.collectLatest

/**
 * Physical / window posture — drives structurally different UIs
 * (cover vs inner fold), not merely font/scale changes.
 */
enum class DevicePosture {
    /** Cover / closed phone — single column + bottom summary. */
    Closed,
    /** Open book / wide inner — dual pane side-by-side. */
    Book,
    /** Half-open tabletop — top sheet, bottom controls. */
    Tabletop,
}

enum class AppWidthClass { Compact, Medium, Expanded }

data class AdaptiveMetrics(
    val widthClass: AppWidthClass,
    val posture: DevicePosture,
    val isUnfolded: Boolean,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
    val hingeGap: Dp,
    val foldingFeature: FoldingFeature? = null,
)

val LocalAdaptive = compositionLocalOf {
    AdaptiveMetrics(
        widthClass = AppWidthClass.Compact,
        posture = DevicePosture.Closed,
        isUnfolded = false,
        maxContentWidth = 720.dp,
        horizontalPadding = 0.dp,
        hingeGap = 0.dp,
    )
}

@Composable
fun rememberWindowLayoutInfo(): WindowLayoutInfo? {
    val context = LocalContext.current
    val activity = context as? Activity ?: return null
    val info by produceState<WindowLayoutInfo?>(initialValue = null, activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collectLatest { value = it }
    }
    return info
}

@Composable
fun AdaptiveShell(content: @Composable () -> Unit) {
    val layoutInfo = rememberWindowLayoutInfo()
    val folding = layoutInfo
        ?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> AppWidthClass.Compact
            maxWidth < 840.dp -> AppWidthClass.Medium
            else -> AppWidthClass.Expanded
        }

        val separating = folding?.isSeparating == true
        val halfOpen = folding?.state == FoldingFeature.State.HALF_OPENED
        val flat = folding?.state == FoldingFeature.State.FLAT
        val horizontalHinge =
            folding?.orientation == FoldingFeature.Orientation.HORIZONTAL
        val verticalHinge =
            folding?.orientation == FoldingFeature.Orientation.VERTICAL

        // Prefer real hinge posture (Samsung Fold / Pixel Fold / others).
        val posture = when {
            folding != null && halfOpen && horizontalHinge -> DevicePosture.Tabletop
            folding != null && (flat || halfOpen || separating) && verticalHinge &&
                widthClass != AppWidthClass.Compact -> DevicePosture.Book
            folding != null && (flat || separating) && widthClass != AppWidthClass.Compact ->
                DevicePosture.Book
            // Inner screen / tablet without hinge callback: wide canvas ≠ scaled phone.
            widthClass == AppWidthClass.Expanded -> DevicePosture.Book
            widthClass == AppWidthClass.Medium && maxHeight >= 640.dp -> DevicePosture.Book
            else -> DevicePosture.Closed
        }

        val hingeGap = when {
            posture == DevicePosture.Tabletop && folding != null -> {
                val px = folding.bounds.height().coerceAtLeast(1)
                with(density) { px.toDp() }.coerceIn(12.dp, 56.dp)
            }
            posture == DevicePosture.Tabletop -> 20.dp
            else -> 0.dp
        }

        val metrics = AdaptiveMetrics(
            widthClass = widthClass,
            posture = posture,
            isUnfolded = posture != DevicePosture.Closed,
            maxContentWidth = maxWidth,
            horizontalPadding = when (posture) {
                DevicePosture.Closed -> 0.dp
                DevicePosture.Book -> 10.dp
                DevicePosture.Tabletop -> 8.dp
            },
            hingeGap = hingeGap,
            foldingFeature = folding,
        )

        CompositionLocalProvider(LocalAdaptive provides metrics) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                content()
            }
        }
    }
}

@Composable
fun FoldHingeSpacer(modifier: Modifier = Modifier) {
    val gap = LocalAdaptive.current.hingeGap
    if (gap > 0.dp) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .height(gap)
                .background(SheetLine.copy(alpha = 0.35f)),
        )
    }
}

@Composable
fun BookPaneDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(SheetLine),
    )
}
