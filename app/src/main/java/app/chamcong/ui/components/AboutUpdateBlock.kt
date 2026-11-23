package app.chamcong.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.core.content.FileProvider
import app.chamcong.BuildConfig
import app.chamcong.UpdateChecker
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
                    installUpdateApk(context, file, strings.updateNeedInstallPerm)
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
            style = MaterialTheme.typography.labelMedium,
            color = SheetMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun installUpdateApk(
    context: android.content.Context,
    file: java.io.File,
    needPermMessage: String,
) {
    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
        Toast.makeText(context, needPermMessage, Toast.LENGTH_LONG).show()
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
