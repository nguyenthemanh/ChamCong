package app.chamcong.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.chamcong.data.DayRow
import app.chamcong.data.MonthKey
import app.chamcong.data.MonthSummary
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat { PDF, JPG }

object ReportExporter {

    fun export(
        context: Context,
        format: ExportFormat,
        month: MonthKey,
        rows: List<DayRow>,
        summary: MonthSummary,
        settings: PaySettings,
    ): File {
        val bitmap = renderBitmap(month, rows, summary, settings)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val base = "geppo_${month.year}_${month.month.toString().padStart(2, '0')}"
        return when (format) {
            ExportFormat.JPG -> {
                val file = File(dir, "$base.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                // Also keep a copy in Pictures so the image is saved on the device.
                saveJpgToGallery(context, bitmap, "$base.jpg")
                bitmap.recycle()
                file
            }
            ExportFormat.PDF -> {
                val file = File(dir, "$base.pdf")
                writePdf(file, bitmap)
                bitmap.recycle()
                file
            }
        }
    }

    /** Persist JPG under Pictures/ChamCong (visible in Gallery). */
    private fun saveJpgToGallery(context: Context, bitmap: Bitmap, displayName: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChamCong")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val folder = File(pictures, "ChamCong").apply { mkdirs() }
                val outFile = File(folder, displayName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                @Suppress("DEPRECATION")
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, outFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        }
    }

    private fun writePdf(file: File, bitmap: Bitmap) {
        val doc = PdfDocument()
        val pageWidth = 1240
        val pageHeight = 1754
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val scale = minOf(
            pageWidth.toFloat() / bitmap.width,
            pageHeight.toFloat() / bitmap.height,
        )
        val dx = (pageWidth - bitmap.width * scale) / 2f
        val dy = 40f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.restore()
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun renderBitmap(
        month: MonthKey,
        rows: List<DayRow>,
        summary: MonthSummary,
        settings: PaySettings,
    ): Bitmap {
        val sheet = SheetLabelFactory.of(settings.sheetLanguage)
        val width = 1080
        val margin = 48f
        val titleSize = 54f
        val bodySize = 28f
        val headerSize = 26f
        val rowH = 46f
        val headerH = 44f
        val topBlock = 220f
        val footerH = 120f
        val height = (topBlock + headerH + rows.size * rowH + footerH + margin * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFFFFFF.toInt())

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1B19.toInt()
            textSize = bodySize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val inkBold = Paint(ink).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1B19.toInt()
            textSize = titleSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1F4E79.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val muted = Paint(ink).apply {
            color = 0xFF5C574F.toInt()
            textSize = 24f
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF333333.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val fillHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF2F2F2.toInt()
            style = Paint.Style.FILL
        }
        val fillSunday = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD6EAF8.toInt()
            style = Paint.Style.FILL
        }
        val fillCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }

        var y = margin + 20f
        canvas.drawText(settings.resolvedSheetTitle(), width / 2f, y + titleSize, titlePaint)
        y += titleSize + 36f

        canvas.drawText(sheet.monthLabel(month), margin, y + bodySize, inkBold)

        val nameLabel = sheet.namePrefix
        val nameX = width - margin
        val nameWidth = namePaint.measureText(settings.employeeName)
        val labelWidth = ink.measureText(nameLabel)
        canvas.drawText(settings.employeeName, nameX - nameWidth, y + bodySize, namePaint)
        canvas.drawText(nameLabel, nameX - nameWidth - labelWidth, y + bodySize, ink)
        y += bodySize + 8f
        if (settings.employeeNameKana.isNotBlank()) {
            val kanaX = nameX - muted.measureText(settings.employeeNameKana)
            canvas.drawText(settings.employeeNameKana, kanaX, y + 24f, muted)
            y += 30f
        }
        y += 18f

        val showFlag = settings.flagEnabled
        val showOt = settings.overtimeEnabled
        val tableLeft = margin
        val tableRight = width - margin
        val colDay = 70f
        val colWeek = 160f
        // Cột tăng ca sát ngay bên phải cột công trường
        val colOtW = if (showOt) 240f else 0f
        val colDrive = if (showFlag) 140f else 0f
        val colSite = (tableRight - tableLeft) - colDay - colWeek - colOtW - colDrive
        val x0 = tableLeft
        val x1 = x0 + colDay
        val x2 = x1 + colWeek
        val siteRight = x2 + colSite
        val otLeft = siteRight
        val otRight = otLeft + colOtW
        val flagLeft = otRight
        val flagRight = tableRight

        fun drawHLine(yy: Float) {
            canvas.drawLine(tableLeft, yy, tableRight, yy, line)
        }
        fun drawVLines(top: Float, bottom: Float) {
            buildList {
                add(x0); add(x1); add(x2); add(siteRight)
                if (showOt) add(otRight)
                add(tableRight)
            }.distinct().forEach { x ->
                canvas.drawLine(x, top, x, bottom, line)
            }
        }
        fun centerText(text: String, left: Float, right: Float, baseline: Float, paint: Paint) {
            canvas.drawText(text, (left + right) / 2f, baseline, paint)
        }

        val headerTop = y
        val headerBottom = y + headerH
        canvas.drawRect(tableLeft, headerTop, tableRight, headerBottom, fillHeader)
        drawHLine(headerTop)
        drawHLine(headerBottom)
        drawVLines(headerTop, headerBottom)

        val headerPaint = Paint(inkBold).apply {
            textSize = headerSize
            textAlign = Paint.Align.CENTER
        }
        val headerBaseline = headerTop + headerH * 0.68f
        centerText(sheet.colDay, x0, x1, headerBaseline, headerPaint)
        centerText(sheet.colWeekday, x1, x2, headerBaseline, headerPaint)
        centerText(sheet.colSite, x2, siteRight, headerBaseline, headerPaint)
        if (showOt) {
            centerText(sheet.colOt, otLeft, otRight, headerBaseline, headerPaint)
        }
        if (showFlag) {
            centerText(
                settings.flagLabelOff.ifBlank { sheet.colDriving },
                flagLeft,
                flagRight,
                headerBaseline,
                headerPaint,
            )
        }

        y = headerBottom
        val cellPaint = Paint(ink).apply { textSize = bodySize; textAlign = Paint.Align.CENTER }
        val sitePaint = Paint(ink).apply { textSize = bodySize; textAlign = Paint.Align.LEFT }
        val otPaint = Paint(inkBold).apply {
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val drivePaint = Paint(ink).apply { textSize = 24f; textAlign = Paint.Align.CENTER }

        rows.forEach { row ->
            val top = y
            val bottom = y + rowH
            canvas.drawRect(tableLeft, top, tableRight, bottom, if (row.isSunday) fillSunday else fillCell)
            drawHLine(bottom)
            drawVLines(top, bottom)
            val baseline = top + rowH * 0.68f
            centerText(row.day.toString(), x0, x1, baseline, cellPaint)
            centerText(sheet.weekdayFull(row.dayOfWeek), x1, x2, baseline, cellPaint)
            // Tên công trường
            if (row.entry.site.isNotBlank()) {
                canvas.drawText(row.entry.site, x2 + 12f, baseline, sitePaint)
            }
            // Giờ tăng ca — cột ngay cạnh tên công trường (vd. 1 giờ 30 phút)
            if (showOt) {
                val otMins = row.entry.effectiveOvertimeMinutes
                if (otMins > 0 && row.entry.isWorked) {
                    centerText(sheet.formatDuration(otMins), otLeft, otRight, baseline, otPaint)
                }
            }
            if (showFlag && row.entry.hasFlag && row.entry.isWorked) {
                centerText(settings.flagLabelOn, flagLeft, flagRight, baseline, drivePaint)
            }
            y = bottom
        }

        canvas.drawLine(tableLeft, headerTop, tableLeft, y, line)
        canvas.drawLine(tableRight, headerTop, tableRight, y, line)

        // Footer: Tổng ngày công | Tổng giờ tăng ca | (nhãn)
        y += 16f
        val boxH = 64f
        val boxTop = y
        val boxBottom = y + boxH
        val footLabelPaint = Paint(muted).apply {
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        val footValuePaint = Paint(inkBold).apply {
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val daysText = "${summary.workDays}${sheet.workDaysSuffix.trim()}"
        val otDisplay = when {
            !showOt -> ""
            summary.overtimeMinutes > 0 -> sheet.formatDuration(summary.overtimeMinutes)
            else -> when (settings.sheetLanguage) {
                app.chamcong.data.AppLanguage.JA -> "0分"
                app.chamcong.data.AppLanguage.KO -> "0분"
                app.chamcong.data.AppLanguage.ZH_TW -> "0分"
                app.chamcong.data.AppLanguage.VI -> "0 phút"
                app.chamcong.data.AppLanguage.EN -> "0 minutes"
            }
        }

        fun drawLabeledBox(left: Float, right: Float, label: String, value: String) {
            canvas.drawRect(left, boxTop, right, boxBottom, fillCell)
            canvas.drawRect(RectF(left, boxTop, right, boxBottom), line)
            canvas.drawText(label, (left + right) / 2f, boxTop + 22f, footLabelPaint)
            canvas.drawText(value, (left + right) / 2f, boxTop + 50f, footValuePaint)
        }

        // Tổng ngày làm — thẳng cột công trường
        drawLabeledBox(x2, siteRight, sheet.totalWorkDays, daysText)
        // Tổng giờ tăng ca — thẳng cột cạnh công trường
        if (showOt) {
            drawLabeledBox(otLeft, otRight, sheet.totalOt, otDisplay)
            canvas.drawLine(otLeft, boxTop, otLeft, boxBottom, line)
        }
        if (showFlag) {
            drawLabeledBox(
                flagLeft,
                flagRight,
                settings.flagLabelOff.ifBlank { sheet.colDriving },
                summary.flagDays.toString(),
            )
            canvas.drawLine(flagLeft, boxTop, flagLeft, boxBottom, line)
        }

        return bitmap
    }
}
