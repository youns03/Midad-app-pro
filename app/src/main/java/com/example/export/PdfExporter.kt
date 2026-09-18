package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.kashida.DocumentLayoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    suspend fun exportToPdf(
        context: Context,
        layout: DocumentLayoutEngine.DocumentLayout,
        title: String,
        typeface: Typeface,
        fontSizePt: Float,
        textColor: Int,
        pageColor: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            val bgPaint = Paint().apply { color = pageColor; style = Paint.Style.FILL }
            for (pageLayout in layout.pages) {
                val info = PdfDocument.PageInfo.Builder(
                    pageLayout.widthPt.toInt(), pageLayout.heightPt.toInt(), pageLayout.index + 1
                ).create()
                val page = pdfDocument.startPage(info)
                val canvas = page.canvas
                canvas.drawRect(0f, 0f, pageLayout.widthPt, pageLayout.heightPt, bgPaint)
                DocumentLayoutEngine.drawPage(canvas, pageLayout, typeface, fontSizePt, textColor)
                pdfDocument.finishPage(page)
            }
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val pdfFile = File(exportsDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        } catch (e: Exception) { e.printStackTrace(); null } finally { pdfDocument.close() }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
