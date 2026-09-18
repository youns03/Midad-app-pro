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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

object ImageExporter {
    private const val PX_PER_INCH = 150f
    private const val PT_PER_INCH = 72f

    suspend fun exportToPng(
        context: Context, layout: DocumentLayoutEngine.DocumentLayout, title: String, typeface: Typeface,
        fontSizePt: Float, textColor: Int, pageColor: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val generatedFiles = mutableListOf<File>()
        try {
            val scale = PX_PER_INCH / PT_PER_INCH
            val widthPx = (layout.pageWidthPt * scale).roundToInt().coerceAtLeast(1)
            val heightPx = (layout.pageHeightPt * scale).roundToInt().coerceAtLeast(1)
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")

            layout.pages.forEach { page ->
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(pageColor)
                    canvas.scale(scale, scale)
                    DocumentLayoutEngine.drawPage(canvas, page, typeface, fontSizePt, textColor)
                    val pngFile = File(exportsDir, "${sanitizedTitle}_page_${page.index + 1}_${System.currentTimeMillis()}.png")
                    FileOutputStream(pngFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    generatedFiles += pngFile
                } finally {
                    bitmap.recycle()
                }
            }

            val resultFile = if (generatedFiles.size == 1) {
                generatedFiles.single()
            } else {
                val zipFile = File(exportsDir, "${sanitizedTitle}_${System.currentTimeMillis()}.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                    generatedFiles.forEach { file ->
                        zip.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        file.delete()
                    }
                }
                zipFile
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", resultFile)
        } catch (e: Exception) {
            generatedFiles.forEach { it.delete() }
            e.printStackTrace()
            null
        }
    }
}
