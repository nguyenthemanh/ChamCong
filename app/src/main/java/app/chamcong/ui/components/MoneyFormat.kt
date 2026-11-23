package app.chamcong.ui.components

import java.text.NumberFormat
import java.util.Locale

private val yenFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.JAPAN)

fun Long.formatYen(): String = "${yenFormat.format(this)}円"
