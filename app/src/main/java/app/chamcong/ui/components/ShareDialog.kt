package app.chamcong.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.chamcong.export.ExportFormat
import app.chamcong.ui.i18n.AppStrings

@Composable
fun ShareDialog(
    strings: AppStrings,
    exporting: Boolean,
    onDismiss: () -> Unit,
    onShare: (ExportFormat) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!exporting) onDismiss() },
        title = { Text(strings.shareAs) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (exporting) {
                    Text(strings.sharing)
                } else {
                    TextButton(
                        onClick = { onShare(ExportFormat.PDF) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                        Text("  ${strings.sharePdf}")
                    }
                    TextButton(
                        onClick = { onShare(ExportFormat.JPG) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Text("  ${strings.shareJpg}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !exporting) {
                Text(strings.cancel)
            }
        },
    )
}
