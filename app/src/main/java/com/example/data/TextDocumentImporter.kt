package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class ImportedDocumentResult(
    val title: String,
    val content: String,
    val fileName: String,
    val characterCount: Int,
    val lineCount: Int
)

sealed class ImportDocumentOutcome {
    data class Success(val document: ImportedDocumentResult) : ImportDocumentOutcome()
    data class Error(val message: String) : ImportDocumentOutcome()
}

object TextDocumentImporter {

    private val SUPPORTED_EXTENSIONS = setOf("txt", "md", "markdown", "text")

    suspend fun importFromUri(context: Context, uri: Uri): ImportDocumentOutcome = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(context, uri) ?: "مستند_مستورد.txt"
            val extension = fileName.substringAfterLast('.', "").lowercase()

            if (extension.isNotBlank() && extension !in SUPPORTED_EXTENSIONS) {
                return@withContext ImportDocumentOutcome.Error(
                    "صيغة الملف غير مدعومة ($extension). يرجى اختيار ملف نصي بصيغة .txt أو .md"
                )
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportDocumentOutcome.Error("تعذر فتح الملف للقراءة.")

            val rawBytes = inputStream.use { it.readBytes() }

            if (rawBytes.isEmpty()) {
                return@withContext ImportDocumentOutcome.Error("الملف المحدد فارغ تماماً.")
            }

            // Decode UTF-8 safely, replacing malformed bytes instead of crashing
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)

            val text = decoder.decode(java.nio.ByteBuffer.wrap(rawBytes)).toString()

            // Remove UTF-8 BOM if present
            val cleanText = if (text.startsWith("\uFEFF")) text.substring(1) else text

            if (cleanText.isBlank()) {
                return@withContext ImportDocumentOutcome.Error("محتوى الملف فارغ أو لا يحتوي على نصوص صالحة.")
            }

            val title = fileName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "مستند مستورد" }

            val lineCount = cleanText.lines().size

            ImportDocumentOutcome.Success(
                ImportedDocumentResult(
                    title = title,
                    content = cleanText,
                    fileName = fileName,
                    characterCount = cleanText.length,
                    lineCount = lineCount
                )
            )
        } catch (e: Exception) {
            ImportDocumentOutcome.Error("حدث خطأ أثناء قراءة الملف: ${e.localizedMessage ?: e.message}")
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fall back to uri path segment
        }
        return name ?: uri.lastPathSegment
    }
}
