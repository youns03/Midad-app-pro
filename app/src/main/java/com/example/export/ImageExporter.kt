package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageExporter {

    private const val PX_PER_MM = 11.811f // ~300 DPI for high-resolution PNG export

    suspend fun exportToPng(
        context: Context,
        text: String,
        title: String,
        typeface: Typeface,
        fontSizePt: Float,
        textColor: Int,
        pageColor: Int,
        textAlign: TextAlignOption,
        margins: PageMargins,
        pageSize: PageSize
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val bmpWidth = (pageSize.widthMm * PX_PER_MM).toInt()
            val bmpHeight = (pageSize.heightMm * PX_PER_MM).toInt()

            val leftMarginPx = margins.leftMm * PX_PER_MM
            val rightMarginPx = margins.rightMm * PX_PER_MM
            val topMarginPx = margins.topMm * PX_PER_MM
            val bottomMarginPx = margins.bottomMm * PX_PER_MM

            val printableWidth = (bmpWidth - (leftMarginPx + rightMarginPx)).coerceAtLeast(200f).toInt()
            val printableHeight = (bmpHeight - (topMarginPx + bottomMarginPx)).coerceAtLeast(200f)

            val textPaint = TextPaint().apply {
                this.typeface = typeface
                this.textSize = fontSizePt * (PX_PER_MM / 2.83465f) // Scale pt to 300 DPI px
                this.color = textColor
                this.isAntiAlias = true
            }

            val layoutAlignment = when (textAlign) {
                TextAlignOption.RIGHT, TextAlignOption.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
                TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
                TextAlignOption.LEFT -> Layout.Alignment.ALIGN_OPPOSITE
            }

            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, textPaint, printableWidth)
                    .setAlignment(layoutAlignment)
                    .setTextDirection(TextDirectionHeuristics.RTL)
                    .setLineSpacing(0f, 1.35f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    text,
                    textPaint,
                    printableWidth,
                    layoutAlignment,
                    1.35f,
                    0f,
                    false
                )
            }

            val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw paper background
            val bgPaint = Paint().apply {
                this.color = pageColor
                this.style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, bmpWidth.toFloat(), bmpHeight.toFloat(), bgPaint)

            // Draw text within margins
            canvas.save()
            canvas.clipRect(
                leftMarginPx,
                topMarginPx,
                leftMarginPx + printableWidth,
                topMarginPx + printableHeight
            )
            canvas.translate(leftMarginPx, topMarginPx)
            staticLayout.draw(canvas)
            canvas.restore()

            val exportsDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val imageFile = File(exportsDir, "${sanitizedTitle}_${System.currentTimeMillis()}.png")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
