package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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

object PdfExporter {

    private const val MM_TO_PT = 72f / 25.4f // 1 mm = 2.83465 points in PDF

    suspend fun exportToPdf(
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
        val pdfDocument = PdfDocument()
        try {
            val pageWidth = pageSize.widthPt.toInt()
            val pageHeight = pageSize.heightPt.toInt()

            val leftMarginPt = margins.leftMm * MM_TO_PT
            val rightMarginPt = margins.rightMm * MM_TO_PT
            val topMarginPt = margins.topMm * MM_TO_PT
            val bottomMarginPt = margins.bottomMm * MM_TO_PT

            val printableWidth = (pageWidth - (leftMarginPt + rightMarginPt)).coerceAtLeast(100f).toInt()
            val printableHeight = (pageHeight - (topMarginPt + bottomMarginPt)).coerceAtLeast(100f)

            val textPaint = TextPaint().apply {
                this.typeface = typeface
                this.textSize = fontSizePt
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

            val totalHeight = staticLayout.height.toFloat()
            val numPages = ((totalHeight / printableHeight).toInt() + 1).coerceAtLeast(1)

            val bgPaint = Paint().apply {
                this.color = pageColor
                this.style = Paint.Style.FILL
            }

            val footerPaint = TextPaint().apply {
                this.typeface = Typeface.DEFAULT
                this.textSize = 9f
                this.color = AndroidColor.argb(120, 100, 100, 100)
                this.isAntiAlias = true
                this.textAlign = Paint.Align.CENTER
            }

            for (pageIndex in 0 until numPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw background color
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // Translate to content area
                canvas.save()
                canvas.clipRect(
                    leftMarginPt,
                    topMarginPt,
                    leftMarginPt + printableWidth,
                    topMarginPt + printableHeight
                )
                canvas.translate(
                    leftMarginPt,
                    topMarginPt - (pageIndex * printableHeight)
                )

                staticLayout.draw(canvas)
                canvas.restore()

                // Draw page number in footer
                if (numPages > 1) {
                    val footerY = pageHeight - (bottomMarginPt / 2f)
                    canvas.drawText(
                        "${pageIndex + 1} / $numPages",
                        pageWidth / 2f,
                        footerY,
                        footerPaint
                    )
                }

                pdfDocument.finishPage(page)
            }

            val exportsDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val pdfFile = File(exportsDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
