package app.chamcong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import app.chamcong.ui.AdaptiveShell
import app.chamcong.ui.MonthReportScreen
import app.chamcong.ui.theme.ChamCongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            ChamCongTheme {
                AdaptiveShell {
                    MonthReportScreen()
                }
            }
        }
    }
}
