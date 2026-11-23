package app.chamcong.ui.components

import java.text.NumberFormat
import java.util.Locale

private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.JAPAN)

fun Long.formatMoney(suffix: String): String = "${numberFormat.format(this)}$suffix"

/** @deprecated Prefer [formatMoney] with region currency. */
fun Long.formatYen(): String = formatMoney("円")
