package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.kashida.DocumentLayoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ImageExporter {
    private const val PX_PER_INCH = 150f
    private const val PT_PER_INCH = 72f

    suspend fun exportToPng(
        context: Context, layout: DocumentLayoutEngine.DocumentLayout, title: String, typeface: Typeface,
        fontSizePt: Float, textColor: Int, pageColor: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val scale = PX_PER_INCH / PT_PER_INCH
            val widthPx = (layout.pageWidthPt * scale).roundToInt()
            val heightPx = (layout.pageHeightPt * scale).roundToInt()
            val totalHeightPx = (heightPx.toLong() * layout.pageCount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), totalHeightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint().apply { color = pageColor }
            for (page in layout.pages) {
                val yOffset = page.index * heightPx
                canvas.drawRect(0f, yOffset.toFloat(), widthPx.toFloat(), (yOffset + heightPx).toFloat(), bgPaint)
                val pageStaticLayout = DocumentLayoutEngine.createPageStaticLayout(page, typeface, fontSizePt, com.example.model.TextAlignOption.JUSTIFY)
                canvas.save()
                canvas.translate(page.leftMarginPt * scale, yOffset + page.topMarginPt * scale)
                canvas.scale(scale, scale)
                pageStaticLayout.draw(canvas)
                canvas.restore()
            }
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val imageFile = File(exportsDir, "" + sanitizedTitle + "_" + System.currentTimeMillis() + ".png")
            FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: Exception) { e.printStackTrace(); null }
    }
}