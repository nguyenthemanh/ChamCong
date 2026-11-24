package app.chamcong.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chamcong.data.AppLanguage
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted

/** One UI–style soft card surface. */
val SettingsCardBg = Color(0xFFFFFFFF)
val SettingsPageBg = Color(0xFFF2F0EB)
private val CardShape = RoundedCornerShape(22.dp)
private val FieldShape = RoundedCornerShape(14.dp)

@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 6.dp, bottom = 6.dp, top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = SheetMuted,
        letterSpacing = 0.2.sp,
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SettingsCardBg)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SheetInk,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = SheetMuted,
                lineHeight = 16.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SheetLine,
                uncheckedBorderColor = SheetLine,
            ),
        )
    }
}

@Composable
fun SettingsFolderRow(
    title: String,
    openTitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 12.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (open) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = if (open) title else openTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SheetInk,
            )
            Icon(
                imageVector = if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = SheetMuted,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    supporting: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = singleLine,
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGreen,
            focusedLabelColor = AccentGreen,
            cursorColor = AccentGreen,
            unfocusedBorderColor = SheetLine,
            unfocusedContainerColor = Color(0xFFFAF8F4),
            focusedContainerColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun SettingsNumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGreen,
            focusedLabelColor = AccentGreen,
            cursorColor = AccentGreen,
            unfocusedBorderColor = SheetLine,
            unfocusedContainerColor = Color(0xFFFAF8F4),
            focusedContainerColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun SettingsLanguageRadios(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    /**
     * When set, show country/region names translated in this UI language.
     * Null = show app-language names (Tiếng Việt, 日本語, …).
     */
    countryLabelIn: AppLanguage? = null,
) {
    Column {
        AppLanguage.entries.forEachIndexed { index, lang ->
            if (index > 0) {
                HorizontalDivider(color = SheetLine.copy(alpha = 0.55f), thickness = 0.5.dp)
            }
            val itemLabel = if (countryLabelIn != null) {
                lang.countryLabel(countryLabelIn)
            } else {
                lang.label
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == lang,
                        onClick = { onSelect(lang) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == lang,
                    onClick = { onSelect(lang) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AccentGreen,
                        unselectedColor = SheetMuted,
                    ),
                )
                Text(
                    text = itemLabel,
                    color = if (selected == lang) AccentGreen else SheetInk,
                    fontWeight = if (selected == lang) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun SettingsCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = SheetLine.copy(alpha = 0.6f),
        thickness = 0.5.dp,
    )
}
