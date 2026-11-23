package app.chamcong.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.chamcong.BuildConfig
import app.chamcong.UpdateChecker
import app.chamcong.UpdateInstaller
import app.chamcong.ui.i18n.AppStrings
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.SheetMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Same pattern as JwwViewer AboutBlock: version, author, check update. */
@Composable
fun AboutUpdateBlock(strings: AppStrings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Text(strings.sectionAbout, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        text = strings.aboutVersion.format(BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    Text(strings.sectionAuthor, style = MaterialTheme.typography.titleMedium)
    Text(
        text = BuildConfig.AUTHOR_NAME,
        style = MaterialTheme.typography.titleLarge,
        color = AccentGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.AUTHOR_URL)),
                    )
                }
            },
    )
    Text(
        text = strings.authorRole,
        style = MaterialTheme.typography.labelLarge,
        color = SheetMuted,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            if (busy) return@Button
            busy = true
            status = strings.updateChecking
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        UpdateChecker.check(BuildConfig.VERSION_NAME)
                    }
                    if (!result.newer) {
                        status = strings.updateLatest.format(result.tagName)
                        busy = false
                        return@launch
                    }
                    val apkUrl = result.apkUrl
                    if (apkUrl.isNullOrBlank()) {
                        status = strings.updateNoApk.format(result.tagName)
                        busy = false
                        return@launch
                    }
                    status = strings.updateFound.format(result.tagName)
                    val file = withContext(Dispatchers.IO) {
                        UpdateChecker.downloadApk(context, apkUrl) { pct ->
                            scope.launch(Dispatchers.Main.immediate) {
                                status = strings.updateDownloading.format(pct)
                            }
                        }
                    }
                    status = strings.updateInstalling
                    UpdateInstaller.installApk(context, file, strings.updateNeedInstallPerm)
                } catch (e: Exception) {
                    val msg = strings.updateFail.format(e.message ?: e.javaClass.simpleName)
                    status = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                } finally {
                    busy = false
                }
            }
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(strings.btnCheckUpdate)
    }
    status?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.labelLarge,
            color = SheetMuted,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
    }
}
