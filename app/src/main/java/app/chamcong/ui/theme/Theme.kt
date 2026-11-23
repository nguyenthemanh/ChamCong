package app.chamcong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = AccentGreen,
    onPrimary = Color.White,
    secondary = SummaryBar,
    onSecondary = Color.White,
    background = SheetPaper,
    onBackground = SheetInk,
    surface = SheetPaper,
    onSurface = SheetInk,
    surfaceVariant = SheetHeader,
    onSurfaceVariant = SheetMuted,
    outline = SheetLine,
    error = AccentRed,
)

@Composable
fun ChamCongTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = ChamCongTypography,
        content = content,
    )
}
