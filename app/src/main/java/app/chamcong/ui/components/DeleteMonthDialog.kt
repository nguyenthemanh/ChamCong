package app.chamcong.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import app.chamcong.data.MonthKey
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.AccentRed

@Composable
fun DeleteMonthDialog(
    month: MonthKey,
    sheet: SheetLabels,
    title: String,
    body: String,
    confirm: String,
    cancel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body.format(sheet.monthLabel(month))) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = AccentRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancel)
            }
        },
    )
}
