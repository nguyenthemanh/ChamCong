package app.chamcong.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import kotlinx.coroutines.flow.collectLatest

/** How the physical device is held / folded. */
enum class DevicePosture {
    /** Phone / cover screen — single column. */
    Closed,
    /** Unfolded book / wide landscape dual pane. */
    Book,
    /** Unfolded tabletop (horizontal hinge) — top/bottom split. */
    Tabletop,
}

enum class AppWidthClass { Compact, Medium, Expanded }

data class AdaptiveMetrics(
    val widthClass: AppWidthClass,
    val posture: DevicePosture,
    /** True when foldable is open or device is tablet-wide. */
    val isUnfolded: Boolean,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
    /** Hinge gap in Dp for tabletop spacer (0 if none). */
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

        val posture = when {
            folding != null &&
                folding.state == FoldingFeature.State.HALF_OPENED &&
                folding.orientation == FoldingFeature.Orientation.HORIZONTAL ->
                DevicePosture.Tabletop
            folding != null &&
                (
                    folding.state == FoldingFeature.State.FLAT ||
                        folding.state == FoldingFeature.State.HALF_OPENED
                    ) &&
                folding.orientation == FoldingFeature.Orientation.VERTICAL &&
                widthClass != AppWidthClass.Compact ->
                DevicePosture.Book
            // Large tablets / unfolded main screens without reported hinge
            widthClass == AppWidthClass.Expanded -> DevicePosture.Book
            widthClass == AppWidthClass.Medium && maxHeight >= 600.dp -> DevicePosture.Book
            else -> DevicePosture.Closed
        }

        val hingeGap = if (
            posture == DevicePosture.Tabletop &&
            folding != null &&
            folding.occlusionType == FoldingFeature.OcclusionType.FULL
        ) {
            with(density) {
                (folding.bounds.height().toFloat() / density.density).dp.coerceIn(8.dp, 48.dp)
            }
        } else if (posture == DevicePosture.Tabletop) {
            16.dp
        } else {
            0.dp
        }

        // Unfolded = full canvas; closed = readable phone column (not a scaled tablet).
        val maxContent = when (posture) {
            DevicePosture.Closed -> maxWidth
            DevicePosture.Book, DevicePosture.Tabletop -> maxWidth
        }
        val pad = when (posture) {
            DevicePosture.Closed -> 0.dp
            DevicePosture.Book -> 12.dp
            DevicePosture.Tabletop -> 8.dp
        }

        val metrics = AdaptiveMetrics(
            widthClass = widthClass,
            posture = posture,
            isUnfolded = posture != DevicePosture.Closed,
            maxContentWidth = maxContent,
            horizontalPadding = pad,
            hingeGap = hingeGap,
            foldingFeature = folding,
        )

        CompositionLocalProvider(LocalAdaptive provides metrics) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxContent)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .then(
                            if (pad > 0.dp) Modifier
                            else Modifier,
                        ),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun FoldHingeSpacer(modifier: Modifier = Modifier) {
    val gap = LocalAdaptive.current.hingeGap
    if (gap > 0.dp) {
        Spacer(modifier = modifier.height(gap).fillMaxWidth())
    }
}

@Composable
fun BookPaneDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight(),
    )
}
